/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP

/**
 * Responsible for processing [[NonVirtualMethodCall]]s in an interprocedural fashion.
 * For supported method calls, see the documentation of the `interpret` function.
 *
 * @author Patrick Mell
 */
case class L1NonVirtualMethodCallInterpreter(
    override protected val cfg:             CFG[Stmt[V], TACStmts[V]],
    override protected val exprHandler:     L1InterpretationHandler,
    state:           L1ComputationState
) extends L1StringInterpreter {

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
     */
    override def interpret(instr: T, defSite: Int): EOptionP[Entity, StringConstancyProperty] = {
        val e: Integer = defSite
        instr.name match {
            case "<init>" => interpretInit(instr, e)
            case _        => FinalEP(e, StringConstancyProperty.getNeutralElement)
        }
    }

    /**
     * Processes an `&lt;init&gt;` method call. If it has no parameters,
     * [[StringConstancyProperty.getNeutralElement]] will be returned. Otherwise, only the very
     * first parameter will be evaluated and its result returned (this is reasonable as both,
     * [[StringBuffer]] and [[StringBuilder]], have only constructors with <= 1 arguments and only
     * these are currently interpreted).
     */
    private def interpretInit(init: T, defSite: Integer): EOptionP[Entity, StringConstancyProperty] = {
        init.params.size match {
            case 0 => FinalEP(defSite, StringConstancyProperty.getNeutralElement)
            case _ =>
                val results = init.params.head.asVar.definedBy.map { ds: Int =>
                    (ds, exprHandler.processDefSite(ds, List()))
                }
                if (results.forall(_._2.isFinal)) {
                    val reduced = StringConstancyInformation.reduceMultiple(results.map { r =>
                        r._2.asFinal.p.stringConstancyInformation
                    })
                    FinalEP(defSite, StringConstancyProperty(reduced))
                } else {
                    // Some intermediate results => register necessary information from final results and return an
                    // intermediate result
                    val returnIR = results.find(r => !r._2.isFinal).get._2
                    results.foreach {
                        case (ds, r) =>
                            if (r.isFinal) {
                                state.appendToFpe2Sci(ds, r.asFinal.p.stringConstancyInformation, reset = true)
                            }
                        case _ =>
                    }
                    returnIR
                }
        }
    }
}
