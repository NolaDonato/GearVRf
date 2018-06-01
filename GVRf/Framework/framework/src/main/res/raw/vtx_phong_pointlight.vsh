
Radiance Radiance@LightType(Vertex vertex, in U@LightType data, int index)
{
    vec4 lightpos = u_view * vec4(data.world_position.xyz, 1.0);
	vec3 lightdir = lightpos.xyz - vertex.viewspace_position;
    float distance    = length(lightdir);
    float attenuation = 1.0 / (data.attenuation_constant + data.attenuation_linear * distance +
    					data.attenuation_quadratic * (distance * distance));  
	
	return Radiance(clamp(data.ambient_intensity.xyz, 0.0, 1.0),
				    clamp(data.diffuse_intensity.xyz, 0.0, 1.0),
				    clamp(data.specular_intensity.xyz, 0.0, 1.0),
				    normalize(lightdir),
					attenuation);
}

void Vertex@LightType(Vertex vertex)
{
    for (int i = 0; i < @LightType_VERTEX_LIGHTS; ++i)
    {
        U@LightType light = @LightType[i];

#if @LightType_MAXQUALITY > 1
        if (light.quality == 1)
#endif
        if (length(vertex.viewspace_position) <= light.radius)
        {
            Radiance r = Radiance@LightType(vertex, light, i);
            Reflected ref = LightReflected(r, vertex.viewspace_normal, vertex.view_direction);
            vertex_light_ambient += ref.ambient_color;
            vertex_light_diffuse += ref.diffuse_color;
            vertex_light_specular += ref.specular_color;
        }
    }
}