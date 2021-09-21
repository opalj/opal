/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.av
package checking

import org.junit.runner.RunWith

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.junit.JUnitRunner

import scala.util.matching.Regex

import org.opalj.av.checking.Specification._

/**
 * Systematic tests created to check the behavior of the Specification package.
 *
 * Tests all methods to handle generated classpath files.
 *
 * @author Marco Torsello
 */
@RunWith(classOf[JUnitRunner])
class ClasspathFileTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

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
            """.*(scala-xml_2.11-.*.jar)""".r
        )
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
                """.*(scala.jar)""".r
            )
            PathToJARs(validClassPath, listOfJARs)
        }

        intercept[SpecificationError] {
            val listOfJARs = List[Regex](
                """.*(scalatest_2.11-3.*.jar)""".r,
                """.*(scala-library-.*.jar)""".r,
                """.*(scala-xml_2.11-.*.jar)""".r
            )
            PathToJARs(invalidClassPath, listOfJARs)
        }
    }
}
