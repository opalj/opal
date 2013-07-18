/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st.bat
package dependency

import resolved._
import resolved.reader.Java7Framework

import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import org.scalatest.Suite
import org.scalatest.Reporter
import org.scalatest.Stopper
import org.scalatest.Tracker
import org.scalatest.events.TestStarting
import org.scalatest.events.TestSucceeded
import org.scalatest.events.TestFailed

/**
 * Tests whether all class files contained in the "test/classfiles" directory
 * can be processed by the <code>DependencyExtractor</code> without failure.
 * The results themselves will not be verified in these test cases.
 *
 * @author Thomas Schlosser
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ExtractDependenciesFromClassFilesTest extends Suite {

    /*
    * Registry of all class files stored in the zip files found in the test data directory.
    */
    private val testCases = {

        var tcs = collection.immutable.Map[String, (ZipFile, ZipEntry)]()

        var files = TestSupport.locateTestResources("classfiles", "ext/dependencies").listFiles()

        for {
            file ← files
            if (file.isFile && file.canRead && file.getName.endsWith(".jar"))
        } {
            val zipfile = new ZipFile(file)
            val zipentries = (zipfile).entries
            while (zipentries.hasMoreElements) {
                val zipentry = zipentries.nextElement
                if (!zipentry.isDirectory && zipentry.getName.endsWith(".class")) {
                    val testCase = ("Extract dependencies of class file: "+zipfile.getName+" - "+zipentry.getName -> (zipfile, zipentry))
                    tcs = tcs + testCase
                }
            }
        }

        tcs
    }

    override lazy val testNames: Set[String] = (Set[String]() /: (testCases.keys))(_ + _)

    override def tags: Map[String, Set[String]] = Map()

    override def runTest(testName: String,
                         reporter: Reporter,
                         stopper: Stopper,
                         configMap: Map[String, Any],
                         tracker: Tracker) {

        val ordinal = tracker.nextOrdinal
        reporter(TestStarting(ordinal, "BasicDependencyExtractorTests", None, testName))
        try {
            val dependencyExtractor = new DependencyExtractor(new SourceElementIDsMap {}) with NoSourceElementsVisitor {
                def processDependency(src: Int, trgt: Int, dType: DependencyType) {
                    /* DO NOTHING */
                }
            }

            val (file, entry) = testCases(testName)

            var classFile = Java7Framework.ClassFile(() ⇒ file.getInputStream(entry))

            dependencyExtractor.process(classFile)

            reporter(TestSucceeded(ordinal, "BasicDependencyExtractorTests", None, testName))
        } catch {
            case t: Throwable ⇒ reporter(TestFailed(ordinal, "Failure", "BasicDependencyExtractorTests", None, testName, Some(t)))
        }
    }
}