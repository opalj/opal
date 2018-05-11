/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package issues

import scala.xml.Node
import scala.xml.Group
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import org.opalj.br.Method
import org.opalj.br.ClassFile
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.ai.AIResult
import org.opalj.br.PCAndAnyRef
import org.opalj.collection.immutable.Chain

class MethodReturnValues(
        val method: Method,
        val result: AIResult
) extends IssueDetails with MethodComprehension {

    final def classFile: ClassFile = method.classFile

    private[this] implicit def code = result.code

    private[this] def operandsArray = result.operandsArray

    def collectMethodReturnValues: Chain[PCAndAnyRef[String]] = {
        code.foldLeft(Chain.empty[PCAndAnyRef[String]]) { (returnValues, pc, instruction) ⇒
            instruction match {
                case instr @ MethodInvocationInstruction(declaringClassType, _, name, descriptor) if !descriptor.returnType.isVoidType && {
                    val nextPC = instr.indexOfNextInstruction(pc)
                    val operands = operandsArray(nextPC)
                    operands != null &&
                        operands.head.isMorePreciseThan(result.domain.TypedValue(pc, descriptor.returnType))
                } ⇒
                    val modifier = if (instr.isInstanceOf[INVOKESTATIC]) "static " else ""
                    val nextPCOperandHead = operandsArray(instr.indexOfNextInstruction(pc)).head

                    PCAndAnyRef(
                        pc,
                        s"$nextPCOperandHead ← ${declaringClassType.toJava}{ $modifier ${descriptor.toJava(name)} }"
                    ) :&: returnValues

                case _ ⇒ returnValues
            }

        }
    }

    def toXHTML(basicInfoOnly: Boolean): Node = {
        import PCLineComprehension.{pcNode, lineNode, line}
        val methodReturnValues =
            collectMethodReturnValues.map { methodData ⇒
                val pc = methodData.pc
                val details = methodData.value
                <li>
                    { pcNode(classFileFQN, methodJVMSignature, pc) }
                    &nbsp;
                    { lineNode(classFileFQN, methodJVMSignature, pc, line(pc)) }
                    <span class="value">{ details }</span>
                </li>
            }

        if (methodReturnValues.nonEmpty)
            <details class="method_return_values">
                <summary>Method Return Values</summary>
                <ul>{ methodReturnValues }</ul>
            </details>
        else
            Group(Nil)
    }

    def toAnsiColoredString: String = "" // TODO Support a better representation

    def toEclipseConsoleString: String = "" // TODO Support a better representation

    override def toIDL: JsValue = {
        import PCLineComprehension.line

        Json.obj(
            "type" → "MethodReturnValues",
            "values" → collectMethodReturnValues.map { methodData ⇒
                val pc = methodData.pc
                val details = methodData.value

                Json.obj(
                    "classFileFQN" → classFileFQN,
                    "methodJVMSignature" → methodJVMSignature,
                    "pc" → pc,
                    "line" → line(pc),
                    "details" → details
                )
            }.toIterable
        )
    }
}
