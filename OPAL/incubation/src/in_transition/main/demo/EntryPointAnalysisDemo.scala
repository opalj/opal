/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.analyses.BasicReport

import java.net.URL
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.log.OPALLogger
import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.GlobalLogContext
import org.opalj.log.Warn
import org.opalj.ai.analyses.cg.CallGraphFactory
import org.opalj.br.analyses.AnalysisModeConfigFactory
import org.opalj.fpcf.properties.IsEntryPoint

/**
 * @author Michael Reif
 */
object EntryPointAnalysisDemo extends MethodAnalysisDemo {

    OPALLogger.updateLogger(GlobalLogContext, new ConsoleOPALLogger(true, Warn))

    override def title: String = "Determines the entry points for the given project."

    override def description: String =
        "Determines all methods that are initial entry points when construction a call graph."

    override def doAnalyze(
        project:       org.opalj.br.analyses.Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        var oldEntryPoints = 0

        var oldTime = org.opalj.util.Seconds.None
        org.opalj.util.PerformanceEvaluation.time {
            oldEntryPoints = CallGraphFactory.defaultEntryPointsForLibraries(project).size
        } { t ⇒ oldTime = t.toSeconds }

        /* CPA */

        val cpaProject = AnalysisModeConfigFactory.resetAnalysisMode(project, AnalysisModes.CPA)
        val cpaExecuter = cpaProject.get(FPCFAnalysesManagerKey)

        var analysisTimeCPA = org.opalj.util.Seconds.None
        org.opalj.util.PerformanceEvaluation.time {

            SimpleInstantiabilityAnalysis.run(cpaProject)
            cpaExecuter.runAll(
                CallableFromClassesInOtherPackagesAnalysis,
                MethodAccessibilityAnalysis
            )

            cpaExecuter.run(EntryPointsAnalysis, true)

        } { t ⇒ analysisTimeCPA = t.toSeconds }

        /* OPA */

        val opaProject = AnalysisModeConfigFactory.resetAnalysisMode(project, AnalysisModes.OPA)
        val opaExecuter = opaProject.get(FPCFAnalysesManagerKey)

        var analysisTimeOPA = org.opalj.util.Seconds.None
        org.opalj.util.PerformanceEvaluation.time {

            SimpleInstantiabilityAnalysis.run(opaProject)
            opaExecuter.runAll(
                CallableFromClassesInOtherPackagesAnalysis,
                MethodAccessibilityAnalysis
            )

            opaExecuter.run(EntryPointsAnalysis)

        } { t ⇒ analysisTimeOPA = t.toSeconds }

        /* Analysis Execution done*/

        val cpaStore = cpaProject.get(PropertyStoreKey)
        val opaStore = opaProject.get(PropertyStoreKey)

        val cpaEps = entitiesByProperty(IsEntryPoint)(cpaStore)
        val opaEps = entitiesByProperty(IsEntryPoint)(opaStore)
        //        val noEntryPoints = entitiesByProperty(NoEntryPoint)(propertyStore)

        val methodsCount: Double = project.methodsCount.toDouble
        def getPercentage(value: Int): String = "%1.2f" format (value.toDouble / methodsCount * 100d)

        val outputTable = s"\n\n#methods: ${project.methodsCount}\n"+
            s"#entry points: | $oldEntryPoints (old)     | ${opaEps.size} (opa)     | ${cpaEps.size} (cpa)\n"+
            s"percentage:    | ${getPercentage(oldEntryPoints)}% (old)     | ${getPercentage(opaEps.size)}% (opa)     | ${getPercentage(cpaEps.size)}% (cpa)\n"+
            s"analysisTime:  | $oldTime (old) | ${analysisTimeOPA} (opa) | ${analysisTimeCPA} (cpa)"

        BasicReport(
            outputTable
        )
    }
}
