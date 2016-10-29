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
import org.opalj.collection.mutable.Locals
import org.opalj.br.{ClassFile, Method, PC, Code}
import org.opalj.br.instructions.Instruction
import org.opalj.ai.domain.l1.IntegerRangeValues
import scala.xml.Comment
import org.opalj.br.instructions.SimpleConditionalBranchInstruction
import org.opalj.br.instructions.CompoundConditionalBranchInstruction
import org.opalj.br.instructions.StackManagementInstruction
import org.opalj.br.instructions.IINC
import scala.xml.Group
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.ai.AIResult
import org.opalj.collection.immutable.Chain

trait ClassComprehension {

    def classFile: ClassFile

    def classFileFQN: String = classFile.fqn

}

trait MethodComprehension extends ClassComprehension {

    def method: Method

    def methodJVMSignature: String = method.name + method.descriptor.toJVMDescriptor

}

trait CodeComprehension {

    implicit def code: Code

    def pc: PC

    final def opcode: Int = code.instructions(pc).opcode

    final def instruction: Instruction = code.instructions(pc)
}

trait PCLineComprehension extends MethodComprehension with CodeComprehension {

    final def line(pc: PC): Option[Int] = PCLineComprehension.line(pc)

    final def line: Option[Int] = line(pc)

    final def pcLineToString = PCLineComprehension.pcLineToString(pc)

    def pcNode: Node = PCLineComprehension.pcNode(classFileFQN, methodJVMSignature, pc)

    def lineNode: Node = PCLineComprehension.lineNode(classFileFQN, methodJVMSignature, pc, line)

}
object PCLineComprehension {

    final def line(pc: PC)(implicit code: Code): Option[Int] = code.lineNumber(pc)

    final def pcLineToString(
        pc: PC
    )(
        implicit
        code: Code
    ) = "pc="+pc + line(pc).map(" line="+_).getOrElse("")

    def pcNode(classFileFQN: String, methodJVMSignature: String, pc: PC): Node = {
        <span class="program_counter" data-class={ classFileFQN } data-method={ methodJVMSignature } data-pc={ pc.toString } data-show="bytecode">
            pc={ pc.toString }
        </span>
    }

    def lineNode(classFileFQN: String, methodJVMSignature: String, pc: PC, line: Option[Int]): Node = {
        line.map { line ⇒
            <span class="line_number" data-class={ classFileFQN } data-method={ methodJVMSignature } data-line={ line.toString } data-pc={ pc.toString } data-show="sourcecode">
                line={ line.toString }
            </span>
        }.getOrElse(Group(Nil))
    }

}

/**
 * Information that facilitates the comprehension of a reported issue.
 */
trait IssueDetails extends IssueRepresentations {

}

/**
 * @param localVariables The register values at the given location.
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

        val lvsAsXHTML =
            for ((index, theLV) ← localVariableDefinitions.toSeq.sortWith((a, b) ⇒ a._1 < b._1)) yield {
                val localValue = localVariables(index)
                val localValueAsXHTML =
                    if (localValue == null)
                        <span class="warning">unused</span>
                    else {

                        if ((theLV.fieldType eq org.opalj.br.BooleanType) &&
                            // SPECIAL HANDLING IF THE VALUE IS AN INTEGER RANGE VALUE
                            localValue.isInstanceOf[IntegerRangeValues#IntegerRange]) {
                            val range = localValue.asInstanceOf[IntegerRangeValues#IntegerRange]
                            if ( /*range.lowerBound == 0 &&*/ range.upperBound == 0)
                                Text("false")
                            else if (range.lowerBound == 1 /* && range.upperBound == 1*/ )
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

    def toIDL: String = "" // TODO Support a better representation
}

