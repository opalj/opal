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
import org.opalj.br.fpcf.properties.string.StringTreeConcat
import org.opalj.br.fpcf.properties.string.StringTreeConst
import org.opalj.br.fpcf.properties.string.StringTreeDynamicFloat
import org.opalj.br.fpcf.properties.string.StringTreeDynamicInt
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty
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
                        else computeFinalResult(StringFlowFunctionProperty.identity)
                    case _: IntLikeType =>
                        computeFinalResult(StringFlowFunctionProperty.constForVariableAt(
                            state.pc,
                            pt,
                            StringTreeDynamicInt
                        ))
                    case FloatType | DoubleType =>
                        computeFinalResult(StringFlowFunctionProperty.constForVariableAt(
                            state.pc,
                            pt,
                            StringTreeDynamicFloat
                        ))
                    case _ =>
                        computeFinalResult(StringFlowFunctionProperty.identity)
                }
        }
    }

    private def interpretToStringCall(at: Option[PV], pt: PV)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        if (at.isDefined) {
            computeFinalResult(
                Set(PDUWeb(state.pc, at.get), PDUWeb(state.pc, pt)),
                (env: StringTreeEnvironment) => env.update(state.pc, at.get, env(state.pc, pt))
            )
        } else {
            computeFinalResult(StringFlowFunctionProperty.identity)
        }
    }

    /**
     * Processes calls to [[StringBuilder#replace]] or [[StringBuffer#replace]].
     */
    private def interpretReplaceCall(target: PV)(implicit state: InterpretationState): ProperPropertyComputationResult =
        computeFinalResult(StringFlowFunctionProperty.lb(state.pc, target))
}

private[string] trait L0ArbitraryVirtualFunctionCallInterpreter extends AssignmentLikeBasedStringInterpreter {

    protected def interpretArbitraryCall(target: PV, call: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult =
        computeFinalResult(StringFlowFunctionProperty.lb(state.pc, target))
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

        computeFinalResult(
            Set(PDUWeb(state.pc, paramVar), PDUWeb(state.pc, pt)) ++ at.map(PDUWeb(state.pc, _)),
            (env: StringTreeEnvironment) => {
                val valueState = env(state.pc, paramVar)

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

                val appendedState = StringTreeConcat.fromNodes(env(state.pc, pt), transformedValueState)
                var newEnv = env
                if (at.isDefined) {
                    newEnv = newEnv.update(state.pc, at.get, appendedState)
                }
                newEnv.update(state.pc, pt, appendedState)
            }
        )
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
            return computeFinalResult(StringFlowFunctionProperty.identity);
        }

        val parameterCount = call.params.size
        parameterCount match {
            case 1 =>
                call.params.head.asVar.value match {
                    case TheIntegerValue(intVal) =>
                        computeFinalResult(
                            Set(PDUWeb(state.pc, pt), PDUWeb(state.pc, at.get)),
                            (env: StringTreeEnvironment) => {
                                env(state.pc, pt) match {
                                    case StringTreeConst(string) if intVal <= string.length =>
                                        env.update(state.pc, at.get, StringTreeConst(string.substring(intVal)))
                                    case _ =>
                                        env.update(state.pc, at.get, StringTreeNode.lb)
                                }
                            }
                        )
                    case _ =>
                        computeFinalResult(StringFlowFunctionProperty.noFlow(state.pc, at.get))
                }

            case 2 =>
                (call.params.head.asVar.value, call.params(1).asVar.value) match {
                    case (TheIntegerValue(firstIntVal), TheIntegerValue(secondIntVal)) =>
                        computeFinalResult(
                            Set(PDUWeb(state.pc, pt), PDUWeb(state.pc, at.get)),
                            (env: StringTreeEnvironment) => {
                                env(state.pc, pt) match {
                                    case StringTreeConst(string)
                                        if firstIntVal <= string.length
                                            && secondIntVal <= string.length
                                            && firstIntVal <= secondIntVal =>
                                        env.update(
                                            state.pc,
                                            at.get,
                                            StringTreeConst(string.substring(firstIntVal, secondIntVal))
                                        )
                                    case _ =>
                                        env.update(state.pc, at.get, StringTreeNode.lb)
                                }
                            }
                        )
                    case _ =>
                        computeFinalResult(StringFlowFunctionProperty.noFlow(state.pc, at.get))
                }

            case _ => throw new IllegalStateException(
                    s"Unexpected parameter count for ${call.descriptor.toJava}. Expected one or two, got $parameterCount"
                )
        }
    }
}
