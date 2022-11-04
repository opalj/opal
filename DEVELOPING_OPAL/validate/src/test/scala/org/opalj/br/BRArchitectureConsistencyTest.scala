/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.opalj.av.checking.Specification
import org.opalj.util.ScalaMajorVersion

/**
 * Tests that the implemented architecture of the bytecode representation project
 * is consistent with its specification/with the intended architecture.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class BRArchitectureConsistencyTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

    behavior of "the Bytecode Representation Project's implemented architecture"

    it should "be consistent with the specified architecture" in {
        val expected =
            new Specification(
                Specification.ProjectDirectory(s"OPAL/br/target/scala-$ScalaMajorVersion/classes"),
                useAnsiColors = true
            ) {

                ensemble(Symbol("Br")) {
                    "org.opalj.br.*" except
                        classes("""org\.opalj\.br\..+Test.*""".r)
                }

                ensemble(Symbol("Reader")) {
                    "org.opalj.br.reader.*" except
                        classes("""org\.opalj\.br\.reader\..+Test.*""".r)
                }

                ensemble(Symbol("Instructions")) {
                    "org.opalj.br.instructions.*" except
                        classes("""org\.opalj\.br\.instructions\..+Test.*""".r)
                }

                ensemble(Symbol("CFG")) {
                    "org.opalj.br.cfg.*" except
                        classes("""org\.opalj\.br\.cfg\..+Test.*""".r)
                }

                ensemble(Symbol("Analyses")) {
                    "org.opalj.br.analyses.*" except
                        classes("""org\.opalj\.br\.analyses\..+Test.*""".r)
                }

                // BR and Instructions are considered as one...
                Symbol("Br") is_only_allowed_to (USE, Symbol("Instructions"), Symbol("CFG"))
                Symbol("Instructions") is_only_allowed_to (USE, Symbol("Br"), Symbol("CFG"))
                Symbol("CFG") is_only_allowed_to (USE, Symbol("Br"), Symbol("Instructions"))

                Symbol("Reader") is_only_allowed_to (USE, Symbol("Br"), Symbol("Instructions"))
                Symbol("Analyses") is_only_allowed_to (USE, Symbol("Br"), Symbol("CFG"), Symbol("Instructions"), Symbol("Reader"))

                // 'Reader is allowed to use everything

            }

        val result = expected.analyze()
        if (result.nonEmpty) {
            println("Violations:\n\t"+result.map(_.toString(useAnsiColors = true)).mkString("\n\t"))
            fail("The implemented and the specified architecture are not consistent (see the console for details).")
        }
    }
}
