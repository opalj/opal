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
