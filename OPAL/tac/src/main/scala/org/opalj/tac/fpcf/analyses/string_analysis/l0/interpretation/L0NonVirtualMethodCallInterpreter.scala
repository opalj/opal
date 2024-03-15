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
case class L0NonVirtualMethodCallInterpreter(ps: PropertyStore) extends StringInterpreter {

    override type T = NonVirtualMethodCall[V]

    override def interpret(instr: T, pc: Int)(implicit state: DefSiteState): ProperPropertyComputationResult = {
        instr.name match {
            case "<init>" => interpretInit(instr, pc)
            case _        => computeFinalResult(pc, StringConstancyInformation.neutralElement)
        }
    }

    private def interpretInit(init: T, pc: Int)(implicit state: DefSiteState): ProperPropertyComputationResult = {
        init.params.size match {
            case 0 => computeFinalResult(pc, StringConstancyInformation.neutralElement)
            case _ =>
                // Only StringBuffer and StringBuilder are interpreted which have constructors with <= 1 parameters
                val results = init.params.head.asVar.definedBy.toList.map { ds =>
                    ps(InterpretationHandler.getEntityForDefSite(ds), StringConstancyProperty.key)
                }
                if (results.forall(_.isFinal)) {
                    finalResult(init.pc)(results.asInstanceOf[Seq[FinalEP[DefSiteEntity, StringConstancyProperty]]])
                } else {
                    InterimResult.forLB(
                        InterpretationHandler.getEntityForPC(pc),
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

    private def finalResult(pc: Int)(results: Seq[SomeEPS])(implicit state: DefSiteState): Result =
        computeFinalResult(
            pc,
            StringConstancyInformation.reduceMultiple(results.map {
                _.asFinal.p.asInstanceOf[StringConstancyProperty].sci
            })
        )
}
