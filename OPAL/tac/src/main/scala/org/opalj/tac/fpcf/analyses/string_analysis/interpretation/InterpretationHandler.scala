/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation

import org.opalj.ai.FormalParametersOriginOffset
import org.opalj.ai.ImmediateVMExceptionsOriginOffset
import org.opalj.br.DefinedMethod
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.ProperPropertyComputationResult

/**
 * Processes expressions that are relevant in order to determine which value(s) the string value at a given def site
 * might have.
 *
 * [[InterpretationHandler]]s of any level may use [[StringInterpreter]]s from their level or any level below.
 * [[StringInterpreter]]s defined in the [[interpretation]] package may be used by any level.
 *
 * @author Maximilian Rüsch
 */
abstract class InterpretationHandler {

    def analyze(entity: DUSiteEntity): ProperPropertyComputationResult = {
        val pc = entity.pc
        implicit val duSiteState: DUSiteState = DUSiteState(pc, entity.dm, entity.tac, entity.entity)
        if (pc <= FormalParametersOriginOffset) {
            if (pc == -1 || pc <= ImmediateVMExceptionsOriginOffset) {
                return StringInterpreter.computeFinalResult(pc, StringConstancyInformation.lb)
            } else {
                return StringInterpreter.computeFinalResult(pc, StringConstancyInformation.getElementForParameterPC(pc))
            }
        }

        processNewPC(pc)
    }

    protected def processNewPC(pc: Int)(implicit state: DUSiteState): ProperPropertyComputationResult
}

object InterpretationHandler {

    def getEntityForDefSite(defSite: Int)(implicit state: DUSiteState): DUSiteEntity =
        getEntityForPC(pcOfDefSite(defSite)(state.tac.stmts))

    def getEntityForDefSite(defSite: Int, dm: DefinedMethod, tac: TAC, entity: SEntity): DUSiteEntity =
        getEntityForPC(pcOfDefSite(defSite)(tac.stmts), dm, tac, entity)

    def getEntityForPC(pc: Int)(implicit state: DUSiteState): DUSiteEntity =
        getEntityForPC(pc, state.dm, state.tac, state.entity)

    def getEntity(state: DUSiteState): DUSiteEntity =
        getEntityForPC(state.pc, state.dm, state.tac, state.entity)

    def getEntityForPC(pc: Int, dm: DefinedMethod, tac: TAC, entity: SEntity): DUSiteEntity =
        DUSiteEntity(pc, dm, tac, entity)
}
