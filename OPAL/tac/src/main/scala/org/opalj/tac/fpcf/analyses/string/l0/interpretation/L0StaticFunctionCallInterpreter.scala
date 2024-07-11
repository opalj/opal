/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l0
package interpretation

import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.string.StringTreeConst
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string.interpretation.SoundnessMode
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.StringFlowFunction
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment

/**
 * @author Maximilian Rüsch
 */
case class L0StaticFunctionCallInterpreter()(
    implicit
    override val p:       SomeProject,
    override val ps:      PropertyStore,
    override val project: SomeProject,
    val soundnessMode:    SoundnessMode
) extends AssignmentBasedStringInterpreter
    with L0ArbitraryStaticFunctionCallInterpreter
    with L0StringValueOfFunctionCallInterpreter
    with L0SystemPropertiesInterpreter {

    override type E = StaticFunctionCall[V]

    override def interpretExpr(target: PV, call: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        call.name match {
            case "getProperty" if call.declaringClass == ObjectType.System =>
                interpretGetSystemPropertiesCall(target)
            case "valueOf" if call.declaringClass == ObjectType.String => processStringValueOf(target, call)
            case _
                if call.descriptor.returnType == ObjectType.String ||
                    call.descriptor.returnType == ObjectType.Object =>
                interpretArbitraryCall(target, call)
            case _ => failure(target)
        }
    }
}

private[string] trait L0ArbitraryStaticFunctionCallInterpreter
    extends AssignmentBasedStringInterpreter
    with L0FunctionCallInterpreter {

    implicit val p: SomeProject
    implicit val soundnessMode: SoundnessMode

    override type E <: StaticFunctionCall[V]
    override type CallState = FunctionCallState

    def interpretArbitraryCall(target: PV, call: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        val calleeMethod = call.resolveCallTarget(state.dm.definedMethod.classFile.thisType)
        if (calleeMethod.isEmpty) {
            return failure(target)
        }

        val m = calleeMethod.value
        val params = getParametersForPC(state.pc).map(_.asVar.toPersistentForm(state.tac.stmts))
        val callState = new FunctionCallState(target, params, Seq(m), Map((m, ps(m, TACAI.key))))

        interpretArbitraryCallToFunctions(state, callState)
    }
}

private[string] trait L0StringValueOfFunctionCallInterpreter extends AssignmentBasedStringInterpreter {

    override type E <: StaticFunctionCall[V]

    def processStringValueOf(target: PV, call: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        val pc = state.pc
        val pp = call.params.head.asVar.toPersistentForm(state.tac.stmts)

        val flowFunction: StringFlowFunction = if (call.descriptor.parameterType(0).toJava == "char") {
            (env: StringTreeEnvironment) =>
                {
                    env(pc, pp) match {
                        case const: StringTreeConst if const.isIntConst =>
                            env.update(pc, target, StringTreeConst(const.string.toInt.toChar.toString))
                        case tree =>
                            env.update(pc, target, tree)
                    }
                }
        } else {
            (env: StringTreeEnvironment) => env.update(pc, target, env(pc, pp))
        }

        computeFinalResult(Set(PDUWeb(pc, pp), PDUWeb(pc, target)), flowFunction)
    }
}
