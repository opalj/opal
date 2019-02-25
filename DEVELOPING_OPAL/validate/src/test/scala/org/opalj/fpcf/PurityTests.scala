/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.analyses.EagerL0PurityAnalysis
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyL0FieldMutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.ai.domain.l1
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.TriggeredSystemPropertiesAnalysis
import org.opalj.tac.fpcf.analyses.cg.RTACallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TriggeredFinalizerAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TriggeredInstantiatedTypesAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredLoadedClassesAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredSerializationRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredStaticInitializerAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredThreadRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.cg.reflection.TriggeredReflectionRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.purity.EagerL1PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.EagerL2PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.SystemOutLoggingAllExceptionRater
import org.opalj.tac.fpcf.analyses.TACAITransformer

/**
 * Tests if the properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Dominik Helm
 */
class PurityTests extends PropertiesTest {

    override def withRT = true

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(
            AIDomainFactoryKey,
            (_: Option[Set[Class[_ <: AnyRef]]]) ⇒
                Set[Class[_ <: AnyRef]](classOf[l1.DefaultDomainWithCFGAndDefUse[URL]])
        )
    }

    describe("the org.opalj.fpcf.analyses.L0PurityAnalysis is executed") {
        val as =
            executeAnalyses(
                Set(
                    EagerL0PurityAnalysis,
                    LazyL0FieldMutabilityAnalysis,
                    LazyClassImmutabilityAnalysis,
                    LazyTypeImmutabilityAnalysis
                )
            )
        as.propertyStore.shutdown()
        validateProperties(as, declaredMethodsWithAnnotations(as.project), Set("Purity"))
    }

    describe("the org.opalj.fpcf.analyses.L1PurityAnalysis is executed") {
        val testContext =
            executeAnalyses(
                Set(
                    RTACallGraphAnalysisScheduler,
                    TriggeredStaticInitializerAnalysis,
                    TriggeredLoadedClassesAnalysis,
                    TriggeredFinalizerAnalysisScheduler,
                    TriggeredThreadRelatedCallsAnalysis,
                    TriggeredSerializationRelatedCallsAnalysis,
                    TriggeredReflectionRelatedCallsAnalysis,
                    TriggeredSystemPropertiesAnalysis,
                    TriggeredInstantiatedTypesAnalysis,
                    TACAITransformer,
                    LazyL0BaseAIAnalysis,
                    LazyL1FieldMutabilityAnalysis,
                    LazyClassImmutabilityAnalysis,
                    LazyTypeImmutabilityAnalysis
                )
            )

        L1PurityAnalysis.setRater(Some(SystemOutLoggingAllExceptionRater))

        // TODO: we need final results for the CallersProperty, this should be a task of the manager
        val p = testContext.project
        val manager = p.get(FPCFAnalysesManagerKey)

        val (ps, List((_, a))) = manager.runAll(EagerL1PurityAnalysis)

        val as = TestContext(p, ps, a :: testContext.analyses)
        assert(as != null)

        as.propertyStore.shutdown()
        validateProperties(as, declaredMethodsWithAnnotations(as.project), Set("Purity"))
    }

    describe("the org.opalj.fpcf.analyses.L2PurityAnalysis is executed") {
        val testContext =
            executeAnalyses(
                Set(
                    TACAITransformer,
                    LazyL0BaseAIAnalysis,
                    RTACallGraphAnalysisScheduler,
                    TriggeredStaticInitializerAnalysis,
                    TriggeredLoadedClassesAnalysis,
                    TriggeredFinalizerAnalysisScheduler,
                    TriggeredThreadRelatedCallsAnalysis,
                    TriggeredSerializationRelatedCallsAnalysis,
                    TriggeredReflectionRelatedCallsAnalysis,
                    TriggeredSystemPropertiesAnalysis,
                    TriggeredInstantiatedTypesAnalysis
                )
            )

        L2PurityAnalysis.setRater(Some(SystemOutLoggingAllExceptionRater))

        // TODO: we need final results for the CallersProperty, this should be a task of the manager
        val p = testContext.project
        val manager = p.get(FPCFAnalysesManagerKey)

        val (ps, analyses) = manager.runAll(
            EagerL2PurityAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyL0CompileTimeConstancyAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis,
            LazyL1FieldMutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyTypeImmutabilityAnalysis
        )

        val as = TestContext(p, ps, testContext.analyses ++ analyses.map(_._2))

        as.propertyStore.shutdown()
        validateProperties(as, declaredMethodsWithAnnotations(as.project), Set("Purity"))
    }

}
