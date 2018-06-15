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


#ifdef HAS_diffuseTexture1
layout(location = 17) in vec2 diffuse_coord1;
layout(set = 0, binding = 17) uniform sampler2D diffuseTexture1;
#endif

#ifdef HAS_ambientTexture1
layout(location = 18) in vec2 ambient_coord1;
layout(set = 0, binding = 18) uniform sampler2D ambientTexture1;
#endif

#ifdef HAS_specularTexture1
layout(location = 19) in vec2 specular_coord1;
layout(set = 0, binding = 19) uniform sampler2D specularTexture1;
#endif

#ifdef HAS_lightmapTexture1
layout(location = 20) in vec2 lightmap_coord1;
layout(set = 0, binding = 20) uniform sampler2D lightmapTexture1;
#endif

#ifdef HAS_emissiveTexture1
layout(location = 21) in vec2 emissive_coord1;
layout(set = 0, binding = 21) uniform sampler2D emissiveTexture1;
#endif

#define BLEND_MULTIPLY 0
#define BLEND_ADD 1
#define BLEND_SUBTRACT 2
#define BLEND_DIVIDE 3
#define BLEND_SMOOTH_ADD 4
#define BLEND_SIGNED_ADD 5

//
// Blends two input colors based on a blend operation.
//
vec4 BlendColors(vec4 color1, vec4 color2, int blendop)
{
    vec4 color;

    if (blendop == BLEND_MULTIPLY)
    {
        return vec4(color1.rgb * color2.rgb, color1.a);
    }
    if (blendop == BLEND_ADD)
    {
        color = vec4(color1.rgb + color2.rgb, color1.a);
        return clamp(color, 0.0, 1.0);
    }
    if (blendop == BLEND_SUBTRACT)
    {
        color = vec4(color1.rgb - color2.rgb, color1.a);
        return clamp(color, 0.0, 1.0);
    }
    if (blendop == BLEND_DIVIDE)
    {
        return vec4(color1.rgb / color2.rgb, color1.a);
    }
    if (blendop == BLEND_SMOOTH_ADD)
    {
        color = vec4((color1.rgb + color2.rgb) - (color1.rgb * color2.rgb), color1.a);
        return clamp(color, 0.0, 1.0);
    }
    if (blendop == BLEND_SIGNED_ADD)
    {
        color =  vec4(color1.rgb + (color2.rgb - 0.5), color1.a);
        return clamp(color, 0.0, 1.0);
    }
    return color;
}

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
#ifdef HAS_ambientTexture1_blendop
    temp = texture(ambientTexture1, ambient_coord1.xy);
	ambient = BlendColors(ambient, temp, ambientTexture1_blendop);
#endif

#ifdef HAS_diffuseTexture
	diffuse *= texture(diffuseTexture, diffuse_coord.xy);
#endif
#ifdef HAS_diffuseTexture1_blendop
    temp = texture(diffuseTexture1, diffuse_coord1.xy);
	diffuse = BlendColors(diffuse, temp, diffuseTexture1_blendop);
#endif

#ifdef HAS_opacityTexture
	diffuse.a *= texture(opacityTexture, opacity_coord.xy).a;
#endif
    diffuse.xyz *= diffuse.a;

#ifdef HAS_specularTexture
	specular *= texture(specularTexture, specular_coord.xy);
#endif
#ifdef HAS_specularTexture1_blendop
    temp = texture(specularTexture1, specular_coord1.xy);
	specular = BlendColors(specular, temp, specularTexture1_blendop);
#endif

#ifdef HAS_emissiveTexture
	emission = texture(emissiveTexture, emissive_coord.xy);
#endif
#ifdef HAS_emissiveTexture1_blendop
    temp = texture(emissiveTexture1, emissive_coord1.xy);
    emission = BlendColors(emission, temp, emissiveTexture1_blendop);
#endif

#ifdef HAS_normalTexture
    mat3 tbn = calculateTangentMatrix();
	viewspaceNormal = normalize(texture(normalTexture, normal_coord.xy).xyz * 2.0 - 1.0);
	viewspaceNormal = normalize(tbn * viewspaceNormal);
#else
	viewspaceNormal = viewspace_normal;
#endif

#ifdef HAS_lightmapTexture
	vec2 lcoord = (lightmap_coord * lightmap_scale) + lightmap_offset;
	diffuse *= texture(lightmapTexture, vec2(lcoord.x, 1 - lcoord.y));
	#ifdef HAS_lightmapTexture1
		lcoord = (lightmap_coord1 * lightmap_scale) + lightmap_offset;
    	diffuse = BlendColors(diffuse, texture(lightmapTexture1, vec2(lcoord.x, 1 - lcoord.y), lightmapTexture1_blendop);
    #endif
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

