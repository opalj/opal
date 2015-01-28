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

    val validClassPath = Classpath("OPAL/av/src/test/resources/ValidClasspathFile.txt",':')
    val invalidClassPath = Classpath("OPAL/av/src/test/resources/InvalidClasspathFile.txt")

    it should "return the expected path to the given JAR" in {
        val scalatestJAR: String = PathToJAR(validClassPath, "scalatest_2.11-2.1.7.jar")
        scalatestJAR should be(
            "/Users/Testuser/.m2/repository/org/scalatest/scalatest_2.11/2.1.7/scalatest_2.11-2.1.7.jar"
        )

        val scalaLibraryJAR: String = PathToJAR(validClassPath, "scala-library-2.11.0.jar")
        scalaLibraryJAR should be(
            "/Users/Testuser/.m2/repository/org/scala-lang/scala-library/2.11.0/scala-library-2.11.0.jar"
        )
    }

    it should "throw a specification error if the path to the given JAR couldn't be found" in {
        intercept[SpecificationError] { PathToJAR(validClassPath, "scalatest.jar") }
        intercept[SpecificationError] { PathToJAR(invalidClassPath, "scalatest_2.11-2.1.7.jar") }
    }

    it should "return the expected list of paths to the given JARs" in {
        val listOfJARs = List[String]("scalatest_2.11-2.1.7.jar", "scala-library-2.11.0.jar", "scala-xml_2.11-1.0.1.jar")
        val listOfPaths: Iterable[String] = PathToJARs(validClassPath, listOfJARs)

        val expectedListOfPaths = List[String](
            "/Users/Testuser/.m2/repository/org/scalatest/scalatest_2.11/2.1.7/scalatest_2.11-2.1.7.jar",
            "/Users/Testuser/.m2/repository/org/scala-lang/scala-library/2.11.0/scala-library-2.11.0.jar",
            "/Users/Testuser/.m2/repository/org/scala-lang/modules/scala-xml_2.11/1.0.1/scala-xml_2.11-1.0.1.jar"
        )

        listOfPaths.toList should equal(expectedListOfPaths)
    }

    it should "throw a specification error if the path to one of the given JARs couldn't be found" in {
        intercept[SpecificationError] {
            val listOfJARs = List[String]("scalatest_2.11-2.1.7.jar", "scala-library-2.11.0.jar", "scala.jar")
            PathToJARs(validClassPath, listOfJARs)
        }

        intercept[SpecificationError] {
            val listOfJARs = List[String]("scalatest_2.11-2.1.7.jar", "scala-library-2.11.0.jar", "scala-xml_2.11-1.0.1.jar")
            PathToJARs(invalidClassPath, listOfJARs)
        }
    }
}
