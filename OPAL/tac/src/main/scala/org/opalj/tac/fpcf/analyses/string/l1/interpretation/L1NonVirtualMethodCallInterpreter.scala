/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l1
package interpretation

import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.string.StringTreeEmptyConst
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationState
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment

/**
 * Processes [[NonVirtualMethodCall]]s without a call graph. Currently, only calls to `<init>` of strings, string
 * buffers and string builders are interpreted. For other calls, ID is returned.
 *
 * @author Maximilian Rüsch
 */
case class L1NonVirtualMethodCallInterpreter()(
    implicit val highSoundness: Boolean
) extends StringInterpreter {

    override type T = NonVirtualMethodCall[V]

    override def interpret(instr: T)(implicit state: InterpretationState): ProperPropertyComputationResult = {
        instr.name match {
            case "<init>"
                if instr.declaringClass.asReferenceType == ObjectType.StringBuffer ||
                    instr.declaringClass.asReferenceType == ObjectType.StringBuilder ||
                    instr.declaringClass.asReferenceType == ObjectType.String =>
                interpretInit(instr)
            case _ => computeFinalResult(StringFlowFunctionProperty.identity)
        }
    }

    private def interpretInit(init: T)(implicit state: InterpretationState): ProperPropertyComputationResult = {
        val pc = state.pc
        val targetVar = init.receiver.asVar.toPersistentForm(state.tac.stmts)
        init.params.size match {
            case 0 =>
                computeFinalResult(StringFlowFunctionProperty.constForVariableAt(pc, targetVar, StringTreeEmptyConst))
            case 1 =>
                val paramVar = init.params.head.asVar.toPersistentForm(state.tac.stmts)

                computeFinalResult(
                    Set(PDUWeb(pc, targetVar), PDUWeb(pc, paramVar)),
                    (env: StringTreeEnvironment) => env.update(pc, targetVar, env(pc, paramVar))
                )
            case _ =>
                failure(targetVar)
        }
    }
}
