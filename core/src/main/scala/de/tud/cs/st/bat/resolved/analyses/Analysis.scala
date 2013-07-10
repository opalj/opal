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

import java.io.File

/**
 * Common trait that needs to be mixed in by analyses that want to use the general
 * analysis framework [[de.tud.cs.st.bat.resolved.analyses.AnalysisExecutor]].
 *
 * @author Michael Eichberg
 */
trait Analysis[AnalysisResult] {

    def analyze(project: Project): AnalysisResult

    def description: String

    def copyright: String
}

/**
 * Trait that identifies analysis results that can be reported to the (end-)user.
 */
trait ReportableAnalysisResult {

    /**
     * The results of the analysis in a form suitable for printing it to the
     * command line.
     *
     * If you are generating output related to (a line in) a class file use the
     * following format (as used by other compilers, e.g., CLANG and GCC):
     * <pre>
     * FILENAME[:LINE[:COLUMN]]: TYPE: MESSAGE
     * </pre>
     * where FILENAME denotes the name of the file, LINE is the line number if available,
     * COLUMN is the column – which is usually not available when you analyze class files
     * and TYPE identifies the type of the message (e.g., "note", "warning", "error",
     * "fatal error").
     *
     * Line and column information is optional.
     *
     * If the real filename is not available, use the fully qualified name of the class
     * in binary notation (i.e., using "/" to seperate the package qualifiers)
     * with the suffice ".class" appended.
     *
     * Note that the space after the location information is required!
     *
     * ==Example==
     * <pre>
     * demo/Buggy.class:100: warning: protected field in final class
     * </pre>
     */
    def consoleReport: String
}

trait AnalysisExecutor extends Analysis[ReportableAnalysisResult] {

    def printUsage() {
        println("Usage: java …"+this.getClass().getName()+" <Directory or ZIP/JAR file containing class files>+")
        println(description)
        println(copyright)
    }

    def main(args: Array[String]) {
        if (args.length == 0) {
            printUsage()
            sys.exit(-1)
        }

        //
        // 1. check arguments
        //
        val files = for (arg ← args) yield {
            val file = new File(arg)
            if (!file.canRead ||
                !(arg.endsWith(".zip") ||
                    arg.endsWith(".jar") ||
                    arg.endsWith(".class") ||
                    file.isDirectory())) {
                println("The file: "+file+" cannot be read or is not valid.")
                printUsage()
                sys.exit(-2)
            }
            file
        }

        //
        // 2. setup project context
        //
        val project = setupProject(files)
        println()

        // 
        // 3. execute analyses
        //
        println("Executing analyses.")
        println(analyze(project).consoleReport)
    }

    def setupProject(files: Iterable[File]): Project = {
        println("Reading class files:")
        var project = new Project()
        for {
            file ← files if { println("\t"+file.toString()); true }
            classFiles = Java7Framework.ClassFiles(file)
            classFile ← classFiles
        } {
            project += classFile
        }
        project
    }
}
