/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package analysis

import scala.xml.Node
import scala.xml.Text

import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.collection.mutable.Locals
import org.opalj.br.Code

/**
 * Describes some issue found in the source code.
 *
 * @author Michael Eichberg
 */
trait Issue {

    /**
     * The primarily affected class file.
     */
    def classFile: ClassFile

    /**
     * The primarily affected method.
     */
    def method: Method

    final def code: Code = method.body.get

    /**
     * The primarily affected instruction.
     */
    def pc: PC

    /**
     * The opcode of the relevant instruction.
     */
    final def opcode: Int = method.body.get.instructions(pc).opcode

    /**
     * The primarily affected line of source code; if available.
     */
    final def line: Option[Int] = method.body.get.lineNumber(pc)

    /**
     * The register values at the given location.
     */
    def localVariables: Option[Locals[_ <: AnyRef]]

    def localVariablesToXHTML: Node = {
        def default =
            <div class="warning">
                Local variable information (debug information) is not available.
            </div>

        localVariables.map { lv ⇒
            if (code.localVariablesAt(pc).isEmpty) {
                default
            } else {
                val lvsAsXHTML =
                    for ((index, name) ← code.localVariablesAt(pc)) yield {
                        <tr><td>{ index }</td><td>{ name }</td><td>{ lv(index).toString }</td></tr>
                    }

                <details class="locals">
                    <summary>Local Variable State</summary>
                    <table>
                        <tr><th>Index</th><th>Name</th><th>Value</th></tr>
                        { lvsAsXHTML }
                    </table>
                </details>
            }
        }.getOrElse {
            default
        }
    }

    /**
     * A textual representation of the bug report, well suited for console output.
     */
    def message: String

    /**
     * A string that uses small letters and which describes the category of the issue.
     *
     * The category basically describes '''the property of the software that is
     * affected ''' by this issue.
     */
    def category: String

    /**
     * A string that uses small letters and which describes the kind of the issue.
     *
     * The kind describes how '''this issue manifests itself in the source code'''.
     */
    def kind: String

    /**
     * An HTML representation of the bug report, well suited for browser output.
     *
     * The format has to be:
     * {{{
     * &lt;tr style={
     *      val color = accuracy.map(a ⇒ a.asHTMLColor).getOrElse("rgb(255, 126, 3)")
     *      s"color:$color;"
     *      }&gt;
     *  &lt;td&gt;
     *      XHTML.typeToXHTML(classFile.thisType)
     *  &lt;/td&gt;
     *  &lt;td&gt;
     *      XHTML.methodToXHTML(method.name, method.descriptor)
     *  &lt;/td&gt;
     *  &lt;td&gt;
     *      PROGRAM_COUNTER "/" LINE_NUMBER OR "N/A"
     *  &lt;td&gt;
     *      MESSAGE (FREE FORM)
     * &lt;/tr&gt;
     * }}}
     */
    def toXHTML: Node
}

/**
 * Collection of predefined issue categories.
 *
 * In general, the category basically describes '''the property of the software that is
 * affected ''' by this issue.
 *
 * @author Michael Eichberg
 */
object IssueCategory {

    val Bug = "bug"

    val Flawed = "flawed"

    val Performance = "performance"

    val Comprehensibility = "comprehensibility"

}

/**
 * Collection of predefined issue kinds.
 *
 * In general, an issue kind describes how '''this issue manifests itself in the source
 * code'''.
 *
 * @author Michael Eichberg
 */
object IssueKind {

    val ConstantComputation = "constant computation"

    val DeadBranch = "dead branch"

    val Unused = "unused"
}
