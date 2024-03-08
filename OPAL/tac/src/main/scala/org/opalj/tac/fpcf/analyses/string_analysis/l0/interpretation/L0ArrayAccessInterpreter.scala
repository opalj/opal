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
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeFinalEP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * Responsible for processing [[ArrayLoad]] as well as [[ArrayStore]] expressions without a call graph.
 *
 * @author Maximilian Rüsch
 */
case class L0ArrayAccessInterpreter[State <: L0ComputationState](ps: PropertyStore) extends L0StringInterpreter[State] {

    override type T = ArrayLoad[V]

    override def interpret(instr: T, pc: Int)(implicit state: State): ProperPropertyComputationResult = {
        val defSitePCs = getStoreAndLoadDefSitePCs(instr)(state.tac.stmts)
        val results = defSitePCs.map { pc =>
            ps(InterpretationHandler.getEntityFromDefSitePC(pc), StringConstancyProperty.key)
        }

        if (results.exists(_.isRefinable)) {
            InterimResult.forLB(
                InterpretationHandler.getEntityFromDefSitePC(pc),
                StringConstancyProperty.lb,
                results.filter(_.isRefinable).toSet,
                awaitAllFinalContinuation(
                    EPSDepender(instr, pc, state, results),
                    finalResult(pc)
                )
            )
        } else {
            finalResult(pc)(results.asInstanceOf[Iterable[FinalEP[DefSiteEntity, StringConstancyProperty]]])
        }
    }

    private def finalResult(pc: Int)(results: Iterable[SomeFinalEP])(implicit
        state: State
    ): ProperPropertyComputationResult = {
        var resultSci = StringConstancyInformation.reduceMultiple(results.map {
            _.asFinal.p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
        })
        if (resultSci.isTheNeutralElement) {
            resultSci = StringConstancyInformation.lb
        }

        computeFinalResult(pc, resultSci)
    }

    private def getStoreAndLoadDefSitePCs(instr: T)(implicit stmts: Array[Stmt[V]]): List[Int] = {
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

        val allDefSites = defSites ++ instr.arrayRef.asVar.definedBy.toArray.toIndexedSeq.filter(_ < 0)
        allDefSites.toList.map(pcOfDefSite(_)).sorted
    }
}
