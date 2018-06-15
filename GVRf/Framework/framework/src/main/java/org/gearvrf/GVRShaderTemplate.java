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
package org.gearvrf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gearvrf.shaders.GVRPhongShader;
import org.gearvrf.utility.Log;

/**
 * Generates a set of native vertex and fragment shaders from source code segments.
 * <p>
 * Each shader template keeps a set of named source code segments which are used
 * to compose the shaders. The code in the "FragmentTemplate" slot is the master
 * template for the fragment shader, the one attached as "VertexTemplate" is the
 * master for the vertex shader. Any name in the fragment template that starts
 * with "@Fragment" is replaced by the shader segment attached in that slot.
 * Similarly, names in the vertex template that start with "@Vertex" are
 * replaced with vertex shader segments. This permits multiple segments to be
 * combined into a single fragment or vertex shader. The segment names are also
 * #defined in the shader for conditional compilation.
 * <p>
 * To generate different variants from the same source we use #ifdef creatively.
 * Any shader variable name starting with a "HAS_" is assumed to be a #ifdef
 * that affects the generated code. These names typically correspond to
 * uniforms, textures or vertex buffer attributes. If these parameters are
 * defined by the material or mesh, that name will be #defined to 1 in the
 * shader. Otherwise, it will not be defined.
 * <p>
 * Each shader variant generated has a unique signature so that the same variant
 * will not be generated twice.
 * The shader also defines descriptors that define the
 * names and types of all the uniforms, textures and vertex attributes
 * used by the shader. For uniforms and attributes, each entry is a
 * float or integer type, an optional count and the name of the
 * uniform (e.g. "float3 diffuse_color, float specular_exponent, int is_enabled".
 * For textures, the descriptor contains the sampler type followed by the name:
 * (e.g. "sampler2D u_texture; samplerCube u_cubemap")
 * <p>
 * Multiple lights are supported by specifying light shader source code segments
 * in {@link GVRLight}. You can define different light implementations with
 * their own data structures and these will be included in the generated
 * fragment shader.
 * <p>
 * GearVRF also uses tokens starting with '@' inside the shader to indicate
 * where auto-generated sections should go. By using these tokens, you
 * can implement custom shaders which will work either on OpenGL or Vulkan.
 * <table>
 *     <th><td>Token</td><td>What GearVRF Puts There</td></th>
 *     <tr>
 *         <td>@MATERIAL_UNIFORMS</td>
 *         <td>
 *             Declarations for the materials actually used by the shader.
 *             These come from the uniform descriptor in the constructor
 *             of the Java class which implements the shader.
 *             If the shader can generate multiple variants, these
 *             declarations are probably a subset of those in the descriptor.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>@MATRIX_UNIFORMS</td>
 *         <td>
 *             Declarations for the matrices used by the shader.
 *             These are automatically generated by GearVRF
 *             based on the matrix expression returned by
 *             {@link GVRShader#getMatrixCalc(boolean)}.
 *             The Java implementation of the shader shoulo
 *             override this function to supply a matrix expression
 *             indicating which matrices GearVRF should calculate
 *             for the shader.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>@BONES_UNIFORMS</td>
 *         <td>
 *             Declarations for the bone matrices used by the shader.
 *             These are automatically generated by GearVRF
 *             if a mesh is being skinned and bone matrices are provided
 *             with {@link GVRMesh#setBones(List)}. This is automatically
 *             set up by the asset loader if when a model is imported.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>@BLIGHTSOURCES</td>
 *         <td>
 *             Declarations for the lighting functions and light sources.
 *             These are automatically generated by GearVRF from the
 *             vertex and fragment shaders from the Java implementation
 *             of each light source.
 *         </td>
 *     </tr>
 * </table>
 *
 * @see GVRPhongShader
 * @see GVRLight
 */
public class GVRShaderTemplate extends GVRShader
{
    private final static String TAG = "GVRShaderTemplate";
    // Keeping the start of shadow attribute from 20 since locations less than it are used up by vertex descriptor and texture coords.
    private final int shadowmapStartLocation = 20;

