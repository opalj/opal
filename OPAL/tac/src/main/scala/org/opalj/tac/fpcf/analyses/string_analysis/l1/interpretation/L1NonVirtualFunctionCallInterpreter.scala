/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore

/**
 * Responsible for processing [[NonVirtualFunctionCall]]s in an interprocedural fashion.
 *
 * @author Maximilian Rüsch
 */
case class L1NonVirtualFunctionCallInterpreter(
        override protected val cfg:         CFG[Stmt[V], TACStmts[V]],
        override protected val exprHandler: L1InterpretationHandler,
        ps:                                 PropertyStore,
        state:                              L1ComputationState,
        contextProvider:                    ContextProvider
) extends L1StringInterpreter {

    override type T = NonVirtualFunctionCall[V]

    override def interpret(instr: T, defSite: Int): EOptionP[Entity, StringConstancyProperty] = {
        val methods = getMethodsForPC(instr.pc)(ps, state.callees, contextProvider)
        if (methods._1.isEmpty) {
            // No methods available => Return lower bound
            return FinalEP(instr, StringConstancyProperty.lb)
        }
        val m = methods._1.head

        val (_, tac) = getTACAI(ps, m, state)
        if (tac.isDefined) {
            state.removeFromMethodPrep2defSite(m, defSite)
            // TAC available => Get return UVars and start the string analysis
            val returns = tac.get.stmts.filter(_.isInstanceOf[ReturnValue[V]])
            if (returns.isEmpty) {
                // A function without returns, e.g., because it is guaranteed to throw an exception,
                // is approximated with the lower bound
                FinalEP(instr, StringConstancyProperty.lb)
            } else {
                val results = returns.map { ret =>
                    val puVar = ret.asInstanceOf[ReturnValue[V]].expr.asVar.toPersistentForm(tac.get.stmts)
                    val entity = (puVar, m)

                    val eps = ps(entity, StringConstancyProperty.key)
                    if (eps.isRefinable) {
                        state.dependees = eps :: state.dependees
                        state.appendToVar2IndexMapping(puVar, defSite)
                    }
                    eps
                }
                results.find(_.isRefinable).getOrElse(results.head)
            }
        } else {
            state.appendToMethodPrep2defSite(m, defSite)
            EPK(state.entity, StringConstancyProperty.key)
        }
    }
}
