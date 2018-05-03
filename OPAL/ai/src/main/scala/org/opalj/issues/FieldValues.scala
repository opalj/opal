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

import org.opalj.collection.immutable.Chain
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.ai.AIResult
import org.opalj.br.PCAndAnyRef

/**
 * Describes an issue related to the value of a field.
 *
 * @author Michael Eichberg
 */
class FieldValues(
        val method: Method,
        val result: AIResult
) extends IssueDetails with MethodComprehension {

    final def classFile: ClassFile = method.classFile

    private[this] implicit def code = result.code

    private[this] def operandsArray = result.operandsArray

    def collectReadFieldValues: Chain[PCAndAnyRef[String]] = {
        code.foldLeft(Chain.empty[PCAndAnyRef[String]]) { (readFields, pc, instruction) ⇒
            instruction match {
                case fra @ FieldReadAccess(_ /*decl.ClassType*/ , _ /* name*/ , fieldType) if {
                    val nextPC = fra.indexOfNextInstruction(pc)
                    val operands = operandsArray(nextPC)
                    operands != null &&
                        operands.head.isMorePreciseThan(result.domain.TypedValue(pc, fieldType))
                } ⇒
                    PCAndAnyRef(
                        pc,
                        s"${operandsArray(fra.indexOfNextInstruction(pc)).head} ← $fra"
                    ) :&: readFields
                case _ ⇒
                    readFields
            }
        }
    }

    def toXHTML(basicInfoOnly: Boolean): Node = {
        import PCLineComprehension.{pcNode, lineNode, line}
        val readFieldValues =
            collectReadFieldValues map { fieldData ⇒
                val pc = fieldData.pc
                val details = fieldData.value
                <li>
                    { pcNode(classFileFQN, methodJVMSignature, pc) }
                    &nbsp;
                    { lineNode(classFileFQN, methodJVMSignature, pc, line(pc)) }
                    <span class="value">{ details }</span>
                </li>
            }

        if (readFieldValues.nonEmpty)
            <details class="field_values">
                <summary>Read Field Value Information</summary>
                <ul>{ readFieldValues }</ul>
            </details>
        else
            Group(Nil)
    }

    def toAnsiColoredString: String = "" // TODO Support a better representation

    def toEclipseConsoleString: String = "" // TODO Support a better representation

    override def toIDL: JsValue = {
        import PCLineComprehension.line

        Json.obj(
            "type" → "FieldValues",
            "values" → collectReadFieldValues.map { fieldData ⇒
                val pc = fieldData.pc
                val details = fieldData.value

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