    protected class LightClass
    {
        public LightClass()
        {
            Count = 1;
            MaxQuality = 0;
            PixelLights = 0;
            FragmentUniforms = "";
            VertexOutputs = null;
            FragmentShader = null;
            VertexShader = null;
        }
        public Integer Count;
        public Integer PixelLights;
        public Integer MaxQuality;
        public String FragmentUniforms;
        public String VertexShader;
        public String VertexOutputs;
        public String FragmentShader;
        public String VertexDescriptor;
    };

    /**
     * Construct a shader template for a shader using GLSL version 100.
     * To make a shader for another version use the other form of the constructor.
     *
     * @param uniformDescriptor string describing uniform names and types
     *                          e.g. "float4 diffuse_color, float4 specular_color, float specular_exponent"
     * @param textureDescriptor string describing texture names and types
     *                          e.g. "sampler2D diffuseTexture, sampler2D specularTexture"
     * @param vertexDescriptor  string describing vertex attributes and types
     *                          e.g. "float3 a_position, float2 a_texcoord"
     */
    public GVRShaderTemplate(String uniformDescriptor, String textureDescriptor, String vertexDescriptor)
    {
        super(uniformDescriptor, textureDescriptor, vertexDescriptor);
        mHasVariants = true;
    }

    /**
     * Construct a shader template
     *
     * @param uniformDescriptor string describing uniform names and types
     *                          e.g. "float4 diffuse_color, float4 specular_color, float specular_exponent"
     * @param textureDescriptor string describing texture names and types
     *                          e.g. "sampler2D diffuseTexture, sampler2D specularTexture"
     * @param vertexDescriptor  string describing vertex attributes and types
     *                          e.g. "float3 a_position, float2 a_texcoord"
     * @param glslVersion
     *            GLSL version (e.g. GLSLESVersion.V300)
     */
    public GVRShaderTemplate(String uniformDescriptor, String textureDescriptor, String vertexDescriptor, GLSLESVersion glslVersion)
    {
        super(uniformDescriptor, textureDescriptor, vertexDescriptor, glslVersion);
        mHasVariants = true;
    }

    /**
     * Attach a named shader segment.
     *
     * Shader segment names should start with either "Vertex" or "Fragment" to
     * designate which type of shader the code belongs with. Both vertex and
     * fragment shaders can have more than one code segment contribute to the
     * final shader.
     *
     * @param segmentName
     *            name associated with shader segment
     * @param shaderSource
     *            String with shader source code
     */
    protected void setSegment(String segmentName, String shaderSource)
    {
        super.setSegment(segmentName, shaderSource);
        if (shaderSource == null)
        {
            return;
        }
        Pattern pattern = Pattern.compile("HAS_([a-zA-Z0-9_]+)");
        Matcher matcher = pattern.matcher(shaderSource);
        if (mShaderDefines == null) mShaderDefines = new HashSet<String>();
        int index = 0;
        while (((index = shaderSource.indexOf("HAS_", index)) >= 0) && matcher.find(index))
        {
            String match = matcher.group(1);
            mShaderDefines.add(match);
            index = matcher.end();
        }
    }

    /**
     * Create a unique signature for the lights used by this shader variant.
     * The signature will include the names of the light source classes and
     * how many times each is used in the shader.
     *
     * @param lightlist
     *            list of lights used with this shader
     * @return light string signature for shader
     */
    protected String generateLightSignature(GVRLight[] lightlist)
    {
        String sig = "";
        String lightClass = null;
        Integer numLights = 0;
        Integer pixelLights = 0;

        if (lightlist == null)
        {
            return sig;
        }
        for (GVRLight light : lightlist)
        {
            String s = light.getClass().getSimpleName();

            if (light.getQuality() > 1)
            {
                ++pixelLights;
            }
            if (s.equals(lightClass))
            {
                ++numLights;
            }
            else
            {
                lightClass = s;
                sig += "$" + lightClass;
                numLights = 1;
            }
        }
        if (numLights > 0)
        {
            sig += numLights;
        }
        return sig.trim();
    }

