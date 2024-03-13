/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringTreeConst
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeFinalEP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.properties.TACAI

/**
 * @author Maximilian Rüsch
 */
case class L0StaticFunctionCallInterpreter[State <: L0ComputationState]()(
    implicit
    override val p:  SomeProject,
    override val ps: PropertyStore
) extends L0StringInterpreter[State]
    with L0ArbitraryStaticFunctionCallInterpreter[State]
    with L0StringValueOfFunctionCallInterpreter[State] {

    override type T = StaticFunctionCall[V]

    override def interpret(instr: T, pc: Int)(implicit state: State): ProperPropertyComputationResult = {
        instr.name match {
            case "valueOf" if instr.declaringClass == ObjectType.String => processStringValueOf(instr, pc)
            case _                                                      => interpretArbitraryCall(instr, pc)
        }
    }
}

private[string_analysis] trait L0ArbitraryStaticFunctionCallInterpreter[State <: L0ComputationState]
    extends StringInterpreter[State]
    with L0FunctionCallInterpreter[State] {

    implicit val p: SomeProject

    override type T = StaticFunctionCall[V]

    def interpretArbitraryCall(instr: T, pc: Int)(implicit
        state: State
    ): ProperPropertyComputationResult = {
        val calleeMethod = instr.resolveCallTarget(state.entity._2.classFile.thisType)
        if (calleeMethod.isEmpty) {
            return computeFinalResult(pc, StringConstancyInformation.lb)
        }

        val m = calleeMethod.value
        val callState = FunctionCallState(pc, Seq(m), Map((m, ps(m, TACAI.key))))
        callState.setParamDependees(evaluateParameters(getParametersForPC(pc)))

        interpretArbitraryCallToMethods(state, callState)
    }
}

private[string_analysis] trait L0StringValueOfFunctionCallInterpreter[State <: L0ComputationState]
    extends StringInterpreter[State] {

    override type T <: StaticFunctionCall[V]

    val ps: PropertyStore

    def processStringValueOf(call: T, pc: Int)(implicit state: State): ProperPropertyComputationResult = {
        def finalResult(results: Seq[SomeFinalEP]): Result = {
            // For char values, we need to do a conversion (as the returned results are integers)
            val scis = results.map { r => r.p.asInstanceOf[StringConstancyProperty].sci }
            val finalScis = if (call.descriptor.parameterType(0).toJava == "char") {
                scis.map { sci =>
                    sci.tree match {
                        case const: StringTreeConst if const.isIntConst =>
                            sci.copy(tree = StringTreeConst(const.string.toInt.toChar.toString))
                        case _ =>
                            sci
                    }
                }
            } else {
                scis
            }
            computeFinalResult(pc, StringConstancyInformation.reduceMultiple(finalScis))
        }

        val results = call.params.head.asVar.definedBy.toList.sorted.map { ds =>
            ps(InterpretationHandler.getEntityFromDefSite(ds), StringConstancyProperty.key)
        }
        if (results.exists(_.isRefinable)) {
            InterimResult.forLB(
                InterpretationHandler.getEntityFromDefSitePC(pc),
                StringConstancyProperty.lb,
                results.filter(_.isRefinable).toSet,
                awaitAllFinalContinuation(
                    EPSDepender(call, call.pc, state, results),
                    finalResult
                )
            )
        } else {
            finalResult(results.asInstanceOf[Seq[SomeFinalEP]])
        }
    }
}
