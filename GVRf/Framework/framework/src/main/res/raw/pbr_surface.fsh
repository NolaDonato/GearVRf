
layout(location = 10) in vec2 tex_coord0;
layout(location = 11) in vec2 tex_coord1;
layout(location = 12) in vec2 tex_coord2;
layout(location = 13) in vec2 tex_coord3;

layout(set = 0, binding = 10) uniform sampler2D diffuseTexture;

#ifdef HAS_metallicRoughnessTexture
layout(set = 0, binding = 11) uniform sampler2D metallicRoughnessTexture;
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

#ifdef HAS_brdfLUTTexture
layout(set = 0, binding = 15) uniform sampler2D brdfLUTTexture;
#endif

#ifdef HAS_normalTexture
layout(set = 0, binding = 16) uniform sampler2D normalTexture;
#ifdef HAS_a_tangent
layout(location = 7) in mat3 tangent_matrix;
#endif
#endif

#ifdef HAS_diffuseEnvTex
layout(set = 0, binding = 17) uniform samplerCube diffuseEnvTex;
#endif

#ifdef HAS_specularEnvTexture
layout(set = 0, binding = 18) uniform samplerCube specularEnvTexture;
#endif

vec3 SRGBtoLINEAR(vec3 srgbIn)
{
    //fast srgb approximation
    return pow(srgbIn.xyz, vec3(2.2));
}

#ifdef HAS_normalTexture
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
    float perceptualRoughness;
    vec3 diffuse;
    vec3 specular;
    vec3 emission = emissive_color.xyz;
    float ao = 1.0;

    //specular glossiness workflow
#ifdef HAS_glossinessFactor
    diffuse = diffuse_color.rgb;
    specular = specular_color.rgb;
    float glossiness = glossinessFactor;

#ifdef HAS_specularTexture
    specular.rgb *= SRGBtoLINEAR(texture(specularTexture, specular_coord0.xy).rgb);
    glossiness *= texture(specularTexture, specular_coord0.xy).a;
#endif

#ifdef HAS_diffuseTexture
     diffuse.rgb *= SRGBtoLINEAR(texture(diffuseTexture, diffuse_coord0.xy).rgb);
#endif
     perceptualRoughness = 1.0f - glossiness;

#else // HAS_glossinessFactor
    //metallic roughness workflow
    vec4 basecolor = diffuse_color;
    float metal = metallic;
    perceptualRoughness = roughness;
#ifdef HAS_metallicRoughnessTexture
    // Roughness is stored in the 'g' channel, metallic is stored in the 'b' channel.
    // This layout intentionally reserves the 'r' channel for (optional) occlusion map data
    vec4 mrSample = texture(metallicRoughnessTexture, metallicRoughness_coord0.xy);
    perceptualRoughness = mrSample.g * perceptualRoughness;
    metal = mrSample.b * metal;
#endif
    metal = clamp(metal, 0.0, 1.0);

#ifdef HAS_diffuseTexture
    basecolor.rgb *= SRGBtoLINEAR(texture(diffuseTexture, diffuse_coord0.xy).rgb);
#endif

    vec3 f0 = vec3(0.04);
    diffuse = basecolor.rgb * (vec3(1.0) - f0);
    diffuse *= 1.0 - metal;
    specular = mix(f0, basecolor.rgb, metal);
#endif // HAS_glossinessFactor

    perceptualRoughness = clamp(perceptualRoughness, c_MinRoughness, 1.0);

    // Compute reflectance (BRDF)
    // For typical incident reflectance range (between 4% to 100%) set the grazing reflectance to 100% for typical fresnel effect.
    // For very low reflectance range on highly diffuse objects (below 4%), incrementally reduce grazing reflecance to 0%.

    float reflectance = max(max(specular.r, specular.g), specular.b);
    float reflectance90 = clamp(reflectance * 25.0, 0.0, 1.0);
    vec3 viewspaceNormal;

#ifdef HAS_emissiveTexture
    emission = SRGBtoLINEAR(texture(emissiveTexture, emissive_coord0.xy).rgb);
#endif
#if defined(HAS_normalTexture)
    viewspaceNormal = texture(normalTexture, normal_coord0.xy).xyz * 2.0 - 1.0;
    viewspaceNormal = normalize(calculateTangentMatrix() * viewspaceNormal * vec3(normalScale, normalScale, 1.0));
#else
    viewspaceNormal = viewspace_normal;
#endif
    return Surface(viewspaceNormal, vec4(diffuse, diffuse_color.a),
                   specular, emission,
                   reflectance90, perceptualRoughness);
}

#ifdef HAS_LIGHTSOURCES

void LightPixel(Surface);

Reflected total_light;

vec4 PixelColor(Surface s)
{
    total_light.diffuse_color = vec3(0);
    LightPixel(s);
    return vec4(total_light.diffuse_color, s.diffuse.a);
}

#else
vec4 PixelColor(Surface s)
{
    return s.diffuse;
}
#endif
