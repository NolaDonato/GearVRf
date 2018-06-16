
#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable

#ifdef HAS_MULTIVIEW
#extension GL_OVR_multiview2 : enable
layout(num_views = 2) in;
#endif
precision highp float;
precision lowp int;

@MATRIX_UNIFORMS

@MATERIAL_UNIFORMS

layout(location = 0) in vec3 a_position;

#ifdef HAS_a_texcoord
layout(location = 1) in vec2 a_texcoord;
#endif


#if defined(HAS_a_normal) && defined(HAS_LIGHTSOURCES)
layout(location = 2) in vec3 a_normal;
#endif

#ifdef HAS_a_texcoord1
layout(location = 3) in vec2 a_texcoord1;
#endif

#ifdef HAS_a_texcoord2
layout(location = 4) in vec2 a_texcoord2;
#endif

#ifdef HAS_a_texcoord3
layout(location = 5) in vec2 a_texcoord3;
#endif

#ifdef HAS_VertexSkinShader
#ifdef HAS_a_bone_weights
@BONES_UNIFORMS

layout(location = 6) in vec4 a_bone_weights;
layout(location = 7) in ivec4 a_bone_indices;
#endif
#endif

#ifdef HAS_VertexNormalShader
#ifdef HAS_a_tangent
layout(location = 8) in vec3 a_tangent;
layout(location = 9) in vec3 a_bitangent;
layout(location = 7) out mat3 tangent_matrix;
#endif
#endif

layout(location = 10) out vec2 tex_coord0;
layout(location = 11) out vec2 tex_coord1;
layout(location = 12) out vec2 tex_coord2;
layout(location = 13) out vec2 tex_coord3;

struct Vertex
{
	vec4 local_position;
	vec4 local_normal;
	vec3 viewspace_position;
	vec3 viewspace_normal;
	vec3 view_direction;
};

layout(location = 1) out vec3 viewspace_normal;

#ifdef HAS_LIGHTSOURCES

layout(location = 2) out vec3 view_direction;
layout(location = 3) out vec3 viewspace_position;
layout(location = 4) out vec3 vertex_light_diffuse;
layout(location = 5) out vec3 vertex_light_specular;
layout(location = 6) out vec3 vertex_light_ambient;

#endif

@VertexSurface

#ifdef HAS_LIGHTSOURCES

@VertexAddLight

@LIGHTSOURCES

#endif
	
void main()
{
    Vertex vertex;

    vertex.local_position = vec4(a_position.xyz, 1.0);
    vertex.local_normal = vec4(0.0, 0.0, 1.0, 0.0);

@VertexShader

#ifdef HAS_VertexSkinShader
@VertexSkinShader
#endif

#ifdef HAS_VertexNormalShader
@VertexNormalShader
#endif


    viewspace_normal = vertex.viewspace_normal;
#ifdef HAS_LIGHTSOURCES
    viewspace_position = vertex.viewspace_position;
    view_direction = vertex.view_direction;
    vertex_light_diffuse = vec3(0);
    vertex_light_specular = vec3(0);
    vertex_light_ambient = vec3(0);
    LightVertex(vertex);
#endif
    gl_Position = u_mvp * vertex.local_position;
}
