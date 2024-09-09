/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package flowanalysis

import scala.jdk.CollectionConverters._

import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.log.Error
import org.opalj.log.Info
import org.opalj.log.OPALLogger.logOnce
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.MethodStringFlow
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * @author Maximilian Rüsch
 */
class MethodStringFlowAnalysis(override val project: SomeProject) extends FPCFAnalysis {

    private final val ConfigLogCategory = "analysis configuration - method string flow analysis"

    private val excludedPackages: Seq[String] = {
        val packages =
            try {
                project.config.getStringList(MethodStringFlowAnalysis.ExcludedPackagesConfigKey).asScala
            } catch {
                case t: Throwable =>
                    logOnce {
                        Error(
                            ConfigLogCategory,
                            s"couldn't read: ${MethodStringFlowAnalysis.ExcludedPackagesConfigKey}",
                            t
                        )
                    }
                    Seq.empty[String]
            }

        logOnce(Info(ConfigLogCategory, s"${packages.size} packages are excluded from string flow analysis"))
        packages.toSeq
    }

    private val soundnessMode: SoundnessMode = {
        val mode =
            try {
                SoundnessMode(project.config.getBoolean(MethodStringFlowAnalysis.SoundnessModeConfigKey))
            } catch {
                case t: Throwable =>
                    logOnce {
                        Error(ConfigLogCategory, s"couldn't read: ${MethodStringFlowAnalysis.SoundnessModeConfigKey}", t)
                    }
                    SoundnessMode(false)
            }

        logOnce(Info(ConfigLogCategory, "using soundness mode " + mode))
        mode
    }

    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def analyze(method: Method): ProperPropertyComputationResult = {
        val state = MethodStringFlowAnalysisState(method, declaredMethods(method), ps(method, TACAI.key))

        if (excludedPackages.exists(method.classFile.thisType.packageName.startsWith(_))) {
            Result(state.entity, MethodStringFlow.lb)
        } else if (state.tacDependee.isRefinable) {
            InterimResult.forUB(
                state.entity,
                MethodStringFlow.ub,
                Set(state.tacDependee),
                continuation(state)
            )
        } else if (state.tacDependee.ub.tac.isEmpty) {
            // No TAC available, e.g., because the method has no body
            Result(state.entity, MethodStringFlow.lb)
        } else {
            determinePossibleStrings(state)
        }
    }

    /**
     * Takes the `data` an analysis was started with as well as a computation `state` and determines
     * the possible string values. This method returns either a final [[Result]] or an
     * [[InterimResult]] depending on whether other information needs to be computed first.
     */
    private def determinePossibleStrings(implicit
        state: MethodStringFlowAnalysisState
    ): ProperPropertyComputationResult = {
        implicit val tac: TAC = state.tac

        state.flowGraph = FlowGraph(tac.cfg)
        val (_, superFlowGraph, controlTree) =
            StructuralAnalysis.analyze(state.flowGraph, FlowGraph.entry)
        state.superFlowGraph = superFlowGraph
        state.controlTree = controlTree
        state.flowAnalysis = new DataFlowAnalysis(state.controlTree, state.superFlowGraph, soundnessMode)

        state.flowGraph.nodes.toOuter.foreach {
            case Statement(pc) if pc >= 0 =>
                state.updateDependee(pc, propertyStore(MethodPC(pc, state.dm), StringFlowFunctionProperty.key))

            case _ =>
        }

        computeResults
    }

    private def continuation(state: MethodStringFlowAnalysisState)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case FinalP(_: TACAI) if eps.pk.equals(TACAI.key) =>
                state.tacDependee = eps.asInstanceOf[FinalEP[Method, TACAI]]
                determinePossibleStrings(state)

            case EUBP(e: MethodPC, _: StringFlowFunctionProperty) if eps.pk.equals(StringFlowFunctionProperty.key) =>
                state.updateDependee(e.pc, eps.asInstanceOf[EOptionP[MethodPC, StringFlowFunctionProperty]])
                computeResults(state)

            case _ =>
                throw new IllegalArgumentException(s"Unknown EPS given in continuation: $eps")
        }
    }

    private def computeResults(implicit state: MethodStringFlowAnalysisState): ProperPropertyComputationResult = {
        if (state.hasDependees) {
            InterimResult.forUB(
                state.entity,
                computeNewUpperBound(state),
                state.dependees.toSet,
                continuation(state)
            )
        } else {
            Result(state.entity, computeNewUpperBound(state))
        }
    }

    private def computeNewUpperBound(state: MethodStringFlowAnalysisState): MethodStringFlow = {
        val startEnv = state.getStartEnvAndReset
        MethodStringFlow(state.flowAnalysis.compute(state.getFlowFunctionsByPC)(startEnv))
    }
}

object MethodStringFlowAnalysis {

    final val ExcludedPackagesConfigKey = "org.opalj.fpcf.analyses.string.MethodStringFlowAnalysis.excludedPackages"
    final val SoundnessModeConfigKey = "org.opalj.fpcf.analyses.string.MethodStringFlowAnalysis.highSoundness"
}
