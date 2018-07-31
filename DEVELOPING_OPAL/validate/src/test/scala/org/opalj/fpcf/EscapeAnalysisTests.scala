/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import org.opalj.ai.common.SimpleAIKey
import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse
import org.opalj.br.Method
import org.opalj.fpcf.analyses.LazyVirtualCallAggregatingEscapeAnalysis
import org.opalj.fpcf.analyses.escape.EagerInterProceduralEscapeAnalysis
import org.opalj.fpcf.analyses.escape.EagerSimpleEscapeAnalysis

/**
 * Tests if the escape properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Florian Kuebler
 */
class EscapeAnalysisTests extends PropertiesTest {

    override def executeAnalyses(
        eagerAnalysisRunners: Set[FPCFEagerAnalysisScheduler { type InitializationData = Null }],
        lazyAnalysisRunners:  Set[FPCFLazyAnalysisScheduler { type InitializationData = Null }]
    ): TestContext = {
        val p = FixtureProject.recreate()

        p.getOrCreateProjectInformationKeyInitializationData(
            SimpleAIKey,
            (m: Method) ⇒ {
                new DefaultPerformInvocationsDomainWithCFGAndDefUse(p, m)
            }
        )
        val ps = p.get(PropertyStoreKey)

        ps.setupPhase((eagerAnalysisRunners ++ lazyAnalysisRunners).flatMap(
            _.derives.map(_.asInstanceOf[PropertyMetaInformation].key)
        ))

        lazyAnalysisRunners.foreach(_.startLazily(p, ps, null))
        val as = eagerAnalysisRunners.map(ar ⇒ ar.start(p, ps, null))
        ps.waitOnPhaseCompletion()
        ps.shutdown()
        TestContext(p, ps, as)
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
        val as = executeAnalyses(Set(EagerSimpleEscapeAnalysis))
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
            Set[FPCFEagerAnalysisScheduler { type InitializationData = Null }](
                EagerInterProceduralEscapeAnalysis
            ),
            Set[FPCFLazyAnalysisScheduler { type InitializationData = Null }](
                LazyVirtualCallAggregatingEscapeAnalysis
            )
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
