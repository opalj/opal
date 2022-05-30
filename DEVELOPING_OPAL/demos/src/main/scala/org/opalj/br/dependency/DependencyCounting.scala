/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package de

import java.net.URL
import org.opalj.br.analyses._

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

/**
 * Counts the number of dependencies found in a project.
 *
 * @author Michael Eichberg
 */
object DependencyCounting extends AnalysisApplication with OneStepAnalysis[URL, BasicReport] {

    val analysis = this

    override def description: String = "counts the number of inter-source element dependencies"

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {
        println("Press enter to start the dependency collection.")
        scala.io.StdIn.readLine()

        import org.opalj.util.PerformanceEvaluation._
        val counter = time {
            val counter = new DependencyCountingDependencyProcessor with FilterSelfDependencies
            val extractor = new DependencyExtractor(counter)
            // process the class files in parallel to speed up the collection process
            project.allClassFiles.par foreach (extractor.process)
            counter
        } { t => println(s"[info] Time to count the dependencies: $t") }

        BasicReport(
            (f"Number of inter source-element dependencies: ${counter.currentDependencyCount}%,9d%n") +
                f"Number of dependencies on primitive types:   ${counter.currentDependencyOnPrimitivesCount}%,9d%n"+
                f"Number of dependencies on array types:       ${counter.currentDependencyOnArraysCount}%,9d%n"
        )
    }
}
