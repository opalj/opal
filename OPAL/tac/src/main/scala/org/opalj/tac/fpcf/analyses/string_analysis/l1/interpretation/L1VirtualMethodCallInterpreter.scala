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
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.fpcf.FinalEP

/**
 * Responsible for processing [[VirtualMethodCall]]s in an interprocedural fashion.
 * For supported method calls, see the documentation of the `interpret` function.
 *
 * @author Patrick Mell
 */
case class L1VirtualMethodCallInterpreter(
        override protected val cfg:         CFG[Stmt[V], TACStmts[V]],
        override protected val exprHandler: L1InterpretationHandler
) extends L1StringInterpreter {

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
     */
    override def interpret(instr: T, defSite: Int): FinalEP[T, StringConstancyProperty] = {
        val sci = instr.name match {
            case "setLength" => StringConstancyInformation(StringConstancyLevel.CONSTANT, StringConstancyType.RESET)
            case _           => StringConstancyInformation.getNeutralElement
        }
        FinalEP(instr, StringConstancyProperty(sci))
    }
}
