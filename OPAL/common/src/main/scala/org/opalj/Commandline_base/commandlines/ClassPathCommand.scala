package org.opalj.Commandline_base.commandlines

import org.opalj.ba.CODE.logContext
import org.opalj.log.OPALLogger.error

import java.io.File
import scala.collection.immutable.ArraySeq

object ClassPathCommand extends OpalPlainCommand[String]{
    override var name: String = "classPath"
    override var argName: String = "classPath"
    override var description: String = "Directories or JAR/class files"
    override var defaultValue: Option[String] = Some(System.getProperty("user.dir"))
    override var noshort: Boolean = true

    def parse(classPath: IndexedSeq[String]): IndexedSeq[File] = {
        val effectiveClassPath = if (classPath.isEmpty) {
            ArraySeq.unsafeWrapArray(Array(System.getProperty("user.dir")))
        } else {
            classPath
        }

        val cpFiles = verifyFiles(effectiveClassPath)
        if (cpFiles.isEmpty) {
            showError("Nothing to analyze.")
            sys.exit(1)
        }

        cpFiles
    }

    private def verifyFiles(filenames: IndexedSeq[String]): IndexedSeq[File] = filenames.flatMap(verifyFile)

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

    private def showError(message: String): Unit = error("project configuration", message)
}
