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
package ai
package analyses

import org.opalj.br.analyses.Project

/**
 * A analysis that collects all classes that are immutable inside a jar.
 *
 * @author Andre Pacak
 */
object ImmutabilityChecker {
    private def printUsage(): Unit = {
        println("Usage: java …Main <JAR file containing class files>+")
    }

    def main(args: Array[String]): Unit = {
        if (args.length == 0 || !args.forall(arg ⇒ arg.endsWith(".jar"))) {
            printUsage
            sys.exit(1)
        }

        for (arg ← args) {
            val file = new java.io.File(arg)
            if (!file.canRead() || file.isDirectory) {
                println("The file: "+file+" cannot be read.");
                printUsage
                sys.exit(1)
            }
        }

        for (arg ← args) {
            println(Console.BOLD+"analyzing "+arg.toString())
            val file = new java.io.File(arg)
            val project = Project(file)
            val classFiles = project.classFiles.filter {
                classFile ⇒
                    classFile.isClassDeclaration &&
                        !classFile.isInnerClass
            }

            val result = ImmutabilityAnalysis.doAnalyze(project, () ⇒ false)
            val relevantClasses = result.filter {
                x ⇒
                    val classFile = project.classFile(x._1)
                    classFile.nonEmpty &&
                        classFile.get.isClassDeclaration &&
                        !classFile.get.isInnerClass
            }

            val immutableClasses = relevantClasses.filter {
                _._2 == Immutability.Immutable
            }
            val condimmutableClasses = relevantClasses.filter {
                _._2 == Immutability.ConditionallyImmutable
            }
            val mutableClasses = relevantClasses.filter { _._2 == Immutability.Mutable }
            val unknownClasses = relevantClasses.filter { _._2 == Immutability.Unknown }
            println("The Jar contains "+classFiles.size+" Classes")
            println(immutableClasses.size+" classes are immutable")
            println(condimmutableClasses.size+" classes are conditionally immutable")
            println(mutableClasses.size+" classes are mutable")
            println(unknownClasses.size+" classes cannot be classified")
        }
    }
}