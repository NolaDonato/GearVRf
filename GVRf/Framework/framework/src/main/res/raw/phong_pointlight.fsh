Radiance Radiance@LightType(in U@LightType data, int index)
{
    vec4 lightpos = u_view * vec4(data.world_position.xyz, 1.0);
	vec3 lightdir = lightpos.xyz - viewspace_position;
	// Attenuation
    float distance    = length(lightdir);
    float attenuation = 1.0 / (data.attenuation_constant + data.attenuation_linear * distance +
    					data.attenuation_quadratic * (distance * distance));  
	
	return Radiance(clamp(data.ambient_intensity.xyz, 0.0, 1.0),
				    clamp(data.diffuse_intensity.xyz, 0.0, 1.0),
				    clamp(data.specular_intensity.xyz, 0.0, 1.0),
				    normalize(lightdir),
					attenuation);

}

void Fragment@LightType(Surface s)
{
#if @LightType_MAXQUALITY > 1
    for (int i = @LightType_VERTEX_LIGHTS; i < @LightType_TOTAL_LIGHTS; ++i)
    {
        U@LightType light = @LightType[i];

        if ((light.quality > 1) && (length(viewspace_position) <= light.radius))
        {
            Radiance r = Radiance@LightType(light, i);
            Reflected ref = LightPerPixel(r, s);
            total_light.ambient_color += ref.ambient_color;
            total_light.diffuse_color += ref.diffuse_color;
            total_light.specular_color += ref.specular_color;
        }
    }
#endif
}