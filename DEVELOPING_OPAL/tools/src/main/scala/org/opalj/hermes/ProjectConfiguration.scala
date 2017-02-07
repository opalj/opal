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

import org.opalj.br
import org.opalj.da
import org.opalj.br.analyses.Project

/**
 * Meta-information about a project that belongs to a corpus.
 *
 * @note Represents the corresponding information in the corpus specific configuration file.
 *
 * @author Michael Eichberg
 */
case class ProjectConfiguration(id: String, cp: String, libcp: Option[String]) {

    /**
     * Instantiates the project.
     *
     * For the classes belonging to the project the naive bytecode representation is
     * also returned to facilitate analyses w.r.t. the representativeness of the bytecode.
     */
    def instantiate: ProjectInstantiation = {
        // we will do our best to garbage collect previous projects
        org.opalj.util.gc()

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
            classFiles ++ Project.JavaClassFileReader().ClassFiles(new File(jarFile))
        }
        val libraryClassFiles = {
            libcp match {
                case None ⇒
                    noBRClassFiles
                case Some(cp) ⇒
                    val libraryJARs = cp.split(File.pathSeparatorChar)
                    libraryJARs.foldLeft(noBRClassFiles) { (classFiles, jar) ⇒
                        classFiles ++ Project.JavaLibraryClassFileReader.ClassFiles(new File(jar))
                    }
            }
        }
        val brProject = Project(brProjectClassFiles, libraryClassFiles, true)

        ProjectInstantiation(brProject, daProjectClassFiles)
    }

}