class Operands(
        val code:           Code,
        val pc:             PC,
        val operands:       Chain[_ <: AnyRef],
        val localVariables: Locals[_ <: AnyRef]
) extends IssueDetails with CodeComprehension {

    def toXHTML(basicInfoOnly: Boolean): Node = {
        val detailsNodes = instruction match {
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
                    <span class="value">{ operands.head } </span>,
                    <span> (case values: { cbi.caseValues.mkString(", ") } )</span>
                )

            case smi: StackManagementInstruction ⇒
                val representation =
                    <span class="keyword">{ smi.mnemonic } </span> ::
                        (operands.map(op ⇒ <span class="value">{ op } </span>).to[List])
                representation

            case IINC(lvIndex, constValue) ⇒
                val representation =
                    List(
                        <span class="keyword">iinc </span>,
                        <span class="parameters">
                            (
                            <span class="value">{ localVariables(lvIndex) }</span>
                            <span class="value">{ constValue } </span>
                            )
                        </span>
                    )
                representation

            case instruction ⇒
                val operandsCount =
                    instruction.numberOfPoppedOperands { x ⇒ throw new UnknownError() }

                val parametersNode =
                    operands.take(operandsCount).reverse.map { op ⇒
                        <span class="value">{ op } </span>
                    }
                List(
                    <span class="keyword">{ instruction.mnemonic } </span>,
                    <span class="parameters">({ parametersNode })</span>
                )
        }
        <div>{ detailsNodes }</div>
    }

    def toAnsiColoredString: String = "" // TODO Support a better representation

    def toEclipseConsoleString: String = "" // TODO Support a better representation

    def toIDL: String = "" // TODO Support a better representation

}

class FieldValues(
        val classFile: ClassFile,
        val method:    Method,
        val result:    AIResult
) extends IssueDetails with MethodComprehension {

    private[this] implicit def code = result.code

    private[this] def operandsArray = result.operandsArray

    def collectReadFieldValues: Seq[(PC, String)] = {
        code.collectWithIndex {
            case (pc, instr @ FieldReadAccess(_ /*declaringClassType*/ , _ /* name*/ , fieldType)) if {
                val nextPC = instr.indexOfNextInstruction(pc)
                val operands = operandsArray(nextPC)
                operands != null &&
                    operands.head.isMorePreciseThan(result.domain.TypedValue(pc, fieldType))
            } ⇒
                (pc, s"${operandsArray(instr.indexOfNextInstruction(pc)).head} ← $instr")
        }
    }

    def toXHTML(basicInfoOnly: Boolean): Node = {
        import PCLineComprehension.{pcNode, lineNode, line}
        val readFieldValues =
            collectReadFieldValues.map { fieldData ⇒
                val (pc, details) = fieldData
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

    def toIDL: String = "" // TODO Support a better representation

}

class MethodReturnValues(
        val classFile: ClassFile,
        val method:    Method,
        val result:    AIResult
) extends IssueDetails with MethodComprehension {

    private[this] implicit def code = result.code

    private[this] def operandsArray = result.operandsArray

    def collectMethodReturnValues: Seq[(PC, String)] = {

        code.collectWithIndex {
            case (pc, instr @ MethodInvocationInstruction(declaringClassType, name, descriptor)) if !descriptor.returnType.isVoidType && {
                val nextPC = instr.indexOfNextInstruction(pc)
                val operands = operandsArray(nextPC)
                operands != null &&
                    operands.head.isMorePreciseThan(result.domain.TypedValue(pc, descriptor.returnType))
            } ⇒
                val modifier = if (instr.isInstanceOf[INVOKESTATIC]) "static " else ""
                (
                    pc,
                    s"${operandsArray(instr.indexOfNextInstruction(pc)).head} ← ${declaringClassType.toJava}{ $modifier ${descriptor.toJava(name)} }"
                )
        }
    }

    def toXHTML(basicInfoOnly: Boolean): Node = {
        import PCLineComprehension.{pcNode, lineNode, line}
        val methodReturnValues =
            collectMethodReturnValues.map { methodData ⇒
                val (pc, details) = methodData
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

    def toIDL: String = "" // TODO Support a better representation
}
