/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

import org.opalj.ai.domain.l1
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.EagerL1ThrownExceptionsAnalysis

/**
 * Tests if the properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Andreas Muttscheller
 */
class ThrownExceptionsAnalysisTests extends PropertiesTest {

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey)(_ =>
            Set[Class[? <: AnyRef]](classOf[l1.DefaultDomainWithCFGAndDefUse[URL]])
        )

        p.get(RTACallGraphKey)
    }

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/thrown_exceptions")
    }

    describe("L1ThrownExceptionsAnalysis is executed") {
        val as = executeAnalyses(Set(
            EagerL1ThrownExceptionsAnalysis
        ))
        val TestContext(_, ps, _) = as

        validateProperties(
            as,
            contextsWithAnnotations(as.project),
            Set(
                "ExpectedExceptions",
                "ThrownExceptionsAreUnknown"
            )
        )

        ps.shutdown()
    }

}
