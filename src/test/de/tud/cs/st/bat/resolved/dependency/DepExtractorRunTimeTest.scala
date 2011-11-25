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
import org.scalatest.FunSuite
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import de.tud.cs.st.bat.resolved.reader.Java6Framework
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.junit.Test
import java.io.FileWriter
import de.tud.cs.st.bat.resolved.DependencyType._

/**
 * @author Thomas Schlosser
 *
 */
@RunWith(classOf[JUnitRunner])
class DepExtractorRunTimeTest extends FunSuite with de.tud.cs.st.util.perf.BasicPerformanceEvaluation {

  test("testDepExtraction - Apache ANT 1.7.1 - javac 6 default target.zip") {
    testDepExtraction("test/classfiles/Apache ANT 1.7.1 - javac 6 default target.zip")
  }

  test("testDepExtraction - Apache ANT 1.7.1 - target 1.2.zip") {
    testDepExtraction("test/classfiles/Apache ANT 1.7.1 - target 1.2.zip")
  }

  test("testDepExtraction - Apache ANT 1.7.1 - target 1.4 (no debug).zip") {
    testDepExtraction("test/classfiles/Apache ANT 1.7.1 - target 1.4 (no debug).zip")
  }

  test("testDepExtraction - Apache ANT 1.7.1 - target 1.5.zip") {
    testDepExtraction("test/classfiles/Apache ANT 1.7.1 - target 1.5.zip")
  }

  test("testDepExtraction - BAT2XML - target 1.7.zip") {
    testDepExtraction("test/classfiles/BAT2XML - target 1.7.zip")
  }

  test("testDepExtraction - Columbus 2008_10_16 - target 1.5.zip") {
    testDepExtraction("test/classfiles/Columbus 2008_10_16 - target 1.5.zip")
  }

  test("testDepExtraction - Columbus 2008_10_16 - target 1.6.zip") {
    testDepExtraction("test/classfiles/Columbus 2008_10_16 - target 1.6.zip")
  }

  test("testDepExtraction - Flashcards 0.4 - target 1.6.zip") {
    testDepExtraction("test/classfiles/Flashcards 0.4 - target 1.6.zip")
  }

  test("testDepExtraction - hibernate-core-3.6.0.Final.jar") {
    testDepExtraction("test/classfiles/hibernate-core-3.6.0.Final.jar")
  }

  test("testDepExtraction - Multithreaded RPN Calculator 2008_10_17 - Java 6 all debug info.zip") {
    testDepExtraction("test/classfiles/Multithreaded RPN Calculator 2008_10_17 - Java 6 all debug info.zip")
  }

  test("testDepExtraction - Opal 0.3.zip") {
    testDepExtraction("test/classfiles/Opal 0.3.zip")
  }

  test("testDepExtraction - Signatures.zip") {
    testDepExtraction("test/classfiles/Signatures.zip")
  }

  private def testDepExtraction(zipFile: String) {
    println("testDepExtraction[" + zipFile + "]")

    val clusterBuilder = new BasicDepBuilder
    val depExtractor = new DepExtractor(clusterBuilder)

    var testClasses = getTestClasses(zipFile)
    var min = Long.MaxValue
    var max = Long.MinValue
    for (i <- 1 to 10)
      time(duration => { min = Math.min(duration, min); max = Math.max(duration, max) }) {
        for (classFile <- testClasses) {
          depExtractor.process(classFile)
        }
      }
    println("min time to extract dependencies: " + nanoSecondsToMilliseconds(min) + "ms")
    println("max time to extract dependencies: " + nanoSecondsToMilliseconds(max) + "ms")

    println()
  }

  private def getTestClasses(zipFile: String): Array[ClassFile] = {
    var tcls = Array.empty[ClassFile]
    val zipfile = new ZipFile(new File(zipFile))
    val zipentries = (zipfile).entries
    while (zipentries.hasMoreElements) {
      val zipentry = zipentries.nextElement
      if (!zipentry.isDirectory && zipentry.getName.endsWith(".class")) {
        val testClass = (Java6Framework.ClassFile(() => zipfile.getInputStream(zipentry)))
        tcls :+= testClass
      }
    }
    tcls
  }
}

class BasicDepBuilder extends DepBuilder {
  var cnt = 0

  def getID(identifier: String): Int = {
    cnt += 1
    cnt
  }

  def getID(identifier: String, clazz: ClassFile): Int =
    getID(identifier)

  def getID(identifier: String, field: Field_Info): Int =
    getID(identifier)

  def getID(identifier: String, method: Method_Info): Int =
    getID(identifier)

  def addDep(src: Int, trgt: Int, dType: DependencyType) = {
    //The next line was uncommented to speed up the test runs
    //println("addDep: " + src + "--[" + dType + "]-->" + trgt)
  }
}