    /**
     * Generates the set of unique parameter names that make a particular
     * variant of the shader from the source template. Wherever the source
     * template contains "HAS_" followed by a name of a uniform, texture or
     * attribute used in the material or mesh, a "#define" for that name is
     * generated.
     *
     * @param definedNames
     *            set with defined names for this shader
     * @param vertexDesc
     *            String with vertex attributes, null to ignore them
     * @param material
     *            material used with this shader (may not be null)
     * @return shader signature string with names actually defined by the material and mesh
     */
    protected String generateVariantDefines(HashMap<String, Integer> definedNames, String vertexDesc, GVRShaderData material)
    {
        String signature = getClass().getSimpleName();

        for (String name : mShaderDefines)
        {
            if (definedNames.containsKey(name))
            {
                Integer value = definedNames.get(name);
                if (value != 0)
                {
                    signature += "$" + name;
                }
                continue;
            }
            if (material.hasUniform(name))
            {
                definedNames.put(name, 1);
                signature += "$" + name;
            }
            else if ((vertexDesc != null) && vertexDesc.contains(name))
            {
                definedNames.put(name, 1);
                if (!signature.contains(name))
                    signature += "$" + name;
            }
            else if (material.getTexture(name) != null)
            {
                definedNames.put(name, 1);
                signature += "$" + name;
                String attrname = material.getTexCoordAttr(name);
                if ((attrname != null) && !attrname.equals("a_texcoord"))
                {
                    signature += "-" + "#" + attrname + "#";
                }
            }
        }
        return signature;
    }

    protected void updateDescriptors(GVRShaderData material, String meshDesc,
                                     StringBuilder uniformDesc, StringBuilder textureDesc, StringBuilder vertexDesc)
    {
        Pattern pattern = Pattern.compile("([a-zA-Z0-9]+)[ \t]+([a-zA-Z0-9_]+)[^ ]*");
        Matcher matcher = pattern.matcher(mTextureDescriptor);
        String name;
        String type;

        while (matcher.find())
        {
            type = matcher.group(1);
            name = matcher.group(2);
            {
                textureDesc.append(type);
                textureDesc.append(' ');
                if (!material.hasTexture(name))
                {
                    textureDesc.append('!');
                }
                textureDesc.append(name);
                textureDesc.append(' ');
            }
        }
        matcher = pattern.matcher(mUniformDescriptor);
        while (matcher.find())
        {
            type = matcher.group(1);
            name = matcher.group(2);
            uniformDesc.append(type);
            uniformDesc.append(' ');
            if (!material.hasUniform(name))
            {
                uniformDesc.append('!');
            }
            uniformDesc.append(name);
            uniformDesc.append(' ');
        }
        if (meshDesc != null)
        {
            matcher = pattern.matcher(mVertexDescriptor);
            while (matcher.find())
            {
                type = matcher.group(1);
                name = matcher.group(2);
                vertexDesc.append(type);
                vertexDesc.append(' ');
                if (!meshDesc.contains(name))
                {
                    vertexDesc.append('!');
                }
                vertexDesc.append(name);
                vertexDesc.append(' ');

            }
        }
    }


