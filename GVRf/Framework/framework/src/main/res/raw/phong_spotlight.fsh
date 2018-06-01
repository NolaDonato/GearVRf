
Radiance Radiance@LightType(in U@LightType data, int index)
{
     vec4 lightpos = u_view * vec4(data.world_position.xyz, 1.0);
     vec3 lightdir = normalize(lightpos.xyz - viewspace_position);
          
     // Attenuation
     float distance    = length(lightdir);
     float attenuation = 1.0 / (data.attenuation_constant + data.attenuation_linear * distance +
    					data.attenuation_quadratic * (distance * distance));
	 vec4 spotDir =  normalize(u_view * data.world_direction);

     float cosSpotAngle = dot(spotDir.xyz, -lightdir);
     float outer = data.outer_cone_angle;
     float inner = data.inner_cone_angle;
     float inner_minus_outer = inner - outer;  
     float spot = clamp((cosSpotAngle - outer) / inner_minus_outer, 0.0, 1.0);
     return Radiance(data.ambient_intensity.xyz,
                     data.diffuse_intensity.xyz,
                     data.specular_intensity.xyz,
                     lightdir,
                     attenuation * spot);
                   
}

void Fragment@LightType(Surface s)
{
#if @LightType_MAXQUALITY > 1
    for (int i = 0; i < @LightType_PIXEL_LIGHTS; ++i)
    {
        U@LightType light = @LightType[i];

        if ((light.quality > 1) && (length(viewspace_position) <= light.radius))
        {
            Radiance r = Radiance@LightType(light, i);
            Reflected ref = LightReflected(r, viewspace_normal, view_direction);
            total_light.ambient_color += ref.ambient_color;
            total_light.diffuse_color += ref.diffuse_color;
            total_light.specular_color += ref.specular_color;
        }
    }
#endif
}