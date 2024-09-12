/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ce

import com.typesafe.config.Config

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._

class FileLocator(var config: Config)  {
    def getProjectRoot: String = {
        val projectRoot = this.config.getString("user.dir")
        println("Searching in the following directory: " + projectRoot)
        projectRoot
    }

    def getConfigurationFilenames : mutable.Buffer[String] = {
        val projectNames = this.config.getStringList("org.opalj.ce.configurationFilenames").asScala

        println("Loaded the following Filenames: ")
        for (filename <- projectNames) {
            println(filename)
        }
        println()
        projectNames
    }

    def SearchFiles : mutable.Buffer[Path] = {
        val projectNames = this.getConfigurationFilenames
        val projectRoot = Paths.get(this.getProjectRoot)
        val foundFiles = ListBuffer[Path]()

        Files.walkFileTree(projectRoot, new java.nio.file.SimpleFileVisitor[Path]() {
            override def visitFile(file: Path, attrs: BasicFileAttributes): java.nio.file.FileVisitResult = {
                if (projectNames.contains(file.getFileName.toString) && (file.toAbsolutePath.toString.contains("target\\scala-2.13") == false) && (file.toAbsolutePath.toString.contains("target/scala-2.13") == false)) {
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