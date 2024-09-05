/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string

import scala.collection.mutable.ListBuffer

import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.NoCallers
import org.opalj.br.fpcf.properties.string.StringConstancyInformation
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.log.Error
import org.opalj.log.Info
import org.opalj.log.OPALLogger.logOnce
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.MethodStringFlow

trait StringAnalysis extends FPCFAnalysis {

    private final val ConfigLogCategory = "analysis configuration - string analysis"

    protected val maxDepth: Int = {
        val maxDepth =
            try {
                project.config.getInt(StringAnalysis.MaxDepthConfigKey)
            } catch {
                case t: Throwable =>
                    logOnce(Error(ConfigLogCategory, s"couldn't read: ${StringAnalysis.MaxDepthConfigKey}", t))
                    30
            }

        logOnce(Info(ConfigLogCategory, "using maximum depth " + maxDepth))
        maxDepth
    }
}

object StringAnalysis {

    final val MaxDepthConfigKey = "org.opalj.fpcf.analyses.string.StringAnalysis.maxDepth"
}

/**
 * @author Maximilian Rüsch
 */
private[string] class ContextFreeStringAnalysis(override val project: SomeProject) extends StringAnalysis {

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
                val tree = methodStringFlow(state.entity.pc, state.entity.pv)
                if (tree.depth > maxDepth) {
                    // String constancy information got too complex, abort. This guard can probably be removed once
                    // recursing functions are properly handled using e.g. the widen-converge approach.
                    StringConstancyInformation.lb
                } else {
                    StringConstancyInformation(tree.simplify)
                }
            case _: EPK[_, MethodStringFlow] =>
                StringConstancyInformation.ub
        })
    }
}

/**
 * @author Maximilian Rüsch
 */
class ContextStringAnalysis(override val project: SomeProject) extends StringAnalysis {

    def analyze(vc: VariableContext): ProperPropertyComputationResult = {
        val vdScp = ps(VariableDefinition(vc.pc, vc.pv, vc.m), StringConstancyProperty.key)

        implicit val state: ContextStringAnalysisState = ContextStringAnalysisState(vc, vdScp)
        state.parameterIndices.foreach { index =>
            val mpcDependee = ps(MethodParameterContext(index, state.entity.context), StringConstancyProperty.key)
            state.registerParameterDependee(mpcDependee)
        }

        computeResults
    }

    private def continuation(state: ContextStringAnalysisState)(eps: SomeEPS): ProperPropertyComputationResult = {
        implicit val _state: ContextStringAnalysisState = state
        eps match {
            // "Downwards" dependency
            case EUBP(_: VariableDefinition, _: StringConstancyProperty) =>
                handleStringDefinitionUpdate(eps.asInstanceOf[EPS[VariableDefinition, StringConstancyProperty]])
                computeResults

            // "Upwards" dependency
            case EUBP(_: MethodParameterContext, _: StringConstancyProperty) =>
                state.updateParamDependee(eps.asInstanceOf[EOptionP[MethodParameterContext, StringConstancyProperty]])
                computeResults

            case _ =>
                throw new IllegalArgumentException(s"Unexpected eps in continuation: $eps")
        }
    }

    private def handleStringDefinitionUpdate(
        newDefinitionEPS: EPS[VariableDefinition, StringConstancyProperty]
    )(implicit state: ContextStringAnalysisState): Unit = {
        val previousIndices = state.parameterIndices
        state.updateStringDependee(newDefinitionEPS)
        val newIndices = state.parameterIndices

        newIndices.diff(previousIndices).foreach { index =>
            val mpcDependee = ps(MethodParameterContext(index, state.entity.context), StringConstancyProperty.key)
            state.registerParameterDependee(mpcDependee)
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
            Result(state.entity, StringConstancyProperty(state.currentSciUB))
        }
    }
}

