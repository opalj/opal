/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import scala.util.control.ControlThrowable
import java.io.File
import java.net.URL
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.GlobalLogContext
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.bytecode.RTJar

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

/**
 * Defines convenience methods related to reading in class files.
 *
 * @author Michael Eichberg
 */
package object reader {

    final val ConfigKeyPrefix = org.opalj.br.ConfigKeyPrefix+"reader."

    type Source = AnyRef

    /**
     * Reads in all class files found in the jar files or jar and class files in the
     * folders specified by `args`. The class files are read in using the specified
     * class file reader. This enables, e.g., to use this method to only read in
     * the public interface of a class file or to read in complete class files.
     *
     * @param args An `Iterable` of file and folder names that refer to jar files
     *      or folders in which jar and class files are found.
     * @param classFilesReader A function that – given a file (jar, folder, class file) – 
     *      loads the respective class files and returns an `Iterable`. The second
     *      parameter of the function is a function that should be called back by the
     *      reader whenever the processing of given file fails with an exception.
     *      This design was chosen to enable a reader of jar file to continue processing
     *      class files even if the processing of a class file failed.
     */
    def read(
        args:             Iterable[String],
        classFilesReader: (File, (Source, Throwable) => Unit) => Iterable[(ClassFile, URL)]
    ): (Iterable[(ClassFile, URL)], List[Throwable]) = {
        readClassFiles(args.view.map(new File(_)), classFilesReader)
    }

    def readClassFiles(
        files:            Iterable[File],
        classFilesReader: (File, (Source, Throwable) => Unit) => Iterable[(ClassFile, URL)],
        perFile:          File => Unit                                                      = (f: File) => { /*do nothing*/ }
    ): (Iterable[(ClassFile, URL)], List[Throwable]) = {
        val exceptionsMutex = new Object
        var exceptions: List[Throwable] = Nil
        def handleException(source: Source, t: Throwable): Unit = {
            val e = new RuntimeException(s"exception while processing: $source", t)
            exceptionsMutex.synchronized { exceptions ::= e }
        }

        val allClassFiles = for (file <- files.par) yield {
            try {
                perFile(file)
                classFilesReader(file, handleException)
            } catch {
                case ct: ControlThrowable => throw ct
                case t: Throwable         => handleException(file, t); Iterable.empty
            }
        }
        (allClassFiles.flatten.seq, exceptions)
    }

    /**
     * Loads class files from JRE .jars found in the boot classpath.
     *
     * @return List of class files ready to be passed to a `IndexBasedProject`.
     */
    def readJREClassFiles(
        cache: BytecodeInstructionsCache = new BytecodeInstructionsCache
    )(
        implicit
        reader:     ClassFileBinding = new Java11FrameworkWithCaching(cache),
        logContext: LogContext       = GlobalLogContext
    ): Seq[(ClassFile, URL)] = {
        val classFiles = reader.ClassFiles(JRELibraryFolder)
        if (classFiles.isEmpty) {
            OPALLogger.error("project setup", s"loading the JRE ($JRELibraryFolder) failed")
        }

        classFiles
    }

    def readRTJarClassFiles(
        cache: BytecodeInstructionsCache = new BytecodeInstructionsCache
    )(
        implicit
        reader:     ClassFileBinding = new Java11FrameworkWithCaching(cache),
        logContext: LogContext       = GlobalLogContext
    ): Iterable[(ClassFile, URL)] = {
        val classFiles = reader.ClassFiles(RTJar)
        if (classFiles.isEmpty) {
            OPALLogger.error("project setup", s"loading the JRE ($JRELibraryFolder) failed")
        }
        classFiles
    }
}
