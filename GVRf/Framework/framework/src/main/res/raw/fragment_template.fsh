
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
layout(location = 2) in vec2 diffuse_coord;

//
// locations 3 thru 8 are diffuse_coord,
// ambient_coord, specular_coord, opacity_coord,
// normal_coord, lightmap_coord and emissive_coord
//

#ifdef HAS_LIGHTSOURCES

layout(location = 9) in vec3 view_direction;
layout(location = 10) in vec3 viewspace_position;
layout(location = 11) in vec3 vertex_light_diffuse;
layout(location = 12) in vec3 vertex_light_specular;
layout(location = 13) in vec3 vertex_light_ambient;

#endif

@FragmentSurface

#ifdef HAS_LIGHTSOURCES

#ifdef HAS_SHADOWS
layout(set = 0, binding = 4) uniform highp sampler2DArray u_shadow_maps;

float unpackFloatFromVec4i(const vec4 value)
{
    const vec4 bitSh = vec4(1.0 / (256.0 * 256.0 * 256.0), 1.0 / (256.0 * 256.0), 1.0 / 256.0, 1.0);
    const vec4 unpackFactors = vec4( 1.0 / (256.0 * 256.0 * 256.0), 1.0 / (256.0 * 256.0), 1.0 / 256.0, 1.0 );
    return dot(value,unpackFactors);
}
#endif

@FragmentAddLight

@LIGHTSOURCES

#endif

void main()
{
    Surface s = @ShaderName();
    fragColor = PixelColor(s);
}
