/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation
package interprocedural

import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore

/**
 * The `InterproceduralNonVirtualFunctionCallInterpreter` is responsible for processing
 * [[NonVirtualFunctionCall]]s in an interprocedural fashion.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class InterproceduralNonVirtualFunctionCallInterpreter(
        cfg:             CFG[Stmt[V], TACStmts[V]],
        exprHandler:     InterproceduralInterpretationHandler,
        ps:              PropertyStore,
        state:           InterproceduralComputationState,
        declaredMethods: DeclaredMethods,
        contextProvider:    ContextProvider
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = NonVirtualFunctionCall[V]

    /**
     * Currently, [[NonVirtualFunctionCall]]s are not supported. Thus, this function always returns
     * a list with a single element consisting of
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel.DYNAMIC]],
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyType.APPEND]] and
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation.UnknownWordSymbol]].
     *
     * @note For this implementation, `defSite` plays a role!
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): EOptionP[Entity, StringConstancyProperty] = {
        val methods = getMethodsForPC(instr.pc, ps, state.callees, contextProvider)
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
