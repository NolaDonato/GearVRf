
Radiance Radiance@LightType(Vertex vertex, in U@LightType data, int index)
{
    vec4 L = u_view * vec4(data.world_direction.xyz, 0.0);

	return Radiance(data.ambient_intensity.xyz,
					data.diffuse_intensity.xyz,
					data.specular_intensity.xyz,
					normalize(-L.xyz),
					1.0);
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