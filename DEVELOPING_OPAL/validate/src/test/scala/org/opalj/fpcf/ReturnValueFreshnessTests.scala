/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.fpcf.analyses.escape.EagerReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.fpcf.analyses.LazyVirtualCallAggregatingEscapeAnalysis
import org.opalj.fpcf.analyses.LazyVirtualReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.tac.fpcf.analyses.TACAITransformer

/**
 *  Tests if the return value freshness properties specified in the test project (the classes in the
 *  (sub-)package of org.opalj.fpcf.fixture) and the computed ones match. The actual matching is
 *  delegated to PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Florian Kübler
 */
class ReturnValueFreshnessTests extends PropertiesTest {

    val analysisSchedulers = Set[FPCFAnalysisScheduler](
        LazyL0BaseAIAnalysis,
        TACAITransformer, // LazyL0TACAIAnalysis,
        LazyInterProceduralEscapeAnalysis,
        LazyVirtualCallAggregatingEscapeAnalysis,
        LazyVirtualReturnValueFreshnessAnalysis,
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
