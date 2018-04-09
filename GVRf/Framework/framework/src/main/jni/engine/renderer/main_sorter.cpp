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
 * Renders a scene, a screen.
 ***************************************************************************/

#include "objects/scene.h"
#include "objects/components/render_data.h"
#include "shaders/shader.h"
#include "main_sorter.h"

namespace gvr {

void MainSceneSorter::cull(RenderState& rstate)
{
    if (rstate.is_stereo)
    {
        Scene* scene = rstate.scene;
        scene->lockColliders();
        scene->clearVisibleColliders();
        RenderSorter::cull(rstate);
        scene->unlockColliders();
    }
    else
    {
        RenderSorter::cull(rstate);
    }
}

void MainSceneSorter::add(RenderState& rstate, SceneObject* object)
{
    RenderData* rdata = object->render_data();

    rstate.scene->pick(object);
    if (rdata == nullptr)
    {
        return;
    }
    Mesh* geometry = rdata->mesh();

    if (geometry == nullptr)
    {
        return;
    }
    BoundingVolume bounding_volume_ = object->getBoundingVolume();
    glm::vec3 center(bounding_volume_.center());
    glm::vec3 diff(center - rstate.camera_position);
    float d = (diff.x * diff.x) + (diff.y * diff.y) + (diff.z * diff.z);
    d = sqrtf(d);
    for (int i = 0; i < rdata->pass_count(); ++i)
    {
        RenderPass* rpass = rdata->pass(i);
        Renderable* r = alloc();

        r->mesh = geometry;
        r->matrices[0] = object->transform()->getModelMatrix();
        r->renderData = rdata;
        r->renderPass = rpass;
        r->material = rpass->material();
        r->renderModes = rpass->render_modes();
        r->nextLevel = nullptr;
        r->nextSibling = nullptr;
        r->transformBlock = nullptr;
        r->distanceFromCamera = d;
        r->shader = selectShader(rstate, *r);
        ++mVisibleElems;
    }
}

void MainSceneSorter::validate(RenderState& rstate)
{
    rstate.shadow_map = rstate.scene->getLights().updateLights(&mRenderer);
    RenderSorter::validate(rstate);
}

bool MainSceneSorter::isValid(RenderState& rstate, Renderable& r)
{
    if (r.shader)
    {
        const char* oldsig = r.shader->signature();
        const char* lightsig = rstate.scene->getLights().getDescriptor();
        int nnew = strlen(lightsig);
        int nold = strlen(oldsig);

        oldsig += nold - nnew;
        if ((nold > nnew) && (strcmp(oldsig, lightsig) != 0))
        {
            r.renderPass->setDirty();
        }
        if (r.material->isTransparent())
        {
            if (r.renderModes.getRenderOrder() == RenderModes::Geometry)
            {
                r.renderModes.setRenderOrder(RenderModes::Transparent);
            }
        }
    }
    return RenderSorter::isValid(rstate, r);
}

void MainSceneSorter::merge(Renderable* item)
{
    mergeByOrder(&mRenderList, item);
}


void MainSceneSorter::mergeByOrder(Renderable* prev, Renderable* item)
{
    Renderable* cur = prev->nextLevel;
    int itemOrder = item->renderModes.getRenderOrder();
#ifdef DEBUG_RENDER
    SceneObject* owner = item->renderData->owner_object();
    const char* name = (owner ? owner->name().c_str() : "");
#endif
    if (itemOrder >= RenderData::Transparent)
    {
        item->renderModes.setAlphaBlend(true);
        item->renderModes.setDepthTest(false);
    }
    /*
     * Add this item at the front of the list?
     */
    if ((cur == nullptr) || (itemOrder < cur->renderModes.getRenderOrder()))
    {
        item->nextSibling = cur;
        prev->nextLevel = item;
#ifdef DEBUG_RENDER
        LOGV("RENDER: Front order: %s order = %d shader = %d material = %p",
             name, itemOrder, item->shader->getShaderID(), item->material);
#endif
        return;
    }
    /*
     * Scan the list to see where it fits
     */
    while (1)
    {
        if (itemOrder == cur->renderModes.getRenderOrder())
        {
            if (cur->nextLevel == nullptr)
            {
                addListhead(cur);
            }
            if (itemOrder >= RenderData::Transparent)
            {
                mergeByDistance(cur, item); // merge by distance from camera
            }
            else
            {
                mergeByShader(cur, item);   // merge by shader
            }
            return;
        }
        prev = cur;
        cur = cur->nextSibling;
        if (cur == nullptr)
        {
            break;
        }
        if (itemOrder < cur->renderModes.getRenderOrder())
        {
            item->nextSibling = cur;
            prev->nextSibling = item;
#ifdef DEBUG_RENDER
            LOGV("RENDER: Middle order: %s order = %d shader = %d material = %p",
                 name, itemOrder, item->shader->getShaderID(), item->material);
#endif
            return;
        }
    }
    prev->nextSibling = item;
    item->nextSibling = nullptr;
#ifdef DEBUG_RENDER
    LOGV("RENDER: End order: %s order = %d shader = %d material = %p",
         name, itemOrder, item->shader->getShaderID(), item->material);
#endif
}


void MainSceneSorter::mergeByMesh(Renderable* prev, Renderable* item)
{
    Renderable* cur = prev->nextLevel;
    Mesh* itemMesh = item->mesh;
    float itemDist = item->distanceFromCamera;
#ifdef DEBUG_RENDER
    SceneObject* owner = item->renderData->owner_object();
    int itemOrder = item->renderModes.getRenderOrder();
    int itemShader = item->shader->getShaderID();
    const char* name = (owner ? owner->name().c_str() : "");
#endif

/*
* Add this item at the front of the list?
*/
    if ((cur == nullptr) || (itemMesh < cur->mesh))
    {
        item->nextSibling = cur;
        prev->nextLevel = item;
#ifdef DEBUG_RENDER
        LOGV("RENDER: Front mesh: %s dist = %f order = %d shader = %d material = %p",
             name, itemDist, itemOrder, itemShader, item->material);
#endif
        return;
    }
    /*
     * Scan the list to see where it fits
     */
    while (1)
    {
        if (itemMesh == cur->mesh)          // mesh the same?
        {
            if (cur->nextLevel == nullptr)  // no? add a listhead
            {
                addListhead(cur);
            }
            mergeByMaterial(cur, item);    // merge by material
            return;
        }
        prev = cur;
        cur = cur->nextSibling;
        if (cur == nullptr)
        {
            break;
        }
        if (itemMesh < cur->mesh)
        {
            prev->nextSibling = item;
            item->nextSibling = cur;
#ifdef DEBUG_RENDER
            LOGV("RENDER: Middle mesh: %s dist = %f order = %d shader = %d material = %p",
                 name, itemDist, itemOrder, itemShader, item->material);
#endif
            return;
        }
    }
    prev->nextSibling = item;
    item->nextSibling = nullptr;
#ifdef DEBUG_RENDER
    LOGV("RENDER: End mesh: %s dist = %f order = %d shader = %d material = %p",
         name, itemDist, itemOrder, itemShader, item->material);
#endif
}

void MainSceneSorter::mergeByShader(Renderable* prev, Renderable* item)
{
    int itemShader = item->shader->getShaderID();
    Renderable* cur = prev->nextLevel;

#ifdef DEBUG_RENDER
    SceneObject* owner = item->renderData->owner_object();
    int itemOrder = item->renderModes.getRenderOrder();
    float itemDist = item->distanceFromCamera;
    const char* name = (owner ? owner->name().c_str() : "");
#endif
/*
 * Add this item at the front of the list?
 */
    if ((cur == nullptr) || (itemShader < cur->shader->getShaderID()))
    {
        item->nextSibling = cur;
        prev->nextLevel = item;
#ifdef DEBUG_RENDER
        LOGV("RENDER: Front shader: %s dist = %f order = %d shader = %d material = %p",
             name, itemDist, itemOrder, itemShader, item->material);
#endif
        return;
    }
    /*
     * Scan the list to see where it fits
     */
    while (1)
    {
        if (itemShader == cur->shader->getShaderID())
        {
            if (cur->nextLevel == nullptr)  // no? add a listhead
            {
                addListhead(cur);
            }
            mergeByMesh(cur, item);
            return;
        }
        prev = cur;
        cur = cur->nextSibling;
        if (cur == nullptr)
        {
            break;
        }
        if (itemShader < cur->shader->getShaderID())
        {
            item->nextSibling = cur;
            cur->nextSibling = item;
#ifdef DEBUG_RENDER
            LOGV("RENDER: Middle shader: %s dist = %f order = %d shader = %d material = %p",
                 name, itemDist, itemOrder, itemShader, item->material);
#endif
            return;
        }
    }
    prev->nextSibling = item;
    item->nextSibling = nullptr;
#ifdef DEBUG_RENDER
    LOGV("RENDER: End shader: %s dist = %f order = %d shader = %d material = %p",
         name, itemDist, itemOrder, itemShader, item->material);
#endif
}

void MainSceneSorter::mergeByMaterial(Renderable* prev, Renderable* item)
{
    ShaderData* itemMtl = item->material;
    Renderable* cur = prev->nextLevel;

#ifdef DEBUG_RENDER
    SceneObject* owner = item->renderData->owner_object();
    int itemOrder = item->renderModes.getRenderOrder();
    int itemShader = item->shader->getShaderID();
    const char* name = (owner ? owner->name().c_str() : "");
#endif
/*
 * Add this item at the front of the list?
 */
    if ((cur == nullptr) || (itemMtl <= cur->material))
    {
        item->nextSibling = cur;
        prev->nextLevel = item;
#ifdef DEBUG_RENDER
        LOGV("RENDER: Front material: %s order = %d shader = %d material = %p",
             name, itemOrder, itemShader, item->material);
#endif
        return;
    }
    /*
     * Scan the list to see where it fits
     */
    while (cur)
    {
        if (itemMtl <= cur->material)
        {
            item->nextSibling = cur;
            prev->nextSibling = item;
#ifdef DEBUG_RENDER
            LOGV("RENDER: Middle material: %s order = %d shader = %d material = %p",
                 name, itemOrder, itemShader, item->material);
#endif
            return;
        }
        prev = cur;
        cur = cur->nextSibling;
    }
    prev->nextSibling = item;
    item->nextSibling = nullptr;
#ifdef DEBUG_RENDER
    LOGV("RENDER: End material: %s order = %d shader = %d material = %p",
         name, itemOrder, itemShader, item->material);
#endif
}

void MainSceneSorter::mergeByDistance(Renderable* prev, Renderable* item)
{
    float itemDist = item->distanceFromCamera;
    Renderable* cur = prev->nextLevel;

#ifdef DEBUG_RENDER
    SceneObject* owner = item->renderData->owner_object();
    int itemOrder = item->renderModes.getRenderOrder();
    int itemShader = item->shader->getShaderID();
    const char* name = (owner ? owner->name().c_str() : "");
#endif

/*
* Add this item at the front of the list?
*/
    if ((cur == nullptr) || (itemDist > cur->distanceFromCamera))
    {
        item->nextSibling = cur;
        prev->nextLevel = item;
#ifdef DEBUG_RENDER
        LOGV("RENDER: Front distance: %s dist = %f order = %d shader = %d material = %p",
             name, itemDist, itemOrder, itemShader, item->material);
#endif
        return;
    }
    /*
     * Scan the list to see where it fits
     */
    while (1)
    {
        if (itemDist == cur->distanceFromCamera) // same distance from camera?
        {
            if (cur->nextLevel == nullptr)  // no? add a listhead
            {
                addListhead(cur);
            }
            mergeByShader(cur, item);       // merge by shader
            return;
        }
        prev = cur;
        cur = cur->nextSibling;
        if (cur == nullptr)
        {
            break;
        }
        if (itemDist > cur->distanceFromCamera)
        {
            item->nextSibling = cur;
            prev->nextSibling = item;
#ifdef DEBUG_RENDER
            LOGV("RENDER: Middle distance: %s dist = %f order = %d shader = %d material = %p",
                 name, itemDist, itemOrder, itemShader, item->material);
#endif
            return;
        }
    }
    prev->nextSibling = item;
    item->nextSibling = nullptr;
#ifdef DEBUG_RENDER
    LOGV("RENDER: End distance: %s dist = %f order = %d shader = %d material = %p",
         name, itemDist, itemOrder, itemShader, item->material);
#endif
}


}
