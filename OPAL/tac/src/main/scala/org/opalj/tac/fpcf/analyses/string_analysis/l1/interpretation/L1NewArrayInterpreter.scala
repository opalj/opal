/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.tac.fpcf.analyses.string_analysis.IPResultDependingStringInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * Interprets [[NewArray]] expressions without a call graph.
 * <p>
 *
 * @author Maximilian Rüsch
 */
class L1NewArrayInterpreter[State <: L1ComputationState[State]](
    exprHandler: InterpretationHandler[State]
) extends L1StringInterpreter[State] with IPResultDependingStringInterpreter[State] {

    override type T = NewArray[V]

    override def interpret(instr: T, defSite: Int)(implicit state: State): IPResult = {
        val defSitePC = pcOfDefSite(defSite)(state.tac.stmts)
        if (instr.counts.length != 1) {
            // Only supports 1-D arrays
            return FinalIPResult.lb(state.dm, defSitePC)
        }

        // Get all sites that define array values and process them
        val arrValuesDefSites =
            state.tac.stmts(defSite).asAssignment.targetVar.asVar.usedBy.toArray.toList.sorted
        var allResults = arrValuesDefSites.filter {
            ds => ds >= 0 && state.tac.stmts(ds).isInstanceOf[ArrayStore[V]]
        }.flatMap { ds =>
            // ds holds a site an of array stores; these need to be evaluated for the actual values
            state.tac.stmts(ds).asArrayStore.value.asVar.definedBy.toArray.toList.sorted.map { d =>
                exprHandler.processDefSite(d)
            }
        }

        // Add information of parameters
        arrValuesDefSites.filter(_ < 0).foreach { ds =>
            val paramPos = Math.abs(ds + 2)
            // IMPROVE should we use lb as the fallback value
            val sci = StringConstancyInformation.reduceMultiple(state.params.map(_(paramPos)))
            val r = FinalIPResult(sci, state.dm, pcOfDefSite(ds)(state.tac.stmts))
            state.fpe2ipr(pcOfDefSite(ds)(state.tac.stmts)) = r
            allResults ::= r
        }

        if (allResults.exists(_.isRefinable)) {
            InterimIPResult.lbWithIPResultDependees(
                state.dm,
                defSitePC,
                allResults.filter(_.isRefinable).asInstanceOf[Iterable[RefinableIPResult]],
                awaitAllFinalContinuation(
                    SimpleIPResultDepender(instr, defSitePC, state, allResults),
                    finalResult(defSitePC)
                )
            )
        } else {
            finalResult(defSitePC)(allResults)
        }
    }

    private def finalResult(pc: Int)(results: Iterable[IPResult])(implicit state: State): FinalIPResult = {
        val resultSci = if (results.forall(_.isNoResult)) {
            StringConstancyInformation.lb
            // It might be that there are no results; in such a case, set the string information to
            // the lower bound and manually add an entry to the results list
        } else {
            StringConstancyInformation.reduceMultiple(results.map(_.asFinal.sci))
        }

        FinalIPResult(resultSci, state.dm, pc)
    }
}
