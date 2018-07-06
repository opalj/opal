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
package de

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.opalj.av.checking._
import org.opalj.br.reader.LambdaExpressionsRewriting

/**
 * Tests that the implemented architecture of the dependency extraction
 * library is consistent with its specification/with the intended
 * architecture.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DEArchitectureConsistencyTest extends FlatSpec with Matchers with BeforeAndAfterAll {

    behavior of "the Dependency Extraction Library's implemented architecture"

    it should "be well modularized in the sense that a superpackage does not depend on a subpackage" in {
        val deTargetClasses = Specification
            .ProjectDirectory("OPAL/de/target/scala-2.12/classes")
            .filterNot { cfSrc ⇒
                // Ignore the rewritten lambda expressions
                val (cf, _) = cfSrc
                cf.thisType.toJava.matches(LambdaExpressionsRewriting.LambdaNameRegEx)
            }
        val expected =
            new Specification(
                deTargetClasses,
                useAnsiColors = true
            ) {

                val DependencyExtractorElements: SourceElementsMatcher =
                    "org.opalj.de.DependencyExtractor*"

                val DependencyTypeElements: SourceElementsMatcher =
                    "org.opalj.de.DependencyType*"

                val DependencyProcessorElements: SourceElementsMatcher =
                    "org.opalj.de.DependencyProcessor*"

                ensemble('DependencyExtractorCore) {
                    DependencyExtractorElements and
                        DependencyTypeElements and
                        DependencyProcessorElements
                }

                ensemble('DependencyExtractionSupport) {
                    "org.opalj.de.*" except DependencyExtractorElements
                }

                'DependencyExtractorCore is_only_allowed_to (USE, empty)
            }

        val result = expected.analyze()
        result.map(_.toString(useAnsiColors = true)).mkString("\n") should be("")
    }

}
