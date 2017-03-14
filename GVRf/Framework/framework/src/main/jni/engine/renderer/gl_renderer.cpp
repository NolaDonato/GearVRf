/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "glm/gtc/matrix_inverse.hpp"
#include "gl/gl_material.h"
#include "gl/gl_render_data.h"
#include "gl/gl_bitmap_image.h"
#include "gl/gl_cubemap_image.h"
#include "gl/gl_render_texture.h"
#include "gl/gl_external_image.h"
#include "gl/gl_float_image.h"
#include "gl/gl_imagetex.h"
#include "gl_renderer.h"
#include "objects/scene.h"

namespace gvr
{
    ShaderData *GLRenderer::createMaterial(const std::string &desc)
    {
        return new GLMaterial(desc);
    }

    RenderData *GLRenderer::createRenderData()
    {
        return new GLRenderData();
    }

    void GLRenderer::clearBuffers(const Camera &camera) const
    {
        GLbitfield mask = GL_DEPTH_BUFFER_BIT;

        if (-1 != camera.background_color_r())
        {
            glClearColor(camera.background_color_r(), camera.background_color_g(),
                         camera.background_color_b(), camera.background_color_a());
            mask |= GL_COLOR_BUFFER_BIT;
        }
        if (useStencilBuffer_)
        {
            mask |= GL_STENCIL_BUFFER_BIT;
            glStencilMask(~0);
        }
        glClear(mask);
    }

    UniformBlock *GLRenderer::createUniformBlock(const std::string &desc, int binding,
                                                 const std::string &name)
    {
        return new GLUniformBlock(desc, binding, name);
    }

    Image *GLRenderer::createImage(int type, int format)
    {
        switch (type)
        {
            case Image::ImageType::BITMAP: return new GLBitmapImage(format);
            case Image::ImageType::CUBEMAP: return new GLCubemapImage(format);
            case Image::ImageType::FLOAT_BITMAP: return new GLFloatImage();
        }
        return NULL;
    }

    Texture *GLRenderer::createTexture(int type)
    {
        Texture *tex = new Texture(type);
        Image *gltex = NULL;

        switch (type)
        {
            case Texture::TextureType::TEXTURE_2D: gltex = new GLImageTex(GL_TEXTURE_2D);
                break;
            case Texture::TextureType::TEXTURE_ARRAY: gltex = new GLImageTex(GL_TEXTURE_2D_ARRAY);
                break;
            case Texture::TextureType::TEXTURE_EXTERNAL: gltex = new GLExternalImage();
                break;
            case Texture::TextureType::TEXTURE_EXTERNAL_RENDERER: gltex = new GLExternalImage();
                break;
        }
        if (gltex)
        {
            tex->setImage(gltex);
        }
        return tex;
    }

    RenderTexture *GLRenderer::createRenderTexture(int width, int height, int sample_count,
                                                   int jcolor_format, int jdepth_format,
                                                   bool resolve_depth,
                                                   const TextureParameters *texture_parameters)
    {
        RenderTexture *tex = new GLRenderTexture(width, height, sample_count);
        return tex;
    }

    Texture *GLRenderer::createSharedTexture(int id)
    {
        Texture *tex = new Texture(GL_TEXTURE_2D);
        tex->setImage(new GLImageTex(GL_TEXTURE_2D, id));
        return tex;
    }

    Shader *GLRenderer::createShader(int id, const std::string &signature,
                                     const std::string &uniformDescriptor,
                                     const std::string &textureDescriptor,
                                     const std::string &vertexDescriptor,
                                     const std::string &vertexShader,
                                     const std::string &fragmentShader)
    {
        return new GLShader(id, signature, uniformDescriptor, textureDescriptor, vertexDescriptor,
                            vertexShader, fragmentShader
        );
    }

    GLRenderer::GLRenderer() : transform_ubo_(nullptr)
    {
        std::string desc;

        if (use_multiview)
            desc =
                    " mat4 u_view_[2]; mat4 u_mvp_[2]; mat4 u_mv_[2]; mat4 u_mv_it_[2]; mat4 u_model; mat4 u_view_i; float u_right; ";
        else
            desc =
                    " mat4 u_view; mat4 u_mvp; mat4 u_mv; mat4 u_mv_it; mat4 u_model; mat4 u_view_i; float u_right;";
        transform_ubo_ =
                reinterpret_cast<GLUniformBlock *>(createUniformBlock(desc, TRANSFORM_UBO_INDEX,
                                                                      "Transform_ubo"
                ));
    }

