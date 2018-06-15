
Radiance Radiance@LightType(Vertex vertex, in U@LightType data, int index)
{
    vec4 L = u_view * vec4(data.world_direction.xyz, 0.0);
    vec3 lightdir = normalize(L.xyz);
    float attenuation  = 1.0;

#ifdef HAS_SHADOWS
    vec4 shadowCoord = @LightType_shadow_position[index];

    if ((data.shadow_map_index >= 0) && (shadowCoord.w > 0.0))
    {
        float shadow_index = float(data.shadow_map_index);
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

void Vertex@LightType(Vertex vertex)
{
    for (int i = 0; i < @LightType_VERTEX_LIGHTS; ++i)
    {
        U@LightType light = @LightType[i];

        if ((light.quality == 1) && (length(vertex.viewspace_position) <= light.radius))
        {
#ifdef HAS_SHADOWS
            if (light.shadow_map_index >= 0)
            {
                @LightType_shadow_position[i] = mat4(light.sm0, light.sm1, light.sm2, light.sm3) * u_model * vertex.local_position;
            }
#endif
            Radiance r = Radiance@LightType(vertex, light, i);
            Reflected ref = LightPerVertex(r, vertex);
            vertex_light_ambient += ref.ambient_color;
            vertex_light_diffuse += ref.diffuse_color;
            vertex_light_specular += ref.specular_color;
        }
    }
#ifdef HAS_SHADOWS
    for (int i = @LightType_VERTEX_LIGHTS; i < @LightType_TOTAL_LIGHTS; ++i)
    {
        U@LightType light = @LightType[i];

        if ((light.quality > 1) && (light.shadow_map_index >= 0))
        {
            @LightType_shadow_position[i] = mat4(light.sm0, light.sm1, light.sm2, light.sm3) * u_model * vertex.local_position;
        }
    }
#endif
}
