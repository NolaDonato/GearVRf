#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable
#ifdef HAS_MULTIVIEW
#extension GL_OVR_multiview2 : enable
layout(num_views = 2) in;
#endif

precision highp float;
layout(location = 0) in vec3 a_position;
layout(location = 1) in vec3 a_normal;

@MATRIX_UNIFORMS

layout(location = 1) out vec3 viewspace_position;
layout(location = 2) out vec3 viewspace_normal;

#define u_model u_matrices[u_matrix_offset + uint(1)]

#ifdef HAS_MULTIVIEW
    #define u_modelview_it u_matrices[u_matrix_offset + gl_ViewID_OVR + uint(2)]
#else
    #define u_modelview_it u_matrices[u_matrix_offset + u_right + uint(2)]
#endif

void main()
{
    vec4 vspos = u_view * u_model * vec4(a_position, 1.0);
    viewspace_position = vspos.xyz / vspos.w;
    viewspace_normal = normalize((u_modelview_it * vec4(a_normal, 0)).xyz);
    gl_Position = u_mvp * vec4(a_position, 1.0);
}