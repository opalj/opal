/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
import scala.xml.Text
import scala.xml.Comment

import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.JsNull

import org.opalj.collection.mutable.Locals
import org.opalj.br.PC
import org.opalj.br.Code

/**
 * @param   localVariables The register values at the given location.
 */
class LocalVariables(
        val code:           Code,
        val pc:             PC,
        val localVariables: Locals[_ <: AnyRef]
) extends IssueDetails {

    def toXHTML(basicInfoOnly: Boolean): Node = {
        val localVariableDefinitions = code.localVariablesAt(pc)
        if (localVariableDefinitions.isEmpty)
            return Comment("local variable information are not found in the class file");

        if (basicInfoOnly)
            return Text("");

        val sortedLVDefs = localVariableDefinitions.toSeq.sortWith((a, b) ⇒ a._1 < b._1)
        val lvsAsXHTML =
            for ((index, theLV) ← sortedLVDefs) yield {
                val localValue = localVariables(index)
                val localValueAsXHTML =
                    if (localValue == null)
                        <span class="warning">unused</span>
                    else
                        Text(localVariableToString(theLV, localValue))

                <tr>
                    <td>{ index }</td><td>{ theLV.name }</td><td>{ localValueAsXHTML }</td>
                </tr>
            }

        <details class="locals">
            <summary>Local Variable State</summary>
            <table>
                <tr><th>Index</th><th>Name</th><th>Value</th></tr>
                { lvsAsXHTML }
            </table>
        </details>

    }

    def toAnsiColoredString: String = "" // TODO Support a better representation

    def toEclipseConsoleString: String = "" // TODO Support a better representation

    override def toIDL: JsValue = {
        val lvDefs = code.localVariablesAt(pc)
        val lvValues = lvDefs.toSeq.sortWith((a, b) ⇒ a._1 < b._1).map { e ⇒
            val (index, localVariable) = e
            val localValue = localVariables(index)
            Json.obj(
                "name" → localVariable.name,
                "value" → {
                    if (localValue == null)
                        JsNull
                    else
                        localVariableToString(localVariable, localValue)
                }
            )
        }
        Json.obj("type" → "LocalVariables", "values" → lvValues)
    }
}
