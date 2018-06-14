struct Radiance
{
   vec3 ambient_intensity;
   vec3 diffuse_intensity;
   vec3 specular_intensity;
   vec3 direction;
   float attenuation;
};

//
// PBR does not support vertex lighting
//
Reflected LightPerVertex(Radiance r, Vertex vertex)
{
    return Reflected(vec3(0), vec3(0), vec3(0));
}