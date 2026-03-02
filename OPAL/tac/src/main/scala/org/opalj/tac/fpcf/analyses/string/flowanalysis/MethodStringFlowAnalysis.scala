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
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.log.Error
import org.opalj.log.Info
import org.opalj.log.OPALLogger.logOnce
import org.opalj.si.flowanalysis.DataFlowAnalysis
import org.opalj.si.flowanalysis.Statement
import org.opalj.si.flowanalysis.StructuralAnalysis
import org.opalj.tac.common.FlowGraph
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.MethodStringFlow
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment

/**
 * Analyzes a methods string flow results by applying a [[StructuralAnalysis]] to identify all control flow regions of
 * the methods CFG and subsequently applying a [[DataFlowAnalysis]] to compute a resulting string tree environment
 * using string flow functions derived from the FPCF [[StringFlowFunctionProperty]].
 *
 * @note Packages can be configured to be excluded from analysis entirely due to e.g. size problems. In these cases, the
 *       lower or upper bound string tree environment will be returned, depending on the soundness mode of the analysis.
 *
 * @see [[StructuralAnalysis]], [[DataFlowAnalysis]], [[StringFlowFunctionProperty]], [[StringAnalysisConfig]]
 *
 * @author Maximilian Rüsch
 */
class MethodStringFlowAnalysis(override val project: SomeProject) extends FPCFAnalysis with StringAnalysisConfig {

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

    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def analyze(method: Method): ProperPropertyComputationResult = {
        if (excludedPackages.exists(method.classFile.thisType.packageName.startsWith(_))) {
            return Result(
                method,
                if (highSoundness) MethodStringFlow.lb
                else MethodStringFlow.ub
            );
        }

        val tacDependee = ps(method, TACAI.key)
        if (tacDependee.isRefinable) {
            InterimResult(
                method,
                MethodStringFlow.lb,
                MethodStringFlow.ub,
                Set(tacDependee),
                continuationForTAC(method)
            )
        } else if (tacDependee.ub.tac.isEmpty) {
            // No TAC available, e.g., because the method has no body
            Result(
                method,
                if (highSoundness) MethodStringFlow.lb
                else MethodStringFlow.ub
            )
        } else {
            determinePossibleStrings(using method, tacDependee.ub.tac.get)
        }
    }

    private def determinePossibleStrings(implicit method: Method, tac: TAC): ProperPropertyComputationResult = {

        val flowGraph = FlowGraph(tac.cfg)
        val (_, superFlowGraph, controlTree) =
            StructuralAnalysis.analyze(flowGraph, FlowGraph.entry)(using project.config)
        val flowAnalysis =
            new DataFlowAnalysis[StringTreeNode, StringTreeEnvironment](controlTree, superFlowGraph, highSoundness)

        implicit val state: MethodStringFlowAnalysisState =
            MethodStringFlowAnalysisState(method, declaredMethods(method), tac, flowAnalysis)

        flowGraph.nodes.toOuter.foreach {
            case Statement(pc) if pc >= 0 =>
                state.updateDependee(pc, propertyStore(MethodPC(pc, state.dm), StringFlowFunctionProperty.key))

            case _ =>
        }

        computeResults
    }

    private def continuationForTAC(method: Method)(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        case FinalP(tacai: TACAI) =>
            determinePossibleStrings(using method, tacai.tac.get)
        case tacDependee =>
            InterimResult(
                method,
                MethodStringFlow.lb,
                MethodStringFlow.ub,
                Set(tacDependee),
                continuationForTAC(method)
            )
    }

    private def continuation(state: MethodStringFlowAnalysisState)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case EUBP(e: MethodPC, _: StringFlowFunctionProperty) if eps.pk.equals(StringFlowFunctionProperty.key) =>
                state.updateDependee(e.pc, eps.asInstanceOf[EOptionP[MethodPC, StringFlowFunctionProperty]])
                computeResults(using state)

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
}

/**
 * A shared scheduler trait for analyses that analyse the string flow of given methods.
 *
 * @see [[MethodStringFlowAnalysis]]
 *
 * @author Maximilian Rüsch
 */
sealed trait MethodStringFlowAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)

    final def derivedProperty: PropertyBounds = PropertyBounds.ub(MethodStringFlow)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(TACAI, StringFlowFunctionProperty)
}

object LazyMethodStringFlowAnalysis
    extends MethodStringFlowAnalysisScheduler with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new MethodStringFlowAnalysis(p)
        ps.registerLazyPropertyComputation(MethodStringFlow.key, analysis.analyze)
        analysis
    }
}
