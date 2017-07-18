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
package br

import java.net.URL

import org.opalj.util.gc
import org.opalj.bytecode.RTJar
import org.opalj.br.reader.readJREClassFiles
import org.opalj.br.reader.readRTJarClassFiles
import org.opalj.br.reader.{ClassFileBinding ⇒ ClassFileReader}
import org.opalj.br.analyses.Project
import org.opalj.br.reader.Java9FrameworkWithLambdaExpressionsSupportAndCaching
import org.opalj.br.reader.Java9LibraryFramework
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.bi.TestResources.allBITestJARs

/**
 * Common helper and factory methods required by tests.
 *
 * @author Michael Eichberg
 */
object TestSupport {

    def createJREProject(): Project[URL] = Project(readJREClassFiles(), Traversable.empty, true)

    def createRTJarProject(): Project[URL] = Project(readRTJarClassFiles(), Traversable.empty, true)

    def biProjectWithFullJDK(projectJARName: String): Project[URL] = {
        val resources = locateTestResources(projectJARName, "bi")
        val projectClassFiles: Seq[(ClassFile, URL)] = DefaultJava9Reader.ClassFiles(resources)
        val jreClassFiles: Seq[(ClassFile, URL)] = readJREClassFiles()
        Project(projectClassFiles, jreClassFiles, false)
    }

    def biProject(projectJARName: String): Project[URL] = {
        Project(locateTestResources(projectJARName, "bi"))
    }

    def brProject(projectJARName: String): Project[URL] = {
        Project(locateTestResources(projectJARName, "br"))
    }

    final val DefaultJava9Reader: Java9FrameworkWithLambdaExpressionsSupportAndCaching = {
        new Java9FrameworkWithLambdaExpressionsSupportAndCaching(new BytecodeInstructionsCache)
    }

    /**
     * Iterator over all jars belonging to OPAL's test suite.
     *
     * @note     The projects are not immediately created to facilitate the integration with
     *           ScalaTest.
     * @example
     * {{{
     * allBIProjects() foreach { biProject =>
     *    // DO NOT CREATE THE PROJECT EAGERLY; DELAY IT UNTIL THE TEST'S BODY IS EXECUTED!
     *    val (name, createProject) = biProject
     *    test(s"computation of ... for all methods of $name") {
     *       val count = analyzeProject("JDK", createProject)
     *       info(s"computation of ... succeeded for $count ...")
     *    }
     * }
     * }}}
     */
    def allBIProjects(
        projectReader: ClassFileReader         = DefaultJava9Reader,
        jreReader:     Option[ClassFileReader] = Some(Java9LibraryFramework)
    ): Iterator[(String, () ⇒ Project[URL])] = {
        jreReader match {
            case Some(jreReader) ⇒
                val jreCFs = jreReader.ClassFiles(RTJar) // we share the loaded JRE!
                val jrePublicAPIOnly = jreReader.loadsInterfacesOnly
                allBITestJARs().toIterator map { biProjectJAR ⇒
                    val projectClassFiles = projectReader.ClassFiles(biProjectJAR)
                    val readerFactory = () ⇒ Project(projectClassFiles, jreCFs, jrePublicAPIOnly)
                    (biProjectJAR.getName, readerFactory)
                }
            case None ⇒
                allBITestJARs().toIterator map { biProjectJAR ⇒
                    val readerFactory = () ⇒ Project(biProjectJAR)
                    (biProjectJAR.getName, readerFactory)
                }
        }
    }

    /**
     * @note     Using this method in combination with Scalatest, where the test cases are generated
     *           inside the loop, may lead to the situation that the project's are not gc'ed before
     *           the entire test has completed!
     */
    def foreachBIProject(
        projectReader: ClassFileReader         = DefaultJava9Reader,
        jreReader:     Option[ClassFileReader] = Some(Java9LibraryFramework)
    )(
        f: (String, Project[URL]) ⇒ Unit
    ): Unit = {
        val it = allBIProjects(projectReader, jreReader)
        while (it.hasNext) {
            val (name, project) = it.next
            f(name, project())
            gc()
        }
    }

}
