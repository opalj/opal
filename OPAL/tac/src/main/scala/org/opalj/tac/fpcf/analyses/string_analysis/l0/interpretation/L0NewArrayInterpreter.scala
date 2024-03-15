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
import org.opalj.fpcf.SomeFinalEP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * @author Maximilian Rüsch
 */
case class L0NewArrayInterpreter(ps: PropertyStore) extends StringInterpreter {

    override type T = NewArray[V]

    override def interpret(instr: T, pc: Int)(implicit state: DefSiteState): ProperPropertyComputationResult = {
        if (instr.counts.length != 1) {
            // Only supports 1-D arrays
            return computeFinalResult(pc, StringConstancyInformation.lb)
        }

        // Get all sites that define array values and process them
        val defSite = valueOriginOfPC(pc, state.tac.pcToIndex).get;
        val arrValuesDefSites = state.tac.stmts(defSite).asAssignment.targetVar.asVar.usedBy.toArray.toList.sorted
        val allResults = arrValuesDefSites.flatMap { ds =>
            if (ds >= 0 && state.tac.stmts(ds).isInstanceOf[ArrayStore[V]]) {
                state.tac.stmts(ds).asArrayStore.value.asVar.definedBy.toArray.toList.sorted.map { ds =>
                    ps(InterpretationHandler.getEntityForDefSite(ds), StringConstancyProperty.key)
                }
            } else if (ds < 0) {
                Seq(ps(InterpretationHandler.getEntityForDefSite(ds), StringConstancyProperty.key))
            } else {
                Seq.empty
            }
        }

        if (allResults.exists(_.isRefinable)) {
            InterimResult.forLB(
                InterpretationHandler.getEntityForPC(pc),
                StringConstancyProperty.lb,
                allResults.filter(_.isRefinable).toSet,
                awaitAllFinalContinuation(
                    EPSDepender(instr, pc, state, allResults),
                    finalResult(pc)
                )
            )
        } else {
            finalResult(pc)(allResults.asInstanceOf[Seq[FinalEP[DefSiteEntity, StringConstancyProperty]]])
        }
    }

    private def finalResult(pc: Int)(results: Seq[SomeFinalEP])(implicit state: DefSiteState): Result = {
        val resultsScis = results.map(_.p.asInstanceOf[StringConstancyProperty].sci)
        val sci = if (resultsScis.forall(_.isTheNeutralElement)) {
            // It might be that there are no results; in such a case, set the string information to the lower bound
            StringConstancyInformation.lb
        } else {
            StringConstancyInformation.reduceMultiple(resultsScis)
        }

        computeFinalResult(pc, sci)
    }
}
