
Radiance Radiance@LightType(Vertex vertex, in U@LightType data, int index)
{
     vec4 lightpos = u_view * vec4(data.world_position.xyz, 1.0);
     vec3 lightdir = normalize(lightpos.xyz - vertex.viewspace_position);
          
     // Attenuation
     float distance    = length(lightdir);
     float attenuation = 1.0 / (data.attenuation_constant + data.attenuation_linear * distance +
    					data.attenuation_quadratic * (distance * distance));
	 vec4 spotDir =  normalize(u_view * data.world_direction);

     float cosSpotAngle = dot(spotDir.xyz, -lightdir);
     float outer = data.outer_cone_angle;
     float inner = data.inner_cone_angle;
     float inner_minus_outer = inner - outer;  
     float spot = clamp((cosSpotAngle - outer) / 
                    inner_minus_outer , 0.0, 1.0);
     return Radiance(data.ambient_intensity.xyz,
                     data.diffuse_intensity.xyz,
                     data.specular_intensity.xyz,
                     lightdir,
                     spot);  
                   
}

void Vertex@LightType(Vertex vertex)
{
    for (int i = 0; i < @LightType_TOTAL_LIGHTS; ++i)
    {
        U@LightType light = @LightType[i];

        if (light.quality > 0)
        {
#if @LightType_TOTAL_LIGHTS > @LightType_VERTEX_LIGHTS
            if (light.quality == 1)
#endif
            if (length(vertex.viewspace_position) <= light.radius)
            {
                Radiance r = Radiance@LightType(vertex, light, i);
                Reflected ref = LightPerVertex(r, vertex);
                vertex_light_ambient += ref.ambient_color;
                vertex_light_diffuse += ref.diffuse_color;
                vertex_light_specular += ref.specular_color;
            }
#ifdef HAS_SHADOWS
            @LightType_shadow_position[i] = mat4(light.sm0, light.sm1, light.sm2, light.sm3) * u_model * vertex.local_position;
#endif
        }
    }
}