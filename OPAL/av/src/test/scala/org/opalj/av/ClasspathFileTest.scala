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
package org.opalj.av
package checking

import org.junit.runner.RunWith
import org.opalj.av.checking.Specification._
import org.opalj.bi.TestSupport.locateTestResources
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import scala.util.matching.Regex

/**
 * Systematic tests created to check the behavior of the Specification package.
 *
 * Tests all methods to handle generated classpath files.
 *
 * @author Marco Torsello
 */
@RunWith(classOf[JUnitRunner])
class ClasspathFileTest extends FlatSpec with Matchers with BeforeAndAfterAll {

    behavior of "the Architecture Validation Library when processing classpath files"

    val validClassPath = Classpath("OPAL/av/src/test/resources/ValidClasspathFile.txt", ':')
    val invalidClassPath = Classpath("OPAL/av/src/test/resources/InvalidClasspathFile.txt")

    it should "return the expected list of paths that match the given regular expression" in {
        val scalatestJAR = PathToJARs(validClassPath, """.*(scalatest_2.11-2.1.7.jar)""".r)
        val expectedListOfPaths1 = List[String](
            "/root/org/scalatest/scalatest_2.11/2.1.7/scalatest_2.11-2.1.7.jar"
        )
        scalatestJAR.toList should equal(expectedListOfPaths1)

        val scalaLibraryJAR = PathToJARs(validClassPath, """.*(scala-library-2.11.0.jar)""".r)
        val expectedListOfPaths2 = List[String](
            "/root/org/scala-lang/scala-library/2.11.0/scala-library-2.11.0.jar"
        )
        scalaLibraryJAR.toList should equal(expectedListOfPaths2)
    }

    it should "throw a specification error if path couldn't be found" in {
        intercept[SpecificationError] { PathToJARs(validClassPath, """.*(scalatest.jar)""".r) }
        intercept[SpecificationError] { PathToJARs(invalidClassPath, """.*(scalatest_2.11-2.1.7.jar)""".r) }
    }

    it should "return the expected list of paths that match the given list of regular expressions" in {
        val listOfJARs = List[Regex](
            """.*(scalatest_2.11-.*.jar)""".r,
            """.*(scala-library-.*.jar)""".r,
            """.*(scala-xml_2.11-.*.jar)""".r)
        val listOfPaths: Iterable[String] = PathToJARs(validClassPath, listOfJARs)

        val expectedListOfPaths = List[String](
            "/root/org/scalatest/scalatest_2.11/2.1.7/scalatest_2.11-2.1.7.jar",
            "/root/org/scala-lang/scala-library/2.11.0/scala-library-2.11.0.jar",
            "/root/org/scala-lang/modules/scala-xml_2.11/1.0.1/scala-xml_2.11-1.0.1.jar"
        )

        listOfPaths.toList should equal(expectedListOfPaths)
    }

    it should "throw a specification error if the path to one of the given JARs couldn't be found" in {
        intercept[SpecificationError] {
            val listOfJARs = List[Regex](
                """.*(scalatest_2.11-.*.jar)""".r,
                """.*(scala-library-.*.jar)""".r,
                """.*(scala.jar)""".r)
            PathToJARs(validClassPath, listOfJARs)
        }

        intercept[SpecificationError] {
            val listOfJARs = List[Regex](
                """.*(scalatest_2.11-3.*.jar)""".r,
                """.*(scala-library-.*.jar)""".r,
                """.*(scala-xml_2.11-.*.jar)""".r)
            PathToJARs(invalidClassPath, listOfJARs)
        }
    }
}
