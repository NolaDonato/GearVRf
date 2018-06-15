
#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable
#ifdef HAS_MULTIVIEW
#extension GL_OVR_multiview2 : enable
#endif
precision highp float;
precision lowp int;

@MATRIX_UNIFORMS

@MATERIAL_UNIFORMS

layout(location = 0) out vec4 fragColor;
layout(location = 1) in vec3 viewspace_normal;
layout(location = 10) in vec2 diffuse_coord;


#ifdef HAS_LIGHTSOURCES
layout(location = 2) in vec3 view_direction;
layout(location = 3) in vec3 viewspace_position;
layout(location = 4) in vec3 vertex_light_diffuse;
layout(location = 5) in vec3 vertex_light_specular;
layout(location = 6) in vec3 vertex_light_ambient;

#endif

@FragmentSurface

#ifdef HAS_LIGHTSOURCES
@FragmentAddLight
#endif

@LIGHTSOURCES

void main()
{
    Surface s = @ShaderName();
    fragColor = PixelColor(s);
}
