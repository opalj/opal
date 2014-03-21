/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package analyses

import reader.Java7Framework
import reader.Java7LibraryFramework

import java.net.URL
import java.io.File

/**
 * Provides the necessary infrastructure to easily execute a given analysis that
 * generates some analysis result that can be printed on the command line.
 *
 * This trait is particularly useful to execute an analysis using the command line.
 *
 * To facilitate the usage of this trait several implicit conversions are defined that
 * wrap standard analyses ([[de.tud.cs.st.bat.resolved.analyses]]) such that they report
 * results that are reportable.
 *
 * This class distinguishes between class files belonging to the code base under
 * analysis and those that belong to the libraries. Those belonging to the libraries
 * are loaded using the `ClassFileReader` for library classes (basically, all method
 * bodies are skipped [[de.tud.cs.st.bat.resolved.reader.Java7LibaryFramework]]).
 * The parameter to specify library classes is `-libcp=`, the parameter to specify
 * the "normal" classpath is `-cp=`.
 *
 * @author Michael Eichberg
 * @author Arne Lottmann
 */
trait AnalysisExecutor {

    val analysis: Analysis[URL, ReportableAnalysisResult]

    /**
     * Describes the analysis specific parameters. An analysis specific parameter
     * has to start with a dash ("-") and has to contain an equals sign ("=") and
     * has to come after the list of jar files, class files or directories that
     * specify the classes that will be loaded.
     *
     * @note The parameter `-library=` is already predefined (see general documentation).
     */
    def analysisParametersDescription: String = ""

    def checkAnalysisSpecificParameters(parameters: Seq[String]): Boolean =
        parameters.isEmpty

    def printUsage() {
        println("Usage: java "+
            this.getClass().getName()+"\n"+
            " -cp=<Directories or JAR files containing class files> (If no class path is specified the current folder is used.)\n"+
            " -libcp=<Directories or JAR files containing class files>\n"+
            analysisParametersDescription)
        println(analysis.description)
        println(analysis.copyright)
    }

    def main(args: Array[String]) {

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
                println("The file: "+file+" cannot be read or is not valid.")
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
        println("Executing analyses.")
        val result = analysis.analyze(project, parameters)
        println(result.consoleReport)
    }

    def setupProject(cpFiles: Iterable[File], libcpFiles: Iterable[File]): Project[URL] = {
        println("Reading class files (found in):")
        val (classFiles, exceptions1) =
            reader.readClassFiles(
                cpFiles,
                Java7Framework.ClassFiles,
                (file) ⇒ println("\t"+file))

        val (libraryClassFiles, exceptions2) = {
            if (libcpFiles.nonEmpty) {
                println("Reading library class files (found in):")
                reader.readClassFiles(
                    libcpFiles,
                    Java7LibraryFramework.ClassFiles,
                    (file) ⇒ println("\t"+file))
            } else {
                (Iterable.empty[(ClassFile, URL)], List.empty[Throwable])
            }
        }
        val allExceptions = exceptions1 ++ exceptions2
        if (allExceptions.nonEmpty) {
            Console.err.println("While reading the class files the following exceptions occured:")
            for (exception ← exceptions1 ++ exceptions2) {
                Console.err.println(exception.getMessage())
            }
        }

        var project = IndexBasedProject(classFiles, libraryClassFiles)
        println("Class files loaded: "+project.classFilesCount)
        project
    }
}
