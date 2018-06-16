
layout(set = 0, binding = 10) uniform sampler2D diffuseTexture;

layout(location = 10) in vec2 tex_coord0;
layout(location = 11) in vec2 tex_coord1;
layout(location = 12) in vec2 tex_coord2;
layout(location = 13) in vec2 tex_coord3;

#ifdef HAS_ambientTexture
layout(set = 0, binding = 11) uniform sampler2D ambientTexture;
#endif

#ifdef HAS_specularTexture
layout(set = 0, binding = 12) uniform sampler2D specularTexture;
#endif

#ifdef HAS_emissiveTexture
layout(set = 0, binding = 13) uniform sampler2D emissiveTexture;
#endif

#ifdef HAS_lightmapTexture
layout(set = 0, binding = 14) uniform sampler2D lightmapTexture;
#endif

#ifdef HAS_opacityTexture
layout(set = 0, binding = 15) uniform sampler2D opacityTexture;
#endif

#ifdef HAS_normalTexture
#ifdef HAS_a_tangent
layout(location = 7) in mat3 tangent_matrix;
#endif
layout(set = 0, binding = 16) uniform sampler2D normalTexture;

mat3 calculateTangentMatrix()
{
#ifdef HAS_a_tangent
    return tangent_matrix;
#else
    vec3 pos_dx = dFdx(viewspace_position);
    vec3 pos_dy = dFdy(viewspace_position);
    vec3 tex_dx = dFdx(vec3(normal_coord0, 0.0));
    vec3 tex_dy = dFdy(vec3(normal_coord0, 0.0));

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
	ambient *= texture(ambientTexture, ambient_coord0.xy);
#endif
#ifdef HAS_diffuseTexture
	diffuse *= texture(diffuseTexture, diffuse_coord0.xy);
#endif
#ifdef HAS_opacityTexture
	diffuse.w *= texture(opacityTexture, opacity_coord0.xy).a;
#endif
    diffuse.xyz *= diffuse.w;
#ifdef HAS_specularTexture
	specular *= texture(specularTexture, specular_coord0.xy);
#endif
#ifdef HAS_emissiveTexture
	emission = texture(emissiveTexture, emissive_coord0.xy);
#endif
#ifdef HAS_normalTexture
    mat3 tbn = calculateTangentMatrix();
	viewspaceNormal = normalize(texture(normalTexture, normal_coord0.xy).xyz * 2.0 - 1.0);
	viewspaceNormal = normalize(tbn * viewspaceNormal);
#else
	viewspaceNormal = viewspace_normal;
#endif
#ifdef HAS_lightmapTexture
	vec2 lcoord = (lightmap_coord0 * u_lightmap_scale) + u_lightmap_offset;
	diffuse *= texture(lightmapTexture, vec2(lcoord.x, 1 - lcoord.y));
	return Surface(viewspaceNormal, ambient, vec4(0), specular, emission);
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
    vec3 c = s.ambient.rgb * total_light.ambient_color +
             s.diffuse.rgb * total_light.diffuse_color +
             s.specular.rgb * total_light.specular_color +
             s.emission.rgb;
    return vec4(clamp(c, vec3(0), vec3(1)), s.diffuse.a);
}

#else
vec4 PixelColor(Surface s)
{
    return s.diffuse;
}
#endif



