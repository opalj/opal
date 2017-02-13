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
package hermes

import java.io.File
import java.net.URL

import scala.collection.Map

import org.opalj.log.OPALLogger.error
import org.opalj.log.OPALLogger.info
import org.opalj.log.GlobalLogContext
import org.opalj.br
import org.opalj.da
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.Project.JavaLibraryClassFileReader
import org.opalj.br.analyses.Project.JavaClassFileReader

/**
 * Meta-information about a project that belongs to a corpus.
 *
 * @note Represents the corresponding information in the corpus specific configuration file.
 *
 * @author Michael Eichberg
 */
case class ProjectConfiguration(
        id:             String,
        cp:             String,
        libcp:          Option[String],
        libcp_defaults: Option[String]
) {

    private[this] var theProjectStatistics: Map[String, Int] = Map.empty

    /**
     * General statistics about a project. See
     * [[org.opalj.br.analyses.Project.statistics]] for further information.
     */
    def statistics: Map[String, Int] = {
        theProjectStatistics
    }

    /**
     * Instantiates the project and initializes the meta-information.
     *
     * For the classes belonging to the project the naive bytecode representation is
     * also returned to facilitate analyses w.r.t. the representativeness of the bytecode.
     */
    def instantiate: ProjectInstantiation = {
        // we will do our best to garbage collect previous projects
        org.opalj.util.gc()

        info(
            "project setup",
            s"creating new project: $id\n\t\t"+
                s"cp=$cp\n\t\tlibcp=$libcp\n\t\tlibcp_defaults=$libcp_defaults"
        )(GlobalLogContext)

        val noBRClassFiles = Traversable.empty[(br.ClassFile, URL)]
        val noDAClassFiles = Traversable.empty[(da.ClassFile, URL)]

        val projectJARs = cp.split(File.pathSeparatorChar)

        val daProjectClassFiles = projectJARs.foldLeft(noDAClassFiles) { (classFiles, jarFile) ⇒
            val theFile = new File(jarFile)
            if (!theFile.exists || !theFile.canRead())
                Console.err.println(s"invalid class path: $theFile")
            classFiles ++ da.ClassFileReader.ClassFiles(theFile)
        }
        val brProjectClassFiles = projectJARs.foldLeft(noBRClassFiles) { (classFiles, jarFile) ⇒
            classFiles ++ JavaClassFileReader().ClassFiles(new File(jarFile))
        }
        val explicitlySpecifiedLibraryClassFiles = {
            libcp match {
                case None ⇒
                    noBRClassFiles
                case Some(cp) ⇒
                    val libraryJARs = cp.split(File.pathSeparatorChar)
                    libraryJARs.foldLeft(noBRClassFiles) { (classFiles, jar) ⇒
                        classFiles ++ JavaLibraryClassFileReader.ClassFiles(new File(jar))
                    }
            }
        }
        val libraryClassFiles: Traversable[(br.ClassFile, URL)] = libcp_defaults match {
            case None ⇒ explicitlySpecifiedLibraryClassFiles
            case Some(libraries) ⇒
                var predefinedLibrariesClassFiles = Traversable.empty[(br.ClassFile, URL)]
                var predefinedLibraries = libraries.split(File.pathSeparatorChar)
                while (predefinedLibraries.nonEmpty) {
                    predefinedLibraries.head match {
                        case "RTJar" ⇒
                            predefinedLibrariesClassFiles ++=
                                br.reader.readRTJarClassFiles()(reader = JavaLibraryClassFileReader)
                        case "JRE" ⇒
                            predefinedLibrariesClassFiles ++=
                                br.reader.readJREClassFiles()(reader = JavaLibraryClassFileReader)
                        case unmatched ⇒
                            error(
                                "project configuration", s"unknown library: $unmatched"
                            )(GlobalLogContext)

                    }
                    predefinedLibraries = predefinedLibraries.tail
                }
                predefinedLibrariesClassFiles ++ explicitlySpecifiedLibraryClassFiles
        }

        val brProject = Project(brProjectClassFiles, libraryClassFiles, true)
        theProjectStatistics = brProject.statistics

        ProjectInstantiation(brProject, daProjectClassFiles)
    }

}
