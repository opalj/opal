/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.fpcf.analyses.EagerL0FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.EagerL1FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.EagerL2FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.fpcf.analyses.SystemPropertiesAnalysis
import org.opalj.fpcf.analyses.cg.EagerFinalizerAnalysisScheduler
import org.opalj.fpcf.analyses.cg.EagerInstantiatedTypesAnalysis
import org.opalj.fpcf.analyses.cg.EagerStaticInitializerAnalysis
import org.opalj.fpcf.analyses.cg.EagerRTACallGraphAnalysisScheduler
import org.opalj.fpcf.analyses.cg.EagerSerializationRelatedCallsAnalysis
import org.opalj.fpcf.analyses.cg.EagerThreadRelatedCallsAnalysis
import org.opalj.fpcf.analyses.cg.LazyCalleesAnalysis
import org.opalj.fpcf.analyses.cg.TriggeredLoadedClassesAnalysis
import org.opalj.fpcf.analyses.cg.reflection.EagerReflectionRelatedCallsAnalysis
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.tac.fpcf.analyses.TACAITransformer

/**
 * Tests if the properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Michael Eichberg
 */
class FieldMutabilityTests extends PropertiesTest {

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
                EagerRTACallGraphAnalysisScheduler,
                EagerStaticInitializerAnalysis,
                TriggeredLoadedClassesAnalysis,
                EagerFinalizerAnalysisScheduler,
                EagerThreadRelatedCallsAnalysis,
                EagerSerializationRelatedCallsAnalysis,
                EagerReflectionRelatedCallsAnalysis,
                EagerInstantiatedTypesAnalysis,
                SystemPropertiesAnalysis,
                LazyL0BaseAIAnalysis,
                TACAITransformer,
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyInterProceduralEscapeAnalysis,
                new LazyCalleesAnalysis(Set(
                    StandardInvokeCallees,
                    SerializationRelatedCallees,
                    ReflectionRelatedCallees
                ))
            )
        )
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldMutability"))
    }

    describe("the org.opalj.fpcf.analyses.L2FieldMutabilityAnalysis is executed") {
        val as = executeAnalyses(
            Set(
                EagerL2FieldMutabilityAnalysis,
                EagerRTACallGraphAnalysisScheduler,
                EagerStaticInitializerAnalysis,
                TriggeredLoadedClassesAnalysis,
                EagerFinalizerAnalysisScheduler,
                EagerThreadRelatedCallsAnalysis,
                EagerSerializationRelatedCallsAnalysis,
                EagerReflectionRelatedCallsAnalysis,
                EagerInstantiatedTypesAnalysis,
                SystemPropertiesAnalysis,
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyL2PurityAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyL0BaseAIAnalysis,
                TACAITransformer,
                new LazyCalleesAnalysis(Set(
                    StandardInvokeCallees,
                    SerializationRelatedCallees,
                    ReflectionRelatedCallees
                ))
            )
        )
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldMutability"))
    }

}
