/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import com.typesafe.config.Config

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._

/**
 * File Locator aids locating the config Files that the configuration Explorer needs to parse
 * @param config accepts the config of the Configuration Explorer
 */
class FileLocator(var config: Config)  {

    /**
     * Small helper method for finding the project root
     * @return returns the root directory of the opal project
     */
    def getProjectRoot: String = {
        val projectRoot = this.config.getString("user.dir")
        println("Searching in the following directory: " + projectRoot)
        projectRoot
    }

    /**
     * Loads the filenames of the configuration files that shall be parsed
     * @return is a List of the filenames that shall be parsed by the Configuration Explorer
     */
    def getConfigurationFilenames : mutable.Buffer[String] = {
        val projectNames = this.config.getStringList("org.opalj.ce.configurationFilenames").asScala

        println("Loaded the following Filenames: ")
        for (filename <- projectNames) {
            println(filename)
        }
        println()
        projectNames
    }

    /**
     * Finds all files that are named after one of the configuration filenames and are NOT within the target folder structure
     * @return returns a List of full FilePaths to all found config files
     */
    def getConfigurationPaths : mutable.Buffer[Path] = {
        val projectNames = this.getConfigurationFilenames
        this.SearchFiles(projectNames)
    }

    /**
     * Finds all files that match the filename within the
     * @return returns a List of full FilePaths to all found files
     */
    def SearchFiles(Filenames : mutable.Buffer[String]) : mutable.Buffer[Path] = {
        val projectRoot = Paths.get(this.getProjectRoot)
        val foundFiles = ListBuffer[Path]()

        Files.walkFileTree(projectRoot, new java.nio.file.SimpleFileVisitor[Path]() {
            override def visitFile(file: Path, attrs: BasicFileAttributes): java.nio.file.FileVisitResult = {
                if (Filenames.contains(file.getFileName.toString) && !file.toAbsolutePath.toString.contains("target\\scala-2.13") && !file.toAbsolutePath.toString.contains("target/scala-2.13")) {
                    foundFiles += file
                    println(s"Found file: ${file.toString}")
                }
            java.nio.file.FileVisitResult.CONTINUE
            }
        })
    println()
    foundFiles
    }

    def FindJarArchives() : mutable.Buffer[Path] = {
        val projectRoot = Paths.get(this.getProjectRoot)
        val foundFiles = ListBuffer[Path]()
        Files.walkFileTree(projectRoot, new java.nio.file.SimpleFileVisitor[Path]() {
            override def visitFile(file: Path, attrs: BasicFileAttributes): java.nio.file.FileVisitResult = {
                if (file.getFileName.toString.trim.endsWith(".jar") && file.getFileName.toString.contains("SNAPSHOT")) {
                    foundFiles += file
                    println(s"Found file: ${file.toString}")
                }
                java.nio.file.FileVisitResult.CONTINUE
            }
        })
        println()
        foundFiles
    }
}