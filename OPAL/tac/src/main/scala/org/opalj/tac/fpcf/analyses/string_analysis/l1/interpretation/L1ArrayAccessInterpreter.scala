/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import scala.collection.mutable.ListBuffer

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.tac.ArrayLoad
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.V
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * Responsible for preparing [[ArrayLoad]] as well as [[ArrayStore]] expressions in an interprocedural fashion.
 * <p>
 * Not all (partial) results are guaranteed to be available at once, thus intermediate results might be produced.
 * This interpreter will only compute the parts necessary to later on fully assemble the final result for the array
 * interpretation.
 * For more information, see the [[interpret]] method.
 *
 * @author Patrick Mell
 */
case class L1ArrayAccessInterpreter[State <: ComputationState[State]](
        override protected val cfg:         CFG[Stmt[V], TACStmts[V]],
        override protected val exprHandler: InterpretationHandler[State],
        state:                              State
) extends L1StringInterpreter[State] {

    override type T = ArrayLoad[V]

    /**
     * @note This implementation will extend [[state.fpe2sci]] in a way that it adds the string
     *       constancy information for each definition site where it can compute a final result. All
     *       definition sites producing a refinable result will have to be handled later on to
     *       not miss this information.
     */
    override def interpret(instr: T, defSite: Int)(implicit state: State): EOptionP[Entity, StringConstancyProperty] = {
        val results = ListBuffer[EOptionP[Entity, StringConstancyProperty]]()

        val allDefSitesByPC = L1ArrayAccessInterpreter.getStoreAndLoadDefSites(instr, state.tac.stmts)
            .map(ds => (pcOfDefSite(ds)(state.tac.stmts), ds)).toMap
        allDefSitesByPC.keys.map { pc => (pc, exprHandler.processDefSite(allDefSitesByPC(pc))) }.foreach {
            case (pc, ep) =>
                if (ep.isFinal)
                    state.appendToFpe2Sci(pc, ep.asFinal.p.stringConstancyInformation)
                results.append(ep)
        }

        // Add information of parameters
        instr.arrayRef.asVar.toPersistentForm(state.tac.stmts).defPCs.filter(_ < 0).foreach { pc =>
            val paramPos = Math.abs(pc + 2)
            val sci = StringConstancyInformation.reduceMultiple(state.params.map(_(paramPos)))
            state.appendToFpe2Sci(pc, sci)
        }

        // If there is at least one InterimResult, return one. Otherwise, return a final result
        // (to either indicate that further computation are necessary or a final result is already present)
        val interims = results.find(!_.isFinal)
        if (interims.isDefined) {
            interims.get
        } else {
            var resultSci = StringConstancyInformation.reduceMultiple(results.map {
                _.asFinal.p.stringConstancyInformation
            })
            // It might be that there are no results; in such a case, set the string information to
            // the lower bound and manually add an entry to the results list
            if (resultSci.isTheNeutralElement) {
                resultSci = StringConstancyInformation.lb
            }
            if (results.isEmpty) {
                results.append(FinalEP(
                    (instr.arrayRef.asVar, state.entity._2),
                    StringConstancyProperty(resultSci)
                ))
            }

            state.appendToFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), resultSci)
            results.head
        }
    }

}

object L1ArrayAccessInterpreter {

    type T = ArrayLoad[V]

    /**
     * This function retrieves all definition sites of the array stores and array loads that belong
     * to the given instruction.
     *
     * @param instr The [[ArrayLoad]] instruction to get the definition sites for.
     * @param stmts The set of statements to use.
     * @return Returns all definition sites associated with the array stores and array loads of the
     *         given instruction. The result list is sorted in ascending order.
     */
    def getStoreAndLoadDefSites(instr: T, stmts: Array[Stmt[V]]): List[Int] = {
        var defSites = IntTrieSet.empty
        instr.arrayRef.asVar.definedBy.toArray.filter(_ >= 0).sorted.foreach { next =>
            stmts(next).asAssignment.targetVar.usedBy.toArray.sorted.foreach {
                stmts(_) match {
                    case ArrayStore(_, _, _, value) =>
                        defSites = defSites ++ value.asVar.definedBy.toArray
                    case Assignment(_, _, expr: ArrayLoad[V]) =>
                        defSites = defSites ++ expr.asArrayLoad.arrayRef.asVar.definedBy.toArray
                    case _ =>
                }
            }
        }

        defSites.toList.sorted
    }
}
