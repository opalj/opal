/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string

import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.NoCallers
import org.opalj.br.fpcf.properties.string.StringConstancyInformation
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.MethodStringFlow

/**
 * @author Maximilian Rüsch
 */
private[string] class ContextFreeStringAnalysis(override val project: SomeProject) extends FPCFAnalysis {

    def analyze(vd: VariableDefinition): ProperPropertyComputationResult =
        computeResults(ContextFreeStringAnalysisState(vd, ps(vd.m, MethodStringFlow.key)))

    private def continuation(state: ContextFreeStringAnalysisState)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case _ if eps.pk == MethodStringFlow.key =>
                state.stringFlowDependee = eps.asInstanceOf[EOptionP[Method, MethodStringFlow]]
                computeResults(state)

            case _ =>
                throw new IllegalArgumentException(s"Unexpected eps in continuation: $eps")
        }
    }

    private def computeResults(implicit state: ContextFreeStringAnalysisState): ProperPropertyComputationResult = {
        if (state.hasDependees) {
            InterimResult(
                state.entity,
                StringConstancyProperty.lb,
                computeNewUpperBound(state),
                state.dependees,
                continuation(state)
            )
        } else {
            Result(state.entity, computeNewUpperBound(state))
        }
    }

    private def computeNewUpperBound(state: ContextFreeStringAnalysisState): StringConstancyProperty = {
        StringConstancyProperty(state.stringFlowDependee match {
            case UBP(methodStringFlow) =>
                StringConstancyInformation(methodStringFlow(state.entity.pc, state.entity.pv).simplify)
            case _: EPK[_, MethodStringFlow] => StringConstancyInformation.ub
        })
    }
}

/**
 * @author Maximilian Rüsch
 */
class ContextStringAnalysis(override val project: SomeProject) extends FPCFAnalysis {

    private implicit val contextProvider: ContextProvider = project.get(ContextProviderKey)

    def analyze(vc: VariableContext): ProperPropertyComputationResult = {
        val vdScp = ps(VariableDefinition(vc.pc, vc.pv, vc.m), StringConstancyProperty.key)

        implicit val state: ContextStringAnalysisState = ContextStringAnalysisState(vc, vdScp)
        if (vdScp.isEPK) {
            state._stringDependee = vdScp
            computeResults
        } else {
            continuation(state)(vdScp.asInstanceOf[SomeEPS])
        }
    }

    private def continuation(state: ContextStringAnalysisState)(eps: SomeEPS): ProperPropertyComputationResult = {
        implicit val _state: ContextStringAnalysisState = state
        eps match {
            // "Downwards" dependency
            case EUBP(_: VariableDefinition, _: StringConstancyProperty) =>
                state._stringDependee = eps.asInstanceOf[EOptionP[VariableDefinition, StringConstancyProperty]]

                val parameterIndices = state.stringTree.collectParameterIndices
                if (parameterIndices.isEmpty) {
                    computeResults
                } else {
                    // We have some parameters that need to be resolved for all callers of this method
                    val callersEOptP = ps(state.entity.context.method, Callers.key)
                    state._callersDependee = Some(callersEOptP)

                    if (callersEOptP.hasUBP) {
                        handleNewCallers(NoCallers, callersEOptP.ub)
                    }

                    computeResults
                }

            case UBP(callers: Callers) =>
                val oldCallers = state._callersDependee.get.ub
                state._callersDependee = Some(eps.asInstanceOf[EOptionP[DeclaredMethod, Callers]])
                handleNewCallers(oldCallers, callers)
                computeResults

            case EUBP(m: Method, tacai: TACAI) =>
                handleTACAI(m, tacai)
                computeResults

            // "Upwards" dependency
            case EUBP(_: VariableContext, _: StringConstancyProperty) =>
                state.updateParamDependee(eps.asInstanceOf[EOptionP[VariableContext, StringConstancyProperty]])
                computeResults

            case _ =>
                computeResults
        }
    }

    private def handleNewCallers(
        oldCallers: Callers,
        newCallers: Callers
    )(implicit state: ContextStringAnalysisState): Unit = {
        newCallers.forNewCallerContexts(oldCallers, state.dm) { (_, callerContext, pc, _) =>
            if (callerContext.hasContext && callerContext.method.hasSingleDefinedMethod) {
                val callerMethod = callerContext.method.definedMethod

                System.out.println(s"FOUND RELEVANT CALLER FOR PC ${state.entity.pc} IN ${state.entity.m} FOR ${state.entity.pv}")

                val tacEOptP = ps(callerMethod, TACAI.key)
                state.registerTacaiDepender(tacEOptP, (callerContext, pc))

                if (tacEOptP.hasUBP) {
                    handleTACAI(callerMethod, tacEOptP.ub)
                }
            } else {
                System.out.println(s"NON-SDM FOUND FOR PC ${state.entity.pc} IN ${state.entity.m} FOR ${state.entity.pv}")
            }
        }
    }

    private def handleTACAI(m: Method, tacai: TACAI)(
        implicit state: ContextStringAnalysisState
    ): Unit = {
        if (tacai.tac.isEmpty) {
            state._discoveredUnknownTAC = true
        } else {
            val tac = tacai.tac.get
            val (callerContext, pc) = state.getDepender(m)
            val callExpr = tac.stmts(valueOriginOfPC(pc, tac.pcToIndex).get) match {
                case Assignment(_, _, expr) if expr.isInstanceOf[Call[_]] => expr.asInstanceOf[Call[V]]
                case ExprStmt(_, expr) if expr.isInstanceOf[Call[_]]      => expr.asInstanceOf[Call[V]]
                case call: Call[_]                                        => call.asInstanceOf[Call[V]]
                case node                                                 => throw new IllegalArgumentException(s"Unexpected argument: $node")
            }

            for {
                index <- state.stringTree.collectParameterIndices
            } {
                val paramVC = VariableContext(
                    pc,
                    callExpr.params(index).asVar.toPersistentForm(tac.stmts),
                    callerContext
                )
                state.registerParameterDependee(index, m, ps(paramVC, StringConstancyProperty.key))
            }
        }
    }

    private def computeResults(implicit state: ContextStringAnalysisState): ProperPropertyComputationResult = {
        if (state.hasDependees) {
            InterimResult(
                state.entity,
                StringConstancyProperty.lb,
                StringConstancyProperty(state.currentSciUB),
                state.dependees,
                continuation(state)
            )
        } else {
            Result(state.entity, StringConstancyProperty(state.finalSci))
        }
    }
}
