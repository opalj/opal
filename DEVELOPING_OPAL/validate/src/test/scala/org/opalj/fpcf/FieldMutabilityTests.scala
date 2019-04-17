/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.analyses.EagerL0FieldMutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.br.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.br.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.br.fpcf.cg.properties.ThreadRelatedIncompleteCallSites
import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.fpcf.analyses.TriggeredSystemPropertiesAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.tac.fpcf.analyses.TACAITransformer
import org.opalj.tac.fpcf.analyses.cg.LazyCalleesAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.EagerL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.cg.RTACallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TriggeredFinalizerAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TriggeredLoadedClassesAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredSerializationRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredStaticInitializerAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredThreadRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.cg.reflection.TriggeredReflectionRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredInstantiatedTypesAnalysis
import org.opalj.tac.fpcf.analyses.EagerL1FieldMutabilityAnalysis

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
            _ ⇒ Set[Class[_ <: AnyRef]](classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]])
        }
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
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyL0BaseAIAnalysis,
                TACAITransformer
            )
        )
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldMutability"))
    }

    describe("the org.opalj.fpcf.analyses.L1FieldMutabilityAnalysis is executed") {
        val as = executeAnalyses(
            Set(
                EagerL1FieldMutabilityAnalysis,
                RTACallGraphAnalysisScheduler,
                TriggeredStaticInitializerAnalysis,
                TriggeredLoadedClassesAnalysis,
                TriggeredFinalizerAnalysisScheduler,
                TriggeredThreadRelatedCallsAnalysis,
                TriggeredSerializationRelatedCallsAnalysis,
                TriggeredReflectionRelatedCallsAnalysis,
                TriggeredInstantiatedTypesAnalysis,
                TriggeredSystemPropertiesAnalysis,
                LazyL0BaseAIAnalysis,
                TACAITransformer,
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyCalleesAnalysis(
                    Set(
                        StandardInvokeCallees,
                        SerializationRelatedCallees,
                        ReflectionRelatedCallees,
                        ThreadRelatedIncompleteCallSites
                    )
                )
            )
        )
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldMutability"))
    }

    describe("the org.opalj.fpcf.analyses.L2FieldMutabilityAnalysis is executed") {
        val as = executeAnalyses(
            Set(
                EagerL2FieldMutabilityAnalysis,
                RTACallGraphAnalysisScheduler,
                TriggeredStaticInitializerAnalysis,
                TriggeredLoadedClassesAnalysis,
                TriggeredFinalizerAnalysisScheduler,
                TriggeredThreadRelatedCallsAnalysis,
                TriggeredSerializationRelatedCallsAnalysis,
                TriggeredReflectionRelatedCallsAnalysis,
                TriggeredInstantiatedTypesAnalysis,
                TriggeredSystemPropertiesAnalysis,
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyL2PurityAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyL0BaseAIAnalysis,
                TACAITransformer,
                LazyCalleesAnalysis(Set(
                    StandardInvokeCallees,
                    SerializationRelatedCallees,
                    ReflectionRelatedCallees,
                    ThreadRelatedIncompleteCallSites
                ))
            )
        )
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldMutability"))
    }

}
