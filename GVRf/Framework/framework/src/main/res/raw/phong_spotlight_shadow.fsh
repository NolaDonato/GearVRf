

Radiance Radiance@LightType(in U@LightType data, int index)
{
    vec4 lightpos = u_view * vec4(data.world_position.xyz, 1.0);
	vec4 spotDir =  normalize(u_view * vec4(data.world_direction.xyz, 0.0));
    vec3 lightdir = normalize(lightpos.xyz - viewspace_position);

     // Attenuation
     float distance = length(lightpos.xyz - viewspace_position);
     float attenuation = 1.0 / (data.attenuation_constant + data.attenuation_linear * distance + 
    					data.attenuation_quadratic * (distance * distance));
     float cosSpotAngle = dot(spotDir.xyz, -lightdir);
     float outer = data.outer_cone_angle;
     float inner = data.inner_cone_angle;
     float inner_minus_outer = inner - outer;  
     float spot = clamp((cosSpotAngle - outer) / inner_minus_outer, 0.0, 1.0);

#ifdef HAS_SHADOWS
    vec4 shadowCoord = @LightType_shadow_position[index];
    if ((data.shadow_map_index >= 0) && (shadowCoord.w > 0.0))
    {
        float nDotL = max(dot(viewspace_normal, lightdir), 0.0);
        float bias = 0.001 * tan(acos(nDotL));
        vec3 shadowMapPos = shadowCoord.xyz / shadowCoord.w;
        float shadow_index = float(data.shadow_map_index);
        vec3 texcoord = vec3(shadowMapPos.x, shadowMapPos.y, shadow_index);
        vec4 depth = texture(u_shadow_maps, texcoord);
        float distanceFromLight = unpackFloatFromVec4i(depth);

        bias = clamp(bias, 0.0, 0.01);
        if (distanceFromLight < shadowMapPos.z - bias)
        {
            attenuation = 0.5;
        }
	}
#endif
    return Radiance(data.ambient_intensity.xyz,
                    data.diffuse_intensity.xyz,
                    data.specular_intensity.xyz,
                    lightdir.xyz,
                    spot * attenuation);  
                   
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