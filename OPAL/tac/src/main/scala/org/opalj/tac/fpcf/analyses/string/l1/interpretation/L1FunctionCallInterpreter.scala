/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l1
package interpretation

import org.opalj.br.Method
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.br.fpcf.properties.string.StringTreeOr
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationState
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.StringFlowFunction
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment

/**
 * Base trait for all function call interpreters on L1. Provides support for multiple possible called methods as well as
 * adding called methods and return dependees at runtime.
 *
 * @author Maximilian Rüsch
 */
trait L1FunctionCallInterpreter
    extends AssignmentLikeBasedStringInterpreter
    with ParameterEvaluatingStringInterpreter {

    override type E <: FunctionCall[V]

    implicit val ps: PropertyStore
    implicit val highSoundness: Boolean

    type CallState <: FunctionCallState

    protected[this] class FunctionCallState(
        val target:          PV,
        val parameters:      Seq[PV],
        var calleeMethods:   Seq[Method]                                                             = Seq.empty,
        var tacDependees:    Map[Method, EOptionP[Method, TACAI]]                                    = Map.empty,
        var returnDependees: Map[Method, Seq[EOptionP[VariableDefinition, StringConstancyProperty]]] = Map.empty
    ) {
        var hasUnresolvableReturnValue: Map[Method, Boolean] = Map.empty.withDefaultValue(false)

        def addCalledMethod(m: Method, tacDependee: EOptionP[Method, TACAI]): Unit = {
            calleeMethods = calleeMethods :+ m
            tacDependees = tacDependees.updated(m, tacDependee)
        }

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
        state:     InterpretationState,
        callState: CallState
    ): ProperPropertyComputationResult = {
        callState.calleeMethods.foreach { m =>
            val tacEOptP = callState.tacDependees(m)
            if (tacEOptP.hasUBP) {
                val calleeTac = tacEOptP.ub.tac
                if (calleeTac.isEmpty) {
                    // When we do not have a callee tac, we cannot infer arbitrary call return values at all
                    callState.hasUnresolvableReturnValue += m -> true
                } else {
                    val returns = calleeTac.get.stmts.toIndexedSeq.filter(stmt => stmt.isInstanceOf[ReturnValue[V]])
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

        computeResult
    }

    private def computeResult(
        implicit
        state:     InterpretationState,
        callState: CallState
    ): ProperPropertyComputationResult = {
        val pc = state.pc
        val parameters = callState.parameters.zipWithIndex.map(x => (x._2, x._1)).toMap

        val flowFunction: StringFlowFunction = (env: StringTreeEnvironment) =>
            env.update(
                pc,
                callState.target,
                StringTreeOr {
                    callState.calleeMethods.map { m =>
                        if (callState.hasUnresolvableReturnValue(m)) {
                            // We know we cannot resolve a definitive return value for this function
                            failureTree
                        } else if (callState.returnDependees.contains(m)) {
                            // We have some return dependees and can thus join their state
                            StringTreeOr(callState.returnDependees(m).map { rd =>
                                if (rd.hasUBP) {
                                    if (parameters.nonEmpty)
                                        rd.ub.tree.replaceParameters(parameters.map { kv => (kv._1, env(pc, kv._2)) })
                                    else
                                        rd.ub.tree
                                } else StringTreeNode.ub
                            })
                        } else {
                            // Empty join -> Upper bound
                            StringTreeNode.ub
                        }
                    }
                }
            )

        val newUB = StringFlowFunctionProperty(
            callState.parameters.map(PDUWeb(pc, _)).toSet + PDUWeb(pc, callState.target),
            flowFunction
        )

        if (callState.hasDependees) {
            InterimResult.forUB(
                InterpretationHandler.getEntity(state),
                newUB,
                callState.dependees.toSet,
                continuation(state, callState)
            )
        } else {
            computeFinalResult(newUB)
        }
    }

    protected[this] def continuation(
        state:     InterpretationState,
        callState: CallState
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case EUBP(m: Method, _: TACAI) =>
                callState.tacDependees += m -> eps.asInstanceOf[EOptionP[Method, TACAI]]
                interpretArbitraryCallToFunctions(state, callState)

            case EUBP(_, _: StringConstancyProperty) =>
                val contextEPS = eps.asInstanceOf[EOptionP[VariableDefinition, StringConstancyProperty]]
                callState.updateReturnDependee(contextEPS.e.m, contextEPS)
                computeResult(state, callState)

            case _ => throw new IllegalArgumentException(s"Encountered unknown eps: $eps")
        }
    }
}
