/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.PropertyStore

/**
 * Responsible for processing [[NonVirtualFunctionCall]]s with a call graph.
 *
 * @author Maximilian Rüsch
 */
case class L1NonVirtualFunctionCallInterpreter[State <: L1ComputationState[State]]()(
    implicit val ps:              PropertyStore,
    implicit val contextProvider: ContextProvider
) extends L1StringInterpreter[State] {

    override type T = NonVirtualFunctionCall[V]

    override def interpret(instr: T, defSite: Int)(implicit state: State): IPResult = {
        val methods = getMethodsForPC(instr.pc)
        if (methods._1.isEmpty) {
            // No methods available => Return lower bound
            return FinalIPResult.lb
        }
        val m = methods._1.head

        val (_, tac) = getTACAI(ps, m, state)
        if (tac.isDefined) {
            state.removeFromMethodPrep2defSite(m, defSite)
            // TAC available => Get return UVars and start the string analysis
            val returns = tac.get.stmts.filter(_.isInstanceOf[ReturnValue[V]])
            if (returns.isEmpty) {
                // A function without returns, e.g., because it is guaranteed to throw an exception, is approximated
                // with the lower bound
                FinalIPResult.lb
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
                if (results.exists(_.isRefinable)) {
                    InterimIPResult.lb
                } else {
                    FinalIPResult(results.head.asFinal.p.stringConstancyInformation)
                }
            }
        } else {
            state.appendToMethodPrep2defSite(m, defSite)
            EmptyIPResult
        }
    }
}
