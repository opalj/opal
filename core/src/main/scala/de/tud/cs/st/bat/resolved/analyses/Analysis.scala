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

/**
 * @author Michael Eichberg
 */
trait Analysis[AnalysisResult] {

    def analyze(project: Project): AnalysisResult

    def description: String

    def copyright: String
}

trait AnalysisExecutor extends Analysis[Unit] {

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

        val files = for (arg ← args) yield {
            val file = new java.io.File(arg)
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

        println("Reading class files:")
        var project = new Project()
        for {
            file ← files if { println("\t"+file.toString()); true }
            classFiles = Java7Framework.ClassFiles(file)
            classFile ← classFiles
        } {
            project += classFile
        }
        println("Starting analyses: ")
        analyze(project)
    }
}
