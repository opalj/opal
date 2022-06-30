/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.opalj.av.checking.Specification
import org.opalj.util.ScalaMajorVersion

/**
 * Tests that the implemented architecture of the abstract interpretation
 * framework is consistent with its specification/with the intended
 * architecture.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class AIArchitectureConsistencyTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

    behavior of "the Abstract Interpretation Framework's implemented architecture"

    it should "be consistent with the specified architecture" in {
        val expected =
            new Specification(
                Specification.ProjectDirectory(s"OPAL/ai/target/scala-$ScalaMajorVersion/classes"),
                useAnsiColors = true
            ) {

                ensemble(Symbol("Util")) { "org.opalj.ai.util.*" }

                ensemble(Symbol("AI")) {
                    "org.opalj.ai.*" except classes("""org\.opalj\.ai\..+Test.*""".r)
                }
                ensemble(Symbol("Issues")) {
                    "org.opalj.issues.*" except
                        classes("""org\.opalj\.issues\..+Test.*""".r)
                }
                ensemble(Symbol("Common")) {
                    "org.opalj.ai.common.*" except
                        classes("""org\.opalj\.ai\.common\..+Test.*""".r)
                }
                ensemble(Symbol("Domain")) {
                    "org.opalj.ai.domain.*" except
                        classes("""org\.opalj\.ai\.domain\..+Test.*""".r)
                }
                ensemble(Symbol("DomainL0")) {
                    "org.opalj.ai.domain.l0.*" except
                        classes("""org\.opalj\.ai\.domain\.l0\..+Test.*""".r)
                }
                ensemble(Symbol("DomainL1")) {
                    "org.opalj.ai.domain.l1.*" except
                        classes("""org\.opalj\.ai\.domain\.l1\..+Test.*""".r)
                }
                ensemble(Symbol("DomainL2")) {
                    "org.opalj.ai.domain.l2.*" except
                        classes("""org\.opalj\.ai\.domain\.l2\..+Test.*""".r)
                }
                ensemble(Symbol("DomainTracing")) {
                    "org.opalj.ai.domain.tracing.*" except
                        classes("""org\.opalj\.ai\.domain\.tracing\..+Test.*""".r)
                }

                ensemble(Symbol("Project")) {
                    "org.opalj.ai.project.*" except
                        classes("""org\.opalj\.ai\.project\..+Test.*""".r)
                }

                ensemble(Symbol("DomainLA")) {
                    "org.opalj.ai.domain.la.*" except
                        classes("""org\.opalj\.ai\.domain\.la\..+Test.*""".r)
                }
                ensemble(Symbol("Analyses")) { "org.opalj.fpcf.analyses.**" }

                Symbol("Util") is_only_allowed_to (USE, empty)

                Symbol("AI") is_only_allowed_to (USE, Symbol("Util"))

                Symbol("Issues") is_only_allowed_to (USE, Symbol("AI"))

                Symbol("Domain") is_only_allowed_to (USE, Symbol("Util"), Symbol("AI"))

                Symbol("DomainL0") is_only_allowed_to (USE, Symbol("Util"), Symbol("AI"), Symbol("Domain"))
                Symbol("DomainL1") is_only_allowed_to (USE, Symbol("Util"), Symbol("AI"), Symbol("Domain"), Symbol("DomainL0"))
                Symbol("DomainL2") is_only_allowed_to (USE, Symbol("Util"), Symbol("AI"), Symbol("Domain"), Symbol("DomainL0"), Symbol("DomainL1"))

                Symbol("DomainTracing") is_only_allowed_to (USE, Symbol("Util"), Symbol("AI"), Symbol("Domain"))

                Symbol("Project") is_only_allowed_to (USE, Symbol("Util"), Symbol("AI"), Symbol("Domain"), Symbol("DomainL0"), Symbol("DomainL1"), Symbol("DomainL2"))

                // we have a cyclic dependency between code in ..ai.domain.la and
                // ai.analyses.** which is "intended" since we do fix-point
                // computations
                Symbol("DomainLA") is_only_allowed_to (USE, Symbol("Util"), Symbol("AI"), Symbol("Domain"), Symbol("DomainL0"), Symbol("DomainL1"), Symbol("DomainL2"), Symbol("Analyses"))
                Symbol("Analyses") is_only_allowed_to (USE, Symbol("Util"), Symbol("AI"), Symbol("Common"), Symbol("Domain"), Symbol("DomainL0"), Symbol("DomainL1"), Symbol("DomainL2"), Symbol("DomainLA"), Symbol("Project"))

                // 'Common is allowed to use everything
            }

        val result = expected.analyze()
        if (result.nonEmpty) {
            println("Violations:\n\t"+result.map(_.toString(useAnsiColors = true)).mkString("\n\t"))
            fail("The implemented and the specified architecture are not consistent (see the console for details).")
        }
    }
}
