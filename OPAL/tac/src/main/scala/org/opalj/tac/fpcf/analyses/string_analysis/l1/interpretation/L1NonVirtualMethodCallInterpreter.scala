/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * Responsible for processing [[NonVirtualMethodCall]]s in an interprocedural fashion.
 * For supported method calls, see the documentation of the `interpret` function.
 *
 * @author Patrick Mell
 */
case class L1NonVirtualMethodCallInterpreter[State <: ComputationState[State]](
        exprHandler: InterpretationHandler[State]
) extends L1StringInterpreter[State] {

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
    override def interpret(instr: T, defSite: Int)(implicit state: State): EOptionP[Entity, StringConstancyProperty] = {
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
    private def interpretInit(init: T, defSite: Integer)(implicit
        state: State
    ): EOptionP[Entity, StringConstancyProperty] = {
        init.params.size match {
            case 0 => FinalEP(defSite, StringConstancyProperty.getNeutralElement)
            case _ =>
                val results = init.params.head.asVar.definedBy.map { ds: Int =>
                    (pcOfDefSite(ds)(state.tac.stmts), exprHandler.processDefSite(ds))
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
                        case (pc, r) =>
                            if (r.isFinal) {
                                state.appendToFpe2Sci(pc, r.asFinal.p.stringConstancyInformation, reset = true)
                            }
                        case _ =>
                    }
                    returnIR
                }
        }
    }
}
