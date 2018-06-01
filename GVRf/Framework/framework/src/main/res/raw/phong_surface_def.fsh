
struct Surface
{
   vec3 viewspaceNormal;
   vec4 ambient;
   vec4 diffuse;
   vec4 specular;
   vec4 emission;
};

struct Reflected
{
    vec3 ambient_color;
    vec3 diffuse_color;
    vec3 specular_color;
};

Surface makeSurface(vec3 normal)
{
    Surface surface = Surface(normal,
                              ambient_color,
                              diffuse_color,
                              specular_color,
                              emissive_color);
    return surface;
}



