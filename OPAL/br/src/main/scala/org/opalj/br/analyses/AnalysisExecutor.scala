/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package br
package analyses

import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.br.reader.Java8FrameworkWithCaching
import org.opalj.br.reader.Java8LibraryFrameworkWithCaching

import java.net.URL
import java.io.File

/**
 * Provides the necessary infrastructure to easily execute a given analysis that
 * generates some analysis result that can be printed on the command line.
 *
 * To facilitate the usage of this trait several implicit conversions are defined that
 * wrap standard analyses ([[org.opalj.br.analyses]]) such that they report
 * results that are reportable.
 *
 * This class distinguishes between class files belonging to the code base under
 * analysis and those that belong to the libraries. Those belonging to the libraries
 * are loaded using the `ClassFileReader` for library classes (basically, all method
 * bodies are skipped [[org.opalj.br.reader.Java8LibraryFramework]]).
 * The parameter to specify library classes is `-libcp=`, the parameter to specify
 * the "normal" classpath is `-cp=`.
 *
 * ==Control Flow==
 *  1. The standard parameters are checked.
 *  1. The analysis is called to let it verify the analysis specific parameters.
 *  1. The [[Project]] is created.
 *  1. The `analyze` method of the [[Analysis]] is called with the project and the parameters.
 *  1. The results are printed.
 *
 * @author Michael Eichberg
 * @author Arne Lottmann
 */
trait AnalysisExecutor {

    /**
     * The analysis that will be executed.
     *
     * The `analyze` method implemented by the analysis will be called after loading
     * all class files and creating a `Project`. Additionally,
     * all specified (additional) parameters are passed to the analyze method.
     */
    val analysis: Analysis[URL, ReportableAnalysisResult]

    /**
     * Describes the analysis specific parameters. An analysis specific parameter
     * has to start with a dash ("-") and has to contain an equals sign ("=").
     *
     * @note The parameter `-cp=` is already predefined (see general documentation).
     * @note The parameter `-library=` is already predefined (see general documentation).
     */
    def analysisSpecificParametersDescription: String = ""

    /**
     * Checks if the (additional) parameters are understood by
     * the analysis.
     *
     * This method **must be** overridden if the analysis defines additional
     * parameters. A method that overrides this method should return false if it can't
     * validate all arguments.
     * The default behavior is to check that there are no additional parameters.
     */
    def checkAnalysisSpecificParameters(parameters: Seq[String]): Boolean =
        parameters.isEmpty

    /**
     * Prints out general information how to use this analysis. Printed whenever
     * the set of specified parameters is not valid.
     */
    protected def printUsage() {
        println("Usage: java "+
            this.getClass().getName()+"\n"+
            "[-cp=<Directories or JAR files containing class files> (If no class path is specified the current folder is used.)]\n"+
            "[-libcp=<Directories or JAR files containing class files>]\n"+
            analysisSpecificParametersDescription)
        println(analysis.description)
        println(analysis.copyright)
    }

    def main(args: Array[String]): Unit = {

        //
        // 1. check arguments
        //
        def checkIfFilesAreReadableAndReturnThem(filenames: Array[String]): Array[File] = {
            for (filename ← filenames) yield checkIfFileIsReadableAndReturnIt(filename)
        }
        def checkIfFileIsReadableAndReturnIt(filename: String): File = {
            val file = new File(filename)
            if (!file.exists ||
                !file.canRead ||
                !(filename.endsWith(".jar") ||
                    filename.endsWith(".class") ||
                    file.isDirectory())) {
                println(Console.RED+
                    "[error] The file: "+file+" cannot be read or is not valid."+
                    Console.RESET)
                printUsage()
                sys.exit(-2)
            }
            file
        }

        val (cp, args1) = {
            args.partition(_.startsWith("-cp=")) match {
                case (Array(), args1) ⇒
                    (Array(System.getProperty("user.dir")), args1)
                case (Array(cpParam), args1) ⇒
                    (cpParam.substring(4).split(File.pathSeparator), args1)
            }
        }
        val cpFiles = checkIfFilesAreReadableAndReturnThem(cp)

        val (libcp, parameters) = {
            args1.partition(_.startsWith("-libcp=")) match {
                case (Array(libParam), parameters) ⇒
                    (libParam.substring(7).split(File.pathSeparator), parameters)
                case result ⇒
                    result
            }
        }
        val libcpFiles = checkIfFilesAreReadableAndReturnThem(libcp)

        if (!checkAnalysisSpecificParameters(parameters)) {
            println(Console.RED+
                "[error] Unknown parameters: "+parameters.mkString("\"", " ", "\"") +
                Console.RESET)
            printUsage()
            sys.exit(-3)
        }

        //
        // 2. setup project context
        //
        val project: Project[URL] = setupProject(cpFiles, libcpFiles)

        // 
        // 3. execute analysis
        //
        println("[info] Executing analysis: "+analysis.title+".")
        val result = analysis.analyze(project, parameters)
        println(result.consoleReport)
    }

    def setupProject(
        cpFiles: Iterable[File],
        libcpFiles: Iterable[File]): Project[URL] = {
        println("[info] Reading class files (found in):")
        val cache: BytecodeInstructionsCache = new BytecodeInstructionsCache
        val Java8ClassFileReader = new Java8FrameworkWithCaching(cache)
        val Java8LibraryClassFileReader = new Java8LibraryFrameworkWithCaching(cache)

        val (classFiles, exceptions1) =
            reader.readClassFiles(
                cpFiles,
                Java8ClassFileReader.ClassFiles,
                (file) ⇒ println("[info]\t"+file))

        val (libraryClassFiles, exceptions2) = {
            if (libcpFiles.nonEmpty) {
                println("[info]Reading library class files (found in):")
                reader.readClassFiles(
                    libcpFiles,
                    Java8LibraryClassFileReader.ClassFiles,
                    (file) ⇒ println("[info]\t"+file))
            } else {
                (Iterable.empty[(ClassFile, URL)], List.empty[Throwable])
            }
        }
        val allExceptions = exceptions1 ++ exceptions2
        if (allExceptions.nonEmpty) {
            Console.err.println("[error] While reading the class files the following exceptions occured:")
            val out = new java.io.ByteArrayOutputStream
            val pout = new java.io.PrintStream(out)
            for (exception ← exceptions1 ++ exceptions2) {
                Console.err.println(exception.getMessage())
                pout.println("<<<<<<<<<<< EXCEPTION >>>>>>>>>>>")
                exception.printStackTrace(pout)
            }
            pout.flush
            val message = new String(out.toByteArray())
            util.writeAndOpenDesktopApplication(message, "Exceptions", ".txt") foreach { f ⇒
                Console.err.println("Details can be found in: "+f.toString)
            }
        }

        var project = Project(classFiles, libraryClassFiles)
        print(
            project.statistics.map(kv ⇒ "- "+kv._1+": "+kv._2).toList.sorted.
                mkString("[info] Project statistics:\n[info]\t", "\n[info]\t", "\n")
        )
        project
    }
}
