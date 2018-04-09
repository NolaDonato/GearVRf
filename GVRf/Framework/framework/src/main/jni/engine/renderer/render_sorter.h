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

#ifndef RENDER_SORTER_H_
#define RENDER_SORTER_H_

//#define DEBUG_CULL 1
//#define DEBUG_RENDER 1
//#define DEBUG_TRANSFORM 1

#include <vector>
#include <memory>
#include "glm/glm.hpp"
#include "objects/scene_object.h"
#include "objects/bounding_volume.h"
#include "util/gvr_jni.h"

namespace gvr {

class Camera;
class Scene;
class ShaderData;
class UniformBlock;
class RenderPass;
class RenderTarget;
class RenderState;
class RenderData;
class Mesh;
class Shader;
class Renderer;

class RenderSorter
{
public:
    class Renderable
    {
    public:
        int             matrixOffset;
        float           distanceFromCamera;
        RenderModes     renderModes;
        RenderData*     renderData;
        RenderPass*     renderPass;
        ShaderData*     material;
        Shader*         shader;
        Mesh*           mesh;
        glm::mat4       matrices[2];
        UniformBlock*   transformBlock;
        Renderable*     nextLevel;
        Renderable*     nextSibling;

        Renderable()
        : nextLevel(nullptr),
          nextSibling(nullptr),
          renderData(nullptr),
          shader(nullptr),
          mesh(nullptr),
          material(nullptr),
          distanceFromCamera(0),
          matrixOffset(-1),
          transformBlock(nullptr)
        {
        }
    };

    RenderSorter(Renderer& renderer, const char* name, int numMatrices);
    virtual void cull(RenderState& rstate);
    virtual void init(RenderState& rstate);
    virtual void sort(RenderState& rstate);
    virtual void render(RenderState& rstate);
    virtual void clear();

    void dump();

    Renderer& getRenderer()
    {
        return mRenderer;
    }

    virtual ~RenderSorter();

    Renderable* add(RenderState&, Renderable& r);

private:
    virtual void build_frustum(float frustum[6][4], const float *vp_matrix);
    virtual void frustum_cull(RenderState& rstate,
                              glm::vec3 camera_position,
                              SceneObject *object,
                              float frustum[6][4],
                              bool continue_cull,
                              int planeMask);

    RenderSorter(RenderSorter&&) = delete;
    RenderSorter& operator=(const RenderSorter&) = delete;
    RenderSorter& operator=(RenderSorter&&) = delete;

protected:
    struct BlockHeader
    {
        BlockHeader*    nextBlock;
        int             numElems;
    };
    Renderable*     alloc();
    virtual void    validate(RenderState& rstate);
    virtual void    merge(Renderable* item);
    virtual void    add(RenderState& rstate, SceneObject* object);
    virtual Shader* selectShader(const RenderState& rstate, Renderable& r);
    virtual bool    isValid(RenderState& rstate, Renderable& r);
    virtual void    updateTransform(RenderState& rstate, Renderable& r);
    virtual void    render(RenderState& rstate, const Renderable& r);
    virtual void    dump(const Renderable&, const std::string& pad) const;
    virtual void    mergeByShader(Renderable* list, Renderable* item);
    UniformBlock*   updateTransformBlock(Renderable& r, int numMatrices, const float* matrixdata);
    void            addListhead(Renderable* cur);
    bool            findRenderable(const Renderable* root, const Renderable* findme) const;
    std::string     mName;
    Renderable      mRenderList;
    Renderer&       mRenderer;
    int             mMaxMatricesPerBlock;
    int             mTransBlockIndex;
    int             mNumMatricesInBlock;
    BlockHeader*    mMemoryPool;
    BlockHeader*    mCurBlock;
    int             mMaxElems;
    int             mVisibleElems;
    std::vector<UniformBlock*> mTransformBlocks;
    glm::mat4       mOutputMatrices[10];
};


}
#endif
