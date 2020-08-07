/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import java.net.URL

import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.reference.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis_new

/**
 * @author Tobias Peter Roth
 */
class FieldImmutabilityTests extends PropertiesTest {

    override def withRT = true

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/immutability")
    }

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ ⇒
            Set[Class[_ <: AnyRef]](classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]])
        }
        p.get(RTACallGraphKey)
    }

    describe("no analysis is scheduled") {
        val as = executeAnalyses(Set.empty)
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldImmutability"))
    }

    describe("the org.opalj.fpcf.analyses.L0FieldImmutabilityAnalysis is executed") {
        import org.opalj.br.fpcf.analyses.LazyVirtualCallAggregatingEscapeAnalysis
        val as = executeAnalyses(
            Set(
                LazyL0ReferenceImmutabilityAnalysis,
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyL2PurityAnalysis_new,
                EagerL0FieldImmutabilityAnalysis,
                LazyLxClassImmutabilityAnalysis_new,
                LazyLxTypeImmutabilityAnalysis_new,
                LazyStaticDataUsageAnalysis,
                LazyL0CompileTimeConstancyAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyReturnValueFreshnessAnalysis,
                LazyFieldLocalityAnalysis,
                LazyVirtualCallAggregatingEscapeAnalysis
            )
        )
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldImmutability"))
    }
    /**
     * describe("the org.opalj.fpcf.analyses.L1FieldMutabilityAnalysis is executed") {
     * val as = executeAnalyses(
     * Set(
     * EagerL1FieldMutabilityAnalysis,
     * LazyUnsoundPrematurelyReadFieldsAnalysis,
     * LazyInterProceduralEscapeAnalysis
     * )
     * )
     * as.propertyStore.shutdown()
     * validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldMutability"))
     * }*
     */
    /**
     * describe("the org.opalj.fpcf.analyses.L2FieldMutabilityAnalysis is executed") {
     * val as = executeAnalyses(
     * Set(
     * EagerL2FieldMutabilityAnalysis,
     * LazyUnsoundPrematurelyReadFieldsAnalysis,
     * LazyL2PurityAnalysis,
     * LazyInterProceduralEscapeAnalysis
     * )
     * )
     * as.propertyStore.shutdown()
     * validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldMutability"))
     * } *
     */

}
