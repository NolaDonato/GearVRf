
struct Surface
{
    vec3 viewspaceNormal;       // normal in view space
    vec4 diffuse;               // color contribution from diffuse lighting
    vec3 specular;              // color contribution from specular lighting
    vec3 emission;              // emitted light color
    vec2 brdf;                  // reflectance at 0 and 90
    float roughness;            // roughness value, as authored by the model creator (input to shader)
};


struct Reflected
{
    vec3 ambient_color;
    vec3 diffuse_color;
    vec3 specular_color;
};

const float M_PI = 3.141592653589793;
const float c_MinRoughness = 0.04;