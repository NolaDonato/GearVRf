#ifdef HAS_diffuseTexture
layout(set = 0, binding = 10) uniform sampler2D diffuseTexture;
#else
layout(set = 0, binding = 10) uniform sampler2D u_texture;
#endif

Surface @ShaderName()
{
    vec4 diffuse = vec4(u_color.x, u_color.y, u_color.z, u_opacity);
#ifdef HAS_LIGHTSOURCES
    diffuse *= diffuse_color;
#endif
#ifdef HAS_diffuseTexture
    diffuse *= texture(diffuseTexture, diffuse_coord.xy);
#else
#ifdef HAS_u_texture
    diffuse *= texture(u_texture, diffuse_coord.xy);
#endif
#endif
    float opacity = diffuse.w;
    diffuse = vec4(diffuse.r * opacity, diffuse.g * opacity, diffuse.b * opacity, opacity);
    return Surface(viewspace_normal, ambient_color, diffuse, specular_color, emissive_color);
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


