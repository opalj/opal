/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
                    "org.opalj.ai.common.*" except
                        classes("""org\.opalj\.ai\.common\..+Test.*""".r)
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
