/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package interpretation

import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * Processes expressions that are relevant in order to determine which value(s) the string value at a given def site
 * might have.
 *
 * [[InterpretationHandler]]s of any level may use [[StringInterpreter]]s from their level or any level below.
 * [[StringInterpreter]]s defined in the [[interpretation]] package may be used by any level.
 *
 * @author Maximilian Rüsch
 */
abstract class InterpretationHandler extends FPCFAnalysis {

    def analyze(entity: MethodPC): ProperPropertyComputationResult = {
        val tacaiEOptP = ps(entity.dm.definedMethod, TACAI.key)
        implicit val state: InterpretationState = InterpretationState(entity.pc, entity.dm, tacaiEOptP)

        if (tacaiEOptP.isRefinable) {
            InterimResult.forUB(
                InterpretationHandler.getEntity,
                StringFlowFunctionProperty.ub,
                Set(state.tacDependee),
                continuation(state)
            )
        } else if (tacaiEOptP.ub.tac.isEmpty) {
            // No TAC available, e.g., because the method has no body
            StringInterpreter.computeFinalResult(StringFlowFunctionProperty.constForAll(StringTreeNode.lb))
        } else {
            processNew
        }
    }

    private def continuation(state: InterpretationState)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case finalEP: FinalEP[_, _] if
                    eps.pk.equals(TACAI.key) =>
                state.tacDependee = finalEP.asInstanceOf[FinalEP[Method, TACAI]]
                processNew(state)

            case _ =>
                InterimResult.forUB(
                    InterpretationHandler.getEntity(state),
                    StringFlowFunctionProperty.ub,
                    Set(state.tacDependee),
                    continuation(state)
                )
        }
    }

    protected def processNew(implicit state: InterpretationState): ProperPropertyComputationResult
}

object InterpretationHandler {

    def getEntity(implicit state: InterpretationState): MethodPC = getEntity(state.pc, state.dm)

    def getEntity(pc: Int)(implicit state: InterpretationState): MethodPC = getEntity(pc, state.dm)

    def getEntity(pc: Int, dm: DefinedMethod): MethodPC = MethodPC(pc, dm)
}
