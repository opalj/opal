/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package evaluation
import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory

import org.opalj.log.GlobalLogContext
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.fpcf.ComputationSpecification
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.br.fpcf.cg.properties.ThreadRelatedIncompleteCallSites
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.br.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.analyses.EagerClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.EagerTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.fpcf.analyses.cg.LazyCalleesAnalysis
import org.opalj.tac.fpcf.analyses.cg.RTACallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TriggeredConfiguredNativeMethodsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredFinalizerAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TriggeredLoadedClassesAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredSerializationRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredThreadRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.cg.reflection.TriggeredReflectionRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.TriggeredSystemPropertiesAnalysis
import org.opalj.tac.fpcf.analyses.cg.EagerLibraryEntryPointsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredInstantiatedTypesAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredStaticInitializerAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.purity.EagerL2PurityAnalysis
import org.opalj.tac.fpcf.analyses.EagerL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.EagerReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.EagerFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.TACAITransformer
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.SystemOutLoggingAllExceptionRater

/**
 * Usage:
 * "-eager"
 * "-lazy"
 * "-closedWorld"
 *
 * @author Florian Kuebler
 */
object EvaluateEagerVsLazy {

    def main(args: Array[String]): Unit = {
        var eager = false
        var wasSet = false
        var closedWorldAssumption = false

        args.foreach {
            case "-eager" ⇒
                if (wasSet) throw new IllegalArgumentException()
                else eager = true
                wasSet = true
            case "-lazy" ⇒
                if (wasSet) throw new IllegalArgumentException()
                else eager = false
                wasSet = true
            case "-closedWorld" ⇒ closedWorldAssumption = true
        }

        val baseConfig = if (closedWorldAssumption) BaseConfig.withValue(
            "org.opalj.br.analyses.cg.ClassExtensibilityKey.analysis",
            ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ClassHierarchyIsNotExtensible")
        )
        else BaseConfig

        implicit val config: Config =
            baseConfig.withValue(
                "org.opalj.br.analyses.cg.InitialEntryPointsKey.analysis",
                ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.LibraryEntryPointsFinder")
            ).withValue(
                    "org.opalj.br.analyses.cg.InitialInstantiatedTypesKey.analysis",
                    ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.LibraryInstantiatedTypesFinder")
                )

        var projectTime: Seconds = Seconds.None
        var propertyStoreTime: Seconds = Seconds.None
        var analysisTime: Seconds = Seconds.None
        var callGraphTime: Seconds = Seconds.None

        val project = time {
            Project(org.opalj.bytecode.JRELibraryFolder, GlobalLogContext, config)
        } { t ⇒ projectTime = t.toSeconds }

        project.updateProjectInformationKeyInitializationData(
            AIDomainFactoryKey,
            (i: Option[Set[Class[_ <: AnyRef]]]) ⇒ (i match {
                case None               ⇒ Set(classOf[DefaultDomainWithCFGAndDefUse[_]])
                case Some(requirements) ⇒ requirements + classOf[DefaultDomainWithCFGAndDefUse[_]]
            }): Set[Class[_ <: AnyRef]]
        )

        val ps = time { project.get(PropertyStoreKey) } { t ⇒ propertyStoreTime = t.toSeconds }

        val manager = project.get(FPCFAnalysesManagerKey)

        val cgAnalyses = Set[ComputationSpecification[FPCFAnalysis]](
            RTACallGraphAnalysisScheduler,
            TriggeredStaticInitializerAnalysis,
            TriggeredLoadedClassesAnalysis,
            TriggeredFinalizerAnalysisScheduler,
            TriggeredThreadRelatedCallsAnalysis,
            TriggeredSerializationRelatedCallsAnalysis,
            TriggeredReflectionRelatedCallsAnalysis,
            TriggeredInstantiatedTypesAnalysis,
            TriggeredConfiguredNativeMethodsAnalysis,
            TriggeredSystemPropertiesAnalysis,
            LazyL0BaseAIAnalysis,
            TACAITransformer,
            EagerLibraryEntryPointsAnalysis,
            LazyCalleesAnalysis(
                Set(
                    StandardInvokeCallees,
                    SerializationRelatedCallees,
                    ReflectionRelatedCallees,
                    ThreadRelatedIncompleteCallSites
                )
            )
        )

        time {
            manager.runAll(cgAnalyses)
        } { t ⇒ callGraphTime = t.toSeconds }

        L2PurityAnalysis.setRater(Some(SystemOutLoggingAllExceptionRater))

        val analyses = if (eager) {
            List(
                EagerL2PurityAnalysis,
                LazyInterProceduralEscapeAnalysis, //needs to be lazy
                EagerClassImmutabilityAnalysis,
                EagerTypeImmutabilityAnalysis,
                EagerL2FieldMutabilityAnalysis,
                EagerReturnValueFreshnessAnalysis,
                EagerFieldLocalityAnalysis
            )
        } else {
            List(
                EagerL2PurityAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyClassImmutabilityAnalysis,
                LazyTypeImmutabilityAnalysis,
                LazyL2FieldMutabilityAnalysis,
                LazyReturnValueFreshnessAnalysis,
                LazyFieldLocalityAnalysis
            )
        }

        time {
            manager.runAll(analyses)

        } { t ⇒ analysisTime = t.toSeconds }

        ps.shutdown()

        Console.println(s"Call-graph time: $callGraphTime")
        Console.println(s"Analysis time: $analysisTime")
        Console.println(s"Total runtime ${callGraphTime + analysisTime}")

    }

}
