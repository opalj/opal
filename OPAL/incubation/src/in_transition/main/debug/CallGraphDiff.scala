/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.debug

import java.net.URL

import org.opalj.util.PerformanceEvaluation.time
import org.opalj.br.Method
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.ai.analyses.cg.CHACallGraphAlgorithmConfiguration
import org.opalj.ai.analyses.cg.CallGraphFactory
import org.opalj.ai.analyses.cg.ComputedCallGraph
import org.opalj.ai.analyses.cg.VTAWithPreAnalysisCallGraphAlgorithmConfiguration
import org.opalj.ai.analyses.cg.DefaultVTACallGraphDomain
import org.opalj.ai.analyses.cg.CallGraphDifferenceReport
import org.opalj.ai.analyses.cg.CallGraphComparison
import org.opalj.br.analyses.ProjectAnalysisApplication

/**
 * Calculates and compares the results of two call graphs.
 *
 * @author Michael Eichberg
 */
object CallGraphDiff extends ProjectAnalysisApplication {

    override def title: String = "identifies differences between two call graphs"

    override def description: String = {
        "identifies methods that do not have the same call graph information"
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        val (unexpected, additional) = callGraphDiff(project, Console.println, isInterrupted)
        if (unexpected.nonEmpty || additional.nonEmpty) {
            var r = "Found the following difference(s):\n"
            if (additional.nonEmpty) {
                r = additional.mkString(r+"Additional:\n", "\n\n", "\n\n")
            }
            if (unexpected.nonEmpty) {
                r = unexpected.mkString(r+"Unexpected:\n", "\n\n", "\n\n")
            }
            BasicReport(r)
        } else
            BasicReport("No differences found.")
    }

    def callGraphDiff(
        project:       Project[_],
        println:       String ⇒ Unit,
        isInterrupted: () ⇒ Boolean
    ): (List[CallGraphDifferenceReport], List[CallGraphDifferenceReport]) = {
        // TODO Add support for interrupting the calculation of the control-flow graph
        import CallGraphFactory.defaultEntryPointsForLibraries
        val entryPoints = () ⇒ defaultEntryPointsForLibraries(project)
        val ComputedCallGraph(lessPreciseCG, _, _) = time {
            CallGraphFactory.create(
                project,
                entryPoints,
                new CHACallGraphAlgorithmConfiguration(project)
            )
        } { t ⇒ println("creating the less precise call graph took "+t) }

        if (isInterrupted())
            return null;

        val ComputedCallGraph(morePreciseCG, _, _) = time {
            CallGraphFactory.create(
                project,
                entryPoints,
                new VTAWithPreAnalysisCallGraphAlgorithmConfiguration(project) {
                    override def Domain(method: Method) = {
                        new DefaultVTACallGraphDomain(
                            project, fieldValueInformation, methodReturnValueInformation,
                            cache,
                            method //, 4
                        )
                    }
                }
            )
        } { ns ⇒ println("creating the more precise call graph took "+ns.toSeconds) }

        if (isInterrupted())
            return null;

        CallGraphComparison(project, lessPreciseCG, morePreciseCG)
    }
}
