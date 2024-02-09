/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation

import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalP

/**
 * @author Maximilian Rüsch
 */
trait DependingStringInterpreter[State <: ComputationState[State]] extends StringInterpreter[State] {

    protected def handleDependentDefSite(defSite: Int)(implicit
        state:       State,
        exprHandler: InterpretationHandler[State]
    ): Option[StringConstancyInformation] = {
        handleInterpretationResult(exprHandler.processDefSite(defSite))
    }

    protected def handleInterpretationResult(ep: EOptionP[Entity, StringConstancyProperty])(implicit
        state: State
    ): Option[StringConstancyInformation] = {
        ep match {
            case FinalP(p) =>
                Some(p.stringConstancyInformation)
            case eps =>
                state.dependees = eps :: state.dependees
                None
        }
    }
}