    /**
     * Construct the source code for a GL shader based on the input defines. The
     * shader segments attached to slots that start with <type> are combined to
     * form the GL shader. #define statements are added to define compile-time
     * constants to control the code generated.
     *
     * @param type
     *            "Fragment" or "Vertex" indicating shader type.
     * @param definedNames
     *            set of names to define for this shader.
     * @param scene
     *            scene being rendered
     * @param lightClasses
     *            map of existing light classes used in scene
     * @param material
     *            GVRMaterial shader is being used with
     * @return GL shader code with parameters substituted.
     */
    private String generateShaderVariant(String type, HashMap<String, Integer> definedNames, GVRScene scene, Map<String, LightClass> lightClasses, GVRShaderData material)
    {
        String template = getSegment(type + "Template");
        StringBuilder shaderSource = new StringBuilder();

        if (template == null)
        {
            throw new IllegalArgumentException(type + "Template segment missing - cannot make shader");
        }
        boolean useLights = lightClasses != null;
        String lightShaderSource = "";

        String combinedSource = replaceTransforms(template, useLights);
        shaderSource.append("#version " + mGLSLVersion.toString() + "\n");
        if (definedNames.containsKey("LIGHTSOURCES") &&
            definedNames.get("LIGHTSOURCES") == 0)
        {
            useLights = false;
        }
        if (useLights)
        {
            if (type.equals("Vertex"))
            {
                lightShaderSource = generateLightVertexShader(scene, lightClasses);
            }
            else
            {
                lightShaderSource = generateLightFragmentShader(scene, lightClasses);
            }
            shaderSource.append("#define HAS_LIGHTSOURCES 1\n");
        }

        for (Map.Entry<String, String> entry : mShaderSegments.entrySet())
        {
            String key = entry.getKey();
            if (key.startsWith(type))
            {
                String segmentSource = entry.getValue();
                if (segmentSource == null)
                    segmentSource = "";
                else if (!definedNames.containsKey(key) ||
                        (definedNames.get(key) != 0))
                {
                    shaderSource.append("#define HAS_" + key + " 1\n");
                }
                combinedSource = combinedSource.replace("@" + key, segmentSource);
            }
        }

        combinedSource = combinedSource.replace("@ShaderName", getClass().getSimpleName());
        combinedSource = combinedSource.replace("@LIGHTSOURCES", lightShaderSource);
        combinedSource = combinedSource.replace("@MATERIAL_UNIFORMS", material.makeShaderLayout());
        combinedSource = combinedSource.replace("@BONES_UNIFORMS", GVRShaderManager.makeLayout(sBonesDescriptor, "Bones_ubo", true));
        if (type.equals("Vertex"))
        {
            String texcoordSource = assignTexcoords(material);
            if (texcoordSource.length() > 0)
            {
                shaderSource.append("#define HAS_TEXCOORDS 1\n");
            }
            combinedSource = combinedSource.replace("@TEXCOORDS", texcoordSource);
        }
        for (Map.Entry<String, Integer> entry : definedNames.entrySet())
        {
            if (entry.getValue() != 0)
                shaderSource.append("#define HAS_" + entry.getKey() + " 1\n");
        }
        shaderSource.append(combinedSource);
        return shaderSource.toString();
    }

    /**
     * Generate the vertex shader assignments to copy texture
     * coordinates from the vertex array to shader variables.
     * @param mtl GVRMaterial being used with this shader.
     * @return shader code to assign texture coordinates
     */
    private String assignTexcoords(GVRShaderData mtl)
    {
        Set<String> texnames = mtl.getTextureNames();
        String shadercode = "";
        for (String name : texnames)
        {
            String texCoordAttr = mtl.getTexCoordAttr(name);
            String shaderVar = mtl.getTexCoordShaderVar(name);
            if (texCoordAttr != null)
            {
                shadercode += "    " + shaderVar + " = " + texCoordAttr + ";\n";
            }
        }
        return shadercode;
    }

