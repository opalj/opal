/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
import java.net.URL

import org.opalj.fpcf.properties.taint.FlowPath
import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.{DeclaredMethodsKey, Project}
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.br.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.br.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.br.fpcf.cg.properties.ThreadRelatedIncompleteCallSites
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.tac.fpcf.analyses.TaintAnalysis
import org.opalj.tac.fpcf.analyses.NullFact
import org.opalj.tac.fpcf.analyses.cg.LazyCalleesAnalysis
import org.opalj.tac.fpcf.analyses.cg.RTACallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.reflection.TriggeredReflectionRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredLoadedClassesAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredStaticInitializerAnalysis
import org.opalj.tac.fpcf.analyses.TACAITransformer
import org.opalj.tac.fpcf.analyses.TriggeredSystemPropertiesAnalysis
import org.opalj.tac.fpcf.analyses.cg.EagerLibraryEntryPointsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredFinalizerAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TriggeredSerializationRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredThreadRelatedCallsAnalysis

/**
 * @author Mario Trageser
 */
class TaintAnalysisTest extends PropertiesTest {

    val cgRelatedAnalysisSchedulers: Set[FPCFAnalysisScheduler] = Set[FPCFAnalysisScheduler](
        RTACallGraphAnalysisScheduler,
        EagerLibraryEntryPointsAnalysis,
        TriggeredStaticInitializerAnalysis,
        TriggeredLoadedClassesAnalysis,
        TriggeredFinalizerAnalysisScheduler,
        TriggeredThreadRelatedCallsAnalysis,
        TriggeredSerializationRelatedCallsAnalysis,
        TriggeredReflectionRelatedCallsAnalysis,
        TriggeredSystemPropertiesAnalysis,
        LazyL0BaseAIAnalysis,
        TACAITransformer,
        LazyCalleesAnalysis(Set(
            StandardInvokeCallees,
            SerializationRelatedCallees,
            ReflectionRelatedCallees,
            ThreadRelatedIncompleteCallSites
        ))
    )

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(
            AIDomainFactoryKey,
            (_: Option[Set[Class[_ <: AnyRef]]]) ⇒
                Set[Class[_ <: AnyRef]](classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]])
        )
    }

    describe("Test the FlowPath annotations") {
        val testContext = executeAnalyses(cgRelatedAnalysisSchedulers + TaintAnalysis)
        val project = testContext.project
        val declaredMethods = project.get(DeclaredMethodsKey)
        val eas = methodsWithAnnotations(project).map {
            case (methods, entityString, annotations) ⇒ ((declaredMethods(methods), NullFact()), entityString, annotations)
        }
        testContext.propertyStore.shutdown()
        validateProperties(testContext, eas, Set(FlowPath.PROPERTY_VALIDATOR_KEY))
    }
}
