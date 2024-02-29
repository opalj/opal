/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import org.opalj.br.Method
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeFinalEP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0VirtualFunctionCallInterpreter
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Responsible for processing [[VirtualFunctionCall]]s with a call graph where applicable.
 * The list of currently supported function calls can be seen in the documentation of [[interpret]].
 *
 * @author Patrick Mell
 */
class L1VirtualFunctionCallInterpreter[State <: L1ComputationState](
    exprHandler:                  InterpretationHandler[State],
    implicit val ps:              PropertyStore,
    implicit val contextProvider: ContextProvider
) extends L0VirtualFunctionCallInterpreter[State](exprHandler)
    with L1StringInterpreter[State]
    with IPResultDependingStringInterpreter[State]
    with EPSDependingStringInterpreter[State] {

    override type T = VirtualFunctionCall[V]

    /**
     * This function interprets an arbitrary [[VirtualFunctionCall]]. If this method returns a
     * [[FinalEP]] instance, the interpretation of this call is already done. Otherwise, a new
     * analysis was triggered whose result is not yet ready. In this case, the result needs to be
     * finalized later on.
     */
    override protected def interpretArbitraryCall(instr: T, defSite: Int)(
        implicit state: State
    ): IPResult = {
        val defSitePC = pcOfDefSite(defSite)(state.tac.stmts)

        val (methods, _) = getMethodsForPC(instr.pc)
        if (methods.isEmpty) {
            return FinalIPResult.lb(state.dm, defSitePC)
        }
        // TODO: Type Iterator!
        val directCallSites = state.callees.directCallSites(state.methodContext)(ps, contextProvider)
        val instrClassName = instr.receiver.asVar.value.asReferenceValue.asReferenceType.mostPreciseObjectType.toJava

        val relevantPCs = directCallSites.filter {
            case (_, calledMethods) => calledMethods.exists { m =>
                    val mClassName = m.method.declaringClassType.toJava
                    m.method.name == instr.name && mClassName == instrClassName
                }
        }.keys

        val params = evaluateParameters(getParametersForPCs(relevantPCs), exprHandler, instr)
        val refinableResults = getRefinableParameterResults(params.toSeq.map(t => t.toSeq.map(_.toSeq)))
        if (refinableResults.nonEmpty) {
            state.nonFinalFunctionArgs(instr) = params
            InterimIPResult.lbWithIPResultDependees(
                state.dm,
                defSitePC,
                refinableResults.asInstanceOf[Iterable[RefinableIPResult]],
                awaitAllFinalContinuation(
                    SimpleIPResultDepender(instr, instr.pc, state, refinableResults),
                    (_: Iterable[IPResult]) => {
                        val params = state.nonFinalFunctionArgs(instr)
                        state.nonFinalFunctionArgs.remove(instr)
                        interpretArbitraryCallWithParams(instr, defSite, methods)(
                            params.toSeq.map(t => t.toSeq.map(_.toSeq.map(_.asFinal)))
                        )
                    }
                )
            )
        } else {
            interpretArbitraryCallWithParams(instr, defSite, methods)(
                params.toSeq.map(t => t.toSeq.map(_.toSeq.map(_.asFinal)))
            )
        }
    }

    private def interpretArbitraryCallWithParams(instr: T, defSite: Int, methods: Seq[Method])(
        params: Seq[Seq[Seq[FinalIPResult]]]
    )(implicit state: State): IPResult = {
        def finalResult(results: Iterable[IPResult]): FinalIPResult = {
            val sci = StringConstancyInformation.reduceMultiple(results.map(_.asFinal.sci))
            FinalIPResult(sci, state.dm, instr.pc)
        }

        val results = methods.map { m =>
            val (tacEOptP, calleeTac) = getTACAI(ps, m, state)

            if (tacEOptP.isRefinable) {
                InterimIPResult.lbWithEPSDependees(
                    state.dm,
                    instr.pc,
                    Seq(tacEOptP),
                    awaitAllFinalContinuation(
                        SimpleEPSDepender(instr, instr.pc, state, Seq(tacEOptP)),
                        (finalEPs: Iterable[SomeFinalEP]) =>
                            interpretArbitraryCallWithCalleeTACAndParams(instr, defSite, m)(
                                finalEPs.head.ub.asInstanceOf[TACAI].tac.get,
                                params
                            )
                    )
                )
            } else if (calleeTac.isEmpty) {
                // When the tac ep is final but we still do not have a callee tac, we cannot infer arbitrary call values at all
                FinalIPResult.lb(state.dm, instr.pc)
            } else {
                interpretArbitraryCallWithCalleeTACAndParams(instr, defSite, m)(calleeTac.get, params)
            }

            val returns = calleeTac.get.stmts.filter(_.isInstanceOf[ReturnValue[V]])
            if (returns.isEmpty) {
                // A function without returns, e.g., because it is guaranteed to throw an exception, is approximated
                // with the lower bound
                FinalIPResult.lb(state.dm, instr.pc)
            } else {
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

                                interpretArbitraryCallWithCalleeTACAndParams(instr, defSite, m)(
                                    calleeTac.get,
                                    params.toSeq.map(t => t.toSeq.map(_.toSeq.map(_.asFinal)))
                                )
                            }
                        )
                    )
                } else {
                    interpretArbitraryCallWithCalleeTACAndParams(instr, defSite, m)(
                        calleeTac.get,
                        params.toSeq.map(t => t.toSeq.map(_.toSeq.map(_.asFinal)))
                    )
                }
            }
        }

        if (results.exists(_.isRefinable)) {
            InterimIPResult.lbWithIPResultDependees(
                state.dm,
                instr.pc,
                results.filter(_.isRefinable).asInstanceOf[Iterable[RefinableIPResult]],
                awaitAllFinalContinuation(
                    SimpleIPResultDepender(instr, instr.pc, state, results),
                    finalResult _
                )
            )
        } else {
            finalResult(results)
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
            finalResult(results.asInstanceOf[Iterable[SomeFinalEP]])
        }
    }
}
