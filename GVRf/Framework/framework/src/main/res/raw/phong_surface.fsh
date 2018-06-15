@MATERIAL_UNIFORMS

layout(set = 0, binding = 10) uniform sampler2D diffuseTexture;

#ifdef HAS_ambientTexture
layout(location = 11) in vec2 ambient_coord;
layout(set = 0, binding = 11) uniform sampler2D ambientTexture;
#endif

#ifdef HAS_specularTexture
layout(location = 12) in vec2 specular_coord;
layout(set = 0, binding = 12) uniform sampler2D specularTexture;
#endif

#ifdef HAS_emissiveTexture
layout(location = 13) in vec2 emissive_coord;
layout(set = 0, binding = 13) uniform sampler2D emissiveTexture;
#endif

#ifdef HAS_lightmapTexture
layout(location = 14) in vec2 lightmap_coord;
layout(set = 0, binding = 14) uniform sampler2D lightmapTexture;
#endif

#ifdef HAS_opacityTexture
layout(location = 15) in vec2 opacity_coord;
layout(set = 0, binding = 15) uniform sampler2D opacityTexture;
#endif

#ifdef HAS_normalTexture
#ifdef HAS_a_tangent
layout(location = 7) in mat3 tangent_matrix;
#endif

layout(location = 16) in vec2 normal_coord;
layout(set = 0, binding = 16) uniform sampler2D normalTexture;

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
	vec2 lmap_coord = (lightmap_coord * lightmap_scale) + lightmap_offset;
	diffuse *= texture(lightmapTexture, vec2(lmap_coord.x, 1 - lmap_coord.y);
	return Surface(viewspaceNormal, ambient, vec4(0.0, 0.0, 0.0, 0.0), specular, diffuse);
#else
	return Surface(viewspaceNormal, ambient, diffuse, specular, emission);
#endif
}

#ifdef HAS_LIGHTSOURCES

void LightPixel(Surface);

Reflected total_light;

vec4 PixelColor(Surface s)
{
    total_light.ambient_color = vertex_light_ambient;
    total_light.diffuse_color = vertex_light_diffuse;
    total_light.specular_color = vertex_light_specular;

    LightPixel(s);  // Surface s not used in LightPixel
    vec3 c = s.ambient.xyz * total_light.ambient_color +
             s.diffuse.xyz * total_light.diffuse_color +
             s.specular.xyz * total_light.specular_color +
             s.emission.xyz;
    return vec4(clamp(c, vec3(0), vec3(1)), s.diffuse.w);
}

#else
vec4 PixelColor(Surface s)
{
    return s.diffuse;
}
#endif

