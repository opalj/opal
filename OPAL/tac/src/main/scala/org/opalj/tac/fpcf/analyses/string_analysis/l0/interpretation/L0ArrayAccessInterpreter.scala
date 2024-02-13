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
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.DependingStringInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * Responsible for processing [[ArrayLoad]] as well as [[ArrayStore]] expressions in an intraprocedural fashion.
 *
 * @author Maximilian Rüsch
 */
case class L0ArrayAccessInterpreter[State <: L0ComputationState[State]](
    exprHandler: InterpretationHandler[State]
) extends L0StringInterpreter[State] with DependingStringInterpreter[State] {

    implicit val _exprHandler: InterpretationHandler[State] = exprHandler

    override type T = ArrayLoad[V]

    override def interpret(instr: T, defSite: Int)(implicit state: State): EOptionP[Entity, StringConstancyProperty] = {
        implicit val stmts: Array[Stmt[V]] = state.tac.stmts

        val allDefSitesByPC =
            L0ArrayAccessInterpreter.getStoreAndLoadDefSites(instr).map(ds => (pcOfDefSite(ds), ds)).toMap
        val sciOpts = allDefSitesByPC.keys.toList.sorted.map { pc =>
            (pc, handleDependentDefSite(allDefSitesByPC(pc)))
        }.map {
            case (pc, sciOpt) =>
                if (sciOpt.isDefined)
                    state.appendToFpe2Sci(pc, sciOpt.get)
                sciOpt
        }

        // Add information of parameters
        // TODO dont we have to incorporate parameter information into the scis?
        instr.arrayRef.asVar.toPersistentForm.defPCs.filter(_ < 0).foreach { pc =>
            val paramPos = Math.abs(pc + 2)
            val sci = StringConstancyInformation.reduceMultiple(state.params.map(_(paramPos)))
            state.appendToFpe2Sci(pc, sci)
        }

        val unfinishedDependees = sciOpts.exists(_.isEmpty)
        if (unfinishedDependees) {
            // IMPROVE return interim here
            FinalEP((instr.arrayRef.asVar, state.entity._2), StringConstancyProperty.lb)
        } else {
            var resultSci = StringConstancyInformation.reduceMultiple(sciOpts.map(_.get))
            if (resultSci.isTheNeutralElement) {
                resultSci = StringConstancyInformation.lb
            }

            state.appendToFpe2Sci(pcOfDefSite(defSite), resultSci)
            FinalEP((instr.arrayRef.asVar, state.entity._2), StringConstancyProperty(resultSci))
        }
    }
}

object L0ArrayAccessInterpreter {

    type T = ArrayLoad[V]

    /**
     * This function retrieves all definition sites of the array stores and array loads that belong to the given instruction.
     *
     * @return All definition sites associated with the array stores and array loads sorted in ascending order.
     */
    def getStoreAndLoadDefSites(instr: T)(implicit stmts: Array[Stmt[V]]): List[Int] = {
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
