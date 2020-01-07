/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.AbstractStringInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralComputationState

/**
 * The `InterproceduralNonVirtualMethodCallInterpreter` is responsible for processing
 * [[NonVirtualMethodCall]]s in an interprocedural fashion.
 * For supported method calls, see the documentation of the `interpret` function.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class InterproceduralNonVirtualMethodCallInterpreter(
        cfg:             CFG[Stmt[V], TACStmts[V]],
        exprHandler:     InterproceduralInterpretationHandler,
        ps:              PropertyStore,
        state:           InterproceduralComputationState,
        declaredMethods: DeclaredMethods
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = NonVirtualMethodCall[V]

    /**
     * Currently, this function supports the interpretation of the following non virtual methods:
     * <ul>
     * <li>
     * `&lt;init&gt;`, when initializing an object (for this case, currently zero constructor or
     * one constructor parameter are supported; if more params are available, only the very first
     * one is interpreted).
     * </li>
     * </ul>
     * For all other calls, an empty list will be returned at the moment.
     *
     * @note For this implementation, `defSite` plays a role!
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(
        instr: NonVirtualMethodCall[V], defSite: Int
    ): EOptionP[Entity, StringConstancyProperty] = {
        val e: Integer = defSite
        instr.name match {
            case "<init>" ⇒ interpretInit(instr, e)
            case _        ⇒ FinalEP(e, StringConstancyProperty.getNeutralElement)
        }
    }

    /**
     * Processes an `&lt;init&gt;` method call. If it has no parameters,
     * [[StringConstancyProperty.getNeutralElement]] will be returned. Otherwise, only the very
     * first parameter will be evaluated and its result returned (this is reasonable as both,
     * [[StringBuffer]] and [[StringBuilder]], have only constructors with <= 1 arguments and only
     * these are currently interpreted).
     */
    private def interpretInit(
        init: NonVirtualMethodCall[V], defSite: Integer
    ): EOptionP[Entity, StringConstancyProperty] = {
        init.params.size match {
            case 0 ⇒ FinalEP(defSite, StringConstancyProperty.getNeutralElement)
            case _ ⇒
                val results = init.params.head.asVar.definedBy.map { ds: Int ⇒
                    (ds, exprHandler.processDefSite(ds, List()))
                }
                if (results.forall(_._2.isFinal)) {
                    // Final result is available
                    val reduced = StringConstancyInformation.reduceMultiple(results.map { r ⇒
                        val prop = r._2.asFinal.p.asInstanceOf[StringConstancyProperty]
                        prop.stringConstancyInformation
                    })
                    FinalEP(defSite, StringConstancyProperty(reduced))
                } else {
                    // Some intermediate results => register necessary information from final
                    // results and return an intermediate result
                    val returnIR = results.find(r ⇒ !r._2.isFinal).get._2
                    results.foreach {
                        case (ds, r) ⇒
                            if (r.isFinal) {
                                val p = r.asFinal.p.asInstanceOf[StringConstancyProperty]
                                state.appendToFpe2Sci(
                                    ds, p.stringConstancyInformation, reset = true
                                )
                            }
                        case _ ⇒
                    }
                    returnIR
                }
        }
    }

}
