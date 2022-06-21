/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.analyses.EagerL0PurityAnalysis
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyL0FieldMutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.ai.domain.l1
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.purity.EagerL1PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.EagerL2PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.SystemOutLoggingAllExceptionRater

/**
 * Tests if the properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Dominik Helm
 */
class PurityTests extends PropertiesTest {

    override def withRT = true

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey)(
            _ => Set[Class[_ <: AnyRef]](classOf[l1.DefaultDomainWithCFGAndDefUse[URL]])
        )

        p.get(RTACallGraphKey)
    }

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/purity")
    }

    describe("the org.opalj.fpcf.analyses.L0PurityAnalysis is executed") {
        val as =
            executeAnalyses(
                Set(
                    EagerL0PurityAnalysis,
                    LazyL0FieldMutabilityAnalysis,
                    LazyClassImmutabilityAnalysis,
                    LazyTypeImmutabilityAnalysis
                )
            )
        as.propertyStore.shutdown()
        validateProperties(as, contextsWithAnnotations(as.project), Set("Purity"))
    }

    describe("the org.opalj.fpcf.analyses.L1PurityAnalysis is executed") {
        L1PurityAnalysis.setRater(Some(SystemOutLoggingAllExceptionRater))

        val as = executeAnalyses(
            Set(
                LazyL1FieldMutabilityAnalysis,
                LazyClassImmutabilityAnalysis,
                LazyTypeImmutabilityAnalysis,
                EagerL1PurityAnalysis
            )
        )

        as.propertyStore.shutdown()
        validateProperties(as, contextsWithAnnotations(as.project), Set("Purity"))
    }

    describe("the org.opalj.fpcf.analyses.L2PurityAnalysis is executed") {

        L2PurityAnalysis.setRater(Some(SystemOutLoggingAllExceptionRater))

        val as = executeAnalyses(Set(
            EagerL2PurityAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyL0CompileTimeConstancyAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis,
            LazyL1FieldMutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyTypeImmutabilityAnalysis
        ))

        as.propertyStore.shutdown()
        validateProperties(as, contextsWithAnnotations(as.project), Set("Purity"))
    }

}
