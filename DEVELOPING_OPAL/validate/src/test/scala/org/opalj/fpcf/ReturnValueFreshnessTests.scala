/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.fpcf.analyses.escape.EagerReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.fpcf.analyses.LazyVirtualCallAggregatingEscapeAnalysis
import org.opalj.fpcf.analyses.LazyVirtualReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis

/**
 *  Tests if the return value freshness properties specified in the test project (the classes in the
 *  (sub-)package of org.opalj.fpcf.fixture) and the computed ones match. The actual matching is
 *  delegated to PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Florian Kuebler
 */
class ReturnValueFreshnessTests extends PropertiesTest {

    val lazyAnalysisSchedulers = Set[FPCFLazyAnalysisScheduler { type InitializationData = Null }](
        LazyInterProceduralEscapeAnalysis,
        LazyVirtualCallAggregatingEscapeAnalysis,
        LazyVirtualReturnValueFreshnessAnalysis,
        LazyFieldLocalityAnalysis
    )

    describe("return value freshness analysis is executed") {
        val as = executeAnalyses(
            Set[FPCFEagerAnalysisScheduler { type InitializationData = Null }](EagerReturnValueFreshnessAnalysis),
            lazyAnalysisSchedulers
        )
        as.propertyStore.shutdown()
        validateProperties(
            as,
            declaredMethodsWithAnnotations(as.project),
            Set("ReturnValueFreshness")
        )
    }
}
