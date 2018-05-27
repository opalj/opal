/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj.fpcf

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
        final val Key: PropertyKey[DummyProperty] =
            PropertyKey.create[Entity, DummyProperty](
                "DummyProperty",
                new DummyProperty
            )
    }

    sealed class DummyProperty extends Property {
        override type Self = DummyProperty

        override def key = DummyProperty.Key
    }

    describe("no analysis is scheduled and fallback is used") {
        val as = executeAnalyses(Set.empty)
        val pk = Set("ExpectedExceptions", "ExpectedExceptionsByOverridingMethods", "ThrownExceptionsAreUnknown")
        val TestContext(p, ps, _) = as
        ps.setupPhase(Set(ThrownExceptions.key))
        for {
            (e, _, annotations) ← methodsWithAnnotations(as.project)
            if annotations.flatMap(getPropertyMatcher(p, pk)).nonEmpty
        } {
            val epk = EPK(e, ThrownExceptions.key)
            ps.scheduleEagerComputationForEntity(e) { e ⇒
                IntermediateResult(e, new DummyProperty, new DummyProperty, Set(epk), _ ⇒ NoResult)
            }
        }

        ps.waitOnPhaseCompletion()

        validateProperties(
            as,
            methodsWithAnnotations(as.project),
            pk
        )
    }

    describe("L1ThrownExceptionsAnalysis and EagerVirtualMethodAllocationFreenessAnalysis are executed") {
        val as = executeAnalyses(Set(
            EagerVirtualMethodAllocationFreenessAnalysis,
            EagerL1ThrownExceptionsAnalysis
        ))
        validateProperties(
            as,
            methodsWithAnnotations(as.project),
            Set(
                "ExpectedExceptions",
                "ExpectedExceptionsByOverridingMethods",
                "ThrownExceptionsAreUnknown"
            )
        )
    }

}
