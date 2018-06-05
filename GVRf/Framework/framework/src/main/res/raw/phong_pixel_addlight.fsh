
struct Radiance
{
   vec3 ambient_intensity;
   vec3 diffuse_intensity;
   vec3 specular_intensity;
   vec3 direction;
   float attenuation;
};



Reflected LightPerPixel(Radiance r, Surface s)
{
	vec3 L = r.direction;	// From surface to light, unit length, view-space
	float nDotL = max(dot(s.viewspaceNormal, L), 0.0);

	vec3 kA = clamp(r.ambient_intensity.xyz, 0.0, 1.0);
	vec3 kS = vec3(0, 0, 0);
	vec3 kD = r.attenuation * nDotL * clamp(r.diffuse_intensity.xyz, 0.0, 1.0);
	if (nDotL > 0.0)
	{
		vec3 reflect = normalize(reflect(-L, s.viewspaceNormal));
		float cosAngle = dot(view_direction, reflect);
		if (cosAngle > 0.0)
		{
			kS = r.attenuation * clamp(r.specular_intensity, 0.0, 1.0);
			kS *= pow(cosAngle, specular_exponent);
		}
	}
	return Reflected(kA, kD, kS);
}
