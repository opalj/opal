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
package de.tud.cs.st
package bat
package resolved
package dependency

import reader.Java7Framework.ClassFiles

/**
 * This class (the implementation) demonstrates how to load all class files
 * from a jar file and how to create a dependency matrix.
 *
 * @author Michael Eichberg
 * @author Thomas Schlosser
 */
object DependencyMatrix {

    import util.debug.PerformanceEvaluation.{ time, ns2ms }

    private def printUsage(): Unit = {
        println("Loads all classes stored in the jar files and creates a dependency matrix.")
        println("Usage: java …DependencyMatrix <JAR file containing class files>+")
    }

    def main(args: Array[String]) {

        if (args.length == 0 ||
            !args.forall(arg ⇒ arg.endsWith(".zip") ||
                arg.endsWith(".jar"))) {
            printUsage
            sys.exit(1)
        }

        for (arg ← args) {
            val file = new java.io.File(arg)
            if (!file.canRead() || file.isDirectory()) {
                println(arg+" is not a valid ZIP/Jar file.");
                printUsage()
                sys.exit(1)
            }
        }

        analyze(args)
        sys.exit(0)
    }

    def analyze(jarFiles: Array[String]) {
        import scala.collection.mutable.Map
        import scala.collection.mutable.Set
        val dependencyMatrix = Map[VirtualSourceElement, Set[(VirtualSourceElement, DependencyType)]]()
        val dependencyExtractor =
            new DependencyExtractor(
                new DependencyProcessorAdapter {
                    override def processDependency(
                        source: VirtualSourceElement,
                        target: VirtualSourceElement,
                        dType: DependencyType) {
                        val emptySet: Set[(VirtualSourceElement, DependencyType)] = Set.empty
                        dependencyMatrix.get(source) match {
                            case Some(s) ⇒ s += ((target, dType))
                            case None    ⇒ dependencyMatrix += (source -> Set((target, dType)))
                        }
                        // [Scala 2.9.X Compiler crashes on:] dependencyMatrix.getOrElseUpdate(sourceID, emptySet)  + ((targetID, dType))
                    }
                }
            )

        println("Reading all class files - "+jarFiles.mkString(", ")+".")
        var count = 0
        time {
            for {
                jarFile ← jarFiles
                (classFile, _ /*drop urls*/ ) ← ClassFiles(new java.io.File(jarFile))
            } {
                count += 1
                dependencyExtractor.process(classFile)
            }
        } { executionTime ⇒
            println(
                "\nReading all "+(count)+
                    " class files and building the dependency matrix required: "+
                    ns2ms(executionTime)+"milliseconds.")
        }
    }
}