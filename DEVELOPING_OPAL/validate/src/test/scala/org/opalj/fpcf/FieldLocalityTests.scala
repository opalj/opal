/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.fpcf.analyses.EagerFieldLocalityAnalysis
import org.opalj.fpcf.analyses.SystemPropertiesAnalysis
import org.opalj.fpcf.analyses.cg.EagerFinalizerAnalysisScheduler
import org.opalj.fpcf.analyses.cg.EagerRTACallGraphAnalysisScheduler
import org.opalj.fpcf.analyses.cg.EagerSerializationRelatedCallsAnalysis
import org.opalj.fpcf.analyses.cg.EagerStaticInitializerAnalysis
import org.opalj.fpcf.analyses.cg.EagerThreadRelatedCallsAnalysis
import org.opalj.fpcf.analyses.cg.LazyCalleesAnalysis
import org.opalj.fpcf.analyses.cg.TriggeredLoadedClassesAnalysis
import org.opalj.fpcf.analyses.cg.reflection.EagerReflectionRelatedCallsAnalysis
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.tac.fpcf.analyses.TACAITransformer

class FieldLocalityTests extends PropertiesTest {

    val analyses = Set[FPCFAnalysisScheduler](
        EagerFieldLocalityAnalysis,
        EagerRTACallGraphAnalysisScheduler,
        EagerStaticInitializerAnalysis,
        TriggeredLoadedClassesAnalysis,
        EagerFinalizerAnalysisScheduler,
        EagerThreadRelatedCallsAnalysis,
        EagerSerializationRelatedCallsAnalysis,
        EagerReflectionRelatedCallsAnalysis,
        SystemPropertiesAnalysis,
        LazyL0BaseAIAnalysis,
        TACAITransformer,
        LazyInterProceduralEscapeAnalysis,
        LazyReturnValueFreshnessAnalysis,
        new LazyCalleesAnalysis(Set(
            StandardInvokeCallees, SerializationRelatedCallees, ReflectionRelatedCallees
        ))
    )

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(
            AIDomainFactoryKey,
            (_: Option[Set[Class[_ <: AnyRef]]]) ⇒
                Set(classOf[DefaultDomainWithCFGAndDefUse[URL]]): Set[Class[_ <: AnyRef]]
        )
    }

    describe("field locality analysis is executed") {
        val as = executeAnalyses(analyses)
        as.propertyStore.shutdown()
        validateProperties(
            as,
            fieldsWithAnnotations(as.project),
            Set("FieldLocality")
        )
    }
}
