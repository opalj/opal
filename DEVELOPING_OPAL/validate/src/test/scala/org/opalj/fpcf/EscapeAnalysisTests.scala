/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import java.net.URL

import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.fpcf.analyses.LazyVirtualCallAggregatingEscapeAnalysis
import org.opalj.fpcf.analyses.SystemPropertiesAnalysis
import org.opalj.fpcf.analyses.cg.EagerFinalizerAnalysisScheduler
import org.opalj.fpcf.analyses.cg.EagerLoadedClassesAnalysis
import org.opalj.fpcf.analyses.cg.EagerRTACallGraphAnalysisScheduler
import org.opalj.fpcf.analyses.cg.EagerReflectionRelatedCallsAnalysis
import org.opalj.fpcf.analyses.cg.EagerSerializationRelatedCallsAnalysis
import org.opalj.fpcf.analyses.cg.EagerThreadRelatedCallsAnalysis
import org.opalj.fpcf.analyses.cg.LazyCalleesAnalysis
import org.opalj.fpcf.analyses.escape.EagerInterProceduralEscapeAnalysis
import org.opalj.fpcf.analyses.escape.EagerSimpleEscapeAnalysis
import org.opalj.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.tac.fpcf.analyses.LazyL0TACAIAnalysis

/**
 * Tests if the escape properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Florian Kuebler
 */
class EscapeAnalysisTests extends PropertiesTest {

    val eagerAnalyses: Set[FPCFEagerAnalysisScheduler] = Set(
        EagerRTACallGraphAnalysisScheduler,
        EagerLoadedClassesAnalysis,
        EagerFinalizerAnalysisScheduler,
        EagerThreadRelatedCallsAnalysis,
        EagerSerializationRelatedCallsAnalysis,
        EagerReflectionRelatedCallsAnalysis,
        SystemPropertiesAnalysis
    )

    val lazyAnalyses: Set[FPCFLazyAnalysisScheduler] = Set(
        LazyL0TACAIAnalysis,
        new LazyCalleesAnalysis(
            Set(StandardInvokeCallees, SerializationRelatedCallees, ReflectionRelatedCallees)
        )
    )

    override def init(p: Project[URL]): Unit = {
        val performInvocationsDomain = classOf[DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]

        p.updateProjectInformationKeyInitializationData(
            AIDomainFactoryKey,
            (i: Option[Set[Class[_ <: AnyRef]]]) ⇒ (i match {
                case None               ⇒ Set(performInvocationsDomain)
                case Some(requirements) ⇒ requirements + performInvocationsDomain
            }): Set[Class[_ <: AnyRef]]
        )
    }

    describe("no analysis is scheduled") {
        val as = executeAnalyses(Set.empty)
        as.propertyStore.shutdown()
        validateProperties(
            as,
            allocationSitesWithAnnotations(as.project) ++
                explicitFormalParametersWithAnnotations(as.project),
            Set("EscapeProperty")
        )
    }

    describe("the org.opalj.fpcf.analyses.escape.SimpleEscapeAnalysis is executed") {
        val as = executeAnalyses(eagerAnalyses + EagerSimpleEscapeAnalysis, lazyAnalyses)
        as.propertyStore.shutdown()
        validateProperties(
            as,
            allocationSitesWithAnnotations(as.project) ++
                explicitFormalParametersWithAnnotations(as.project),
            Set("EscapeProperty")
        )
    }

    describe("the org.opalj.fpcf.analyses.escape.InterProceduralEscapeAnalysis is executed") {
        val as = executeAnalyses(
            eagerAnalyses + EagerInterProceduralEscapeAnalysis,
            lazyAnalyses + LazyVirtualCallAggregatingEscapeAnalysis
        )
        as.propertyStore.shutdown()
        validateProperties(
            as,
            allocationSitesWithAnnotations(as.project) ++
                explicitFormalParametersWithAnnotations(as.project),
            Set("EscapeProperty")
        )
    }

}
