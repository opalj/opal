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
package org.opalj
package ai

import org.junit.runner.RunWith

import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers

import org.opalj.av.checking.Specification

/**
 * Tests that the implemented architecture of the abstract interpretation
 * framework is consistent with its specification/with the intended
 * architecture.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class AIArchitectureConsistencyTest extends FlatSpec with Matchers with BeforeAndAfterAll {

    behavior of "the Abstract Interpretation Framework's implemented architecture"

    it should "be consistent with the specified architecture" in {
        val expected =
            new Specification(
                Specification.ProjectDirectory("OPAL/ai/target/scala-2.12/classes"),
                useAnsiColors = true
            ) {

                ensemble('Util) { "org.opalj.ai.util.*" }

                ensemble('AI) {
                    "org.opalj.ai.*" except classes("""org\.opalj\.ai\..+Test.*""".r)
                }
                ensemble('Issues) {
                    "org.opalj.issues.*" except
                        classes("""org\.opalj\.issues\..+Test.*""".r)
                }
                ensemble('Common) {
                    "org.opalj.common.*" except
                        classes("""org\.opalj\.common\..+Test.*""".r)
                }
                ensemble('Domain) {
                    "org.opalj.ai.domain.*" except
                        classes("""org\.opalj\.ai\.domain\..+Test.*""".r)
                }
                ensemble('DomainL0) {
                    "org.opalj.ai.domain.l0.*" except
                        classes("""org\.opalj\.ai\.domain\.l0\..+Test.*""".r)
                }
                ensemble('DomainL1) {
                    "org.opalj.ai.domain.l1.*" except
                        classes("""org\.opalj\.ai\.domain\.l1\..+Test.*""".r)
                }
                ensemble('DomainL2) {
                    "org.opalj.ai.domain.l2.*" except
                        classes("""org\.opalj\.ai\.domain\.l2\..+Test.*""".r)
                }
                ensemble('DomainTracing) {
                    "org.opalj.ai.domain.tracing.*" except
                        classes("""org\.opalj\.ai\.domain\.tracing\..+Test.*""".r)
                }

                ensemble('Project) {
                    "org.opalj.ai.project.*" except
                        classes("""org\.opalj\.ai\.project\..+Test.*""".r)
                }

                ensemble('DomainLA) {
                    "org.opalj.ai.domain.la.*" except
                        classes("""org\.opalj\.ai\.domain\.la\..+Test.*""".r)
                }
                ensemble('Analyses) { "org.opalj.fpcf.analyses.**" }

                'Util is_only_allowed_to (USE, empty)

                'AI is_only_allowed_to (USE, 'Util)

                'Issues is_only_allowed_to (USE, 'AI)

                'Domain is_only_allowed_to (USE, 'Util, 'AI)

                'DomainL0 is_only_allowed_to (USE, 'Util, 'AI, 'Domain)
                'DomainL1 is_only_allowed_to (USE, 'Util, 'AI, 'Domain, 'DomainL0)
                'DomainL2 is_only_allowed_to (USE, 'Util, 'AI, 'Domain, 'DomainL0, 'DomainL1)

                'DomainTracing is_only_allowed_to (USE, 'Util, 'AI, 'Domain)

                'Project is_only_allowed_to (USE, 'Util, 'AI, 'Domain, 'DomainL0, 'DomainL1, 'DomainL2)

                // we have a cyclic dependency between code in ..ai.domain.la and
                // ai.analyses.** which is "intended" since we do fix-point
                // computations
                'DomainLA is_only_allowed_to (USE, 'Util, 'AI, 'Domain, 'DomainL0, 'DomainL1, 'DomainL2, 'Analyses)
                'Analyses is_only_allowed_to (USE, 'Util, 'AI, 'Common, 'Domain, 'DomainL0, 'DomainL1, 'DomainL2, 'DomainLA, 'Project)

                // 'Common is allowed to use everything
            }

        val result = expected.analyze()
        if (result.nonEmpty) {
            println("Violations:\n\t"+result.map(_.toString(useAnsiColors = true)).mkString("\n\t"))
            fail("The implemented and the specified architecture are not consistent (see the console for details).")
        }
    }
}
