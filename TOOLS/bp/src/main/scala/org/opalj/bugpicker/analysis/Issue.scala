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
import org.opalj.collection.mutable.Locals
import org.opalj.br.instructions.Instruction
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.Code
import org.opalj.ai.domain.ConcreteIntegerValues
import org.opalj.ai.domain.l1.IntegerRangeValues
import org.opalj.br.analyses.SomeProject

/**
 * Describes some issue found in the source code.
 *
 * @author Michael Eichberg
 */
trait Issue {

    /**
     * The affected project.
     */
    def project: SomeProject

    /**
     * The primarily affected class file.
     */
    def classFile: ClassFile

    /**
     * The primarily affected method.
     */
    def method: Option[Method]

    /**
     * The primarily affected instruction.
     *
     * If `pc` is defined `method` has to be defined too and the method must have
     * a `Code` block.
     */
    def pc: Option[PC]

    /**
     * Other instructions that are related to this finding and which may facilitate
     * the comprehension of this issue.
     */
    def otherPCs: Seq[(PC, String)]

    /**
     * A value in the range [1..100] and which is an estimation of the relevance of
     * this issue from the point of view of the developer.
     */
    def relevance: Relevance

    /**
     * The register values at the given location.
     *
     * If `localVariables` is defined, `pc` and `method` has to be defined, too!
     */
    def localVariables: Option[Locals[_ <: AnyRef]]

    def localVariablesToXHTML: Option[Node] = {
        if (this.pc.isEmpty || this.code.isEmpty)
            return None;

        val pc = this.pc.get
        val code = this.code.get

        def default =
            Some(
                <div class="warning">
                    Local variable information (debug information) is not available.
                </div>
            )

        if (this.localVariables.isEmpty) {
            return default;
        }
        val localVariableValues = this.localVariables.get

        val localVariableDefinitions = code.localVariablesAt(pc)
        if (localVariableDefinitions.isEmpty)
            return default;

        val lvsAsXHTML =
            for ((index, theLV) ← localVariableDefinitions) yield {
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
                <summary>Local Variable State</summary>
                <table>
                    <tr><th>Index</th><th>Name</th><th>Value</th></tr>
                    { lvsAsXHTML }
                </table>
            </details>
        )

    }

    /**
     * The issue in '''one''' sentence.
     */
    def summary: String

    /**
     * The description of the issue.
     */
    def description: Option[String]

    /**
     * A string that uses small letters and which describes the category of the issue.
     *
     * The category basically describes '''the property of the software that is
     * affected ''' by this issue (see [[IssueCategory]] for further details).
     */
    def categories: Set[String]

    /**
     * A string that uses small letters and which describes the kind of the issue.
     *
     * The kind describes how '''this issue manifests itself in the source code'''
     * (see [[IssueKind]] for further details).
     */
    def kind: Set[String]

    // __________________________________________________________________________________
    //
    // DERIVED INFORMATION
    // __________________________________________________________________________________
    //

    /**
     * The method's code.
     */
    final def code: Option[Code] = method.flatMap(_.body)

    /**
     * The opcode of the relevant instruction.
     */
    final def opcode: Option[Int] =
        pc.flatMap(pc ⇒ code.map(_.instructions(pc).opcode))

    final def instruction: Option[Instruction] =
        pc.flatMap(pc ⇒ code.map(_.instructions(pc)))

    /**
     * The primarily affected line of source code; if available.
     */
    final def line: Option[Int] = pc.flatMap(pc ⇒ code.flatMap(_.lineNumber(pc)))

    /**
     * An (x)HTML5 representation of the bug report, well suited for browser output.
     */
    def asXHTML: Node

    /**
     * A representation of this bug report well suited for console output.
     */
    def asAnsiColoredString: String
}

