/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._

import com.typesafe.config.Config

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

/**
 * File Locator aids locating the config Files that the configuration Explorer needs to parse.
 * @param config accepts the config of the Configuration Explorer.
 */
class FileLocator(var config: Config) {
    implicit val logContext: LogContext = GlobalLogContext

    /**
     * Small helper method for finding the project root.
     * @return returns the root directory of the opal project.
     */
    def getProjectRoot: String = {
        val subprojectDirectory = config.getString("user.dir")
        val projectRoot = Paths.get(subprojectDirectory).getParent.getParent.toString
        OPALLogger.info("Configuration Explorer", s"Searching in the following directory: $projectRoot")
        projectRoot
    }

    /**
     * Loads the filenames of the configuration files that shall be parsed.
     * @return is a List of the filenames that shall be parsed by the Configuration Explorer.
     */
    def getConfigurationFilenames: mutable.Buffer[String] = {
        val projectNames = config.getStringList("org.opalj.ce.configurationFilenames").asScala

        OPALLogger.info("Configuration Explorer", "Loaded the following Filenames: ")
        for (filename <- projectNames) {
            OPALLogger.info("Configuration Explorer", filename)
        }
        projectNames
    }

    /**
     * Finds all files that are named after one of the configuration filenames and are NOT within the target folder structure.
     * @return returns a List of full FilePaths to all found config files.
     */
    def getConfigurationPaths: mutable.Buffer[Path] = {
        val projectNames = getConfigurationFilenames
        searchFiles(projectNames)
    }

    /**
     * Finds all files that match the filename within the.
     * @param Filenames Accepts a List of all filenames that should be included in the result.
     * @return returns a List of full FilePaths to all found files.
     */
    def searchFiles(Filenames: mutable.Buffer[String]): mutable.Buffer[Path] = {
        val projectRoot = Paths.get(getProjectRoot)
        val foundFiles = ListBuffer[Path]()

        Files.walkFileTree(
            projectRoot,
            new java.nio.file.SimpleFileVisitor[Path]() {
                override def visitFile(file: Path, attrs: BasicFileAttributes): java.nio.file.FileVisitResult = {
                    if (Filenames.contains(file.getFileName.toString) && !file.toAbsolutePath.toString.contains(
                            "target\\scala"
                        ) && !file.toAbsolutePath.toString.contains("target/scala")
                    ) {
                        foundFiles += file
                        OPALLogger.info("Configuration Explorer", s"Found file: ${file.toString}")
                    }
                    java.nio.file.FileVisitResult.CONTINUE
                }
            }
        )
        foundFiles
    }

    /**
     * Finds all jar archives in the project, where the file name contains the pathWildcard.
     * @param pathWildcard accepts a String to filter the filenames of the jar archives. Will only return jar archives that contain the parameter in their file name.
     * @return Will only return jar archives that contain the parameter in their file name and that are not in the bg-jobs folder.
     */
    def FindJarArchives(pathWildcard: String): mutable.Buffer[File] = {
        val projectRoot = Paths.get(getProjectRoot)
        val foundFiles = ListBuffer[File]()
        Files.walkFileTree(
            projectRoot,
            new java.nio.file.SimpleFileVisitor[Path]() {
                override def visitFile(file: Path, attrs: BasicFileAttributes): java.nio.file.FileVisitResult = {
                    if (file.getFileName.toString.endsWith(".jar") && file.getFileName.toString.contains(
                            pathWildcard
                        ) && !file.toString.contains("bg-jobs")
                    ) {
                        foundFiles += file.toFile
                        OPALLogger.info("Configuration Explorer", s"Found file: ${file.toString}")
                    }
                    java.nio.file.FileVisitResult.CONTINUE
                }
            }
        )
        foundFiles
    }
}
