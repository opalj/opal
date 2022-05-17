/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package issues

import scala.xml.Node

import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.JsNull

import org.opalj.collection.mutable.Locals
import org.opalj.br.PC
import org.opalj.br.Code
import org.opalj.br.instructions.SimpleConditionalBranchInstruction
import org.opalj.br.instructions.CompoundConditionalBranchInstruction
import org.opalj.br.instructions.StackManagementInstruction
import org.opalj.br.instructions.IINC

class Operands(
        val code:           Code,
        val pc:             PC,
        val operands:       List[_ <: AnyRef],
        val localVariables: Locals[_ <: AnyRef]
) extends IssueDetails with CodeComprehension {

    def toXHTML(basicInfoOnly: Boolean): Node = {
        val detailsNodes = instruction match {
            case cbi: SimpleConditionalBranchInstruction[_] =>

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

            case cbi: CompoundConditionalBranchInstruction =>
                Seq(
                    <span class="keyword">switch </span>,
                    <span class="value">{ operands.head } </span>,
                    <span> (case values: { cbi.caseValues.mkString(", ") } )</span>
                )

            case smi: StackManagementInstruction =>
                val representation =
                    <span class="keyword">{ smi.mnemonic } </span> ::
                        (operands.map(op => <span class="value">{ op } </span>))
                representation

            case IINC(lvIndex, constValue) =>
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

            case instruction =>
                val operandsCount =
                    instruction.numberOfPoppedOperands { x => throw new UnknownError() }

                val parametersNode =
                    operands.take(operandsCount).reverse.map { op =>
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
            case cbi: SimpleConditionalBranchInstruction[_] =>
                Json.obj(
                    "type" -> "SimpleConditionalBranchInstruction",
                    "operator" -> cbi.operator,
                    "value" -> {
                        cbi.operandCount match {
                            case 1 => operands.head.toString
                            case _ => operands.tail.head.toString
                        }
                    },
                    "value2" -> {
                        cbi.operandCount match {
                            case 1 => JsNull
                            case _ => operands.head.toString
                        }
                    }
                )
            case cbi: CompoundConditionalBranchInstruction =>
                Json.obj(
                    "type" -> "CompoundConditionalBranchInstruction",
                    "value" -> operands.head.toString,
                    "caseValues" -> cbi.caseValues.mkString(", ")
                )
            case smi: StackManagementInstruction =>
                Json.obj(
                    "type" -> "StackManagementInstruction",
                    "mnemonic" -> smi.mnemonic,
                    "values" -> operands.map(_.toString).toList
                )
            case IINC(lvIndex, constValue) =>
                Json.obj(
                    "type" -> "IINC",
                    "value" -> localVariables(lvIndex).toString,
                    "constValue" -> constValue
                )

            case instruction =>
                val operandsCount =
                    instruction.numberOfPoppedOperands { x =>
                        val message = "a stack management instruction is related to an issue"
                        throw new UnknownError(message)
                    }
                Json.obj(
                    "type" -> instruction.getClass.getSimpleName,
                    "mnemonic" -> instruction.mnemonic,
                    "parameters" -> operands.take(operandsCount).reverse.map(_.toString).toList
                )
        }
    }
}
