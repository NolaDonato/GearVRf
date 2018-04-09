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

/***************************************************************************
 * Containing data about material and per pass configurations.             *
 ***************************************************************************/

#ifndef RENDER_PASS_H_
#define RENDER_PASS_H_

#include <memory>
#include <unordered_set>
#include "objects/hybrid_object.h"
#include "engine/renderer/render_state.h"

namespace gvr {

class ShaderData;
struct RenderState;

class RenderPass : public HybridObject {
public:
    RenderPass();
    virtual ~RenderPass() {}

    ShaderData* material() const {
        return material_;
    }

    void set_material(ShaderData* material);

    void enable_light()
    {
        if (render_modes_.setUseLights(true))
        {
            markDirty();
        }
    }

    void disable_light()
    {
        if (render_modes_.setUseLights(false))
        {
            markDirty();
        }
    }

    bool light_enabled()
    {
        return render_modes_.useLights();
    }

    void enable_lightmap()
    {
        render_modes_.setUseLightmap(true);
    }

    void disable_lightmap()
    {
        render_modes_.setUseLightmap(false);
    }

    int rendering_order() const
    {
        return render_modes_.getRenderOrder();
    }

    void set_rendering_order(int ro)
    {
        render_modes_.setRenderOrder(ro);
    }

    int draw_mode() const
    {
        return render_modes_.getDrawMode();
    }

    void set_draw_mode(int draw_mode)
    {
        render_modes_.setDrawMode(draw_mode);
    }

    int stencil_test() const
    {
        return render_modes_.isStencilTestEnabled();
    }

    void set_stencil_test(int stencil_test)
    {
        render_modes_.setStencilTest(stencil_test);
    }

    int render_mask() const
    {
        return render_modes_.getRenderMask();
    }

    void set_render_mask(int render_mask)
    {
        render_modes_.setRenderMask(render_mask);
    }

    bool cast_shadows()
    {
        return render_modes_.castShadows();
    }

    void set_cast_shadows(bool cast_shadows)
    {
        if (render_modes_.setCastShadows(cast_shadows))
        {
            markDirty();
        }
    }

    bool alpha_blend() const
    {
        return render_modes_.isAlphaBlendEnabled();
    }

    int source_alpha_blend_func() const
    {
        return render_modes_.getSourceBlendFunc();
    }

    int dest_alpha_blend_func() const
    {
        return render_modes_.getDestBlendFunc();
    }

    void set_alpha_blend_func(int sourceblend, int destblend)
    {
        render_modes_.setSourceBlendFunc(sourceblend);
        render_modes_.setDestBlendFunc(destblend);
    }

    void set_alpha_blend(bool alpha_blend)
    {
        render_modes_.setAlphaBlend(alpha_blend);
    }

    bool alpha_to_coverage() const
    {
        return render_modes_.isAlphaToCoverageEnabled();
    }

    void set_alpha_to_coverage(bool alpha_to_coverage)
    {
        render_modes_.setAlphaToCoverage(alpha_to_coverage);
    }

    bool invert_coverage_mask() const
    {
        return render_modes_.invertCoverageMask();
    }

    void set_invert_coverage_mask(bool invert_coverage_mask)
    {
        render_modes_.setInvertCoverageMask(invert_coverage_mask);
    }

    int cull_face() const
    {
        return render_modes_.getCullFace();
    }

    bool depth_test() const
    {
        return render_modes_.isDepthTestEnabled();
    }

    void set_depth_test(bool depth_test)
    {
        render_modes_.setDepthTest(depth_test);
    }

    void set_depth_mask(bool depth_mask)
    {
        render_modes_.setDepthMask(depth_mask);
    }

    bool depth_mask() const
    {
        return render_modes_.isDepthMaskEnabled();
    }

    bool offset() const
    {
        return render_modes_.isOffsetEnabled();
    }

    void set_offset(bool offset)
    {
        render_modes_.setOffset(offset);
    }

    float offset_units() const
    {
        return render_modes_.getOffsetUnits();
    }

    void set_offset_units(float units)
    {
        render_modes_.setOffsetUnits(units);
    }

    float offset_factor() const
    {
        return render_modes_.getOffsetFactor();
    }

    void set_offset_factor(float units)
    {
        render_modes_.setOffsetFactor(units);
    }

    float sample_coverage() const
    {
        return render_modes_.getSampleCoverage();
    }

    void set_sample_coverage(float f)
    {
        render_modes_.setSampleCoverage(f);
    }

    void set_cull_face(int cull_face)
    {
        render_modes_.setCullFace(cull_face);
    }

    void setStencilFunc(int func, int ref, int mask)
    {
        render_modes_.setStencilFunc(func);
        render_modes_.setStencilRef(ref);
        render_modes_.setStencilFuncMask(mask);
    }

    void setStencilOp(int sfail, int dpfail, int dppass)
    {
        render_modes_.setStencilFail(sfail);
        render_modes_.setDepthFail(dpfail);
        render_modes_.setStencilPass(dppass);
    }

    void setStencilTest(bool flag)
    {
        render_modes_.setStencilTest(flag);
    }

    void setStencilMask(unsigned int mask)
    {
        render_modes_.setStencilMask(mask);
    }

    unsigned int getStencilMask() const
    { return render_modes_.getStencilMask(); }

    int stencil_func_func() const
    { return render_modes_.getStencilFunc(); }

    int stencil_func_ref() const
    { return render_modes_.getStencilRef(); }

    int stencil_func_mask() const
    { return render_modes_.getStencilFuncMask(); }

    int stencil_op_sfail() const
    { return render_modes_.getStencilFail(); }

    int stencil_op_dpfail() const
    { return render_modes_.getDepthFail(); }

    int stencil_op_dppass()
    { return render_modes_.getStencilPass(); }

    void set_shader(int shaderid, bool useMultiview);

    int get_shader(bool useMultiview) const { return shaderID_[useMultiview]; }

    void markDirty()
    {
        shader_dirty_ = true;
    }

    bool isDirty()
    {
        return shader_dirty_;
    }

    void setDirty()
    {
        shader_dirty_ = true;
    }

    void clearDirty()
    {
        shader_dirty_ = false;
    }

    const RenderModes& render_modes() const
    {
        return render_modes_;
    }

    RenderModes& render_modes()
    {
        return render_modes_;
    }

private:
    ShaderData* material_;
    int shaderID_[2];
    RenderModes render_modes_;
    bool shader_dirty_;
};

}

#endif