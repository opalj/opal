/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.junit.runner.RunWith

import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers

import org.opalj.av.checking.Specification

/**
 * Tests that the implemented architecture of the bytecode representation project
 * is consistent with its specification/with the intended architecture.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class BRArchitectureConsistencyTest extends FlatSpec with Matchers with BeforeAndAfterAll {

    behavior of "the Bytecode Representation Project's implemented architecture"

    it should "be consistent with the specified architecture" in {
        val expected =
            new Specification(
                Specification.ProjectDirectory("OPAL/br/target/scala-2.12/classes"),
                useAnsiColors = true
            ) {

                ensemble('Br) {
                    "org.opalj.br.*" except
                        classes("""org\.opalj\.bi\..+Test.*""".r)
                }

                ensemble('Reader) {
                    "org.opalj.br.reader.*" except
                        classes("""org\.opalj\.br\.reader\..+Test.*""".r)
                }

                ensemble('Instructions) {
                    "org.opalj.br.instructions.*" except
                        classes("""org\.opalj\.br\.instructions\..+Test.*""".r)
                }

                ensemble('CFG) {
                    "org.opalj.br.cfg.*" except
                        classes("""org\.opalj\.br\.cfg\..+Test.*""".r)
                }

                ensemble('Analyses) {
                    "org.opalj.br.analyses.*" except
                        classes("""org\.opalj\.br\.analyses\..+Test.*""".r)
                }

                // BR and Instructions are considered as one...
                'Br is_only_allowed_to (USE, 'Instructions, 'CFG)
                'Instructions is_only_allowed_to (USE, 'Br, 'CFG)
                'CFG is_only_allowed_to (USE, 'Br, 'Instructions)

                'Reader is_only_allowed_to (USE, 'Br, 'Instructions)
                'Analyses is_only_allowed_to (USE, 'Br, 'CFG, 'Instructions, 'Reader)

                // 'Reader is allowed to use everything

            }

        val result = expected.analyze()
        if (result.nonEmpty) {
            println("Violations:\n\t"+result.map(_.toString(useAnsiColors = true)).mkString("\n\t"))
            fail("The implemented and the specified architecture are not consistent (see the console for details).")
        }
    }
}
