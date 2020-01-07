/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.AbstractStringInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralComputationState
import org.opalj.tac.ArrayStore
import org.opalj.tac.NewArray

/**
 * The `NewArrayPreparer` is responsible for preparing [[NewArray]] expressions.
 * <p>
 * Not all (partial) results are guaranteed to be available at once, thus intermediate results
 * might be produced. This interpreter will only compute the parts necessary to later on fully
 * assemble the final result for the array interpretation.
 * For more information, see the [[interpret]] method.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class NewArrayPreparer(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: InterproceduralInterpretationHandler,
        state:       InterproceduralComputationState,
        params:      List[Seq[StringConstancyInformation]]
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = NewArray[V]

    /**
     * @note This implementation will extend [[state.fpe2sci]] in a way that it adds the string
     *       constancy information for each definition site where it can compute a final result. All
     *       definition sites producing a refineable result will have to be handled later on to
     *       not miss this information.
     *
     * @note For this implementation, `defSite` plays a role!
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): EOptionP[Entity, StringConstancyProperty] = {
        // Only support for 1-D arrays
        if (instr.counts.length != 1) {
            FinalEP(instr, StringConstancyProperty.lb)
        }

        // Get all sites that define array values and process them
        val arrValuesDefSites =
            state.tac.stmts(defSite).asAssignment.targetVar.asVar.usedBy.toArray.toList.sorted
        var allResults = arrValuesDefSites.filter {
            ds ⇒ ds >= 0 && state.tac.stmts(ds).isInstanceOf[ArrayStore[V]]
        }.flatMap { ds ⇒
            // ds holds a site an of array stores; these need to be evaluated for the actual values
            state.tac.stmts(ds).asArrayStore.value.asVar.definedBy.toArray.toList.sorted.map { d ⇒
                val r = exprHandler.processDefSite(d, params)
                if (r.isFinal) {
                    state.appendToFpe2Sci(d, r.asFinal.p.stringConstancyInformation)
                }
                r
            }
        }

        // Add information of parameters
        arrValuesDefSites.filter(_ < 0).foreach { ds ⇒
            val paramPos = Math.abs(ds + 2)
            // lb is the fallback value
            val sci = StringConstancyInformation.reduceMultiple(params.map(_(paramPos)))
            state.appendToFpe2Sci(ds, sci)
            val e: Integer = ds
            allResults ::= FinalEP(e, StringConstancyProperty(sci))
        }

        val interims = allResults.find(!_.isFinal)
        if (interims.isDefined) {
            interims.get
        } else {
            var resultSci = StringConstancyInformation.reduceMultiple(allResults.map {
                _.asFinal.p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
            })
            // It might be that there are no results; in such a case, set the string information to
            // the lower bound and manually add an entry to the results list
            if (resultSci.isTheNeutralElement) {
                resultSci = StringConstancyInformation.lb
            }
            if (allResults.isEmpty) {
                val toAppend = FinalEP(instr, StringConstancyProperty(resultSci))
                allResults = toAppend :: allResults
            }
            state.appendToFpe2Sci(defSite, resultSci)
            FinalEP(Integer.valueOf(defSite), StringConstancyProperty(resultSci))
        }
    }

}