    /**
     * Select the specific vertex and fragment shader to use.
     *
     * The shader template is used to generate the sources for the vertex and
     * fragment shader based on the vertex, material and light properties. This
     * function may compile the shader if it does not already exist.
     *
     * @param context
     *            GVRContext
     * @param rdata
     *            renderable entity with mesh and material
     * @param scene
     *            scene being rendered
     */
    public int bindShader(GVRContext context, IRenderable rdata, GVRScene scene, boolean isMultiview)
    {
        GVRMesh mesh = rdata.getMesh();
        GVRShaderData material = rdata.getMaterial();
        GVRLight[] lightlist = (scene != null) ? scene.getLightList() : null;
        HashMap<String, Integer> variantDefines = getRenderDefines(rdata, scene);
        boolean useLights = usesLights() && variantDefines.containsKey("LIGHTSOURCES") && (variantDefines.get("LIGHTSOURCES") != 0);

        if (isMultiview)
            variantDefines.put("MULTIVIEW", 1);
        else
            variantDefines.put("MULTIVIEW", 0);

        String meshDesc = mesh.getVertexBuffer().getDescriptor();
        String signature = generateVariantDefines(variantDefines, meshDesc, material);
        if (useLights)
        {
            signature += generateLightSignature(lightlist);
        }
        GVRShaderManager shaderManager = context.getShaderManager();
        int nativeShader = shaderManager.getShader(signature);

        synchronized (shaderManager)
        {
            if (nativeShader == 0)
            {
                Map<String, LightClass> lightClasses = useLights ? scanLights(lightlist) : null;

                String vertexShaderSource = generateShaderVariant("Vertex", variantDefines,
                                                                  scene, lightClasses, material);
                String fragmentShaderSource = generateShaderVariant("Fragment", variantDefines,
                                                                    scene, lightClasses, material);
                StringBuilder uniformDescriptor = new StringBuilder();
                StringBuilder textureDescriptor = new StringBuilder();
                StringBuilder vertexDescriptor = new StringBuilder();
                updateDescriptors(material, meshDesc, uniformDescriptor, textureDescriptor, vertexDescriptor);
                nativeShader = shaderManager.addShader(signature, uniformDescriptor.toString(),
                                                       textureDescriptor.toString(),
                                                       vertexDescriptor.toString(),
                                                       vertexShaderSource, fragmentShaderSource,
                                                       getMatrixCalc(useLights));
                if (mWriteShadersToDisk)
                {
                    writeShader("V-" + signature + ".glsl", vertexShaderSource);
                    writeShader("F-" + signature + ".glsl", fragmentShaderSource);
                }
                Log.i(TAG, "SHADER: generated shader #%d %s", nativeShader, signature);
            }
            else
            {
                //Log.i(TAG, "SHADER: found shader #%d %s", nativeShader, signature);
            }
            if (nativeShader > 0)
            {
                rdata.setShader(nativeShader, isMultiview);
            }
            return nativeShader;
        }
    }


    /**
     * Select the specific vertex and fragment shader to use with this material.
     * 
     * The shader template is used to generate the sources for the vertex and
     * fragment shader based on the material properties only.
     * It will ignore the mesh attributes and all lights.
     * 
     * @param context       GVRContext
     * @param material      material to use with the shader
     * @param meshDesc      string with vertex descriptor
     */
    public int bindShader(GVRContext context, GVRShaderData material, String meshDesc)
    {
        HashMap<String, Integer> variantDefines = new HashMap<String, Integer>();
        String signature = generateVariantDefines(variantDefines, meshDesc, material);
        GVRShaderManager shaderManager = context.getShaderManager();
        int nativeShader = shaderManager.getShader(signature);
        boolean useLights = usesLights() && variantDefines.containsKey("LIGHTSOURCES") && (variantDefines.get("LIGHTSOURCES") != 0);

        synchronized (shaderManager)
        {
            if (nativeShader == 0)
            {
                String vertexShaderSource =
                        generateShaderVariant("Vertex", variantDefines, null, null, material);
                String fragmentShaderSource =
                        generateShaderVariant("Fragment", variantDefines, null, null, material);
                StringBuilder uniformDescriptor = new StringBuilder();
                StringBuilder textureDescriptor = new StringBuilder();
                StringBuilder vertexDescriptor = new StringBuilder();

                updateDescriptors(material, meshDesc, uniformDescriptor, textureDescriptor, vertexDescriptor);
                nativeShader = shaderManager.addShader(signature, uniformDescriptor.toString(),
                                                       textureDescriptor.toString(), mVertexDescriptor,
                                                       vertexShaderSource, fragmentShaderSource,
                                                       getMatrixCalc(useLights));
                if (mWriteShadersToDisk)
                {
                    writeShader("V-" + signature + ".glsl", vertexShaderSource);
                    writeShader("F-" + signature + ".glsl", fragmentShaderSource);
                }
                Log.i(TAG, "SHADER: generated shader #%d %s", nativeShader, signature);
            }
            else
            {
                //Log.i(TAG, "SHADER: found shader #%d %s", nativeShader, signature);
            }
            return nativeShader;
        }
    }

