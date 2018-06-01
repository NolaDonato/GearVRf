
Radiance Radiance@LightType(in U@LightType data, int index)
{
    vec4 L = u_view * vec4(data.world_direction.xyz, 0.0);
    float attenuation = 1.0;
    vec3 lightdir = normalize(L.xyz);

#ifdef HAS_SHADOWS
    vec4 shadowCoord = @LightType_shadow_position[index];
    float shadow_index = float(data.shadow_map_index);

    if ((shadow_index >= 0.0) && (shadowCoord.w > 0.0))
    {
        float nDotL = max(dot(viewspace_normal, lightdir), 0.0);
        float bias = 0.001 * tan(acos(nDotL));
        vec3 shadowMapPos = shadowCoord.xyz / shadowCoord.w;
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
                    -lightdir,
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
            Reflected ref = LightReflected(r, viewspace_normal, view_direction);
            total_light.ambient_color += ref.ambient_color;
            total_light.diffuse_color += ref.diffuse_color;
            total_light.specular_color += ref.specular_color;
        }
    }
#endif
}

