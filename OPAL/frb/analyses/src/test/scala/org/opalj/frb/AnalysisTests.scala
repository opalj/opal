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
package org.opalj
package frb

import br._
import br.reader._
import br.analyses._
import org.scalatest._
import java.io.File
import java.net.URL

import org.opalj.bi.TestSupport.locateTestResources

/**
 * Superclass for all analysis unit-tests.
 *
 * @author Florian Brandherm
 * @author Daniel Klauer
 */
trait AnalysisTest extends FlatSpec with Matchers with ParallelTestExecution {

    protected def println(m: String) { info(m) }

    def createProject(jarFileName: String): Project[URL] = {
        AnalysisTest.createProject(Seq(jarFileName), println)
    }

    def createProject(jarFileNames: Seq[String]): Project[URL] = {
        AnalysisTest.createProject(jarFileNames, println)
    }
}

/**
 * Helper functions used by various tests.
 *
 * @author Florian Brandherm
 * @author Daniel Klauer
 */
object AnalysisTest {

    /**
     * Builds a project from one or more .jar files in src/test/resources/.
     *
     * @param filenames The files names, containing the path relative to
     * ext/findrealbugs/src/test/resources/.
     * @param useJDK Whether the JDK classes should be added to the project, if available.
     * @return A `Project` representing the class files from the provided .jar file.
     */
    def createProject(filenames: Seq[String], println: String ⇒ Unit): Project[URL] = {

        val classFiles = filenames.map(filename ⇒
            Java8Framework.ClassFiles(
                locateTestResources("classfiles/analyses/"+filename, "frb/analyses")
            )
        ).flatten

        println("creating project: "+filenames.mkString(", "))
        Project(classFiles)
    }
}
