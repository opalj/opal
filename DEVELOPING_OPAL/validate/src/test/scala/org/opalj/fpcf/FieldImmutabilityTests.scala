/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import java.net.URL

import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.analyses.EagerClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.EagerL0FieldImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.EagerL0PurityAnalysis
import org.opalj.br.fpcf.analyses.EagerTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.EagerUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.EagerL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.EagerL1FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.TACAITransformer
import org.opalj.tac.fpcf.analyses.escape.EagerSimpleEscapeAnalysis

/**
 * @author Tobias Peter Roth
 */
class FieldImmutabilityTests extends PropertiesTest {

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ ⇒
            Set[Class[_ <: AnyRef]](classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]])
        }
        p.get(RTACallGraphKey)
    }

    describe("no analysis is scheduled") {
        val as = executeAnalyses(Set.empty)
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("ReferenceImmutability"))
    }

    describe("the org.opalj.fpcf.analyses.L0FieldImmutabilityAnalysis is executed") {
        val as = executeAnalyses(
            Set(
                EagerTypeImmutabilityAnalysis,
                EagerUnsoundPrematurelyReadFieldsAnalysis,
                EagerClassImmutabilityAnalysis,
                EagerL0ReferenceImmutabilityAnalysis,
                EagerL0PurityAnalysis,
                EagerL1FieldMutabilityAnalysis,
                EagerSimpleEscapeAnalysis,
                TACAITransformer,
                EagerL0FieldImmutabilityAnalysis
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
