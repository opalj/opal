/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l1
package interpretation

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.SystemProperties
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.br.fpcf.properties.string.StringTreeOr
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationState
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * Processes assignments of system property references to variables by using the [[SystemProperties]] FPCF property.
 *
 * @author Maximilian Rüsch
 */
private[string] trait L1SystemPropertiesInterpreter extends StringInterpreter {

    implicit val ps: PropertyStore
    implicit val project: SomeProject

    private case class SystemPropertiesDepender(target: PV, var dependee: EOptionP[SomeProject, SystemProperties])

    protected def interpretGetSystemPropertiesCall(target: PV)(
        implicit state: InterpretationState
    ): ProperPropertyComputationResult = {
        val depender = SystemPropertiesDepender(target, ps(project, SystemProperties.key))

        if (depender.dependee.isEPK) {
            InterimResult.forUB(
                InterpretationHandler.getEntity,
                StringFlowFunctionProperty.constForVariableAt(
                    state.pc,
                    depender.target,
                    StringTreeNode.ub
                ),
                Set(depender.dependee),
                continuation(state, depender)
            )
        } else {
            continuation(state, depender)(depender.dependee.asInstanceOf[SomeEPS])
        }
    }

    private def continuation(
        state:    InterpretationState,
        depender: SystemPropertiesDepender
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(ub: SystemProperties) =>
                depender.dependee = eps.asInstanceOf[EOptionP[SomeProject, SystemProperties]]
                val newUB = StringFlowFunctionProperty.constForVariableAt(
                    state.pc,
                    depender.target,
                    StringTreeOr(ub.values.toSeq)
                )
                if (depender.dependee.isRefinable) {
                    InterimResult.forUB(
                        InterpretationHandler.getEntity(using state),
                        newUB,
                        Set(depender.dependee),
                        continuation(state, depender)
                    )
                } else {
                    computeFinalResult(newUB)(using state)
                }

            case _ => throw new IllegalArgumentException(s"Encountered unknown eps: $eps")
        }
    }
}
