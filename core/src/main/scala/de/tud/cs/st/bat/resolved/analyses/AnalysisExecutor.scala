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
 */
trait AnalysisExecutor {

    val analysis: Analysis[URL, ReportableAnalysisResult]

    /**
     * Describes the analysis specific parameters. An analysis specific parameter
     * has to start with a dash ("-") and has to contain an equals sign ("=") and
     * has to come after the list of jar files, class files or directories that
     * specify the classes that will be loaded.
     */
    def analysisParametersDescription: String = ""

    def checkAnalysisSpecificParameters(parameters: Seq[String]): Boolean = parameters.isEmpty

    def printUsage() {
        println("Usage: java "+
            this.getClass().getName()+
            " <Directories or JAR files containing class files> "+
            analysisParametersDescription)
        println(analysis.description)
        println(analysis.copyright)
    }

    def main(args: Array[String]) {
        if (args.length == 0) {
            printUsage()
            sys.exit(-1)
        }

        //
        // 1. check arguments
        //
        val (parameters, sourceFiles) =
            args.partition(arg ⇒ arg.startsWith("-") && arg.contains("="))

        val files = for (arg ← sourceFiles) yield {
            val file = new File(arg)
            if (!file.exists ||
                !file.canRead ||
                !(arg.endsWith(".jar") ||
                    arg.endsWith(".class") ||
                    file.isDirectory())) {
                println("The file: "+file+" cannot be read or is not valid.")
                printUsage()
                sys.exit(-2)
            }
            file
        }

        if (!checkAnalysisSpecificParameters(parameters)) {
            printUsage()
            sys.exit(-3)
        }

        //
        // 2. setup project context
        //
        val project: Project[URL] = setupProject(files)
        println()

        // 
        // 3. execute analysis
        //
        println("Executing analyses.")
        val result = analysis.analyze(project, parameters)
        println(result.consoleReport)
    }

    def setupProject(files: Iterable[File]): Project[URL] = {
        println("Reading class files:")
        var project = Project.initial[URL]
        for {
            file ← files if { println("\t"+file.toString()); true }
            classFiles = Java7Framework.ClassFiles(file)
            classFile ← classFiles
        } {
            project += classFile
        }
        println("\tClass files loaded: "+project.sources.size)
        project
    }
}
