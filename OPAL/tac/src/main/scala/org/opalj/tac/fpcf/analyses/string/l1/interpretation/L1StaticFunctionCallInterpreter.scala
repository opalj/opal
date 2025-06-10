/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l1
package interpretation

import org.opalj.br.ClassType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.string.StringTreeConst
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationState
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.StringFlowFunction
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment

/**
 * Interprets some specific static calls in the context of their method as well as arbitrary static calls without a call
 * graph.
 *
 * @see [[L1ArbitraryStaticFunctionCallInterpreter]], [[L1StringValueOfFunctionCallInterpreter]],
 *      [[L1SystemPropertiesInterpreter]]
 *
 * @author Maximilian Rüsch
 */
case class L1StaticFunctionCallInterpreter()(
    implicit
    override val p:       SomeProject,
    override val ps:      PropertyStore,
    override val project: SomeProject,
    val highSoundness:    Boolean
) extends AssignmentBasedStringInterpreter
    with L1ArbitraryStaticFunctionCallInterpreter
    with L1StringValueOfFunctionCallInterpreter
    with L1SystemPropertiesInterpreter {

    override type E = StaticFunctionCall[V]

    override def interpretExpr(target: PV, call: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        call.name match {
            case "getProperty" if call.declaringClass == ClassType.System => interpretGetSystemPropertiesCall(target)
            case "valueOf" if call.declaringClass == ClassType.String     => processStringValueOf(target, call)
            case _
                if call.descriptor.returnType == ClassType.String ||
                    call.descriptor.returnType == ClassType.Object =>
                interpretArbitraryCall(target, call)
            case _ => failure(target)
        }
    }
}

private[string] trait L1ArbitraryStaticFunctionCallInterpreter
    extends AssignmentBasedStringInterpreter
    with L1FunctionCallInterpreter {

    implicit val p: SomeProject

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

private[string] trait L1StringValueOfFunctionCallInterpreter extends AssignmentBasedStringInterpreter {

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
