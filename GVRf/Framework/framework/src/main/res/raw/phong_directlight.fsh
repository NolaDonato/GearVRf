
Radiance Radiance@LightType(in U@LightType data, int index)
{
    vec4 L = u_view * vec4(data.world_direction.xyz, 0.0);

	return Radiance(data.ambient_intensity.xyz,
					data.diffuse_intensity.xyz,
					data.specular_intensity.xyz,
					normalize(-L.xyz),
					1.0);
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

