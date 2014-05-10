/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package de.tud.cs.st
package bat
package resolved
package ai

import dependency.checking.Specification

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers

/**
 * Tests that OPAL-AI's implemented design is as expected.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ArchitectureTest extends FlatSpec with Matchers with BeforeAndAfterAll {

    behavior of "the Abstract Interpretation Framework's implemented architecture"

    it should "be consistent with the specified architecture" in {
        val expected =
            new Specification {

                ensemble('Core) {
                    "de.tud.cs.st.bat.resolved.ai.*" except
                        classes("""de\.tud\.cs\.st\.bat\.resolved\.ai\..+Test.*""".r)
                }

                ensemble('Domain_Tracing) {
                    "de.tud.cs.st.bat.resolved.ai.domain.tracing.*" except
                        classes("""de\.tud\.cs\.st\.bat\.resolved\.ai\.domain\.tracing\..+Test.*""".r)
                }

                ensemble('Util) {
                    "de.tud.cs.st.bat.resolved.ai.util.*"
                }

                ensemble('Domains) {
                    "de.tud.cs.st.bat.resolved.ai.domain.*" except
                        classes("""de\.tud\.cs\.st\.bat\.resolved\.ai\.domain\..+Test.*""".r)
                }

                ensemble('Project) {
                    "de.tud.cs.st.bat.resolved.ai.project.*" except
                        classes("""de\.tud\.cs\.st\.bat\.resolved\.ai\.project\..+Test.*""".r)
                }

                ensemble('Debug) {
                    "de.tud.cs.st.bat.resolved.ai.debug.*"
                }

                'Util is_only_allowed_to_use empty

                'Core is_only_allowed_to_use ('Util)

                'Domains is_only_allowed_to_use ('Util, 'Core)

                'Project is_only_allowed_to_use ('Util, 'Core, 'Domains)

                'Domain_Tracing is_only_allowed_to_use ('Util, 'Core, 'Domains)

                // 'Debug is allowed to use everything  
            }

        val result = expected.analyze(
            Specification.SourceDirectory("OPAL/ai/target/scala-2.11/classes")
        )
        if (result.nonEmpty) {
            println("Violations:\n\t"+result.mkString("\n\t"))
            fail("The implemented and the specified architecture are not consistent (see the console for details).")
        }
    }
}