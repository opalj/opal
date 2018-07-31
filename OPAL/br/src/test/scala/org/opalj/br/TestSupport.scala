/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.net.URL

import org.opalj.util.gc
import org.opalj.bytecode.RTJar
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.br.reader.readJREClassFiles
import org.opalj.br.reader.readRTJarClassFiles
import org.opalj.br.reader.{ClassFileBinding ⇒ ClassFileReader}
import org.opalj.br.analyses.Project
import org.opalj.br.reader.Java9FrameworkWithLambdaExpressionsSupportAndCaching
import org.opalj.br.reader.Java9LibraryFramework
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.bi.TestResources.allBITestProjectFolders
import org.opalj.bi.TestResources.allBITestJARs
import org.opalj.bi.TestResources.allManagedBITestJARs

/**
 * Common helper and factory methods required by tests.
 *
 * @author Michael Eichberg
 */
object TestSupport {

    final val DefaultJava9Reader: Java9FrameworkWithLambdaExpressionsSupportAndCaching = {
        new Java9FrameworkWithLambdaExpressionsSupportAndCaching(new BytecodeInstructionsCache)
    }

    def createJREProject(): Project[URL] = Project(readJREClassFiles(), Traversable.empty, true)

    def createRTJarProject(): Project[URL] = Project(readRTJarClassFiles(), Traversable.empty, true)

    def biProjectWithJDK(projectJARName: String, jdkAPIOnly: Boolean = false): Project[URL] = {
        val resources = locateTestResources(projectJARName, "bi")
        val projectClassFiles: Seq[(ClassFile, URL)] = DefaultJava9Reader.ClassFiles(resources)
        val jreClassFiles: Seq[(ClassFile, URL)] =
            if (jdkAPIOnly)
                Java9LibraryFramework.ClassFiles(JRELibraryFolder)
            else
                readJREClassFiles()
        Project(projectClassFiles, jreClassFiles, libraryClassFilesAreInterfacesOnly = jdkAPIOnly)
    }

    def biProject(projectJARName: String): Project[URL] = {
        Project(locateTestResources(projectJARName, "bi"))
    }

    def brProject(projectJARName: String): Project[URL] = {
        Project(locateTestResources(projectJARName, "br"))
    }

    /**
     * Iterator over all jars (explicitly added and those from the test fixtures)
     * belonging to OPAL's test suite.
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
                (allBITestJARs().toIterator ++ allBITestProjectFolders().toIterator) map { biProject ⇒
                    val projectClassFiles = projectReader.ClassFiles(biProject)
                    val readerFactory = () ⇒ Project(projectClassFiles, jreCFs, jrePublicAPIOnly)
                    (biProject.getName, readerFactory)
                }
            case None ⇒
                (allBITestJARs().toIterator ++ allBITestProjectFolders().toIterator) map { biProjectJAR ⇒
                    val readerFactory = () ⇒ Project(biProjectJAR)
                    (biProjectJAR.getName, readerFactory)
                }
        }
    }

    def allManagedBITestProjects(
        projectReader: ClassFileReader = DefaultJava9Reader,
        jreReader:     ClassFileReader = Java9LibraryFramework
    ): Iterator[(String, () ⇒ Project[URL])] = {
        val jreCFs = jreReader.ClassFiles(RTJar) // we share the loaded JRE!
        val jrePublicAPIOnly = jreReader.loadsInterfacesOnly
        allManagedBITestJARs().toIterator map { biProject ⇒
            val projectClassFiles = projectReader.ClassFiles(biProject)
            val readerFactory = () ⇒ Project(projectClassFiles, jreCFs, jrePublicAPIOnly)
            (biProject.getName, readerFactory)
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
