
struct Radiance
{
   vec3 ambient_intensity;
   vec3 diffuse_intensity;
   vec3 specular_intensity;
   vec3 direction;
   float attenuation;
};

//
// Schlick Implementation of microfacet occlusion from
// "An Inexpensive BRDF Model for Physically based Rendering" by Christophe Schlick.
// assumes roughness of 1.0
float geometricOcclusionSchlick(float LdotH, float NdotH)
{
    float k = 0.79788; // 0.79788 = sqrt(2.0/3.1415);
    float l = LdotH / (LdotH * (1.0 - k) + k);
    float n = NdotH / (NdotH * (1.0 - k) + k);
    return l * n;
}

//
// In the per-vertex case, roughness is assumed to be 1.0
//
Reflected LightPerVertex(Radiance r, Vertex vertex)
{
    vec3 l = r.direction.xyz;                  // From surface to light, unit length, view-space
    vec3 n = vertex.viewspace_normal;          // normal at surface point
    vec3 v = vertex.view_direction;            // Vector from surface point to camera
    vec3 h = normalize(l + v);                 // Half vector between both l and v
    float NdotL = clamp(dot(n, l), 0.001, 1.0);
    float NdotV = abs(dot(n, v)) + 0.001;
    float NdotH = clamp(dot(n, h), 0.0, 1.0);
    float LdotH = clamp(dot(l, h), 0.0, 1.0);
    float VdotH = clamp(dot(v, h), 0.0, 1.0);

    vec3 F = vec3(1.0) * pow(clamp(1.0 - VdotH, 0.0, 1.0), 5.0);
    float G = geometricOcclusionSchlick(NdotL, NdotV); // Schlick implementation
    float D = 1.0 / M_PI;
    vec3 kD = (vec3(1.0) - F) / M_PI;
    vec3 kS = F * G * D / (4.0 * NdotL * NdotV);
    return Reflected(vec3(0), r.diffuse_intensity * kD, r.specular_intensity * kS);
}