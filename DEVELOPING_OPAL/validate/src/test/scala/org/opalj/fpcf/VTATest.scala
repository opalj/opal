/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import java.net.URL

import org.opalj.fpcf.properties.vta.ExpectedType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.fpcf.analyses.cg.EagerLibraryEntryPointsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredInstantiatedTypesAnalysis
import org.opalj.tac.fpcf.analyses.TACAITransformer
import org.opalj.tac.fpcf.analyses.cg.LazyCalleesAnalysis
import org.opalj.tac.fpcf.analyses.cg.RTACallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.IFDSBasedVariableTypeAnalysis
import org.opalj.tac.fpcf.analyses.VTANullFact

class VTATest extends PropertiesTest {

    val cgRelatedAnalysisSchedulers: Set[FPCFAnalysisScheduler] = Set[FPCFAnalysisScheduler](
        RTACallGraphAnalysisScheduler,
        EagerLibraryEntryPointsAnalysis,
        LazyL0BaseAIAnalysis,
        TACAITransformer,
        TriggeredInstantiatedTypesAnalysis,
        LazyCalleesAnalysis(Set(StandardInvokeCallees))
    )

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(
            AIDomainFactoryKey
        )(
            (_: Option[Set[Class[_ <: AnyRef]]]) ⇒
                Set[Class[_ <: AnyRef]](classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]])
        )
    }

    describe("Test the ExpectedType annotations") {
        val testContext = executeAnalyses(cgRelatedAnalysisSchedulers + IFDSBasedVariableTypeAnalysis)
        val project = testContext.project
        val declaredMethods = project.get(DeclaredMethodsKey)
        val eas = methodsWithAnnotations(project).map {
            case (methods, entityString, annotations) ⇒ ((declaredMethods(methods), VTANullFact), entityString, annotations)
        }
        testContext.propertyStore.shutdown()
        validateProperties(testContext, eas, Set(ExpectedType.PROPERTY_VALIDATOR_KEY))
    }

}
