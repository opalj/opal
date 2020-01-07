/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.AbstractStringInterpreter

/**
 * The `InterproceduralVirtualMethodCallInterpreter` is responsible for processing
 * [[VirtualMethodCall]]s in an interprocedural fashion.
 * For supported method calls, see the documentation of the `interpret` function.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class InterproceduralVirtualMethodCallInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: InterproceduralInterpretationHandler,
        callees:     Callees
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = VirtualMethodCall[V]

    /**
     * Currently, this function supports the interpretation of the following virtual methods:
     * <ul>
     * <li>
     * `setLength`: `setLength` is a method to reset / clear a [[StringBuilder]] / [[StringBuffer]]
     * (at least when called with the argument `0`). For simplicity, this interpreter currently
     * assumes that 0 is always passed, i.e., the `setLength` method is currently always regarded as
     * a reset mechanism.
     * </li>
     * </ul>
     * For all other calls, an empty list will be returned.
     *
     * @note For this implementation, `defSite` plays a role!
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): EOptionP[Entity, StringConstancyProperty] = {
        val sci = instr.name match {
            case "setLength" ⇒ StringConstancyInformation(
                StringConstancyLevel.CONSTANT, StringConstancyType.RESET
            )
            case _ ⇒ StringConstancyInformation.getNeutralElement
        }
        FinalEP(instr, StringConstancyProperty(sci))
    }

}
