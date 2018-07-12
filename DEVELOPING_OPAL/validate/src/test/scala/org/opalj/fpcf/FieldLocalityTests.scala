/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.fpcf.analyses.EagerFieldLocalityAnalysis
import org.opalj.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.LazyVirtualCallAggregatingEscapeAnalysis
import org.opalj.fpcf.analyses.LazyVirtualReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis

class FieldLocalityTests extends PropertiesTest {

    val lazyAnalysisSchedulers = Set[FPCFLazyAnalysisScheduler { type InitializationData = Null }](
        LazyInterProceduralEscapeAnalysis,
        LazyVirtualCallAggregatingEscapeAnalysis,
        LazyVirtualReturnValueFreshnessAnalysis,
        LazyReturnValueFreshnessAnalysis
    )

    describe("field locality analysis is executed") {
        val as = executeAnalyses(Set(EagerFieldLocalityAnalysis), lazyAnalysisSchedulers)
        as.propertyStore.shutdown()
        validateProperties(
            as,
            fieldsWithAnnotations(as.project),
            Set("FieldLocality")
        )
    }
}
