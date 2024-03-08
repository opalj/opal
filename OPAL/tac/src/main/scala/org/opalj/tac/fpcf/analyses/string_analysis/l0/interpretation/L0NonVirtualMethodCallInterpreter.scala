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
 * @author Maximilian Rüsch
 */
case class L0NonVirtualMethodCallInterpreter[State <: L0ComputationState](ps: PropertyStore)
    extends L0StringInterpreter[State] {

    override type T = NonVirtualMethodCall[V]

    override def interpret(instr: T, pc: Int)(implicit state: State): ProperPropertyComputationResult = {
        instr.name match {
            case "<init>" => interpretInit(instr, pc)
            case _        => computeFinalResult(pc, StringConstancyInformation.getNeutralElement)
        }
    }

    private def interpretInit(init: T, pc: Int)(implicit state: State): ProperPropertyComputationResult = {
        init.params.size match {
            case 0 => computeFinalResult(pc, StringConstancyInformation.getNeutralElement)
            case _ =>
                // Only StringBuffer and StringBuilder are interpreted which have constructors with <= 1 parameters
                val results = init.params.head.asVar.definedBy.toList.map { ds =>
                    ps(InterpretationHandler.getEntityFromDefSite(ds), StringConstancyProperty.key)
                }
                if (results.forall(_.isFinal)) {
                    finalResult(init.pc)(results.asInstanceOf[Iterable[FinalEP[DefSiteEntity, StringConstancyProperty]]])
                } else {
                    InterimResult.forLB(
                        InterpretationHandler.getEntityFromDefSitePC(pc),
                        StringConstancyProperty.lb,
                        results.toSet,
                        awaitAllFinalContinuation(
                            EPSDepender(init, pc, state, results),
                            finalResult(pc)
                        )
                    )
                }
        }
    }

    private def finalResult(pc: Int)(results: Iterable[SomeEPS])(implicit
        state: State
    ): Result =
        computeFinalResult(
            pc,
            StringConstancyInformation.reduceMultiple(results.map {
                _.asFinal.p.asInstanceOf[StringConstancyProperty].sci
            })
        )
}
