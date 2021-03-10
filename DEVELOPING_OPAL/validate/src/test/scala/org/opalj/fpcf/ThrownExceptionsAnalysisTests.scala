/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.br.fpcf.analyses.EagerL1ThrownExceptionsAnalysis
import org.opalj.br.fpcf.analyses.EagerVirtualMethodThrownExceptionsAnalysis

/**
 * Tests if the properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Andreas Muttscheller
 */
class ThrownExceptionsAnalysisTests extends PropertiesTest {

    object DummyProperty {
        final val Key: PropertyKey[DummyProperty] = {
            PropertyKey.create[Entity, DummyProperty]("DummyProperty", new DummyProperty)
        }
    }

    sealed class DummyProperty extends Property {
        override type Self = DummyProperty

        override def key: PropertyKey[DummyProperty] = DummyProperty.Key
    }

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/thrown_exceptions")
    }

    describe("L1ThrownExceptionsAnalysis and VirtualMethodThrownExceptionsAnalysis are executed") {
        val as = executeAnalyses(Set(
            EagerVirtualMethodThrownExceptionsAnalysis,
            EagerL1ThrownExceptionsAnalysis
        ))
        val TestContext(_, ps, _) = as

        validateProperties(
            as,
            methodsWithAnnotations(as.project),
            Set(
                "ExpectedExceptions",
                "ExpectedExceptionsByOverridingMethods",
                "ThrownExceptionsAreUnknown"
            )
        )

        ps.shutdown()
    }

}
