
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

layout(location = 10) in vec2 diffuse_coord;
layout(location = 1) in vec3 viewspace_normal;


#ifdef HAS_LIGHTSOURCES

layout(location = 2) out vec3 view_direction;
layout(location = 3) out vec3 viewspace_position;
layout(location = 4) out vec3 vertex_light_diffuse;
layout(location = 5) out vec3 vertex_light_specular;
layout(location = 6) out vec3 vertex_light_ambient;

#ifdef HAS_SHADOWS
layout(set = 0, binding = 9) uniform highp sampler2DArray u_shadow_maps;

float unpackFloatFromVec4i(const vec4 value)
{
    const vec4 unpackFactors = vec4(1.0 / (256.0 * 256.0 * 256.0), 1.0 / (256.0 * 256.0), 1.0 / 256.0, 1.0);
    return dot(value, unpackFactors);
}
#endif
#endif

@FragmentSurface

#ifdef HAS_LIGHTSOURCES

@FragmentAddLight

@LIGHTSOURCES

#endif

void main()
{
    Surface s = @ShaderName();
    fragColor = PixelColor(s);
}