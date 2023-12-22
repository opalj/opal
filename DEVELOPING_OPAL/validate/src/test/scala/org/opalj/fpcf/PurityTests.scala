/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

import org.opalj.ai.domain.l1
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.analyses.EagerL0PurityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyTypeImmutabilityAnalysis
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL0FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL1FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL2FieldAssignabilityAnalysis
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
                    LazyL0FieldAssignabilityAnalysis,
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
                EagerL1PurityAnalysis,
                LazyL1FieldAssignabilityAnalysis,
                LazyFieldImmutabilityAnalysis,
                LazyClassImmutabilityAnalysis,
                LazyTypeImmutabilityAnalysis,
                EagerFieldAccessInformationAnalysis
            )
        )

        as.propertyStore.shutdown()
        validateProperties(as, contextsWithAnnotations(as.project), Set("Purity"))
    }

    describe(
        "the org.opalj.fpcf.analyses.L2PurityAnalysis is executed " +
            "together with the FieldImmutabilityAnalysis and L2FieldAssignabilityAnalysis"
    ) {

            L2PurityAnalysis.setRater(Some(SystemOutLoggingAllExceptionRater))

            val as = executeAnalyses(
                Set(
                    EagerL2PurityAnalysis,
                    LazyL2FieldAssignabilityAnalysis,
                    LazyFieldImmutabilityAnalysis,
                    LazyClassImmutabilityAnalysis,
                    LazyTypeImmutabilityAnalysis,
                    LazyStaticDataUsageAnalysis,
                    LazyL0CompileTimeConstancyAnalysis,
                    LazyInterProceduralEscapeAnalysis,
                    LazyReturnValueFreshnessAnalysis,
                    LazyFieldLocalityAnalysis,
                    LazyInterProceduralEscapeAnalysis,
                    EagerFieldAccessInformationAnalysis
                )
            )

            as.propertyStore.shutdown()
            validateProperties(as, contextsWithAnnotations(as.project), Set("Purity"))
        }
}
