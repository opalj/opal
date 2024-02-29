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
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeFinalEP
import org.opalj.tac.fpcf.analyses.string_analysis.EPSDependingStringInterpreter

/**
 * Responsible for processing [[NonVirtualFunctionCall]]s with a call graph.
 *
 * @author Maximilian Rüsch
 */
case class L1NonVirtualFunctionCallInterpreter[State <: L1ComputationState]()(
    implicit val ps:              PropertyStore,
    implicit val contextProvider: ContextProvider
) extends L1StringInterpreter[State] with EPSDependingStringInterpreter[State] {

    override type T = NonVirtualFunctionCall[V]

    override def interpret(instr: T, defSite: Int)(implicit state: State): IPResult = {
        val methods = getMethodsForPC(instr.pc)
        if (methods._1.isEmpty) {
            // No methods available => Return lower bound
            return FinalIPResult.lb(state.dm, instr.pc)
        }
        val m = methods._1.head

        val (_, tac) = getTACAI(ps, m, state)
        if (tac.isDefined) {
            // TAC available => Get return UVars and start the string analysis
            val returns = tac.get.stmts.filter(_.isInstanceOf[ReturnValue[V]])
            if (returns.isEmpty) {
                // A function without returns, e.g., because it is guaranteed to throw an exception, is approximated
                // with the lower bound
                FinalIPResult.lb(state.dm, instr.pc)
            } else {
                val results = returns.map { ret =>
                    val puVar = ret.asInstanceOf[ReturnValue[V]].expr.asVar.toPersistentForm(tac.get.stmts)
                    val entity = (puVar, m)

                    val eps = ps(entity.asInstanceOf[Entity], StringConstancyProperty.key)
                    if (eps.isRefinable) {
                        state.dependees = eps :: state.dependees
                        state.appendToVar2IndexMapping(puVar, defSite)
                    }
                    eps
                }
                if (results.exists(_.isRefinable)) {
                    InterimIPResult.lbWithEPSDependees(
                        state.dm,
                        instr.pc,
                        results.filter(_.isRefinable),
                        awaitAllFinalContinuation(
                            SimpleEPSDepender(instr, instr.pc, state, results.toIndexedSeq),
                            finalResult(instr.pc)
                        )
                    )
                } else {
                    finalResult(instr.pc)(results.asInstanceOf[Iterable[SomeFinalEP]])
                }
            }
        } else {
            EmptyIPResult(state.dm, instr.pc)
        }
    }

    def finalResult(pc: Int)(results: Iterable[SomeFinalEP])(implicit state: State): FinalIPResult = {
        val sci = StringConstancyInformation.reduceMultiple(
            results.asInstanceOf[Iterable[EOptionP[_, StringConstancyProperty]]].map(
                _.asFinal.p.stringConstancyInformation
            )
        )

        FinalIPResult(sci, state.dm, pc)
    }
}
