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
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformationConst
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
case class L0StaticFunctionCallInterpreter()(
    implicit
    override val p:  SomeProject,
    override val ps: PropertyStore
) extends StringInterpreter
    with L0ArbitraryStaticFunctionCallInterpreter
    with L0StringValueOfFunctionCallInterpreter {

    override type T = StaticFunctionCall[V]

    override def interpret(instr: T, pc: Int)(implicit state: DUSiteState): ProperPropertyComputationResult = {
        instr.name match {
            case "valueOf" if instr.declaringClass == ObjectType.String => processStringValueOf(instr, pc)
            case _                                                      => interpretArbitraryCall(instr, pc)
        }
    }
}

private[string_analysis] trait L0ArbitraryStaticFunctionCallInterpreter
    extends StringInterpreter
    with L0FunctionCallInterpreter {

    implicit val p: SomeProject

    override type T = StaticFunctionCall[V]

    def interpretArbitraryCall(instr: T, pc: Int)(implicit
        state: DUSiteState
    ): ProperPropertyComputationResult = {
        val calleeMethod = instr.resolveCallTarget(state.dm.definedMethod.classFile.thisType)
        if (calleeMethod.isEmpty) {
            return computeFinalResult(pc, StringConstancyInformation.lb)
        }

        val m = calleeMethod.value
        val callState = FunctionCallState(state, Seq(m), Map((m, ps(m, TACAI.key))))
        callState.setParamDependees(evaluateParameters(getParametersForPC(pc)))

        interpretArbitraryCallToMethods(callState)
    }
}

private[string_analysis] trait L0StringValueOfFunctionCallInterpreter extends StringInterpreter {

    override type T <: StaticFunctionCall[V]

    val ps: PropertyStore

    def processStringValueOf(call: T, pc: Int)(implicit state: DUSiteState): ProperPropertyComputationResult = {
        def finalResult(results: Seq[SomeFinalEP]): Result = {
            // For char values, we need to do a conversion (as the returned results are integers)
            val scis = results.map { r => r.p.asInstanceOf[StringConstancyProperty].sci }
            val finalScis = if (call.descriptor.parameterType(0).toJava == "char") {
                scis.map {
                    case StringConstancyInformationConst(const: StringTreeConst) if const.isIntConst =>
                        StringConstancyInformationConst(StringTreeConst(const.string.toInt.toChar.toString))
                    case sci =>
                        sci
                }
            } else {
                scis
            }
            computeFinalResult(pc, StringConstancyInformation.reduceMultiple(finalScis))
        }

        val results = call.params.head.asVar.definedBy.toList.sorted.map { ds =>
            ps(InterpretationHandler.getEntityForDefSite(ds), StringConstancyProperty.key)
        }
        if (results.exists(_.isRefinable)) {
            InterimResult.forUB(
                InterpretationHandler.getEntityForPC(pc),
                StringConstancyProperty.ub,
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