    /**
     * Generate shader-specific defines from the rendering information.
     * You can override this function in your shader class to change which
     * variant is generated depending on the GVRRenderData settings.
     * 
     * The base implementation LIGHTSOURCES as 0 if lighting is not enabled by the render data,
     * and it defines SHADOWS as 1 if any light source enables shadow casting. 
     * 
     * @param renderable object being rendered by this shader
     * @param scene scene being rendered
     * @return list of symbols to be defined (value 1) or undefined (value 0) in the shader
     * 
     * @see GVRLight#setCastShadow(boolean) setCastShadow
     */
    public HashMap<String, Integer> getRenderDefines(IRenderable renderable, GVRScene scene) {
        HashMap<String, Integer> defines = new HashMap<String, Integer>();
        int castShadow = 0;
        GVRLight[] lights = (scene != null) ? scene.getLightList() : null;

        if (renderable.getGVRContext().getActivity().getAppSettings().isMultiviewSet())
        {
            defines.put("MULTIVIEW", 1);
        }
        if ((lights == null) ||
            (lights.length == 0) ||
            !renderable.getMesh().hasAttribute("a_normal") ||
            !renderable.isLightEnabled())
        {
            defines.put("LIGHTSOURCES", 0);
            defines.put("a_normal", 0);
            return defines;
        }
        defines.put("LIGHTSOURCES", 1);
        for (GVRLight light : lights)
        {
            if (light.getCastShadow())
            {
                castShadow = 1;
            }
        }
        defines.put("SHADOWS", castShadow);
        return defines;
    }


    /**
     * Generates the shader code to compute fragment lighting for each light source.
     * The fragment shader defines a <LightPixel> function which computes the effect of all
     * light sources on the fragment color. This function calls the
     * <AddLight> function which integrates the light sources. This function
     * is defined in the fragment shader template.
     *
     * This functon emits specific code for individual lights but will
     * produce a loop if there is more than one light of a specific type.
     *
     * @param lightClasses
     *            list of light classes generated by scanLights
     * @return string with shader source code for fragment lighting
     */
    private String generateLightFragmentShader(GVRScene scene, Map<String, LightClass> lightClasses)
    {
        StringBuilder lightFunction = new StringBuilder("");
        StringBuilder lightDefs = new StringBuilder("");
        StringBuilder lightShaders = new StringBuilder("");
        lightFunction.append("\nvoid LightPixel(Surface surface)\n{\n");

        for (Map.Entry<String, LightClass> entry : lightClasses.entrySet())
        {
            LightClass lclass = entry.getValue();
            String lightType = entry.getKey();

            lightShaders.append(lclass.FragmentShader);
            lightShaders.append('\n');
            lightDefs.append(lclass.FragmentUniforms);
            lightDefs.append("\n#define ");
            lightDefs.append(entry.getKey());
            lightDefs.append("_MAXQUALITY ");
            lightDefs.append(lclass.MaxQuality);
            lightDefs.append("\n#define ");
            lightDefs.append(entry.getKey());
            lightDefs.append("_TOTAL_LIGHTS ");
            lightDefs.append(lclass.Count);
            lightDefs.append("\n#define ");
            lightDefs.append(entry.getKey());
            lightDefs.append("_PIXEL_LIGHTS ");
            lightDefs.append(lclass.Count);
            lightDefs.append("\n#define ");
            lightDefs.append(entry.getKey());
            lightDefs.append("_VERTEX_LIGHTS ");
            lightDefs.append(lclass.Count - lclass.PixelLights);
            lightDefs.append("\n\n");
            if (lclass.VertexDescriptor != null)
            {
                String vertexOutputs = lclass.VertexOutputs.replace("$PREFIX", "layout(location = 20) in ");
                lightDefs.append(vertexOutputs.replace("$COUNT", lclass.Count.toString()));
            }
            lightDefs.append('\n');
            lightFunction.append("Fragment" + lightType + "(surface);\n");
        }
        lightFunction.append("}\n\n");
        return lightDefs + GVRLight.makeShaderBlock(scene) + lightShaders + lightFunction;
    }

