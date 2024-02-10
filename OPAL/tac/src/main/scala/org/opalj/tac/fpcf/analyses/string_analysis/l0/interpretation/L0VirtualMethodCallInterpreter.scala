/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP

/**
 * Responsible for processing [[VirtualMethodCall]]s in an intraprocedural fashion.
 * For supported method calls, see the documentation of the `interpret` function.
 *
 * @author Maximilian Rüsch
 */
case class L0VirtualMethodCallInterpreter[State <: L0ComputationState[State]]() extends L0StringInterpreter[State] {

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
     *
     * For all other calls, a result containing [[StringConstancyProperty.getNeutralElement]] will be returned.
     */
    override def interpret(instr: T, defSite: Int)(implicit state: State): EOptionP[Entity, StringConstancyProperty] = {
        val sci = instr.name match {
            case "setLength" => StringConstancyInformation(StringConstancyLevel.CONSTANT, StringConstancyType.RESET)
            case _           => StringConstancyInformation.getNeutralElement
        }
        FinalEP(instr, StringConstancyProperty(sci))
    }
}
