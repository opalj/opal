/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ce

import com.typesafe.config.Config
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path, Paths}
import scala.collection.mutable
import scala.jdk.CollectionConverters._

class FileLocator(var config: Config)  {
    println("FileLocator created. Initializing...")
    def LocateConfigurationFiles(): Unit = {

    }

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
      projectNames
    }

    def SearchFiles() : Unit = {
      val projectNames = this.getConfigurationFilenames
      val projectRoot = Paths.get(this.getProjectRoot)

      Files.walkFileTree(projectRoot, new java.nio.file.SimpleFileVisitor[Path]() {
        override def visitFile(file: Path, attrs: BasicFileAttributes): java.nio.file.FileVisitResult = {
          if (projectNames.contains(file.getFileName.toString)) {
            println(s"Found file: ${file.toString}")
          }
          java.nio.file.FileVisitResult.CONTINUE
        }
      })
    }
}