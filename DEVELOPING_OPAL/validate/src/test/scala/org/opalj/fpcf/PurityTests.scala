/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.concurrent.ConcurrentExceptions
import org.opalj.fpcf.analyses.EagerL0PurityAnalysis
import org.opalj.fpcf.analyses.purity.EagerL1PurityAnalysis
import org.opalj.fpcf.analyses.purity.EagerL2PurityAnalysis
import org.opalj.fpcf.analyses.purity.L1PurityAnalysis
import org.opalj.fpcf.analyses.purity.L2PurityAnalysis
import org.opalj.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.fpcf.analyses.LazyVirtualCallAggregatingEscapeAnalysis
import org.opalj.fpcf.analyses.LazyVirtualMethodPurityAnalysis
import org.opalj.fpcf.analyses.LazyVirtualReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.purity.SystemOutLoggingAllExceptionRater
import org.opalj.fpcf.analyses.LazyL0FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.fpcf.analyses.LazyVirtualMethodStaticDataUsageAnalysis
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis

/**
 * Tests if the properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Dominik Helm
 */
class PurityTests extends PropertiesTest {

    override def withRT = true

    describe("the org.opalj.fpcf.analyses.L0PurityAnalysis is executed") {
        val as = try {
            executeAnalyses(
                Set(EagerL0PurityAnalysis),
                Set(
                    LazyL0FieldMutabilityAnalysis,
                    LazyClassImmutabilityAnalysis,
                    LazyTypeImmutabilityAnalysis
                )
            )
        } catch {
            case ce: ConcurrentExceptions ⇒
                ce.getSuppressed.foreach(e ⇒ e.printStackTrace())
                throw ce;
        }
        as.propertyStore.shutdown()
        validateProperties(as, declaredMethodsWithAnnotations(as.project), Set("Purity"))
    }

    describe("the org.opalj.fpcf.analyses.L1PurityAnalysis is executed") {
        L1PurityAnalysis.setRater(Some(SystemOutLoggingAllExceptionRater))
        val as = executeAnalyses(
            Set(EagerL1PurityAnalysis),
            Set(
                LazyL1FieldMutabilityAnalysis,
                LazyClassImmutabilityAnalysis,
                LazyTypeImmutabilityAnalysis,
                LazyVirtualMethodPurityAnalysis
            )
        )
        as.propertyStore.shutdown()
        validateProperties(as, declaredMethodsWithAnnotations(as.project), Set("Purity"))
    }

    describe("the org.opalj.fpcf.analyses.L2PurityAnalysis is executed") {
        L2PurityAnalysis.setRater(Some(SystemOutLoggingAllExceptionRater))
        val as = executeAnalyses(
            Set(EagerL2PurityAnalysis),
            Set(
                LazyL0CompileTimeConstancyAnalysis,
                LazyStaticDataUsageAnalysis,
                LazyVirtualMethodStaticDataUsageAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyVirtualCallAggregatingEscapeAnalysis,
                LazyReturnValueFreshnessAnalysis,
                LazyVirtualReturnValueFreshnessAnalysis,
                LazyFieldLocalityAnalysis,
                LazyL1FieldMutabilityAnalysis,
                LazyClassImmutabilityAnalysis,
                LazyTypeImmutabilityAnalysis,
                LazyVirtualMethodPurityAnalysis
            )
        )
        as.propertyStore.shutdown()
        validateProperties(as, declaredMethodsWithAnnotations(as.project), Set("Purity"))
    }

}