    void GLRenderer::renderCamera(Scene *scene, Camera *camera, int framebufferId, int viewportX,
                                  int viewportY, int viewportWidth, int viewportHeight,
                                  ShaderManager *shader_manager,
                                  PostEffectShaderManager *post_effect_shader_manager,
                                  RenderTexture *post_effect_render_texture_a,
                                  RenderTexture *post_effect_render_texture_b)
    {

        resetStats();
        RenderState rstate;
        rstate.shadow_map = false;
        rstate.material_override = NULL;
        rstate.viewportX = viewportX;
        rstate.viewportY = viewportY;
        rstate.viewportWidth = viewportWidth;
        rstate.viewportHeight = viewportHeight;
        rstate.shader_manager = shader_manager;
        rstate.uniforms.u_view = camera->getViewMatrix();
        rstate.uniforms.u_proj = camera->getProjectionMatrix();
        rstate.shader_manager = shader_manager;
        rstate.scene = scene;
        rstate.render_mask = camera->render_mask();
        rstate.uniforms.u_right = rstate.render_mask & RenderData::RenderMaskBit::Right;
        rstate.depth_shader = NULL;

        std::vector<ShaderData *> post_effects = camera->post_effect_data();

        GL(glEnable(GL_DEPTH_TEST));
        GL(glDepthFunc(GL_LEQUAL));
        GL(glEnable(GL_CULL_FACE));
        GL(glFrontFace(GL_CCW));
        GL(glCullFace(GL_BACK));
        GL(glEnable(GL_BLEND));
        GL(glDisable(GL_SAMPLE_ALPHA_TO_COVERAGE));
        GL(glBlendEquation(GL_FUNC_ADD));
        GL(glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA));
        GL(glDisable(GL_POLYGON_OFFSET_FILL));
        GL(glLineWidth(1.0f));
        if (post_effects.size() == 0)
        {
            glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
            glViewport(viewportX, viewportY, viewportWidth, viewportHeight);

            clearBuffers(*camera);
            renderRenderDataVector(rstate);
        }
        else
        {
            GLRenderTexture *texture_render_texture =
                    static_cast<GLRenderTexture *>(post_effect_render_texture_a);
            RenderTexture *target_render_texture;

            GL(glBindFramebuffer(GL_FRAMEBUFFER, texture_render_texture->getFrameBufferId()));
            GL(glViewport(0, 0, texture_render_texture->width(), texture_render_texture->height()));
            clearBuffers(*camera);
            GL(glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT));
            for (auto it = render_data_vector.begin(); it != render_data_vector.end(); ++it)
            {
                GL(renderRenderData(rstate, *it));
            }

            GL(glDisable(GL_DEPTH_TEST));
            GL(glDisable(GL_CULL_FACE));
            rstate.shader_manager = shader_manager;
            for (int i = 0; i < post_effects.size() - 1; ++i)
            {
                if (i % 2 == 0)
                {
                    texture_render_texture =
                            static_cast<GLRenderTexture *>(post_effect_render_texture_a);
                    target_render_texture =
                            static_cast<GLRenderTexture *>(post_effect_render_texture_b);
                }
                else
                {
                    texture_render_texture =
                            static_cast<GLRenderTexture *>(post_effect_render_texture_b);
                    target_render_texture =
                            static_cast<GLRenderTexture *>(post_effect_render_texture_a);
                }
                GL(glBindFramebuffer(GL_FRAMEBUFFER, framebufferId));
                GL(glViewport(viewportX, viewportY, viewportWidth, viewportHeight));

                GL(glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT));
                GL(renderPostEffectData(rstate, texture_render_texture, post_effects[i]));
            }

            GL(glBindFramebuffer(GL_FRAMEBUFFER, framebufferId));
            GL(glViewport(viewportX, viewportY, viewportWidth, viewportHeight));
            GL(glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT));
            renderPostEffectData(rstate, texture_render_texture, post_effects.back());
        }
        GL(glDisable(GL_DEPTH_TEST));
        GL(glDisable(GL_CULL_FACE));
        GL(glDisable(GL_BLEND));
    }

