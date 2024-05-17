/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l0
package interpretation

import org.opalj.br.ComputationalTypeDouble
import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.DoubleType
import org.opalj.br.FloatType
import org.opalj.br.IntLikeType
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.string.StringConstancyLevel
import org.opalj.br.fpcf.properties.string.StringTreeConst
import org.opalj.br.fpcf.properties.string.StringTreeDynamicFloat
import org.opalj.br.fpcf.properties.string.StringTreeDynamicInt
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.tac.fpcf.properties.string.ConstantResultFlow
import org.opalj.tac.fpcf.properties.string.IdentityFlow
import org.opalj.tac.fpcf.properties.string.StringFlowFunction
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment
import org.opalj.value.TheIntegerValue

/**
 * Responsible for processing [[VirtualFunctionCall]]s without a call graph.
 *
 * @author Maximilian Rüsch
 */
case class L0VirtualFunctionCallInterpreter()
    extends AssignmentLikeBasedStringInterpreter
    with L0ArbitraryVirtualFunctionCallInterpreter
    with L0AppendCallInterpreter
    with L0SubstringCallInterpreter {

    override type T = AssignmentLikeStmt[V]
    override type E = VirtualFunctionCall[V]

    override def interpretExpr(instr: T, call: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        val at = Option.unless(!instr.isAssignment)(instr.asAssignment.targetVar.asVar.toPersistentForm(state.tac.stmts))
        val pt = call.receiver.asVar.toPersistentForm(state.tac.stmts)

        call.name match {
            case "append"   => interpretAppendCall(at, pt, call)
            case "toString" => interpretToStringCall(at, pt)
            case "replace"  => interpretReplaceCall(pt)
            case "substring" if call.descriptor.returnType == ObjectType.String =>
                interpretSubstringCall(at, pt, call)
            case _ =>
                call.descriptor.returnType match {
                    case obj: ObjectType if obj == ObjectType.String =>
                        if (at.isDefined) interpretArbitraryCall(at.get, call)
                        else computeFinalResult(IdentityFlow)
                    case _: IntLikeType =>
                        computeFinalResult(ConstantResultFlow.forVariable(pt, StringTreeDynamicInt))
                    case FloatType | DoubleType =>
                        computeFinalResult(ConstantResultFlow.forVariable(pt, StringTreeDynamicFloat))
                    case _ =>
                        computeFinalResult(IdentityFlow)
                }
        }
    }

    private def interpretToStringCall(at: Option[PV], pt: PV)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        if (at.isDefined) {
            computeFinalResult((env: StringTreeEnvironment) => env.update(at.get, env(pt)))
        } else {
            computeFinalResult(IdentityFlow)
        }
    }

    /**
     * Processes calls to [[StringBuilder#replace]] or [[StringBuffer#replace]].
     */
    private def interpretReplaceCall(target: PV)(implicit state: InterpretationState): ProperPropertyComputationResult =
        computeFinalResult(StringFlowFunction.lb(target))
}

private[string] trait L0ArbitraryVirtualFunctionCallInterpreter extends AssignmentLikeBasedStringInterpreter {

    protected def interpretArbitraryCall(target: PV, call: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult =
        computeFinalResult(StringFlowFunction.lb(target))
}

/**
 * Interprets calls to [[StringBuilder#append]] or [[StringBuffer#append]].
 */
private[string] trait L0AppendCallInterpreter extends AssignmentLikeBasedStringInterpreter {

    override type E = VirtualFunctionCall[V]

    def interpretAppendCall(at: Option[PV], pt: PV, call: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        // .head because we want to evaluate only the first argument of append
        val paramVar = call.params.head.asVar.toPersistentForm(state.tac.stmts)

        computeFinalResult((env: StringTreeEnvironment) => {
            val valueState = env(paramVar)

            val transformedValueState = paramVar.value.computationalType match {
                case ComputationalTypeInt =>
                    if (call.descriptor.parameterType(0).isCharType && valueState.isInstanceOf[StringTreeConst]) {
                        StringTreeConst(valueState.asInstanceOf[StringTreeConst].string.toInt.toChar.toString)
                    } else {
                        valueState
                    }
                case ComputationalTypeFloat | ComputationalTypeDouble =>
                    if (valueState.constancyLevel == StringConstancyLevel.CONSTANT) {
                        valueState
                    } else {
                        StringTreeDynamicFloat
                    }
                case _ =>
                    valueState
            }

            var newEnv = env
            if (at.isDefined) {
                newEnv = newEnv.update(at.get, transformedValueState)
            }
            newEnv.update(pt, transformedValueState)
        })
    }
}

/**
 * Interprets calls to [[String#substring]].
 */
private[string] trait L0SubstringCallInterpreter extends AssignmentLikeBasedStringInterpreter {

    override type E <: VirtualFunctionCall[V]

    def interpretSubstringCall(at: Option[PV], pt: PV, call: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        if (at.isEmpty) {
            return computeFinalResult(IdentityFlow);
        }

        val parameterCount = call.params.size
        parameterCount match {
            case 1 =>
                call.params.head.asVar.value match {
                    case intValue: TheIntegerValue =>
                        computeFinalResult((env: StringTreeEnvironment) => {
                            env(pt) match {
                                case const: StringTreeConst =>
                                    env.update(at.get, StringTreeConst(const.string.substring(intValue.value)))
                                case _ =>
                                    env.update(at.get, StringTreeNode.lb)
                            }
                        })
                    case _ =>
                        computeFinalResult(StringFlowFunction.noFlow(at.get))
                }

            case 2 =>
                (call.params.head.asVar.value, call.params(1).asVar.value) match {
                    case (firstIntValue: TheIntegerValue, secondIntValue: TheIntegerValue) =>
                        computeFinalResult((env: StringTreeEnvironment) => {
                            env(pt) match {
                                case const: StringTreeConst =>
                                    env.update(
                                        at.get,
                                        StringTreeConst(const.string.substring(
                                            firstIntValue.value,
                                            secondIntValue.value
                                        ))
                                    )
                                case _ =>
                                    env.update(at.get, StringTreeNode.lb)
                            }
                        })
                    case _ =>
                        computeFinalResult(StringFlowFunction.noFlow(at.get))
                }

            case _ => throw new IllegalStateException(
                    s"Unexpected parameter count for ${call.descriptor.toJava}. Expected one or two, got $parameterCount"
                )
        }
    }
}
