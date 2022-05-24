/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.opalj.av.checking.Specification
import org.opalj.util.ScalaMajorVersion

/**
 * Tests that the implemented architecture of the infrastructure project
 * is consistent with its specification/with the intended architecture.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class BIArchitectureConsistencyTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

    behavior of "the Infrastructure Project's implemented architecture"

    it should "be consistent with the specified architecture" in {
        val expected =
            new Specification(
                Specification.ProjectDirectory(s"OPAL/bi/target/scala-$ScalaMajorVersion/classes"),
                useAnsiColors = true
            ) {

                ensemble(Symbol("Bi")) {
                    "org.opalj.bi.*" except
                        classes("""org\.opalj\.bi\..+Test.*""".r)
                }

                ensemble(Symbol("Reader")) {
                    "org.opalj.bi.reader.*" except
                        classes("""org\.opalj\.bi\.reader\..+Test.*""".r)
                }

                Symbol("Bi") is_only_allowed_to (USE, empty)

                // 'Reader is allowed to use everything

            }

        val result = expected.analyze()
        if (result.nonEmpty) {
            println("Violations:\n\t"+result.map(_.toString(useAnsiColors = true)).mkString("\n\t"))
            fail("The implemented and the specified architecture are not consistent (see the console for details).")
        }
    }
}