/**
 * Set the render states for render data
 */
    void GLRenderer::setRenderStates(RenderData *render_data, RenderState &rstate)
    {

        if (!(rstate.render_mask & render_data->render_mask()))
            return;

        if (render_data->offset())
        {
            GL(glEnable(GL_POLYGON_OFFSET_FILL));
            GL(glPolygonOffset(render_data->offset_factor(), render_data->offset_units()));
        }
        if (!render_data->depth_test())
        {
            GL(glDisable(GL_DEPTH_TEST));
        }

        if (render_data->stencil_test())
        {
            GL(glEnable(GL_STENCIL_TEST));

            GL(glStencilFunc(render_data->stencil_func_func(), render_data->stencil_func_ref(),
                             render_data->stencil_func_mask()));

            int sfail = render_data->stencil_op_sfail();
            int dpfail = render_data->stencil_op_dpfail();
            int dppass = render_data->stencil_op_dppass();
            if (0 != sfail && 0 != dpfail && 0 != dppass)
            {
                GL(glStencilOp(sfail, dpfail, dppass));
            }

            GL(glStencilMask(render_data->stencil_mask_mask()));
            if (RenderData::Queue::Stencil == render_data->rendering_order())
            {
                GL(glDepthMask(GL_FALSE));
                GL(glColorMask(GL_FALSE, GL_FALSE, GL_FALSE, GL_FALSE));
            }
        }

        if (!render_data->alpha_blend())
        {
            GL(glDisable(GL_BLEND));
        }
        if (render_data->alpha_to_coverage())
        {
            GL(glEnable(GL_SAMPLE_ALPHA_TO_COVERAGE));
            GL(glSampleCoverage(render_data->sample_coverage(),
                                render_data->invert_coverage_mask()));
        }
        glBlendFunc(render_data->source_alpha_blend_func(), render_data->dest_alpha_blend_func());
    }

/**
 * Restore the render states for render data
 */
    void GLRenderer::restoreRenderStates(RenderData *render_data)
    {
        if (render_data->cull_face() != RenderData::CullBack)
        {
            GL(glEnable(GL_CULL_FACE));
            GL(glCullFace(GL_BACK));
        }

        if (render_data->offset())
        {
            GL(glDisable(GL_POLYGON_OFFSET_FILL));
        }
        if (!render_data->depth_test())
        {
            GL(glEnable(GL_DEPTH_TEST));
        }

        if (render_data->stencil_test())
        {
            GL(glDisable(GL_STENCIL_TEST));
            if (RenderData::Queue::Stencil == render_data->rendering_order())
            {
                GL(glDepthMask(GL_TRUE));
                GL(glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE));
            }
        }

        if (!render_data->alpha_blend())
        {
            GL(glEnable(GL_BLEND));
        }
        if (render_data->alpha_to_coverage())
        {
            GL(glDisable(GL_SAMPLE_ALPHA_TO_COVERAGE));
        }
    }

/**
 * Generate shadow maps for all the lights that cast shadows.
 * The scene is rendered from the viewpoint of the light using a
 * special depth shader (GVRDepthShader) to create the shadow map.
 * @see Renderer::renderShadowMap Light::makeShadowMap
 */
    void GLRenderer::makeShadowMaps(Scene *scene, ShaderManager *shader_manager, int width,
                                    int height)
    {
        const std::vector<Light *> lights = scene->getLightList();
        GL(glEnable(GL_DEPTH_TEST));
        GL(glDepthFunc(GL_LEQUAL));
        GL(glEnable(GL_CULL_FACE));
        GL(glFrontFace(GL_CCW));
        GL(glCullFace(GL_BACK));
        GL(glDisable(GL_SAMPLE_ALPHA_TO_COVERAGE));

        int texIndex = 0;
        std::vector<SceneObject *> scene_objects;
        scene_objects.reserve(1024);
        for (auto it = lights.begin(); it != lights.end(); ++it)
        {
            if ((*it)->castShadow() &&
                (*it)->makeShadowMap(scene, shader_manager, texIndex, scene_objects, width, height))
                ++texIndex;
        }
        GL(glDisable(GL_DEPTH_TEST));
        GL(glDisable(GL_CULL_FACE));

    }

