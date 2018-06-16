

#ifdef HAS_LIGHTSOURCES
#ifdef HAS_MULTIVIEW
    #define u_modelview_it u_matrices[u_matrix_offset + gl_ViewID_OVR + uint(2)]
#else
    #define u_modelview_it u_matrices[u_matrix_offset + u_right + uint(2)]
#endif
    vertex.local_normal = vec4(normalize(a_normal), 0.0);
    vec4 pos = u_model * vertex.local_position;
    pos = u_view * pos;
    vertex.viewspace_position = pos.xyz / pos.w;
    vertex.viewspace_normal = normalize((u_modelview_it * vertex.local_normal).xyz);
    vertex.view_direction = normalize(-vertex.viewspace_position);
#endif

#ifdef HAS_a_texcoord
   tex_coord0 = a_texcoord.xy;
#endif
#ifdef HAS_a_texcoord1
    tex_coord1 = a_texcoord1.xy;
#endif
#ifdef HAS_a_texcoord2
    tex_coord2 = a_texcoord2.xy;
#endif
#ifdef HAS_a_texcoord3
    tex_coord3 = a_texcoord3.xy;
#endif


