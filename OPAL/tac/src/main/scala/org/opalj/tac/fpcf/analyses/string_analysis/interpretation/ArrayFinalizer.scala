/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation

import scala.collection.mutable.ListBuffer

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.tac.fpcf.analyses.string_analysis.ComputationState
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.ArrayLoad
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts

/**
 * @author Patrick Mell
 */
class ArrayFinalizer(cfg: CFG[Stmt[V], TACStmts[V]], state: ComputationState) {

    type T = ArrayLoad[V]

    def interpret(
        instr: T, defSite: Int
    ): Unit = {
        val stmts = cfg.code.instructions
        val allDefSites = ListBuffer[Int]()
        val defSites = instr.arrayRef.asVar.definedBy.toArray
        defSites.filter(_ >= 0).sorted.foreach { next ⇒
            val arrDecl = stmts(next)
            val sortedArrDeclUses = arrDecl.asAssignment.targetVar.usedBy.toArray.sorted
            // Process ArrayStores
            sortedArrDeclUses.filter {
                stmts(_).isInstanceOf[ArrayStore[V]]
            } foreach { f: Int ⇒
                allDefSites.appendAll(stmts(f).asArrayStore.value.asVar.definedBy.toArray)
            }
            // Process ArrayLoads
            sortedArrDeclUses.filter {
                stmts(_) match {
                    case Assignment(_, _, _: ArrayLoad[V]) ⇒ true
                    case _                                 ⇒ false
                }
            } foreach { f: Int ⇒
                val defs = stmts(f).asAssignment.expr.asArrayLoad.arrayRef.asVar.definedBy
                allDefSites.appendAll(defs.toArray)
            }
        }

        state.fpe2sci(defSite) = StringConstancyInformation.reduceMultiple(
            allDefSites.sorted.map { state.fpe2sci(_) }.toList
        )
    }

}