/**
 * Generates a shadow map into the specified framebuffer.
 * @param rstate        RenderState with rendering parameters
 * @param camera        camera with light viewpoint
 * @param framebufferId ID of framebuffer to render shadow map into
 * @param scene_objects temporary storage for culling
 * @see Light::makeShadowMap Renderer::makeShadowMaps
 */
    void GLRenderer::renderShadowMap(RenderState &rstate, Camera *camera, GLuint framebufferId,
                                     std::vector<SceneObject *> &scene_objects)
    {

        cullFromCamera(rstate.scene, camera, rstate.shader_manager, scene_objects);

        GLint drawFbo = 0, readFbo = 0;
        glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, &drawFbo);
        glGetIntegerv(GL_READ_FRAMEBUFFER_BINDING, &readFbo);
        const GLenum
                attachments[] = {GL_COLOR_ATTACHMENT0, GL_DEPTH_ATTACHMENT, GL_STENCIL_ATTACHMENT};

        GL(glBindFramebuffer(GL_FRAMEBUFFER, framebufferId));
        GL(glInvalidateFramebuffer(GL_FRAMEBUFFER, 3, attachments));
        GL(glViewport(rstate.viewportX, rstate.viewportY, rstate.viewportWidth,
                      rstate.viewportHeight
        ));
        glClearColor(0, 0, 0, 1);
        GL(glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT));
        rstate.shadow_map = true;
        std::string shader_sig = "GVRDepthShader";

        rstate.depth_shader = rstate.shader_manager->findShader(shader_sig);
        for (auto it = render_data_vector.begin(); it != render_data_vector.end(); ++it)
        {
            RenderData *rdata = *it;
            if (rdata->cast_shadows())
            {
                GL(renderRenderData(rstate, rdata));
            }
        }
        rstate.shadow_map = false;
        GL(glInvalidateFramebuffer(GL_FRAMEBUFFER, 2, &attachments[1]));
        glBindFramebuffer(GL_READ_FRAMEBUFFER, readFbo);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, drawFbo);
    }

    void GLRenderer::renderCamera(Scene *scene, Camera *camera, ShaderManager *shader_manager,
                                  PostEffectShaderManager *post_effect_shader_manager,
                                  RenderTexture *post_effect_render_texture_a,
                                  RenderTexture *post_effect_render_texture_b)
    {
        GLint curFBO;
        GLint viewport[4];
        glGetIntegerv(GL_FRAMEBUFFER_BINDING, &curFBO);
        glGetIntegerv(GL_VIEWPORT, viewport);

        renderCamera(scene, camera, curFBO, viewport[0], viewport[1], viewport[2], viewport[3],
                     shader_manager, post_effect_shader_manager, post_effect_render_texture_a,
                     post_effect_render_texture_b
        );
    }

    void GLRenderer::renderCamera(Scene *scene, Camera *camera, RenderTexture *render_texture,
                                  ShaderManager *shader_manager,
                                  PostEffectShaderManager *post_effect_shader_manager,
                                  RenderTexture *post_effect_render_texture_a,
                                  RenderTexture *post_effect_render_texture_b)
    {
        renderCamera(scene, camera, render_texture->getFrameBufferId(), 0, 0,
                     render_texture->width(), render_texture->height(), shader_manager,
                     post_effect_shader_manager, post_effect_render_texture_a,
                     post_effect_render_texture_b
        );

    }

    void GLRenderer::renderCamera(Scene *scene, Camera *camera, int viewportX, int viewportY,
                                  int viewportWidth, int viewportHeight,
                                  ShaderManager *shader_manager,
                                  PostEffectShaderManager *post_effect_shader_manager,
                                  RenderTexture *post_effect_render_texture_a,
                                  RenderTexture *post_effect_render_texture_b)
    {

        renderCamera(scene, camera, 0, viewportX, viewportY, viewportWidth, viewportHeight,
                     shader_manager, post_effect_shader_manager, post_effect_render_texture_a,
                     post_effect_render_texture_b
        );
    }

    void GLRenderer::set_face_culling(int cull_face)
    {
        switch (cull_face)
        {
            case RenderData::CullFront:glEnable(GL_CULL_FACE);
                glCullFace(GL_FRONT);
                break;

            case RenderData::CullNone:glDisable(GL_CULL_FACE);
                break;

                // CullBack as Default
            default:glEnable(GL_CULL_FACE);
                glCullFace(GL_BACK);
                break;
        }
    }

    void GLRenderer::occlusion_cull(RenderState &rstate, std::vector<SceneObject *> &scene_objects)
    {

        if (!occlusion_cull_init(rstate.scene, scene_objects))
            return;

        for (auto it = scene_objects.begin(); it != scene_objects.end(); ++it)
        {
            SceneObject *scene_object = (*it);
            RenderData *render_data = scene_object->render_data();
            if (render_data == 0 || render_data->material(0) == 0)
            {
                continue;
            }

            //If a query was issued on an earlier or same frame and if results are
            //available, then update the same. If results are unavailable, do nothing
            if (!scene_object->is_query_issued())
            {
                continue;
            }

            //If a previous query is active, do not issue a new query.
            //This avoids overloading the GPU with too many queries
            //Queries may span multiple frames

            bool is_query_issued = scene_object->is_query_issued();
            if (!is_query_issued)
            {
                //Setup basic bounding box and material
                RenderData *bounding_box_render_data(createRenderData());
                Mesh *bounding_box_mesh = render_data->mesh()->createBoundingBox();
                ShaderData *bbox_material = new GLMaterial("");
                RenderPass *pass = new RenderPass();
                GLShader *bboxShader = reinterpret_cast<GLShader *>(rstate.shader_manager
                        ->findShader(std::string("GVRBoundingBoxShader")));
                pass->set_shader(bboxShader->getProgramId());
                pass->set_material(bbox_material);
                bounding_box_render_data->set_mesh(bounding_box_mesh);
                bounding_box_render_data->add_pass(pass);

                GLuint *query = scene_object->get_occlusion_array();

                glDepthFunc(GL_LEQUAL);
                glEnable(GL_DEPTH_TEST);
                glColorMask(GL_FALSE, GL_FALSE, GL_FALSE, GL_FALSE);

                rstate.uniforms.u_model = scene_object->transform()->getModelMatrix();
                rstate.uniforms.u_mv = rstate.uniforms.u_view * rstate.uniforms.u_model;
                rstate.uniforms.u_mv_it = glm::inverseTranspose(rstate.uniforms.u_mv);
                rstate.uniforms.u_mvp = rstate.uniforms.u_proj * rstate.uniforms.u_mv;

                //Issue the query only with a bounding box
                glBeginQuery(GL_ANY_SAMPLES_PASSED, query[0]);
                renderWithShader(rstate, bboxShader, bounding_box_render_data, bounding_box_render_data->material(0));
                glEndQuery(GL_ANY_SAMPLES_PASSED);
                scene_object->set_query_issued(true);

                glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);

                //Delete the generated bounding box mesh
                bounding_box_mesh->cleanUp();
                delete bbox_material;
                delete pass;
                delete bounding_box_render_data;
            }

            GLuint query_result = GL_FALSE;
            GLuint *query = (*it)->get_occlusion_array();
            glGetQueryObjectuiv(query[0], GL_QUERY_RESULT_AVAILABLE, &query_result);

            if (query_result)
            {
                GLuint pixel_count;
                glGetQueryObjectuiv(query[0], GL_QUERY_RESULT, &pixel_count);
                bool visibility = ((pixel_count & GL_TRUE) == GL_TRUE);

                (*it)->set_visible(visibility);
                (*it)->set_query_issued(false);
                addRenderData((*it)->render_data(), rstate.scene);
                rstate.scene->pick(scene_object);
            }
        }
        rstate.scene->unlockColliders();
    }

    void GLRenderer::renderMesh(RenderState &rstate, RenderData *render_data)
    {
        for (int curr_pass = 0; curr_pass < render_data->pass_count(); ++curr_pass)
        {
            numberTriangles += render_data->mesh()->getNumTriangles();
            numberDrawCalls++;

            set_face_culling(render_data->pass(curr_pass)->cull_face());
            ShaderData *curr_material = rstate.material_override;
            Shader *shader = rstate.depth_shader;
            if (shader == nullptr)
                shader = rstate.shader_manager->getShader(render_data->get_shader(curr_pass));
            if (curr_material == nullptr)
                curr_material = render_data->pass(curr_pass)->material();
            if (curr_material != nullptr)
            {
                GL(renderMaterialShader(rstate, render_data, curr_material, shader));
            }
        }
    }

    void GLRenderer::renderMaterialShader(RenderState &rstate, RenderData *render_data,
                                          ShaderData *curr_material, Shader* shader)
    {
        SceneObject *owner = render_data->owner_object();
        ShaderManager *shader_manager = rstate.shader_manager;
        GLMaterial* material = static_cast<GLMaterial*>(curr_material);
        GLRenderData* rdata = static_cast<GLRenderData*>(render_data);

        if (shader == NULL)
        {
            LOGE("SHADER: shader not ready %s %p", owner->name().c_str(), render_data);
            return;
        }
        if (material->updateGPU(this, shader) < 0)
        {
            LOGE("SHADER: Texture: textures not ready %s", owner->name().c_str());
            return;
        }
        GLUniformBlock* transform_ubo = getTransformUbo();
        updateTransforms(rstate, transform_ubo, owner->transform());
        rdata->updateGPU(this);
        try
        {
            if ((rdata->draw_mode() == GL_LINE_STRIP) ||
                (rdata->draw_mode() == GL_LINES) ||
                (rdata->draw_mode() == GL_LINE_LOOP))
            {
                float lineWidth;
                if (curr_material->getFloat("line_width", lineWidth))
                {
                    glLineWidth(lineWidth);
                }
                else
                {
                    glLineWidth(1.0f);
                }
            }
            shader->useShader();
        }
        catch (const std::string &error)
        {
            LOGE("Error detected in Renderer::renderRenderData; name : %s, error : %s",
                 render_data->owner_object()->name().c_str(), error.c_str());
            shader = shader_manager->findShader(std::string("GVRErrorShader"));
            shader->useShader();
        }
        int texIndex = material->bindToShader(shader);
        if (texIndex >= 0)
        {
            transform_ubo->bindBuffer(shader);
            updateLights(rstate, shader, texIndex);
            rdata->render(shader, this);
        }
        checkGLError("renderMesh::renderMaterialShader");
    }

    int GLRenderer::renderWithShader(RenderState& rstate, Shader* shader, RenderData* renderData, ShaderData* shaderData)
    {
        GLMaterial* material = static_cast<GLMaterial*>(shaderData);

        if (shader == NULL)
        {
            LOGE("SHADER: shader %d not found", shaderData->getNativeShader());
            return 0;
        }
        GLRenderData* rdata = static_cast<GLRenderData*>(renderData);
        if (material->updateGPU(this, shader) < 0)
        {
            return 0;
        }
        rdata->updateGPU(this);
        try
        {
            shader->useShader();
        }
        catch (const std::string& error)
        {
            LOGE("Error detected in Renderer::renderWithShader; error : %s", error.c_str());
            return -1;
        }
        int texIndex = material->bindToShader(shader);
        if (texIndex >= 0)
        {
            rdata->render(shader, this);
        }
        return texIndex;
    }

    void GLRenderer::updateLights(RenderState &rstate, Shader* shader, int texIndex)
    {
        const std::vector<Light *> &lightlist = rstate.scene->getLightList();
        bool castShadow = false;

        for (auto it = lightlist.begin(); it != lightlist.end(); ++it)
        {
            Light *light = (*it);
            if (light != NULL)
            {
                light->render(shader);
                if (light->castShadow())
                    castShadow = true;
            }
        }
        if (castShadow)
        {
            Light::bindShadowMap(shader, texIndex);
        }
        checkGLError("Shader::render");
    }
}



