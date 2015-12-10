/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package bugpicker
package core
package reporting

import scala.xml.Node
import scala.xml.Text
import org.opalj.ai.domain.l1.IntegerRangeValues

/**
 * Common functionality to create XML/HTML reports.
 */
object XMLReporter {

    def localVariablesToXHTML(issue: Issue): Option[Node] = {
        if (issue.pc.isEmpty || issue.code.isEmpty)
            return None;

        val pc = issue.pc.get
        val code = issue.code.get

        if (issue.localVariables.isEmpty) {
            return None;
        }
        val localVariableValues = issue.localVariables.get

        val localVariableDefinitions = code.localVariablesAt(pc)
        if (localVariableDefinitions.isEmpty)
            return None;

        val lvsAsXHTML =
            for ((index, theLV) ← localVariableDefinitions.toSeq.sortWith((a, b) ⇒ a._1 < b._1)) yield {
                val localValue = localVariableValues(index)
                val localValueAsXHTML =
                    if (localValue == null)
                        <span class="warning">unused</span>
                    else {

                        if ((theLV.fieldType eq org.opalj.br.BooleanType) &&
                            // SPECIAL HANDLING IF THE VALUE IS AN INTEGER RANGE VALUE
                            localValue.isInstanceOf[IntegerRangeValues#IntegerRange]) {
                            val range = localValue.asInstanceOf[IntegerRangeValues#IntegerRange]
                            if (range.lowerBound == 0 && range.upperBound == 0)
                                Text("false")
                            else if (range.lowerBound == 1 && range.upperBound == 1)
                                Text("true")
                            else
                                Text("true or false")
                        } else
                            Text(localValue.toString)
                    }

                <tr>
                    <td>{ index }</td><td>{ theLV.name }</td><td>{ localValueAsXHTML }</td>
                </tr>
            }

        Some(
            <details class="locals">
                <summary>Local Variable State [pc={ pc + issue.line(pc).map(" line="+_).getOrElse("") }]</summary>
                <table>
                    <tr><th>Index</th><th>Name</th><th>Value</th></tr>
                    { lvsAsXHTML }
                </table>
            </details>
        )

    }
}
