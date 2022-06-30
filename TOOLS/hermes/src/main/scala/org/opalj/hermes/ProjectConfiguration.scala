/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes

import java.io.File
import java.net.URL

import scala.collection.Map
import scala.collection.immutable

import org.opalj.br
import org.opalj.da

import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger.error
import org.opalj.log.OPALLogger.info
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.Project.JavaClassFileReader
import org.opalj.br.analyses.Project.JavaLibraryClassFileReader

/**
 * Meta-information about a project that belongs to a corpus.
 *
 * @note Represents one project of the configured using the config key: "org.opalj.hermes.projects".
 *
 * @author Michael Eichberg
 */
case class ProjectConfiguration(
        id:             String,
        cp:             String,
        libcp:          Option[String],
        libcp_defaults: Option[String]
) {

    @volatile private[this] var theProjectStatistics: immutable.Map[String, Double] = immutable.Map.empty

    /**
     * General statistics about a project.
     * See [[org.opalj.br.analyses.Project.statistics]] for further information.
     *
     * @note This information is only available after instantiate was called.
     */
    def statistics: Map[String, Double] = {
        theProjectStatistics
    }

    /**
     * @param  key The unique name of the statistic. If the name is not unique an exception will
     *         be thrown.
     */
    def addStatistic(key: String, value: Double) = {
        this.synchronized {
            if (theProjectStatistics.contains(key))
                throw new IllegalArgumentException(s"$id - $key is already set")
            else
                theProjectStatistics += ((key, value))
        }
    }

    /**
     * Instantiates the project and initializes the meta-information.
     *
     * For the classes belonging to the project the naive bytecode representation is
     * also returned to facilitate analyses w.r.t. the representativeness of the bytecode.
     */
    def instantiate: ProjectInstantiation = {

        // let's try to garbage collect previous projects
        new Thread(new Runnable { def run: Unit = { System.gc() } }).start

        info(
            "project setup",
            s"creating new project: $id\n\t\t"+
                s"cp=$cp\n\t\tlibcp=$libcp\n\t\tlibcp_defaults=$libcp_defaults"
        )(GlobalLogContext)

        val cpJARs = cp.split(File.pathSeparatorChar).flatMap { jar =>
            val jarFile = new File(jar)
            if (!jarFile.exists || !jarFile.canRead()) {
                error("project configuration", s"invalid class path: $jarFile")(GlobalLogContext)
                None
            } else {
                Some(jarFile)
            }
        }

        //
        // SETUP BR PROJECT
        //
        val noBRClassFiles = Iterable.empty[(br.ClassFile, URL)]
        val brProjectClassFiles = cpJARs.foldLeft(noBRClassFiles) { (classFiles, cpJAR) =>
            classFiles ++ JavaClassFileReader().ClassFiles(cpJAR)
        }
        val libcpJARs = {
            libcp match {
                case None =>
                    noBRClassFiles
                case Some(libs) =>
                    val libcpJARs = libs.split(File.pathSeparatorChar)
                    libcpJARs.foldLeft(noBRClassFiles) { (classFiles, libcpJAR) =>
                        val libcpJARFile = new File(libcpJAR)
                        if (!libcpJARFile.exists || !libcpJARFile.canRead()) {
                            error(
                                "project configuration", s"invalid library: $libcpJARFile"
                            )(GlobalLogContext)
                            classFiles
                        } else
                            classFiles ++ JavaLibraryClassFileReader.ClassFiles(libcpJARFile)
                    }
            }
        }
        val libraryClassFiles: Iterable[(br.ClassFile, URL)] = libcp_defaults match {
            case None => libcpJARs
            case Some(libraries) =>
                var predefinedLibrariesClassFiles = Iterable.empty[(br.ClassFile, URL)]
                var predefinedLibraries = libraries.split(File.pathSeparatorChar)
                while (predefinedLibraries.nonEmpty) {
                    predefinedLibraries.head match {
                        case "RTJar" =>
                            predefinedLibrariesClassFiles ++=
                                br.reader.readRTJarClassFiles()(reader = JavaLibraryClassFileReader)
                        case "JRE" =>
                            predefinedLibrariesClassFiles ++=
                                br.reader.readJREClassFiles()(reader = JavaLibraryClassFileReader)
                        case unmatched =>
                            error(
                                "project configuration", s"unknown library: $unmatched"
                            )(GlobalLogContext)

                    }
                    predefinedLibraries = predefinedLibraries.tail
                }
                predefinedLibrariesClassFiles ++ libcpJARs
        }
        val brProject = Project(brProjectClassFiles, libraryClassFiles, true)
        this.synchronized {
            theProjectStatistics ++= brProject.statistics.map { kv => val (k, v) = kv; (k, v.toDouble) }
        }

        //
        // SETUP DA CLASS FILE
        //
        val noDAClassFiles = Iterable.empty[(da.ClassFile, URL)]
        val daProjectClassFiles = cpJARs.foldLeft(noDAClassFiles) { (classFiles, cpJAR) =>
            classFiles ++ da.ClassFileReader.ClassFiles(cpJAR)
        }

        ProjectInstantiation(brProject, daProjectClassFiles)
    }

}
