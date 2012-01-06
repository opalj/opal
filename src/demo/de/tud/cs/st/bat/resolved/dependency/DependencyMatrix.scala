/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
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
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st
package bat
package resolved
package dependency

import reader.Java6Framework

/**
 * This class (the implementation) demonstrates how to load all class files
 * from a zip file and how to easily measure the runtime performance.
 *
 * @author Michael Eichberg
 * @author Thomas Schlosser
 */
object DependencyMatrix {

    val performance = new util.perf.PerformanceEvaluation {}
    import performance._

    private def printUsage: Unit = {
        println("Loads all classes stored in the zip files 5 times and prints out the fastest time.")
        println("Usage: java …LoadClassFiles <ZIP or JAR file containing class files>+")
        println("(c) 2011 Michael Eichberg (eichberg@informatik.tu-darmstadt.de)")
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

    def analyze(zipFiles: Array[String]) {
        import scala.collection.mutable.{ Map, Set }
        val dependencyMatrix = Map[Int, Set[(Int, DependencyType)]]()
        val dependencyExtractor = new DependencyExtractor with SourceElementIDsMap with DoNothingSourceElementsVisitor {
            def processDependency(sourceID: Int, targetID: Int, dType: DependencyType) {
                dependencyMatrix.getOrElseUpdate(sourceID, { Set[(Int, DependencyType)]() }) + ((targetID, dType))
            }
        }

        print("Reading all class files - "+zipFiles.mkString(", ")+" - (5 times): ")
        var count = 0
        var min = Long.MaxValue

        for (i ← 1 to 5) {
            dependencyMatrix.clear()
            print(".")
            time(duration ⇒ { min = math.min(duration, min) }) {
                for (
                    zipFile ← zipFiles;
                    classFile ← Java6Framework.ClassFiles(zipFile)
                ) {
                    require(classFile.thisClass ne null)
                    count += 1
                    dependencyExtractor.process(classFile)
                }
            }

        }
        println("\nReading all "+(count / 5)+" class files and building the dependency matrix required at least: "+nanoSecondsToMilliseconds(min)+"milliseconds.")
    }
}