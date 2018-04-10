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

#ifndef MAIN_SCENE_SORTER_H_
#define MAIN_SCENE_SORTER_H_

#include "render_sorter.h"

namespace gvr {
/**
 * RenderSorter variant used to render the main scene.
 * This sorter used the material, shader and render modes
 * from the RenderPasses for rendering. Opaque objects are
 * sorted based on rendering order, shader, mesh and material.
 * Transparent objects are sorted based on rendering order,
 * distance from the camera, shader, mesh and material.
 */
class MainSceneSorter : public RenderSorter
{
public:
    MainSceneSorter(Renderer& renderer, int numMatrices = 0)
    : RenderSorter(renderer, "MainSorter", numMatrices)
    {
    }

    virtual void cull(RenderState& rstate);

protected:
    virtual void merge(Renderable* item);
    virtual void add(RenderState& rstate, SceneObject* object);
    virtual void validate(RenderState&);
    virtual bool isValid(RenderState& rstate, Renderable& r);
    virtual void mergeByOrder(Renderable* list, Renderable* item);
    virtual void mergeByShader(Renderable* list, Renderable* item);
    virtual void mergeByMesh(Renderable* list, Renderable* item);
    virtual void mergeByMaterial(Renderable* list, Renderable* item);
    virtual void mergeByDistance(Renderable* list, Renderable* item);

private:
    MainSceneSorter(const MainSceneSorter&) = delete;
    MainSceneSorter(MainSceneSorter&&) = delete;
    MainSceneSorter& operator=(const MainSceneSorter&) = delete;
    MainSceneSorter& operator=(MainSceneSorter&&) = delete;
};


}
#endif
