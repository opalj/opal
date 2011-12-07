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
package de.tud.cs.st.bat

import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.Enumeration

import de.tud.cs.st.bat.canonical.reader.BasicJava6Framework
import de.tud.cs.st.bat.resolved.reader.Java6Framework

import org.scalatest.Suite
import org.scalatest.Reporter
import org.scalatest.Stopper
import org.scalatest.Tracker
import org.scalatest.Filter
import org.scalatest.Distributor
import org.scalatest.events._


/**
 * This test(suite) just loads a very large number of class files to make sure the library
 * can handle them and to test the "corner" cases. Basically, we test for NPEs and
 * ArrayIndexOutOfBoundExceptions.
 *
 * @author Michael Eichberg
 */
class RegressionSuite extends Suite with de.tud.cs.st.util.perf.PerformanceEvaluation {


	/*
	 * Registry of all class files stored in the zip files found in the test data directory.
	 */
	private val testCases = {

		var tcs = scala.collection.immutable.Map[String,(ZipFile,ZipEntry)]()

		// The location of the "test/data" directory depends on the current directory used for
		// running this test suite... i.e. whether the current directory is the directory where
		// this class / this source file is stored or BAT's root directory.
		var files = new File("../../../../../../test/classfiles").listFiles()
		if (files == null) files = new File("test/classfiles").listFiles()

		for {
			file <- files
			if (file.isFile && file.canRead && file.getName.endsWith(".zip"))
		} {
			val zipfile = new ZipFile(file)
			val zipentries = (zipfile).entries
			while (zipentries.hasMoreElements) {
				val zipentry = zipentries.nextElement
				if (!zipentry.isDirectory && zipentry.getName.endsWith(".class")) {
					val testCase = ("Read class file: "+zipfile.getName+" - "+zipentry.getName -> (zipfile,zipentry))
					tcs = tcs + testCase
				}
			}
		}

		tcs
	}

	override lazy val testNames : Set[String] = {
		val r = (Set[String]() /: (testCases.keys)) ( _ + _ )
		r
	}

	override def tags: Map[String, Set[String]] = Map()


	override def runTest(
		testName : String,
		reporter: Reporter,
		stopper: Stopper,
		configMap: Map[String, Any],
		tracker : Tracker
	) {

		val ordinal = tracker.nextOrdinal
		reporter(TestStarting(ordinal,"BATTests",None,testName))
		try {

			// 1. Test ... just read in the class file and use a basic representation that represents everything as is
			val (file,entry) = testCases(testName)
			/*time( duration => {
					reporter.infoProvided(
						new Report(
							testName,
							("BasicJava6Framework - time for reading the classfile: "+nanoSecondsToMilliseconds(duration)))
					)
				}
			){*/
				BasicJava6Framework.ClassFile(() => file.getInputStream(entry) )
			//}

			// 2.1. Test ... read in the class file and resolve the constant pool
			var classFile : de.tud.cs.st.bat.resolved.ClassFile = null
			/*time( duration => {
					reporter.infoProvided(
						new Report(
							testName,
							("Java6Framework - time for reading the classfile: "+nanoSecondsToMilliseconds(duration)))
					)
				}
			){*/
				classFile = Java6Framework.ClassFile(() => file.getInputStream(entry) )
			//}
			// 2.2. Test ... if we can create the XML representation for the class file without generating errors
			classFile.toXML

			reporter(TestSucceeded(ordinal,"BATTests",None,testName))
		} catch {
			case t: Throwable => reporter(TestFailed(ordinal,"Failure","BATTests",None,testName,Some(t)))
		}
	}

}