    /**
     * Generates the shader code to compute vertex lighting for each light source.
     * The vertex shader defines a "LightVertex" function which computes
     * vertex output information for each light. The generated shader emits
     * a loop for each different type of light.
     *
     * @param lightClasses
     *            list of light classes generated by scanLights
     * @return string with shader source code for vertex lighting
     */
    private String generateLightVertexShader(GVRScene scene, Map<String, LightClass> lightClasses)
    {
        StringBuilder lightDefs = new StringBuilder("");
        StringBuilder lightFunction = new StringBuilder("");
        StringBuilder lightShaders = new StringBuilder("");

        lightFunction.append("\nvoid LightVertex(Vertex vertex)\n{\n");
        for (Map.Entry<String, LightClass> entry : lightClasses.entrySet())
        {
            LightClass lclass = entry.getValue();
            String vertexOutputs = lclass.VertexOutputs;

            if (lclass.VertexShader != null)
            {
                lightShaders.append(lclass.VertexShader);
                lightShaders.append('\n');
            }
            lightDefs.append(lclass.FragmentUniforms);
            lightDefs.append("\n#define ");
            lightDefs.append(entry.getKey());
            lightDefs.append("_MAXQUALITY ");
            lightDefs.append(lclass.MaxQuality);
            lightDefs.append("\n#define ");
            lightDefs.append(entry.getKey());
            lightDefs.append("_TOTAL_LIGHTS ");
            lightDefs.append(lclass.Count);
            lightDefs.append("\n#define ");
            lightDefs.append(entry.getKey());
            lightDefs.append("_PIXEL_LIGHTS ");
            lightDefs.append(lclass.Count);
            lightDefs.append("\n#define ");
            lightDefs.append(entry.getKey());
            lightDefs.append("_VERTEX_LIGHTS ");
            lightDefs.append(lclass.Count - lclass.PixelLights);
            lightDefs.append("\n\n");
            if (vertexOutputs != null)
            {
                vertexOutputs = vertexOutputs.replace("$PREFIX", "\nlayout(location = 20) out ");
                lightDefs.append(vertexOutputs.replace("$COUNT", lclass.Count.toString()));
            }
            lightFunction.append("   Vertex" + entry.getKey() + "(vertex);\n");
        }
        lightFunction.append("}\n");
        return lightDefs + GVRLight.makeShaderBlock(scene) + lightShaders + lightFunction;
    }

    private Map<String, LightClass> scanLights(GVRLight[] lightlist)
    {
        Map<String, LightClass> lightClasses = new HashMap<String, LightClass>();

        if ((lightlist == null) || (lightlist.length == 0))
        {
            return lightClasses;
        }
        for (GVRLight light : lightlist)
        {
            String lightShader = light.getFragmentShaderSource();
            String lightType = light.getLightClass();
            LightClass lightClass = lightClasses.get(lightType);

            if (lightClass != null)
            {
                ++(lightClass.Count);
            }
            else
            {
                lightClass = new LightClass();
                lightClass.FragmentShader = lightShader.replace("@LightType", lightType);
                lightClass.FragmentUniforms = makeUniformStruct(light);
                lightClasses.put(lightType, lightClass);
                lightClass.VertexDescriptor = light.getVertexDescriptor();
                if (lightClass.VertexDescriptor != null)
                {
                    lightClass.VertexOutputs = makeVertexOutputs(light);
                }
                if (light.getVertexShaderSource() != null)
                {
                    lightClass.VertexShader = light.getVertexShaderSource().replace("@LightType", lightType);
                }
                if (light.getQuality() > lightClass.MaxQuality)
                {
                    lightClass.MaxQuality = light.getQuality();
                }
            }
            if (light.getQuality() > 1)
            {
                ++(lightClass.PixelLights);
            }
        }
        return lightClasses;
    }

    private String makeUniformStruct(GVRLight light)
    {
        String structDesc = "struct U" + light.getLightClass() + "\n{\n";
        structDesc += light.makeShaderLayout();
        structDesc += "};\n";
        return structDesc;
    }

    private String makeVertexOutputs(GVRLight light)
    {
        String lightClassName = light.getLightClass();
        Pattern pattern = Pattern.compile("[ ]*([a-zA-Z0-9_]+)[ ]+([A-Za-z0-9_]+)[,;:]*");
        Matcher matcher = pattern.matcher(light.getVertexDescriptor());
        String desc = "";
        while (matcher.find())
        {
            String name = matcher.group(2);
            String type = matcher.group(1);

            type = light.getShaderType(type);
            desc += "$PREFIX" + type + " " + lightClassName + "_" + name + "[$COUNT];\n";
        }
        return desc;
    }

    protected Set<String> mShaderDefines;
}
