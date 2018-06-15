#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable

#ifdef HAS_MULTIVIEW
#extension GL_OVR_multiview2 : enable
layout(num_views = 2) in;
#endif

precision mediump float;

layout ( location = 0 ) in vec3 a_position;
layout ( location = 1 ) in vec4 a_color;

@MATRIX_UNIFORMS

layout ( location = 0 ) out vec4 v_color;

void main()
{
    vec4 pos = vec4(a_position, 1);
    gl_Position = u_mvp  * pos;
    v_color = a_color;
}
