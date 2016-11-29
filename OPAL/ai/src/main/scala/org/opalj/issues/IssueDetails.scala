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
import scala.xml.Group

import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.JsNull
import play.api.libs.json.JsObject
import play.api.libs.json.JsNumber
import play.api.libs.json.JsArray

import org.opalj.collection.mutable.Locals
import org.opalj.collection.immutable.Chain
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.PC
import org.opalj.br.Code
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.SimpleConditionalBranchInstruction
import org.opalj.br.instructions.CompoundConditionalBranchInstruction
import org.opalj.br.instructions.StackManagementInstruction
import org.opalj.br.instructions.IINC
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.ai.AIResult

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

    def lineJson: JsValue = PCLineComprehension.lineJson(classFileFQN, methodJVMSignature, pc, line)
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

    def pcJson(classFileFQN: String, methodJVMSignature: String, pc: PC): JsObject = {
        Json.obj(
            "classFileFQN" → classFileFQN,
            "methodJVMSignature" → methodJVMSignature,
            "pc" → pc
        )
    }

    def lineNode(
        classFileFQN:       String,
        methodJVMSignature: String,
        pc:                 PC,
        line:               Option[Int]
    ): Node = {
        line.map { line ⇒
            <span class="line_number" data-class={ classFileFQN } data-method={ methodJVMSignature } data-line={ line.toString } data-pc={ pc.toString } data-show="sourcecode">
                line={ line.toString }
            </span>
        }.getOrElse(Group(Nil))
    }

    def lineJson(classFileFQN: String, methodJVMSignature: String, pc: PC, line: Option[Int]): JsValue = {
        val result = pcJson(classFileFQN, methodJVMSignature, pc)
        if (line.isDefined)
            result + (("line", JsNumber(line.get)))
        else
            result
    }
}

/**
 * Information that facilitates the comprehension of a reported issue.
 */
trait IssueDetails extends IssueRepresentations

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
        Json.obj("type" → getClass.getSimpleName, "values" → lvValues)
    }
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

    override def toIDL: JsValue = {
        instruction match {
            case cbi: SimpleConditionalBranchInstruction ⇒
                Json.obj(
                    "type" → "SimpleConditionalBranchInstruction",
                    "operator" → cbi.operator,
                    "value" → {
                        cbi.operandCount match {
                            case 1 ⇒ operands.head.toString
                            case _ ⇒ operands.tail.head.toString
                        }
                    },
                    "value2" → {
                        cbi.operandCount match {
                            case 1 ⇒ JsNull
                            case _ ⇒ operands.head.toString
                        }
                    }
                )
            case cbi: CompoundConditionalBranchInstruction ⇒
                Json.obj(
                    "type" → "CompoundConditionalBranchInstruction",
                    "value" → operands.head.toString,
                    "caseValues" → cbi.caseValues.mkString(", ")
                )
            case smi: StackManagementInstruction ⇒
                Json.obj(
                    "type" → "StackManagementInstruction",
                    "mnemonic" → smi.mnemonic,
                    "values" → operands.map(_.toString).toList
                )
            case IINC(lvIndex, constValue) ⇒
                Json.obj(
                    "type" → "IINC",
                    "value" → localVariables(lvIndex).toString,
                    "constValue" → constValue
                )

            case instruction ⇒
                val operandsCount =
                    instruction.numberOfPoppedOperands { x ⇒
                        val message = "a stack management instruction is related to an issue"
                        throw new UnknownError(message)
                    }
                Json.obj(
                    "type" → instruction.getClass.getSimpleName,
                    "mnemonic" → instruction.mnemonic,
                    "parameters" → operands.take(operandsCount).reverse.map(_.toString).toList
                )
        }
    }
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

    override def toIDL: JsValue = {
        import PCLineComprehension.line

        Json.obj(
            "type" → getClass.getSimpleName,
            "values" → collectReadFieldValues.map { fieldData ⇒
                val (pc, details) = fieldData

                Json.obj(
                    "classFileFQN" → classFileFQN,
                    "methodJVMSignature" → methodJVMSignature,
                    "pc" → pc,
                    "line" → line(pc),
                    "details" → details
                )
            }
        )
    }
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

    override def toIDL: JsValue = {
        import PCLineComprehension.line

        JsArray(collectMethodReturnValues.map { methodData ⇒
            val (pc, details) = methodData

            Json.obj(
                "classFileFQN" → classFileFQN,
                "methodJVMSignature" → methodJVMSignature,
                "pc" → pc,
                "line" → line(pc),
                "details" → details
            )
        })
    }
}
