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
package dependency

import util.debug.nanoSecondsToMilliseconds

import resolved._
import resolved.reader.Java7Framework.ClassFiles

/**
 * This class (the implementation) demonstrates how to load all class files
 * from a jar file and how to create a dependency matrix.
 *
 * @author Michael Eichberg
 * @author Thomas Schlosser
 */
object DependencyMatrix {

    val performance = new util.debug.PerformanceEvaluation {}
    import performance._
    import de.tud.cs.st.util.debug._

    private def printUsage: Unit = {
        println("Loads all classes stored in the jar files and creates a dependency matrix.")
        println("Usage: java …DependencyMatrix <JAR file containing class files>+")        
    }

    def main(args: Array[String]) {

        if (args.length == 0 || !args.forall(arg ⇒ arg.endsWith(".zip") || arg.endsWith(".jar"))) {
            printUsage
            sys.exit(1)
        }

        for (arg ← args) {
            val file = new java.io.File(arg)
            if (!file.canRead() || file.isDirectory()) {
                println(arg+" is not a valid ZIP/Jar file.");
                printUsage
                sys.exit(1)
            }
        }

        analyze(args)
        sys.exit(0)
    }

    def analyze(jarFiles: Array[String]) {
        import scala.collection.mutable.Map
        import scala.collection.mutable.Set
        val dependencyMatrix = Map[Int, Set[(Int, DependencyType.Value)]]()
        val dependencyExtractor = new DependencyExtractor(new SourceElementIDsMap {}) with NoSourceElementsVisitor {
            def processDependency(sourceID: Int, targetID: Int, dType: DependencyType.Value) {
                val emptySet: Set[(Int, DependencyType)] = Set.empty
                dependencyMatrix.get(sourceID) match {
                    case Some(s) ⇒ s += ((targetID, dType))
                    case None    ⇒ dependencyMatrix += (sourceID -> Set((targetID, dType)))
                }
                // [Scala 2.9.X Compiler crashes on:] dependencyMatrix.getOrElseUpdate(sourceID, emptySet)  + ((targetID, dType))
            }
        }

        println("Reading all class files - "+jarFiles.mkString(", ")+".")
        var count = 0
        var duration = Long.MaxValue
        time(duration = _) {
            for (
                jarFile ← jarFiles;
                (classFile, _) ← ClassFiles(jarFile)
            ) {
                require(classFile.thisClass ne null)
                count += 1
                dependencyExtractor.process(classFile)
            }
        }
        println("\nReading all "+(count)+" class files and building the dependency matrix required: "+nanoSecondsToMilliseconds(duration)+"milliseconds.")
    }
}