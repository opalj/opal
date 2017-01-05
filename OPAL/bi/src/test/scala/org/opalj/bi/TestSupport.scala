/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package bi

import java.io.File
import java.io.FileFilter

import scala.util.Properties.versionNumberString

/**
 * Common functionality required by many tests.
 *
 * @author Michael Eichberg
 */
object TestSupport {

    val ScalaMajorVersion = versionNumberString.split('.').take(2).mkString(".") // e.g. 2.10, 2.11

    val unmanagedResourcesFolder = "src/test/resources/"
    val managedResourcesFolder = s"target/scala-$ScalaMajorVersion/resource_managed/test/"

    private def pathPrefixCandidates(subProjectFolder: String) = Array[String ⇒ Option[String]](
        // if the current path is set to OPAL's root folder
        (resourceFile) ⇒ { Some("OPAL/"+resourceFile) },
        // if the current path is set to "<SUB-PROJECT>/<BIN>"
        (resourceFile) ⇒ { Some("../../"+resourceFile) },
        // if the current path is set to "DEVELOPING_OPAL/<SUB-PROJECT>/<BIN>"
        (resourceFile) ⇒ { Some("../../../OPAL/"+resourceFile) },
        // if we are in the sub-project's root folder
        (resourceFile) ⇒ { Some("../"+subProjectFolder + resourceFile) },
        // if we are in a "developing opal" sub-project's root folder
        (resourceFile) ⇒ { Some("../../OPAL/"+resourceFile) },
        // if the current path is set to "target/scala-.../classes"
        (resourceFile) ⇒ {
            val userDir = System.getProperty("user.dir")
            if ("""target/scala\-[\w\.]+/classes$""".r.findFirstIn(userDir).isDefined) {
                Some("../../../src/test/resources/"+resourceFile)
            } else {
                None
            }
        }
    )

    /**
     * This function tries to locate resources (at runtime) that are used by tests and
     * which are stored in the `SUBPROJECT-ROOT-FOLDER/src/test/resources` folder or
     * in the `resources_managed` folder.
     * I.e., when the test suite is executed, the current folder may be either Eclipse's
     * `bin` bolder or OPAL's root folder when we use sbt to build the project.
     *
     * @param   resourceName The name of the resource relative to the test/resources
     *          folder. The name must not begin with a "/".
     *
     * @param   subProjectFoler The root folder of the OPAL subproject; e.g., "ai".
     */
    def locateTestResources(resourceName: String, subProjectFolder: String): File = {
        val resourceFiles /*CANDIDATES*/ = Array(
            s"$subProjectFolder/$unmanagedResourcesFolder$resourceName",
            s"$subProjectFolder/$managedResourcesFolder/$resourceName"
        )

        pathPrefixCandidates(subProjectFolder) foreach { pathFunction ⇒
            resourceFiles foreach { rf ⇒
                pathFunction(rf) map { fCandidate ⇒
                    val f = new File(fCandidate)
                    if (f.exists) return f; // <======== NORMAL RETURN
                }
            }
        }

        throw new IllegalArgumentException("cannot locate resource: "+resourceName)
    }

    /**
     * Returns all JARs that are intended to be used by tests.
     */
    def allManagedBITestJARs(): Traversable[File] = {
        var allJARs: List[File] = Nil
        pathPrefixCandidates("bi") foreach { pathFunction ⇒
            pathFunction(s"bi/$managedResourcesFolder") map { fCandidate ⇒
                val f = new File(fCandidate)
                if (f.exists && f.isDirectory) {
                    val s = f.listFiles(new FileFilter {
                        def accept(path: File): Boolean = {
                            path.isFile && path.getName.endsWith(".jar") && path.canRead
                        }
                    })
                    allJARs ++= s
                }
            }
        }
        allJARs
    }

}
