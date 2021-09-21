/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package de

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.opalj.av.checking._
import org.opalj.br.reader.InvokedynamicRewriting

/**
 * Tests that the implemented architecture of the dependency extraction
 * library is consistent with its specification/with the intended
 * architecture.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DEArchitectureConsistencyTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

    behavior of "the Dependency Extraction Library's implemented architecture"

    it should "be well modularized in the sense that a superpackage does not depend on a subpackage" in {
        val deTargetClasses = Specification
            .ProjectDirectory("OPAL/de/target/scala-2.12/classes")
            .filterNot { cfSrc ⇒
                // Ignore the rewritten lambda expressions
                val (cf, _) = cfSrc
                cf.thisType.toJava.matches(InvokedynamicRewriting.LambdaNameRegEx)
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
