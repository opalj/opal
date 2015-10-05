package org.opalj.fpa

import org.opalj.fpa.demo.util.MethodAnalysisDemo
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import java.net.URL
import org.opalj.br.analyses.NonOverridden
import org.opalj.br.analyses.IsOverridden
import org.opalj.br.analyses.OverridingAnalysis

/**
 * @author Michael Reif
 */
object OverridingAnalysisDemo extends MethodAnalysisDemo {

    override def title: String =
        "method override analysis"

    override def description: String =
        "determines the overriden methods of a library"

    override def doAnalyze(
        project: Project[URL],
        parameters: Seq[String],
        isInterrupted: () ⇒ Boolean): BasicReport = {

        val propertyStore = project.get(SourceElementsPropertyStoreKey)

        var analysisTime = org.opalj.util.Seconds.None
        org.opalj.util.PerformanceEvaluation.time {
            val fmat = new Thread(new Runnable { def run = OverridingAnalysis.analyze(project) });
            fmat.start
            fmat.join
            propertyStore.waitOnPropertyComputationCompletion( /*default: true*/ )
        } { t ⇒ analysisTime = t.toSeconds }

        val nonOverriddenMethods = entitiesByProperty(NonOverridden)(propertyStore)
        val nonOverriddenInfo = buildMethodInfo(nonOverriddenMethods)(project)

        val overriddenMethods = entitiesByProperty(IsOverridden)(propertyStore)
        val overriddenInfo = buildMethodInfo(overriddenMethods)(project)

        val nonOverriddenInfoString = finalReport(nonOverriddenInfo, "Found non-overridden methods")
        val overriddenInfoString = finalReport(overriddenInfo, "Found overridden methods")

        BasicReport(
            overriddenInfoString +
                nonOverriddenInfoString +
                propertyStore+
                "\nAnalysis time: "+analysisTime
        )
    }
}