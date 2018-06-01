#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable
#ifdef HAS_MULTIVIEW
#extension GL_OVR_multiview2 : enable
layout(num_views = 2) in;
#endif
layout ( location = 0 ) in vec3 a_position;
layout ( location = 0 ) out vec3 diffuse_coord;

@MATRIX_UNIFORMS

#define u_model u_matrices[u_matrix_offset + uint(1)]

void main()
{
    vec4 pos = vec4(a_position, 1.0);
    diffuse_coord = normalize((u_model * pos).xyz);
    diffuse_coord.z = -diffuse_coord.z;
    gl_Position = u_mvp  * pos;
}
