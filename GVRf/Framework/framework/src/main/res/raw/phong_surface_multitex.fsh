
@MATERIAL_UNIFORMS

#ifdef HAS_ambientTexture
layout(location = 5) in vec2 ambient_coord;
#endif

#ifdef HAS_specularTexture
layout(location = 6) in vec2 specular_coord;
#endif

#ifdef HAS_emissiveTexture
layout(location = 7) in vec2 emissive_coord;
#endif

#ifdef HAS_lightmapTexture
layout(location = 8) in vec2 lightmap_coord;
#endif

#ifdef HAS_opacityTexture
layout(location = 9) in vec2 opacity_coord;
#endif

#ifdef HAS_normalTexture
layout(location = 10) in vec2 normal_coord;

#ifdef HAS_a_tangent
layout(location = 16) in mat3 tangent_matrix;
#endif
#endif

layout(set = 0, binding = 5) uniform sampler2D diffuseTexture;
layout(set = 0, binding = 6) uniform sampler2D ambientTexture;
layout(set = 0, binding = 7) uniform sampler2D specularTexture;
layout(set = 0, binding = 8) uniform sampler2D lightmapTexture;
layout(set = 0, binding = 9) uniform sampler2D opacityTexture;
layout(set = 0, binding = 10) uniform sampler2D normalTexture;
layout(set = 0, binding = 11) uniform sampler2D emissiveTexture;


struct Surface
{
   vec3 viewspaceNormal;
   vec4 ambient;
   vec4 diffuse;
   vec4 specular;
   vec4 emission;
};

#ifdef HAS_normalTexture
mat3 calculateTangentMatrix()
{
#ifdef HAS_a_tangent
    return tangent_matrix;
#else
    vec3 pos_dx = dFdx(viewspace_position);
    vec3 pos_dy = dFdy(viewspace_position);
    vec3 tex_dx = dFdx(vec3(normal_coord, 0.0));
    vec3 tex_dy = dFdy(vec3(normal_coord, 0.0));

    vec3 dp2perp = cross(pos_dy, viewspace_normal);
    vec3 dp1perp = cross(viewspace_normal, pos_dx);
    vec3 t = dp2perp * tex_dx.x + dp1perp * tex_dy.x;
    vec3 b = dp2perp * tex_dx.y + dp1perp * tex_dy.y;
    float invmax = inversesqrt(max(dot(t, t), dot(b, b)));
    return mat3(t * invmax, b * invmax, viewspace_normal);
#endif
}
#endif

Surface @ShaderName()
{
	vec4 diffuse = diffuse_color;
	vec4 emission = emissive_color;
	vec4 specular = specular_color;
	vec4 ambient = ambient_color;
	vec3 viewspaceNormal;
	vec4 temp;

#ifdef HAS_ambientTexture
	ambient *= texture(ambientTexture, ambient_coord.xy);
#endif
#ifdef HAS_diffuseTexture
	diffuse *= texture(diffuseTexture, diffuse_coord.xy);
#endif
#ifdef HAS_opacityTexture
	diffuse.w *= texture(opacityTexture, opacity_coord.xy).a;
#endif
diffuse.xyz *= diffuse.w;
#ifdef HAS_specularTexture
	specular *= texture(specularTexture, specular_coord.xy);
#endif
#ifdef HAS_emissiveTexture
	emission = texture(emissiveTexture, emissive_coord.xy);
#endif
#ifdef HAS_normalTexture
    mat3 tbn = calculateTangentMatrix();
	viewspaceNormal = normalize(texture(normalTexture, normal_coord.xy).xyz * 2.0 - 1.0);
	viewspaceNormal = normalize(tbn * viewspaceNormal);
#else
    viewspaceNormal = viewspace_normal;
#endif

#ifdef HAS_lightmapTexture
	vec2 lcoord = (lightmap_coord * u_lightmap_scale) + u_lightmap_offset;
	diffuse *= texture(lightmapTexture, vec2(lcoord.x, 1 - lcoord.y));
	return Surface(viewspaceNormal, ambient, vec4(0.0, 0.0, 0.0, 0.0), specular, emission);
#else
	return Surface(viewspaceNormal, ambient, diffuse, specular, emission);
#endif
}
