/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

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
import org.opalj.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.fpcf.analyses.LazyL0FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.fpcf.analyses.LazyVirtualMethodStaticDataUsageAnalysis
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.br.analyses.Project
import org.opalj.ai.domain.l1
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIResultAnalysis
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
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
        val as = try {
            executeAnalyses(
                EagerL0PurityAnalysis,
                LazyL0FieldMutabilityAnalysis,
                LazyClassImmutabilityAnalysis,
                LazyTypeImmutabilityAnalysis

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
            EagerL1PurityAnalysis,
            // Lazy ones
            LazyL0BaseAIResultAnalysis,
            TACAITransformer, // LazyL0TACAIAnalysis,
            LazyL1FieldMutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyTypeImmutabilityAnalysis,
            LazyVirtualMethodPurityAnalysis
        )
        as.propertyStore.shutdown()
        validateProperties(as, declaredMethodsWithAnnotations(as.project), Set("Purity"))
    }

    describe("the org.opalj.fpcf.analyses.L2PurityAnalysis is executed") {
        L2PurityAnalysis.setRater(Some(SystemOutLoggingAllExceptionRater))
        val as = executeAnalyses(
            EagerL2PurityAnalysis,
            // Lazy analyses
            LazyL0BaseAIResultAnalysis,
            TACAITransformer, // LazyL0TACAIAnalysis,
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
        as.propertyStore.shutdown()
        validateProperties(as, declaredMethodsWithAnnotations(as.project), Set("Purity"))
    }

}
