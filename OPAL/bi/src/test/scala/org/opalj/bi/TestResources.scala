/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi

import java.io.File
import org.opalj.io.JARsFileFilter
import org.opalj.util.ScalaMajorVersion

import scala.collection.immutable.ArraySeq

/**
 * Common functionality to find resources required by many tests.
 *
 * @author Michael Eichberg
 */
object TestResources {

    final val unmanagedResourcesFolder = "src/test/resources/"
    final val managedResourcesFolder = s"target/scala-$ScalaMajorVersion/resource_managed/test/"

    private def pathPrefixCandidates(
        subProjectFolder: String
    ): ArraySeq[String => Option[String]] = ArraySeq(
        // if the current path is set to OPAL's root folder
        resourceFile => { Some("OPAL/"+resourceFile) },
        // if the current path is set to "<SUB-PROJECT>/<BIN>"
        resourceFile => { Some("../../"+resourceFile) },
        // if the current path is set to "DEVELOPING_OPAL/<SUB-PROJECT>/<BIN>"
        resourceFile => { Some("../../../OPAL/"+resourceFile) },
        // if we are in the sub-project's root folder
        resourceFile => { Some("../"+subProjectFolder + resourceFile) },
        // if we are in a "developing opal" sub-project's root folder
        resourceFile => { Some("../../OPAL/"+resourceFile) },
        // if the current path is set to "target/scala-.../classes"
        resourceFile => {
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
     * @param   subProjectFolder The root folder of the OPAL subproject; e.g., "ai".
     */
    def locateTestResources(resourceName: String, subProjectFolder: String): File = {
        val resourceFiles /*CANDIDATES*/ = Array(
            s"$subProjectFolder/$unmanagedResourcesFolder$resourceName",
            s"$subProjectFolder/$managedResourcesFolder/$resourceName"
        )
        pathPrefixCandidates(subProjectFolder) foreach { pathFunction =>
            resourceFiles foreach { rf =>
                pathFunction(rf) foreach { fCandidate =>
                    val f = new File(fCandidate)
                    if (f.exists) return f; // <======== NORMAL RETURN
                }
            }
        }

        throw new IllegalArgumentException("cannot locate resource: "+resourceName)
    }

    /**
     * Returns all JARs that are intended to be used by tests and which were compiled
     * using the test fixtures.
     */
    def allManagedBITestJARs(): Seq[File] = {
        for {
            pathFunction <- pathPrefixCandidates("bi")
            fCandidate = pathFunction(s"bi/$managedResourcesFolder")
            if fCandidate.isDefined
            f = new File(fCandidate.get)
            if f.exists
            if f.canRead
            if f.isDirectory
            jarFile <- f.listFiles(JARsFileFilter)
        } yield {
            jarFile
        }
    }

    def allUnmanagedBITestJARs(): Seq[File] = {
        var allJARs: List[File] = Nil
        val f = locateTestResources("classfiles", "bi")
        if (f.exists && f.isDirectory && f.canRead) {
            allJARs ++= f.listFiles(JARsFileFilter)
        }
        allJARs
    }

    /**
     * Returns all folders in the `classfiles` folder.
     * @return
     */
    def allBITestProjectFolders(): Seq[File] = {
        val f = locateTestResources("classfiles", "bi")
        if (!f.exists || !f.isDirectory)
            return Nil;

        for {
            file <- ArraySeq.unsafeWrapArray(f.listFiles())
            if file.isDirectory
            if file.canRead
        } yield {
            file
        }
    }

    /**
     * Returns all JARs created based on the set of test fixtures and the explicitly selected JARs.
     *
     * @note This set never includes the JRE.
     */
    def allBITestJARs(): Seq[File] = allManagedBITestJARs() ++ allUnmanagedBITestJARs()

}
