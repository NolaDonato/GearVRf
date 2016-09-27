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

public class GVRColorShader extends GVRShader
{
    private String vertexShader =
            "attribute vec3 a_position;\n" +
            "attribute vec3 a_normal;\n" +
            "uniform mat4 u_mvp;\n" +
            "void main() {\n" +
            "  gl_Position = u_mvp * vec4(a_position, 1);\n" +
            "}\n";

    private String fragmentShader =
        "uniform vec4 u_color;\n" +
        "void main()\n" +
        "{\n" +
        "  gl_FragColor = u_color;" +
        "}\n";

    public GVRColorShader()
    {
        super("float4 u_color");
        setSegment("FragmentTemplate", fragmentShader);
        setSegment("VertexTemplate", vertexShader);
    }
}