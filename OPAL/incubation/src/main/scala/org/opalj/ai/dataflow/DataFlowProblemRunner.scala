/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package dataflow

import java.net.URL
import br.analyses._
import org.opalj.util.PerformanceEvaluation.time

/**
 * Support methods to make it possible to solve a single [[DataFlowProblem]].
 *
 * ==Usage==
 * Mix in this trait in the object which specifies your data-flow problem. After
 * that you can run it.
 *
 * @author Michael Eichberg and Ben Hermann
 */
trait DataFlowProblemRunner extends AnalysisExecutor {
    dataFlowProblemFactory: DataFlowProblemFactory ⇒

    final override val analysis = new Analysis[URL, ReportableAnalysisResult] {

        override def title: String =
            dataFlowProblemFactory.title

        override def description: String =
            dataFlowProblemFactory.description

        override def analyze(
            project:                Project[URL],
            parameters:             Seq[String],
            initProgressManagement: (Int) ⇒ ProgressManagement
        ): ReportableAnalysisResult = {

            val pm = initProgressManagement(2)
            pm.start(1, "setup")
            val initializedDataFlowProblem = time {
                val params = dataFlowProblemFactory.processAnalysisParameters(parameters)
                val dataFlowProblem = dataFlowProblemFactory.create(project, params)
                dataFlowProblem.initializeSourcesAndSinks
                println(f"[info] Number of source values: ${dataFlowProblem.sourceValues.size}.")
                println(f"[info] Number of sinks: ${dataFlowProblem.sinkInstructions.size}.")
                dataFlowProblem
            } { t ⇒
                println(s"[info] Setup of the data-flow problem took ${t.toSeconds}")
            }
            pm.end(1)

            if (pm.isInterrupted())
                return null

            pm.start(2, "solving data-flow problem")
            val result = time {
                initializedDataFlowProblem.solve()
            } { t ⇒
                println(s"[info] Solving the data-flow problem took ${t.toSeconds}")
            }
            pm.end(2)

            BasicReport(result)
        }
    }
}

