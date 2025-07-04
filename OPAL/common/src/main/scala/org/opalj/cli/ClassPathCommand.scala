/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import java.io.File
import scala.collection.immutable.ArraySeq

import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger.error
import org.opalj.log.OPALLogger.info

import org.rogach.scallop.stringConverter

object ClassPathCommand extends ClassPathLikeCommand {
    override val name: String = "cp"
    override val argName: String = "classPath"
    override val description: String = "Directories or JAR/class files to process"
}

object ProjectDirectoryCommand extends ParsedCommand[String, String] {
    override val name: String = "projectDir"
    override val description: String = "Directory with project class files relative to --cp"

    override def parse(projectDir: String): String = {
        projectDir
    }
}

object LibraryClassPathCommand extends ClassPathLikeCommand {
    override val name: String = "libcp"
    override val argName: String = "libraryClassPath"
    override val description: String = "Directories or JAR/class files to process as libraries"
}

object LibraryDirectoryCommand extends PlainCommand[String] {
    override val name: String = "libDir"
    override val description: String = "Directory with library class files relative to --(lib)cp"
}

abstract class ClassPathLikeCommand extends ParsedCommand[String, Iterable[File]] {

    override val defaultValue: Option[String] = Some(System.getProperty("user.dir"))

    override def parse(arg: String): Iterable[File] = {
        var cp = IndexedSeq.empty[String]

        cp = arg.substring(arg.indexOf('=') + 1).split(File.pathSeparator).toIndexedSeq

        if (cp.isEmpty) cp = ArraySeq.unsafeWrapArray(Array(System.getProperty("user.dir")))

        info("project configuration", s"the $argName is ${cp.mkString}")(GlobalLogContext)
        verifyFiles(cp)
    }

    private def verifyFiles(filenames: IndexedSeq[String]): Seq[File] = filenames.flatMap(verifyFile)

    private def verifyFile(filename: String): Option[File] = {
        val file = new File(filename)

        def workingDirectory: String = {
            s"(working directory: ${System.getProperty("user.dir")})"
        }

        if (!file.exists) {
            showError(s"File does not exist: $file $workingDirectory.")
            None
        } else if (!file.canRead) {
            showError(s"Cannot read: $file $workingDirectory.")
            None
        } else if (!file.isDirectory &&
                   !filename.endsWith(".jar") &&
                   !filename.endsWith(".ear") &&
                   !filename.endsWith(".war") &&
                   !filename.endsWith(".zip") &&
                   !filename.endsWith(".jmod") &&
                   !filename.endsWith(".class")
        ) {
            showError(s"Input file is neither a directory nor a class or JAR/JMod file: $file.")
            None
        } else
            Some(file)
    }

    private def showError(message: String): Unit = error("project configuration", message)(GlobalLogContext)
}
