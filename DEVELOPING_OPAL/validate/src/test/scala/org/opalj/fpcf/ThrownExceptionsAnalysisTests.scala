/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import org.opalj.br.Type
import org.opalj.br.AnnotationLike
import org.opalj.fpcf.analyses.EagerVirtualMethodAllocationFreenessAnalysis
import org.opalj.fpcf.analyses.EagerL1ThrownExceptionsAnalysis
import org.opalj.fpcf.properties.ThrownExceptions

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

    describe("no analysis is scheduled and fallback is used") {
        val as = executeAnalyses(Set.empty)
        val TestContext(p, ps, _) = as

        val pk = Set("ExpectedExceptions", "ExpectedExceptionsByOverridingMethods", "ThrownExceptionsAreUnknown")
        ps.setupPhase(Set.empty, Set.empty)
        for {
            (e, _, annotations) ← methodsWithAnnotations(as.project)
            if annotations.flatMap[(AnnotationLike, String, Type)](a ⇒ getPropertyMatcher(p, pk)(a).toSeq).nonEmpty
        } {
            val epk = EPK(e, ThrownExceptions.key)
            ps.scheduleEagerComputationForEntity(e) { e ⇒
                IntermediateResult(e, new DummyProperty, new DummyProperty, Set(epk), _ ⇒ NoResult)
            }
        }

        ps.waitOnPhaseCompletion()

        validateProperties(as, methodsWithAnnotations(as.project), pk)

        ps.shutdown()
    }

    describe("L1ThrownExceptionsAnalysis and EagerVirtualMethodAllocationFreenessAnalysis are executed") {
        val as = executeAnalyses(Set(
            EagerVirtualMethodAllocationFreenessAnalysis,
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
