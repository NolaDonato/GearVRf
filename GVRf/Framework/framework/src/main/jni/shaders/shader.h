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
 * A shader which an user can add in run-time.
 ***************************************************************************/

#ifndef SHADER_H_
#define SHADER_H_

#include <map>
#include <memory>
#include <string>
#include <mutex>
#include <vector>
#include <objects/mesh.h>
#include <objects/lightlist.h>

#include "glm/glm.hpp"
#include "glm/gtc/type_ptr.hpp"
#include "objects/uniform_block.h"

//#define DEBUG_SHADER 1

namespace gvr {
class Mesh;
class MatrixCalc;

/**
 * Contains information about the vertex attributes, textures and
 * uniforms used by the shader source and the sources for the
 * vertex and fragment shaders.
 *
 * Shaders are only created by the ShaderManager when addShader is called.
 */
class Shader
{
public:
/*
 * Creates a native shader description.
 * The actual GL program is not made until the first call to render()
 * @param id                ShaderManager ID for the shader
 * @param signature         Unique signature string
 * @param uniformDescriptor String giving the names and types of shader material uniforms
 *                          This does NOT include uniforms used by light sources
 * @param textureDescriptor String giving the names and types of texture samplers
 * @param vertexDescriptor  String giving the names and types of vertex attributes
 * @param vertexShader      String with GLSL source for vertex shader
 * @param fragmentShader    String with GLSL source for fragment shader
 * @see ShaderManager::addShader
 */
    explicit Shader(int id, const char* signature,
                    const char* uniformDescriptor,
                    const char* textureDescriptor,
                    const char* vertexDescriptor,
                    const char* vertexShader,
                    const char* fragmentShader,
                    const char* matrixCalc);

    virtual ~Shader() { };

    /*
     * Returns the unique signature for this shader (provided
     * to ShaderManager::addShader when this Shader was created).
     */
    const char* signature() const { return mSignature.c_str(); }

    /*
     * Returns the ShaderManager ID (generated by addShader)
     */
    int getShaderID() const { return mId; }

    /*
     *  returns the vertex attributes descriptor
     */
    DataDescriptor& getVertexDescriptor()
    {
        return mVertexDesc;
    }
    /*
    *  returns the texture descriptor
    */
    DataDescriptor& getTextureDescriptor()
    {
        return mTextureDesc;
    }
    /*
     *  returns the uniform descriptor
     */
    DataDescriptor& getUniformDescriptor()
    {
        return mUniformDesc;
    }

    void useMatrixUniforms(bool flag)
    {
        mUseMatrixUniforms = flag;
    }

    bool usesMatrixUniforms() const
    {
        return mUseMatrixUniforms;
    }

    bool useShadowMaps() const
    {
        return mUseShadowMaps;
    }

    bool useLights() const
    {
        return mUseLights;
    }
    bool hasBones()
    {
        return mUseHasBones;
    }

    int getOutputBufferSize() const
    {
        return mOutputBufferSize;
    }

    bool isShaderDirty()
    {
        return  shaderDirty;
    }

    void setShaderDirty(bool flag)
    {
        shaderDirty = flag;
    }

    bool useMaterialGPUBuffer() const
    {
        return mUseMaterialGPUBuffer;
    }

    int calcMatrix(const glm::mat4* inputMatrices, glm::mat4* outputMatrices);
    virtual bool useShader(bool) = 0;
    virtual void bindLights(LightList& lights, Renderer* r) = 0;
    static int calcSize(const char* type);

private:
    Shader(const Shader& shader) = delete;
    Shader(Shader&& shader) = delete;
    Shader& operator=(const Shader& shader) = delete;
    Shader& operator=(Shader&& shader) = delete;

protected:
    MatrixCalc* mMatrixCalc;
    bool shaderDirty = true;
    DataDescriptor mUniformDesc;
    DataDescriptor mVertexDesc;
    DataDescriptor mTextureDesc;
    std::string mSignature;
    std::string mVertexShader;
    std::string mFragmentShader;
    std::string mClassName;
    int mOutputBufferSize;
    int mId;
    bool mUseMatrixUniforms;
    bool mUseLights;
    bool mUseShadowMaps;
    bool mUseHasBones;
    bool mUseMaterialGPUBuffer;
    jmethodID mCalcMatrixMethod;
};

}
#endif
