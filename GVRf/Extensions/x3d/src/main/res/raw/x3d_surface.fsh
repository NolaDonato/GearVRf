
layout(set = 0, binding = 2) uniform sampler2D diffuseTexture;

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
        return color1 * color2;
    }
    if (blendop == BLEND_ADD)
    {
        color = color1 + color2;
        return clamp(color, 0.0, 1.0);
    }
    if (blendop == BLEND_SUBTRACT)
    {
        color = vec4((color1 - color2).rgb, 1.0);
        return clamp(color, 0.0, 1.0);
    }
    if (blendop == BLEND_DIVIDE)
    {
        return color1 / color2;
    }
    if (blendop == BLEND_SMOOTH_ADD)
    {
        color = (color1 + color2) - (color1 * color2);
        return clamp(color, 0.0, 1.0);
    }
    if (blendop == BLEND_SIGNED_ADD)
    {
        color =  color1 + (color2 - 0.5);
        return clamp(color, 0.0, 1.0);
    }
    return color1;
}

Surface @ShaderName()
{
	vec4 diffuse = diffuse_color;
	vec4 emission = emissive_color;
	vec4 specular = specular_color;
	vec4 ambient = ambient_color;

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
	viewspaceNormal = texture(normalTexture, normal_coord.xy).xyz * 2.0 - 1.0;
#else
	viewspaceNormal = viewspace_normal;
#endif

#ifdef HAS_lightMapTexture
	vec2 lcoord = (lightmap_coord * u_lightMap_scale) + u_lightMap_offset;
	diffuse *= texture(lightMapTexture, vec2(lcoord.x, 1 - lcoord.y));
	#ifdef HAS_lightMapTexture1
		lcoord = (lightmap_coord1 * u_lightMap_scale) + u_lightMap_offset;
    	diffuse = BlendColors(diffuse, texture(lightMapTexture1, vec2(lcoord.x, 1 - lcoord.y), lightMapTexture1_blendop);
    #endif
	return Surface(viewspaceNormal, ambient, vec4(0.0, 0.0, 0.0, 0.0), specular, emission);
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
    return s.emission;
}
#endif