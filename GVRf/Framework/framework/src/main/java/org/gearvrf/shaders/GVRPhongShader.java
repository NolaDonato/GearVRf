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
package org.gearvrf.shaders;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRRenderData;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRShaderData;
import org.gearvrf.GVRShaderTemplate;
import org.gearvrf.IRenderable;
import org.gearvrf.utility.TextFile;

import android.content.Context;

import org.gearvrf.R;
import org.joml.Matrix4f;

/**
    * Manages a set of variants on vertex and fragment shaders from the same source
    * code.
    */
   public class GVRPhongShader extends GVRShaderTemplate
{
    private static String fragTemplate = null;
    private static String vtxTemplate = null;
    private static String surfaceShader = null;
    private static String addLight = null;
    private static String vtxShader = null;
    private static String normalShader = null;
    private static String skinShader = null;
    static private Matrix4f mTempMatrix1 = new Matrix4f();
    static private Matrix4f mTempMatrix2 = new Matrix4f();

    public GVRPhongShader(GVRContext gvrcontext)
    {
        super("float4 ambient_color; float4 diffuse_color; float4 specular_color; float4 emissive_color; float3 u_color; float u_opacity; float specular_exponent; float line_width; float2 u_lightmap_offset; float2 u_lightmap_scale",
              "sampler2D diffuseTexture; sampler2D ambientTexture; sampler2D specularTexture; sampler2D opacityTexture; sampler2D lightmapTexture; sampler2D normalTexture; sampler2D emissiveTexture",
              "float3 a_position float2 a_texcoord float2 a_texcoord1 float2 a_texcoord2 float2 a_texcoord3 float3 a_normal float4 a_bone_weights int4 a_bone_indices float4 a_tangent float4 a_bitangent",
              GLSLESVersion.VULKAN);

        if (fragTemplate == null)
        {
            Context context = gvrcontext.getContext();
            fragTemplate = TextFile.readTextFile(context, R.raw.fragment_template_multitex);
            vtxTemplate = TextFile.readTextFile(context, R.raw.vertex_template_multitex);
            surfaceShader = TextFile.readTextFile(context, R.raw.phong_surface_multitex);
            vtxShader = TextFile.readTextFile(context, R.raw.pos_norm_multitex);
            normalShader = TextFile.readTextFile(context, R.raw.normalmap);
            skinShader = TextFile.readTextFile(context, R.raw.vertexskinning);
            addLight = TextFile.readTextFile(context, R.raw.addlight);
        }
        setSegment("FragmentTemplate", fragTemplate);
        setSegment("VertexTemplate", vtxTemplate);
        setSegment("FragmentSurface", surfaceShader);
        setSegment("FragmentAddLight", addLight);
        setSegment("VertexSkinShader", skinShader);
        setSegment("VertexShader", vtxShader);
        setSegment("VertexNormalShader", normalShader);
        mHasVariants = true;
        mUsesLights = true;
    }

    public HashMap<String, Integer> getRenderDefines(IRenderable renderable, GVRScene scene)
    {
        HashMap<String, Integer> defines = super.getRenderDefines(renderable, scene);
        boolean lightMapEnabled = (renderable instanceof GVRRenderData) ?
                ((GVRRenderData) renderable).isLightMapEnabled() : false;

        if (!lightMapEnabled)
        {
            defines.put("lightMapTexture", 0);
        }
        return defines;
    }

    protected void setMaterialDefaults(GVRShaderData material)
    {
        material.setVec4("ambient_color", 0.2f, 0.2f, 0.2f, 1.0f);
        material.setVec4("diffuse_color", 0.8f, 0.8f, 0.8f, 1.0f);
        material.setVec4("specular_color", 0.0f, 0.0f, 0.0f, 1.0f);
        material.setVec4("emissive_color", 0.0f, 0.0f, 0.0f, 1.0f);
        material.setFloat("specular_exponent", 0.0f);
        material.setFloat("line_width", 1.0f);
        material.setFloat("u_opacity", 0.0f);
    }

    @Override
    public String getMatrixCalc(boolean usesLights)
    {
        return usesLights ? "left_mvp; right_mvp; model; (model~ * inverse_left_view)^; (model~ * inverse_right_view)^": null;
    }

    /**
     * Calculate the matrices required by this shader.
     * This function is called every frame to calculate the matrices
     * used by the shader. The Phong shader uses the MVP matrix only
     * if there is no lighting. If lights are enabled, it also must
     * calculate the inverse of the model view matrix.
     * @param inputMatrices input matrices - projection, left view, right view and model.
     * @param outputMatrices left model view projection, right model view projection
     *                       (for lighting) left model view inverse transpose,
     *                       right model view inverse transpose
     * @param numMatrices    number of output matrices expected for stereo
     * @param isStereo       true for stereo rendering, false for mono
     * @returns number of matrices actually calculated and stored in the output buffer
     */
    public static int calcMatrix(float[] inputMatrices, float[] outputMatrices, int numMatrices, boolean isStereo)
    {
        // Input matrices
        int PROJECTION = 0;
        int LEFT_VIEW = 1 * 16;
        int RIGHT_VIEW = 2 * 16;
        int LEFT_VIEW_INVERSE = 3 * 16;
        int RIGHT_VIEW_INVERSE = 4 * 16;
        int MODEL = 5 * 16;
        int LEFT_MVP = 6 * 16;
        int RIGHT_MVP = 7 * 16;

        // Output matrices
        int OUT_MODEL = 0;
        int OUT_LEFT_MVP = 1 * 16;
        int OUT_RIGHT_MVP = 2 * 16;
        int OUT_LEFT_MODEL_VIEW_INVERSE = 3 * 16;
        int OUT_RIGHT_MODEL_VIEW_INVERSE = 4 * 16;

        // Copy model matrix, left model view projection, right model view projection
        System.arraycopy(inputMatrices, MODEL, outputMatrices, 0, 3 * 16);
        if (numMatrices > 3)
        {
            return 3 + calcLightMatrix(inputMatrices, outputMatrices,
                                       mTempMatrix1, mTempMatrix2, isStereo);
        }
        return 3;
    }
}

