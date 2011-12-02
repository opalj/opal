/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*  - Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*  - Neither the name of the Software Technology Group or Technische 
*    Universität Darmstadt nor the names of its contributors may be used to 
*    endorse or promote products derived from this software without specific 
*    prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat.resolved
package dependency

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import de.tud.cs.st.bat.resolved.DependencyType._
import de.tud.cs.st.bat.resolved.reader.Java6Framework
import scala.math.Ordering$Long$

/**
 * @author Thomas Schlosser
 *
 */
@RunWith(classOf[JUnitRunner])
class DependencyExtractorRunTimeTest extends FunSuite
        with ClassFileTestUtility
        with de.tud.cs.st.util.perf.BasicPerformanceEvaluation {

    test("testDependencyExtraction - Apache ANT 1.7.1 - javac 6 default target.zip") {
        testDependencyExtraction("test/classfiles/Apache ANT 1.7.1 - javac 6 default target.zip")
    }

    test("testDependencyExtraction - Apache ANT 1.7.1 - target 1.2.zip") {
        testDependencyExtraction("test/classfiles/Apache ANT 1.7.1 - target 1.2.zip")
    }

    test("testDependencyExtraction - Apache ANT 1.7.1 - target 1.4 (no debug).zip") {
        testDependencyExtraction("test/classfiles/Apache ANT 1.7.1 - target 1.4 (no debug).zip")
    }

    test("testDependencyExtraction - Apache ANT 1.7.1 - target 1.5.zip") {
        testDependencyExtraction("test/classfiles/Apache ANT 1.7.1 - target 1.5.zip")
    }

    test("testDependencyExtraction - BAT2XML - target 1.7.zip") {
        testDependencyExtraction("test/classfiles/BAT2XML - target 1.7.zip")
    }

    test("testDependencyExtraction - Columbus 2008_10_16 - target 1.5.zip") {
        testDependencyExtraction("test/classfiles/Columbus 2008_10_16 - target 1.5.zip")
    }

    test("testDependencyExtraction - Columbus 2008_10_16 - target 1.6.zip") {
        testDependencyExtraction("test/classfiles/Columbus 2008_10_16 - target 1.6.zip")
    }

    test("testDependencyExtraction - Flashcards 0.4 - target 1.6.zip") {
        testDependencyExtraction("test/classfiles/Flashcards 0.4 - target 1.6.zip")
    }

    test("testDependencyExtraction - hibernate-core-3.6.0.Final.jar") {
        testDependencyExtraction("test/classfiles/hibernate-core-3.6.0.Final.jar")
    }

    test("testDependencyExtraction - Multithreaded RPN Calculator 2008_10_17 - Java 6 all debug info.zip") {
        testDependencyExtraction("test/classfiles/Multithreaded RPN Calculator 2008_10_17 - Java 6 all debug info.zip")
    }

    test("testDependencyExtraction - Opal 0.3.zip") {
        testDependencyExtraction("test/classfiles/Opal 0.3.zip")
    }

    test("testDependencyExtraction - Signatures.zip") {
        testDependencyExtraction("test/classfiles/Signatures.zip")
    }

    private def testDependencyExtraction(zipFile: String) {
        println("testDependencyExtraction["+zipFile+"]")

        val dependencyBuilder = new DependencyBuilder with DefaultIDMappingDependencyBuilder {
            def addDependency(src: Int, trgt: Int, dType: DependencyType) {}
        }
        val dependencyExtractor = new DependencyExtractor(dependencyBuilder)

        var testClasses = ClassFiles(zipFile)
        var min = Long.MaxValue
        var max = Long.MinValue
        for (i ← 1 to 10)
            time(duration ⇒ { min = Ordering[Long].min(duration, min); max = Ordering[Long].max(duration, max) }) {
                testClasses foreach { dependencyExtractor.process(_) }
            }
        println("min time to extract dependencies: "+nanoSecondsToMilliseconds(min)+"ms")
        println("max time to extract dependencies: "+nanoSecondsToMilliseconds(max)+"ms")

        println()
    }
}
