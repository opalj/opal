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

import scala.xml.Node
import scala.xml.UnprefixedAttribute
import scala.Console.BLUE
import scala.Console.BOLD
import scala.Console.GREEN
import scala.Console.RESET
import scala.collection.SortedMap
import org.opalj.br.{ ClassFile, Method }
import org.opalj.ai.debug.XHTML
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.ConditionalBranchInstruction
import org.opalj.br.instructions.SimpleConditionalBranchInstruction
import org.opalj.br.instructions.CompoundConditionalBranchInstruction
import scala.xml.Unparsed

case class DeadCode(
        classFile: ClassFile,
        method: Method,
        ctiPC: PC,
        operands: List[_],
        deadPC: PC,
        accuracy: Option[Percentage]) extends BugReport {

    def ctiInstruction = method.body.get.instructions(ctiPC)

    def pc = ctiPC

    def ctiLineNumber: Option[PC] =
        method.body.get.lineNumber(ctiPC)

    def line = ctiLineNumber

    def deadLineNumber: Option[PC] =
        method.body.get.lineNumber(deadPC)

    def message: String = {
        ctiInstruction match {
            case i: SimpleConditionalBranchInstruction ⇒
                val conditionIsAlwaysTrue =
                    method.body.get.pcOfNextInstruction(ctiPC) == deadPC
                s"the condition (${i.operator}) is always $conditionIsAlwaysTrue; "+
                    s"the instruction with program counter $deadPC "+
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
                        .<br/>
                        The instruction with program counter&nbsp;{ deadPC }
                        { deadLineNumber.map(l ⇒ s"(line ${l}) ").getOrElse("") }
                        is never reached.
                    </div>
                case i: CompoundConditionalBranchInstruction ⇒
                    val (caseValues, defaultCase) = i.caseValueOfJumpOffset(deadPC - ctiPC)
                    var message = ""
                    if (caseValues.nonEmpty)
                        message += "dead case "+caseValues.mkString(" or ")
                    if (defaultCase) {
                        if (message.nonEmpty)
                            message += "; "
                        message += "default case is dead"
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
                                <span class="value">{ operands.head }</span>,
                                <span class="operator">{ cbi.operator }</span>
                            )
                        else
                            List(
                                <span class="value">{ operands.tail.head }</span>,
                                <span class="operator">{ cbi.operator }</span>,
                                <span class="value">{ operands.head }</span>
                            )
                    <span class="keyword">if</span> :: condition

                case cbi: CompoundConditionalBranchInstruction ⇒
                    Seq(
                        <span class="keyword">switch</span>,
                        <span class="value">{ operands.head }</span>
                        <span> (case values: { cbi.caseValues.mkString(", ") } )</span>
                    )
            }

        val pcNode =
            <span class="tooltip">
                { ctiPC }
                <span>{ iNode }</span>
            </span>

        val node =
            <tr style={
                val color = accuracy.map(a ⇒ a.asHTMLColor).getOrElse("rgb(255, 126, 3)")
                s"color:$color;"
            }>
                <td>
                    { XHTML.typeToXHTML(classFile.thisType) }
                </td>
                <td>{ XHTML.methodToXHTML(method.name, method.descriptor) }</td>
                <td>{ pcNode }{ "/ "+ctiLineNumber.getOrElse("N/A") }</td>
                <td>{ message }</td>
            </tr>

        accuracy match {
            case Some(a) ⇒
                node % (
                    new UnprefixedAttribute("data-accuracy", a.value.toString(), scala.xml.Null)
                )
            case None ⇒
                node
        }

    }

    override def toString = {
        import Console._
        val declaringClassOfMethod = classFile.thisType.toJava

        "Dead code in "+BOLD + BLUE +
            declaringClassOfMethod+"{ "+method.toJava+"{ "+
            GREEN+"PC: "+ctiPC + ctiLineNumber.map("; Line: "+_+" - ").getOrElse("; Line: N/A - ") +
            ctiInstruction + operands.reverse.mkString("(", ",", ")") +
            RESET+" }}"
    }
}

