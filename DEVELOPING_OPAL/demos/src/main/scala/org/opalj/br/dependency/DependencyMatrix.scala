/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package de

import org.opalj.br._
import org.opalj.br.reader.Java8Framework.ClassFiles

/**
 * This class (the implementation) demonstrates how to load all class files
 * from a jar file and how to create a dependency matrix.
 *
 * @author Michael Eichberg
 * @author Thomas Schlosser
 */
object DependencyMatrix {

    import org.opalj.util.PerformanceEvaluation.time

    private def printUsage(): Unit = {
        println("Loads all classes stored in the jar files and creates a dependency matrix.")
        println("Usage: java …DependencyMatrix <JAR file containing class files>+")
    }

    def main(args: Array[String]): Unit = {

        if (args.length == 0 ||
            !args.forall(arg => arg.endsWith(".zip") ||
                arg.endsWith(".jar"))) {
            printUsage()
            sys.exit(1)
        }

        for (arg <- args) {
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

    def analyze(jarFiles: Array[String]): Unit = {
        import scala.collection.mutable.Map
        import scala.collection.mutable.Set
        val dependencyMatrix = Map[VirtualSourceElement, Set[(VirtualSourceElement, DependencyType)]]()
        val dependencyExtractor =
            new DependencyExtractor(
                new DependencyProcessorAdapter {
                    override def processDependency(
                        source: VirtualSourceElement,
                        target: VirtualSourceElement,
                        dType:  DependencyType
                    ): Unit = {
                        dependencyMatrix.get(source) match {
                            case Some(s) => s += ((target, dType))
                            case None    => dependencyMatrix += (source -> Set((target, dType)))
                        }
                        // [Scala 2.9.X Compiler crashes on:] dependencyMatrix.getOrElseUpdate(sourceID, emptySet)  + ((targetID, dType))
                    }
                }
            )

        println("Reading all class files - "+jarFiles.mkString(", ")+".")
        var count = 0
        time {
            for {
                jarFile <- jarFiles
                (classFile, _ /*drop urls*/ ) <- ClassFiles(new java.io.File(jarFile))
            } {
                count += 1
                dependencyExtractor.process(classFile)
            }
        } { t =>
            println(
                s"\nReading all $count class files and building the dependency matrix took "+
                    t.toSeconds
            )
        }
    }
}
