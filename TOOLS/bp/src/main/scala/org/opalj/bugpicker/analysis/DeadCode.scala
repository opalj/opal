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

import scala.Console.BLUE
import scala.Console.BOLD
import scala.Console.GREEN
import scala.Console.RESET
import scala.xml.Node
import scala.xml.Text
import scala.xml.UnprefixedAttribute

import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.instructions.CompoundConditionalBranchInstruction
import org.opalj.br.instructions.SimpleConditionalBranchInstruction
import org.opalj.br.methodToXHTML
import org.opalj.br.typeToXHTML
import org.opalj.collection.mutable.Locals

/**
 * Collection of all information related to some piece of code that was identified
 * as being dead.
 */
case class DeadCode(
        classFile: ClassFile,
        method: Method,
        ctiPC: PC,
        operands: List[_],
        localVariables: Option[Locals[_ <: AnyRef]],
        deadPC: PC,
        relevance: Option[Relevance]) extends Issue {

    override def category: String = IssueCategory.Flawed

    override def kind: String = IssueKind.DeadBranch

    def ctiInstruction = method.body.get.instructions(ctiPC)

    def pc = ctiPC

    def ctiLineNumber: Option[PC] =
        method.body.get.lineNumber(ctiPC)

    def deadLineNumber: Option[PC] =
        method.body.get.lineNumber(deadPC)

    def message: String = {
        ctiInstruction match {
            case i: SimpleConditionalBranchInstruction ⇒
                val conditionIsAlwaysTrue =
                    method.body.get.pcOfNextInstruction(ctiPC) == deadPC
                s"The condition (${i.operator}) is always $conditionIsAlwaysTrue; "+
                    s"the instruction with pc $deadPC "+
                    s"${deadLineNumber.map(l ⇒ s"(line ${l}) ").getOrElse("")}is never reached"
            case i: CompoundConditionalBranchInstruction ⇒
                val (caseValues, defaultCase) = i.caseValueOfJumpOffset(deadPC - ctiPC)
                var message = ""
                if (caseValues.nonEmpty)
                    message += "the case "+caseValues.mkString(" or ")+" is never reached"
                if (defaultCase) {
                    if (message.nonEmpty)
                        message += "; "
                    message += "the switch statement's default case is dead"
                }

                message
        }
    }

    def toXHTML: Node = {

        val message: Node = {
            ctiInstruction match {
                case i: SimpleConditionalBranchInstruction ⇒
                    val conditionIsAlwaysTrue =
                        method.body.get.pcOfNextInstruction(ctiPC) == deadPC

                    <div>
                        The condition (&nbsp;{ i.operator }
                        ) is always&nbsp;{ conditionIsAlwaysTrue }
                        . The instruction with program counter&nbsp;{ deadPC }
                        { deadLineNumber.map(l ⇒ s"(line ${l}) ").getOrElse("") }
                        is never reached.
                    </div>
                case i: CompoundConditionalBranchInstruction ⇒
                    val (caseValues, defaultCase) = i.caseValueOfJumpOffset(deadPC - ctiPC)
                    var message = ""
                    if (caseValues.nonEmpty)
                        message += "Dead case(s): "+caseValues.mkString(", ")+"."
                    if (defaultCase) {
                        if (message.nonEmpty)
                            message += " "
                        message += "The \"default\" case is dead."
                    }

                    <div>{ message }</div>
            }
        }

        val iNode =
            ctiInstruction match {
                case cbi: SimpleConditionalBranchInstruction ⇒

                    val condition =
                        if (cbi.operandCount == 1)
                            List(
                                <span class="value">{ operands.head } </span>,
                                <span class="operator">{ cbi.operator } </span>
                            )
                        else
                            List(
                                <span class="value">{ operands.tail.head } </span>,
                                <span class="operator">{ cbi.operator } </span>,
                                <span class="value">{ operands.head } </span>
                            )
                    <span class="keyword">if&nbsp;</span> :: condition

                case cbi: CompoundConditionalBranchInstruction ⇒
                    Seq(
                        <span class="keyword">switch </span>,
                        <span class="value">{ operands.head } </span>
                        <span> (case values: { cbi.caseValues.mkString(", ") } )</span>
                    )
            }

        val methodId = method.name + method.descriptor.toJVMDescriptor

        val pcNode =
            <span data-class={ classFile.fqn } data-method={ methodId } data-pc={ ctiPC.toString } data-line={ line.map(_.toString).getOrElse("") } data-show="bytecode">
                { ctiPC }
            </span>

        val methodLine: String =
            method.body.flatMap(_.firstLineNumber.map { ln ⇒
                if (ln > 0) (ln - 1).toString else "0"
            }).getOrElse("")

        val color = s"color:${relevance.map(a ⇒ a.asHTMLColor).getOrElse("rgb(255, 126, 3)")};"

        val node =
            <div class="an_issue" style={ color }>
                <dl>
                    <dt>class</dt>
                    <dd class="declaring_class" data-class={ classFile.fqn }>{ typeToXHTML(classFile.thisType) }</dd>
                    <dt>method</dt>
                    <dd class="method" data-class={ classFile.fqn } data-method={ methodId } data-line={ methodLine }>
                        { methodToXHTML(method.name, method.descriptor) }
                    </dd>
                    <dt>instruction</dt>
                    <dd>
                        <span class="program_counter">pc={ pcNode }</span>
                        &nbsp;
                        <span class="line_number">line={ line.map(ln ⇒ <span data-class={ classFile.fqn } data-method={ methodId } data-line={ ln.toString } data-pc={ pc.toString } data-show="sourcecode">{ ln }</span>).getOrElse(Text("N/A")) }</span>
                    </dd>
                    <dt class="issue">issue</dt>
                    <dd class="issue_message">
                        { message }
                        <p>{ iNode }</p>
                        { localVariablesToXHTML }
                    </dd>
                </dl>
            </div>

        relevance match {
            case Some(a) ⇒
                node % (
                    new UnprefixedAttribute("data-relevance", a.value.toString(), scala.xml.Null)
                )
            case None ⇒
                node
        }

    }

    override def toString = {
        import scala.Console._
        val declaringClassOfMethod = classFile.thisType.toJava

        "Dead code in "+BOLD + BLUE +
            declaringClassOfMethod+"{ "+method.toJava+"{ "+
            GREEN+"PC: "+ctiPC + ctiLineNumber.map("; Line: "+_+" - ").getOrElse("; Line: N/A - ") +
            ctiInstruction + operands.reverse.mkString("(", ",", ")") +
            RESET+" }}"
    }
}

