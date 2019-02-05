/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural

import org.opalj.fpcf.ProperOnUpdateContinuation
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.ComputationState
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.AbstractStringInterpreter

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
        cfg: CFG[Stmt[V], TACStmts[V]],
        // TODO: Do not let an instance of InterproceduralInterpretationHandler handler pass here
        //  but let it be instantiated in this class
        exprHandler:     InterproceduralInterpretationHandler,
        ps:              PropertyStore,
        state:           ComputationState,
        declaredMethods: DeclaredMethods,
        c:               ProperOnUpdateContinuation
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
    ): ProperPropertyComputationResult = {
        val e: Integer = defSite
        instr.name match {
            case "<init>" ⇒ interpretInit(instr, e)
            case _        ⇒ Result(e, StringConstancyProperty.getNeutralElement)
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
    ): ProperPropertyComputationResult = {
        init.params.size match {
            case 0 ⇒ Result(defSite, StringConstancyProperty.getNeutralElement)
            case _ ⇒
                val results = init.params.head.asVar.definedBy.map { ds: Int ⇒
                    (ds, exprHandler.processDefSite(ds, List()))
                }
                if (results.forall(_._2.isInstanceOf[Result])) {
                    // Final result is available
                    val scis = results.map(r ⇒
                        StringConstancyProperty.extractFromPPCR(r._2).stringConstancyInformation)
                    val reduced = StringConstancyInformation.reduceMultiple(scis.toList)
                    Result(defSite, StringConstancyProperty(reduced))
                } else {
                    // Some intermediate results => register necessary information from final
                    // results and return an intermediate result
                    val returnIR = results.find(r ⇒ !r._2.isInstanceOf[Result]).get._2
                    results.foreach {
                        case (ds, r: Result) ⇒
                            state.appendResultToFpe2Sci(ds, r)
                        case _ ⇒
                    }
                    returnIR
                }
        }
    }

}
