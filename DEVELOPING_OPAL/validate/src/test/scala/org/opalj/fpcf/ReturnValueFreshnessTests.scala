/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.fpcf.analyses.TriggeredSystemPropertiesAnalysis
import org.opalj.fpcf.analyses.cg.LazyCalleesAnalysis
import org.opalj.fpcf.analyses.cg.TriggeredFinalizerAnalysisScheduler
import org.opalj.fpcf.analyses.cg.TriggeredLoadedClassesAnalysis
import org.opalj.fpcf.analyses.cg.RTACallGraphAnalysisScheduler
import org.opalj.fpcf.analyses.cg.TriggeredSerializationRelatedCallsAnalysis
import org.opalj.fpcf.analyses.cg.TriggeredStaticInitializerAnalysis
import org.opalj.fpcf.analyses.cg.TriggeredThreadRelatedCallsAnalysis
import org.opalj.fpcf.analyses.cg.reflection.TriggeredReflectionRelatedCallsAnalysis
import org.opalj.fpcf.analyses.escape.EagerReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.fpcf.cg.properties.ThreadRelatedIncompleteCallSites
import org.opalj.tac.fpcf.analyses.TACAITransformer

/**
 *  Tests if the return value freshness properties specified in the test project (the classes in the
 *  (sub-)package of org.opalj.fpcf.fixture) and the computed ones match. The actual matching is
 *  delegated to PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Florian Kübler
 */
class ReturnValueFreshnessTests extends PropertiesTest {

    val analysisSchedulers: Set[FPCFAnalysisScheduler] = Set[FPCFAnalysisScheduler](
        RTACallGraphAnalysisScheduler,
        TriggeredStaticInitializerAnalysis,
        TriggeredLoadedClassesAnalysis,
        TriggeredFinalizerAnalysisScheduler,
        TriggeredThreadRelatedCallsAnalysis,
        TriggeredSerializationRelatedCallsAnalysis,
        TriggeredReflectionRelatedCallsAnalysis,
        TriggeredSystemPropertiesAnalysis,
        LazyL0BaseAIAnalysis,
        TACAITransformer,
        LazyInterProceduralEscapeAnalysis,
        LazyCalleesAnalysis(Set(
            StandardInvokeCallees,
            SerializationRelatedCallees,
            ReflectionRelatedCallees,
            ThreadRelatedIncompleteCallSites
        )),
        LazyInterProceduralEscapeAnalysis,
        LazyFieldLocalityAnalysis,
        EagerReturnValueFreshnessAnalysis
    )

    describe("return value freshness analysis is executed") {
        val as = executeAnalyses(analysisSchedulers)
        as.propertyStore.shutdown()
        validateProperties(
            as,
            declaredMethodsWithAnnotations(as.project),
            Set("ReturnValueFreshness")
        )
    }
}
