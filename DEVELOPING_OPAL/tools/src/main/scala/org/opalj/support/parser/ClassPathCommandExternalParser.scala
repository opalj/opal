package org.opalj.support.parser

import org.opalj.Commandline_base.commandlines.OpalCommandExternalParser
import org.opalj.log.OPALLogger.{error, info}
import org.opalj.ba.CODE.logContext

import java.io.File

/**
 * `ClassPathCommandExternalParser` parses and validates a class path string.
 * It processes the class path provided as a command-line argument, splitting it by the system path
 * separator (":" on UNIX, ";" on Windows), and verifies each entry, returning a sequence of valid
 * `File` objects.
 */
object ClassPathCommandExternalParser extends OpalCommandExternalParser[Seq[File]]{
    override def parse[T](arg: T): Seq[File] = {
        val classPath = arg.asInstanceOf[String]
        var cp = IndexedSeq.empty[String]
        import scala.collection.immutable.ArraySeq

        cp = ArraySeq.unsafeWrapArray(
            classPath.substring(classPath.indexOf('=') + 1).split(File.pathSeparator)
        )

        if (cp.isEmpty) cp = ArraySeq.unsafeWrapArray(Array(System.getProperty("user.dir")))

        info("project configuration", s"the classpath is ${cp.mkString}")
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

    private def showError(message: String): Unit = error("project configuration", message)
}
