/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l0
package interpretation

import org.opalj.br.Method
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.StringFlowFunction
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment

/**
 * @author Maximilian Rüsch
 */
trait L0FunctionCallInterpreter
    extends AssignmentLikeBasedStringInterpreter
    with ParameterEvaluatingStringInterpreter {

    override type E <: FunctionCall[V]

    implicit val ps: PropertyStore

    protected[this] case class FunctionCallState(
        state:               InterpretationState,
        target:              PV,
        calleeMethods:       Seq[Method],
        parameters:          Seq[PV],
        var tacDependees:    Map[Method, EOptionP[Method, TACAI]],
        var returnDependees: Map[Method, Seq[EOptionP[VariableDefinition, StringConstancyProperty]]] = Map.empty
    ) {
        def pc: Int = state.pc

        var hasUnresolvableReturnValue: Map[Method, Boolean] = Map.empty.withDefaultValue(false)

        def updateReturnDependee(
            method:      Method,
            newDependee: EOptionP[VariableDefinition, StringConstancyProperty]
        ): Unit = {
            returnDependees = returnDependees.updated(
                method,
                returnDependees(method).updated(
                    returnDependees(method).indexWhere(_.e == newDependee.e),
                    newDependee
                )
            )
        }

        def hasDependees: Boolean = {
            tacDependees.values.exists(_.isRefinable) ||
            returnDependees.values.flatten.exists(_.isRefinable)
        }

        def dependees: Iterable[SomeEOptionP] = {
            tacDependees.values.filter(_.isRefinable) ++
                returnDependees.values.flatten.filter(_.isRefinable)
        }
    }

    protected def interpretArbitraryCallToFunctions(implicit
        callState: FunctionCallState
    ): ProperPropertyComputationResult = {
        callState.calleeMethods.foreach { m =>
            val tacEOptP = callState.tacDependees(m)
            if (tacEOptP.isFinal) {
                val calleeTac = tacEOptP.asFinal.p.tac
                if (calleeTac.isEmpty) {
                    // When the tac ep is final but we still do not have a callee tac, we cannot infer arbitrary call values at all
                    callState.hasUnresolvableReturnValue += m -> true
                } else {
                    val returns = calleeTac.get.stmts.toIndexedSeq.filter(stmt => stmt.isInstanceOf[ReturnValue[V]])
                    if (returns.isEmpty) {
                        // A function without returns, e.g., because it is guaranteed to throw an exception, is approximated
                        // with the lower bound
                        callState.hasUnresolvableReturnValue += m -> true
                    } else {
                        callState.returnDependees += m -> returns.map { ret =>
                            val entity = VariableDefinition(
                                ret.pc,
                                ret.asInstanceOf[ReturnValue[V]].expr.asVar.toPersistentForm(calleeTac.get.stmts),
                                m
                            )
                            ps(entity, StringConstancyProperty.key)
                        }
                    }
                }
            }
        }

        tryComputeFinalResult
    }

    private def tryComputeFinalResult(implicit callState: FunctionCallState): ProperPropertyComputationResult = {
        if (callState.hasDependees) {
            InterimResult.forUB(
                InterpretationHandler.getEntity(callState.state),
                StringFlowFunctionProperty.ub,
                callState.dependees.toSet,
                continuation(callState)
            )
        } else {
            val pc = callState.state.pc
            val parameters = callState.parameters.zipWithIndex.map(x => (x._2, x._1)).toMap

            val flowFunction: StringFlowFunction = (env: StringTreeEnvironment) =>
                env.update(
                    pc,
                    callState.target,
                    StringTreeNode.reduceMultiple {
                        callState.calleeMethods.map { m =>
                            if (callState.hasUnresolvableReturnValue(m)) {
                                StringTreeNode.lb
                            } else {
                                StringTreeNode.reduceMultiple(callState.returnDependees(m).map {
                                    _.asFinal.p.sci.tree.replaceParameters(parameters.map { kv =>
                                        (kv._1, env(pc, kv._2))
                                    })
                                })
                            }
                        }
                    }
                )

            computeFinalResult(
                callState.parameters.map(PDUWeb(pc, _)).toSet + PDUWeb(pc, callState.target),
                flowFunction
            )(callState.state)
        }
    }

    private def continuation(callState: FunctionCallState)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case EUBP(m: Method, _: TACAI) =>
                callState.tacDependees += m -> eps.asInstanceOf[EOptionP[Method, TACAI]]
                interpretArbitraryCallToFunctions(callState)

            case EUBP(_, _: StringConstancyProperty) =>
                val contextEPS = eps.asInstanceOf[EOptionP[VariableDefinition, StringConstancyProperty]]
                callState.updateReturnDependee(contextEPS.e.m, contextEPS)
                tryComputeFinalResult(callState)

            case _ => throw new IllegalArgumentException(s"Encountered unknown eps: $eps")
        }
    }
}