private[string] class MethodParameterContextStringAnalysis(override val project: SomeProject) extends StringAnalysis {

    private implicit val contextProvider: ContextProvider = project.get(ContextProviderKey)

    def analyze(mpc: MethodParameterContext): ProperPropertyComputationResult = {
        implicit val state: MethodParameterContextStringAnalysisState = MethodParameterContextStringAnalysisState(mpc)

        val callersEOptP = ps(state.dm, Callers.key)
        state.updateCallers(callersEOptP)
        if (callersEOptP.hasUBP) {
            handleNewCallers(NoCallers, callersEOptP.ub)
        }

        computeResults
    }

    private def continuation(state: MethodParameterContextStringAnalysisState)(
        eps: SomeEPS
    ): ProperPropertyComputationResult = {
        implicit val _state: MethodParameterContextStringAnalysisState = state
        eps match {
            case UBP(callers: Callers) =>
                val oldCallers = state._callersDependee.get.ub
                state.updateCallers(eps.asInstanceOf[EOptionP[DeclaredMethod, Callers]])
                handleNewCallers(oldCallers, callers)
                computeResults

            case EUBP(m: Method, tacai: TACAI) =>
                if (tacai.tac.isEmpty) {
                    state._discoveredUnknownTAC = true
                } else {
                    state.getCallerContexts(m).foreach(handleTACForContext(tacai.tac.get, _))
                }
                computeResults

            case EUBP(_: VariableContext, _: StringConstancyProperty) =>
                state.updateParamDependee(eps.asInstanceOf[EOptionP[VariableContext, StringConstancyProperty]])
                computeResults

            case _ =>
                throw new IllegalArgumentException(s"Unexpected eps in continuation: $eps")
        }
    }

    private def handleNewCallers(
        oldCallers: Callers,
        newCallers: Callers
    )(implicit state: MethodParameterContextStringAnalysisState): Unit = {
        val relevantCallerContexts = ListBuffer.empty[(Context, Int)]
        newCallers.forNewCallerContexts(oldCallers, state.dm) { (_, callerContext, pc, _) =>
            if (callerContext.hasContext && callerContext.method.hasSingleDefinedMethod) {
                relevantCallerContexts.append((callerContext, pc))
            }
        }

        for { callerContext <- relevantCallerContexts } {
            state.addCallerContext(callerContext)
            val tacEOptP = state.getTacaiForContext(callerContext)
            if (tacEOptP.hasUBP) {
                val tacOpt = tacEOptP.ub.tac
                if (tacOpt.isEmpty) {
                    state._discoveredUnknownTAC = true
                } else {
                    handleTACForContext(tacOpt.get, callerContext)
                }
            }
        }
    }

    private def handleTACForContext(
        tac:     TAC,
        context: (Context, Int)
    )(implicit state: MethodParameterContextStringAnalysisState): Unit = {
        val callExpr = tac.stmts(valueOriginOfPC(context._2, tac.pcToIndex).get) match {
            case Assignment(_, _, expr) if expr.isInstanceOf[Call[_]] => expr.asInstanceOf[Call[V]]
            case ExprStmt(_, expr) if expr.isInstanceOf[Call[_]]      => expr.asInstanceOf[Call[V]]
            case call: Call[_]                                        => call.asInstanceOf[Call[V]]
            case node                                                 => throw new IllegalArgumentException(s"Unexpected argument: $node")
        }

        val previousCallExpr = state.addCallExprInformationForContext(context, callExpr)
        if (previousCallExpr.isEmpty || previousCallExpr.get != callExpr) {
            handleCallExpr(context, callExpr)
        }
    }

    private def handleCallExpr(
        callerContext: (Context, Int),
        callExpr:      Call[V]
    )(implicit state: MethodParameterContextStringAnalysisState): Unit = {
        val dm = callerContext._1.method.asDefinedMethod
        val tac = state.getTACForContext(callerContext)
        val paramVC = VariableContext(
            callerContext._2,
            callExpr.params(state.index).asVar.toPersistentForm(tac.stmts),
            callerContext._1
        )
        state.registerParameterDependee(dm, ps(paramVC, StringConstancyProperty.key))
    }

    private def computeResults(implicit
        state: MethodParameterContextStringAnalysisState
    ): ProperPropertyComputationResult = {
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
