/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * Responsible for processing [[NonVirtualMethodCall]]s without a call graph.
 *
 * @author Maximilian Rüsch
 */
case class L0NonVirtualMethodCallInterpreter[State <: L0ComputationState](ps: PropertyStore)
    extends L0StringInterpreter[State] {

    override type T = NonVirtualMethodCall[V]

    /**
     * Currently, this function supports the interpretation of the following non virtual methods:
     * <ul>
     * <li>
     * `&lt;init&gt;`, when initializing an object (for this case, currently zero constructor or
     * one constructor parameter are supported; if more params are available, only the very first
     * one is interpreted).
     * </li>
     * </ul>
     *
     * For all other calls, a [[NoIPResult]] will be returned.
     */
    override def interpret(instr: T, defSite: Int)(implicit state: State): ProperPropertyComputationResult = {
        instr.name match {
            case "<init>" => interpretInit(instr)
            case _        => computeFinalResult(defSite, StringConstancyInformation.getNeutralElement)
        }
    }

    /**
     * Processes an `&lt;init&gt;` method call. If it has no parameters, [[NoIPResult]] will be returned. Otherwise,
     * only the very first parameter will be evaluated and its result returned (this is reasonable as both,
     * [[StringBuffer]] and [[StringBuilder]], have only constructors with <= 1 arguments and only these are currently
     * interpreted).
     */
    private def interpretInit(init: T)(implicit state: State): ProperPropertyComputationResult = {
        val entity = InterpretationHandler.getEntityFromDefSitePC(init.pc)

        init.params.size match {
            case 0 => computeFinalResult(FinalEP(entity, StringConstancyProperty.getNeutralElement))
            case _ =>
                val results = init.params.head.asVar.definedBy.toList.map { ds =>
                    ps(InterpretationHandler.getEntityFromDefSite(ds), StringConstancyProperty.key)
                }
                if (results.forall(_.isFinal)) {
                    finalResult(init.pc)(results.asInstanceOf[Iterable[FinalEP[DefSiteEntity, StringConstancyProperty]]])
                } else {
                    InterimResult.forLB(
                        entity,
                        StringConstancyProperty.lb,
                        results.toSet,
                        awaitAllFinalContinuation(
                            EPSDepender(init, init.pc, state, results),
                            finalResult(init.pc)
                        )
                    )
                }
        }
    }

    private def finalResult(pc: Int)(results: Iterable[SomeEPS])(implicit
        state: State
    ): Result =
        computeFinalResult(FinalEP(
            InterpretationHandler.getEntityFromDefSitePC(pc),
            StringConstancyProperty(StringConstancyInformation.reduceMultiple(results.map {
                _.asFinal.p.asInstanceOf[StringConstancyProperty].sci
            }))
        ))
}
