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
#include <cstring>
#include "engine/renderer/render_state.h"
#include "matrix_calc.h"

#define OUTPUT_OFFSET 10
namespace gvr
{

    const char* MatrixCalc::mInputMatrixNames[] =
    {
        "left_view_proj", "right_view_proj",
        "projection",
        "left_view", "right_view",
        "inverse_left_view", "inverse_right_view",
        "model",
        "left_mvp", "right_mvp",
        "output0", "output1", "output2", "output3",
        "output4", "output5", "output6", "output7",
        "output8", "output9"
    };

    MatrixCalc::MatrixCalc(const char* expressions)
    {
        int n;
        const char* p = expressions;
        while (*p)
        {
            ExprNode* root = nullptr;
            n = compile(&root, p);
            if (root && (n >= 0))
            {
                mExprTrees.push_back(root);
                p += n;
            }
            else
            {
                LOGE("MatrixCalc: ERROR: bad expression syntax");
                return;
            }
        }
    }


    int MatrixCalc::parseOperand(ExprNode** root, const char* expr)
    {
        const char* p = expr;

        for (int i = 0; i < sizeof(mInputMatrixNames) / sizeof(const char*); ++i)
        {
            const char* name = mInputMatrixNames[i];
            int len = strlen(name);
            if (strncmp(p, name, len) == 0)
            {
                NodeType type = InputOperand;
                int offset = i;
                if (i > OUTPUT_OFFSET)
                {
                    type = OutputOperand;
                    offset -= OUTPUT_OFFSET;
                }
                ExprNode* operand = new ExprNode(type);
                operand->MatrixOffset = offset;
                *root = operand;
                return len;
            }
        }
        return 0;
    }

    int MatrixCalc::compile(ExprNode** root, const char* expression)
    {
        const char* p = expression;
        ExprNode* prevNode = nullptr;
        ExprNode* curNode = nullptr;
        NodeType curType = None;
        int opIndex = 0;

        while (*p)
        {
            if (isspace(*p))
            {
                ++p;
                continue;
            }
            if (curType < Unary)
            {
                int consumed = 0;
                if (*p == '(')
                {
                    consumed = compile(&curNode, ++p);
                }
                else if (isalpha(*p))
                {
                    consumed = parseOperand(&curNode, p);
                }
                p += consumed;
            }
            while (isspace(*p))
            {
                ++p;
            }
            if (*p == 0)
            {
                break;
            }
            if (strchr(");,", *p))
            {
                ++p;
                break;
            }
            if (strchr("*+-^~", *p))
            {
                switch (*p++)
                {
                    case '*': curType = Multiply; break;
                    case '+': curType = Add; break;
                    case '-': curType = Subtract; break;
                    case '^': curType = Transpose; break;
                    case '~': curType = Invert; break;
                }
                prevNode = curNode;
                curNode = new ExprNode(curType);
                if (prevNode)
                {
                    if (prevNode->Type > curNode->Type)
                    {
                        curNode->Operand[opIndex] = prevNode;
                        prevNode = curNode;
                    }
                    else
                    {
                        curNode->Operand[opIndex] = prevNode->Operand[1];
                        prevNode->Operand[1] = curNode;
                    }
                    opIndex = 1;
                    curType = prevNode->Type;
                }
            }
        }
        *root = curNode;
        if (prevNode && (prevNode != curNode))
        {
            if (prevNode->Type > curNode->Type)
            {
                curNode->Operand[opIndex] = prevNode;
                prevNode = curNode;
            }
            else
            {
                curNode->Operand[opIndex] = prevNode->Operand[1];
                prevNode->Operand[1] = curNode;
            }
            *root = prevNode;
        }
        return p - expression;
    }

    bool MatrixCalc::calculate(const glm::mat4* inputMatrices, glm::mat4* outputMatrices)
    {
        mInputMatrices = inputMatrices;
        mOutputMatrices = outputMatrices;
        int offset = 0;
        for (auto it = mExprTrees.begin(); it != mExprTrees.end(); ++it)
        {
            ExprNode* root = *it;
            if (!eval(root, outputMatrices[offset]))
            {
                return false;
            }
            ++offset;
        }
        return true;
    }

    bool MatrixCalc::eval(ExprNode* node, glm::mat4& result)
    {
        glm::mat4 firstOperand;
        glm::mat4 secondOperand;

        if (node->Type == InputOperand)
        {
            result = mInputMatrices[node->MatrixOffset];
            return true;
        }
        if (node->Type == OutputOperand)
        {
            result = mOutputMatrices[node->MatrixOffset];
            return true;
        }
        if (node->Operand[0])
        {
            if (!eval(node->Operand[0], firstOperand))
            {
                return false;
            }
        }
        if (node->Type == Invert)
        {
            result = glm::inverse(firstOperand);
            return true;
        }
        if (node->Type == Transpose)
        {
            result = glm::transpose(firstOperand);
            return true;
        }
        if (node->Operand[1])
        {
            if (!eval(node->Operand[1], secondOperand))
            {
                return false;
            }
        }
        switch (node->Type)
        {
            case Add: result = firstOperand + secondOperand; break;
            case Subtract: result = firstOperand - secondOperand; break;
            case Multiply: result = firstOperand * secondOperand; break;
            default: return false;
        }
        return true;
    }
}