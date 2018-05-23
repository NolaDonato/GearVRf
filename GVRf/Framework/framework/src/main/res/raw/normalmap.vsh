#if defined(HAS_normalTexture) && defined(HAS_a_normal)
#ifdef HAS_MULTIVIEW
   mat3 normalMatrix = mat3(u_mv_it_[gl_ViewID_OVR]);
#else
   mat3 normalMatrix = mat3(u_mv_it);
#endif
   vec3 normal = normalize(normalMatrix * vertex.local_normal.xyz);

#ifdef HAS_a_tangent
   vec3 tangent = normalize(normalMatrix * a_tangent);
   vec3 bitangent = normalize(normalMatrix * a_bitangent);
   tangent_matrix = mat3(tangent, bitangent, normal);
#endif

#endif