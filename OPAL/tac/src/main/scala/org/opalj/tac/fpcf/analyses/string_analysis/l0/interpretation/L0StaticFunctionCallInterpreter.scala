/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import scala.util.Try

import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeFinalEP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Processes [[StaticFunctionCall]]s in without a call graph.
 *
 * @author Maximilian Rüsch
 */
case class L0StaticFunctionCallInterpreter[State <: L0ComputationState[State]](
    exprHandler: InterpretationHandler[State]
)(
    implicit
    p:  SomeProject,
    ps: PropertyStore
) extends L0StringInterpreter[State]
    with IPResultDependingStringInterpreter[State]
    with EPSDependingStringInterpreter[State] {

    override type T = StaticFunctionCall[V]

    override def interpret(instr: T, defSite: Int)(implicit state: State): IPResult = {
        if (instr.declaringClass == ObjectType.String && instr.name == "valueOf") {
            processStringValueOf(instr)
        } else {
            processArbitraryCall(instr, defSite)
        }
    }

    private def processStringValueOf(call: T)(implicit state: State): IPResult = {
        def finalResult(results: Iterable[IPResult]): FinalIPResult = {
            // For char values, we need to do a conversion (as the returned results are integers)
            val scis = results.map { r => r.asFinal.sci }
            val finalScis = if (call.descriptor.parameterType(0).toJava == "char") {
                scis.map { sci =>
                    if (Try(sci.possibleStrings.toInt).isSuccess) {
                        sci.copy(possibleStrings = sci.possibleStrings.toInt.toChar.toString)
                    } else {
                        sci
                    }
                }
            } else {
                scis
            }
            FinalIPResult(StringConstancyInformation.reduceMultiple(finalScis), state.dm, call.pc)
        }

        val results = call.params.head.asVar.definedBy.toArray.sorted.map(exprHandler.processDefSite)
        val hasRefinableResults = results.exists(_.isRefinable)
        if (hasRefinableResults) {
            InterimIPResult.lbWithIPResultDependees(
                state.dm,
                call.pc,
                results.filter(_.isRefinable).asInstanceOf[Iterable[RefinableIPResult]],
                awaitAllFinalContinuation(
                    SimpleIPResultDepender(call, call.pc, state, results.toIndexedSeq),
                    finalResult _
                )
            )
        } else {
            finalResult(results)
        }
    }

    private def processArbitraryCall(instr: T, defSite: Int)(implicit
        state: State
    ): IPResult = {
        val calleeMethod = instr.resolveCallTarget(state.entity._2.classFile.thisType)
        if (calleeMethod.isEmpty) {
            return FinalIPResult.lb(state.dm, instr.pc)
        }

        val m = calleeMethod.value
        val (tacEOptP, calleeTac) = getTACAI(ps, m, state)

        if (tacEOptP.isRefinable) {
            InterimIPResult.lbWithEPSDependees(
                state.dm,
                instr.pc,
                Seq(tacEOptP),
                awaitAllFinalContinuation(
                    SimpleEPSDepender(instr, instr.pc, state, Seq(tacEOptP)),
                    (finalEPs: Iterable[SomeFinalEP]) =>
                        interpretArbitraryCallWithCalleeTAC(instr, defSite, m)(
                            finalEPs.head.ub.asInstanceOf[TACAI].tac.get
                        )
                )
            )
        } else if (calleeTac.isEmpty) {
            // When the tac ep is final but we still do not have a callee tac, we cannot infer arbitrary call values at all
            FinalIPResult.lb(state.dm, instr.pc)
        } else {
            interpretArbitraryCallWithCalleeTAC(instr, defSite, m)(calleeTac.get)
        }
    }

    private def interpretArbitraryCallWithCalleeTAC(instr: T, defSite: Int, calleeMethod: Method)(
        calleeTac: TAC
    )(implicit state: State): IPResult = {
        val returns = calleeTac.stmts.filter(_.isInstanceOf[ReturnValue[V]])
        if (returns.isEmpty) {
            // A function without returns, e.g., because it is guaranteed to throw an exception, is approximated
            // with the lower bound
            return FinalIPResult.lb(state.dm, instr.pc)
        }

        val params = evaluateParameters(getParametersForPCs(List(instr.pc)), exprHandler, instr)
        val refinableResults = getRefinableParameterResults(params.toSeq.map(t => t.toSeq.map(_.toSeq)))
        if (refinableResults.nonEmpty) {
            state.nonFinalFunctionArgs(instr) = params
            InterimIPResult.lbWithIPResultDependees(
                state.dm,
                instr.pc,
                refinableResults.asInstanceOf[Iterable[RefinableIPResult]],
                awaitAllFinalContinuation(
                    SimpleIPResultDepender(instr, instr.pc, state, refinableResults),
                    (_: Iterable[IPResult]) => {
                        val params = state.nonFinalFunctionArgs(instr)
                        state.nonFinalFunctionArgs.remove(instr)

                        interpretArbitraryCallWithCalleeTACAndParams(instr, defSite, calleeMethod)(
                            calleeTac,
                            params.toSeq.map(t => t.toSeq.map(_.toSeq.map(_.asFinal)))
                        )
                    }
                )
            )
        } else {
            interpretArbitraryCallWithCalleeTACAndParams(instr, defSite, calleeMethod)(
                calleeTac,
                params.toSeq.map(t => t.toSeq.map(_.toSeq.map(_.asFinal)))
            )
        }
    }

    private def interpretArbitraryCallWithCalleeTACAndParams(instr: T, defSite: Int, calleeMethod: Method)(
        calleeTac: TAC,
        params:    Seq[Seq[Seq[FinalIPResult]]]
    )(implicit state: State): IPResult = {
        def finalResult(results: Iterable[SomeFinalEP]): FinalIPResult = {
            val sci = StringConstancyInformation.reduceMultiple(
                results.asInstanceOf[Iterable[FinalEP[_, StringConstancyProperty]]].map {
                    _.p.stringConstancyInformation
                }
            )
            FinalIPResult(sci, state.dm, instr.pc)
        }

        val evaluatedParams = convertEvaluatedParameters(params)
        val returns = calleeTac.stmts.filter(_.isInstanceOf[ReturnValue[V]])
        val results = returns.map { ret =>
            val entity = (ret.asInstanceOf[ReturnValue[V]].expr.asVar.toPersistentForm(calleeTac.stmts), calleeMethod)
            state.appendToVar2IndexMapping(entity._1, defSite)

            StringAnalysis.registerParams(entity, evaluatedParams)
            ps(entity, StringConstancyProperty.key)
        }
        if (results.exists(_.isRefinable)) {
            InterimIPResult.lbWithEPSDependees(
                state.dm,
                instr.pc,
                results.filter(_.isRefinable),
                awaitAllFinalContinuation(
                    SimpleEPSDepender(instr, instr.pc, state, results.toIndexedSeq),
                    finalResult _
                )
            )
        } else {
            finalResult(results.toIndexedSeq.asInstanceOf[Iterable[SomeFinalEP]])
        }
    }
}
