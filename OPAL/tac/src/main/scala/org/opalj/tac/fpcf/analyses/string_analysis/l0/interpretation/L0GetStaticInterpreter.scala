/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP

/**
 * The `IntraproceduralGetStaticInterpreter` is responsible for processing
 * [[org.opalj.tac.GetStatic]]s in an intraprocedural fashion. Thus, they are not analyzed but a
 * fixed [[org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation]] is returned
 * (see [[interpret]]).
 *
 * @author Patrick Mell
 */
case class L0GetStaticInterpreter[State <: L0ComputationState[State]]() extends L0StringInterpreter[State] {

    override type T = GetStatic

    /**
     * Currently, this type is not interpreted. Thus, this function always returns [[StringConstancyProperty.lb]].
     */
    override def interpret(instr: T, defSite: Int)(implicit state: State): EOptionP[Entity, StringConstancyProperty] =
        FinalEP(instr, StringConstancyProperty.lb)
}
