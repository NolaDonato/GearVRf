
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

        if ((light.quality == 1) && (length(vertex.viewspace_position) <= light.radius))
        {
            Radiance r = Radiance@LightType(vertex, light, i);
            Reflected ref = LightPerVertex(r, vertex);
            vertex_light_ambient += ref.ambient_color;
            vertex_light_diffuse += ref.diffuse_color;
            vertex_light_specular += ref.specular_color;
        }
    }
}