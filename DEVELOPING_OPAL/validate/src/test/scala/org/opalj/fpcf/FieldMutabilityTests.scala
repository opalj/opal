/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.analyses.EagerL0FieldMutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.tac.fpcf.analyses.EagerL1FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.EagerL2FieldMutabilityAnalysis

/**
 * Tests if the properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Michael Eichberg
 */
class FieldMutabilityTests extends PropertiesTest {

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            _ => Set[Class[_ <: AnyRef]](classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]])
        }
        p.get(RTACallGraphKey)
    }

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/field_mutability")
    }

    describe("no analysis is scheduled") {
        val as = executeAnalyses(Set.empty)
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldMutability"))
    }

    describe("the org.opalj.fpcf.analyses.L0FieldMutabilityAnalysis is executed") {
        val as = executeAnalyses(
            Set(
                EagerL0FieldMutabilityAnalysis,
                LazyUnsoundPrematurelyReadFieldsAnalysis
            )
        )
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldMutability"))
    }

    describe("the org.opalj.fpcf.analyses.L1FieldMutabilityAnalysis is executed") {
        val as = executeAnalyses(
            Set(
                EagerL1FieldMutabilityAnalysis,
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyInterProceduralEscapeAnalysis
            )
        )
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldMutability"))
    }

    describe("the org.opalj.fpcf.analyses.L2FieldMutabilityAnalysis is executed") {
        val as = executeAnalyses(
            Set(
                EagerL2FieldMutabilityAnalysis,
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyL2PurityAnalysis,
                LazyInterProceduralEscapeAnalysis
            )
        )
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldMutability"))
    }

}
