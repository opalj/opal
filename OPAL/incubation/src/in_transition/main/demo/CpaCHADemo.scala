/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg
package cha

import java.net.URL
import org.opalj.ai.analyses.cg.ComputedCallGraph
import org.opalj.br.analyses.{BasicReport, ProjectAnalysisApplication, Project, PropertyStoreKey}
import org.opalj.br.analyses.AnalysisModeConfigFactory
import org.opalj.util.PerformanceEvaluation
import org.opalj.fpcf.properties.IsEntryPoint

object CpaCHADemo extends ProjectAnalysisApplication {

    override def title: String = "Test stuff."

    override def description: String = ""

    //OPALLogger.updateLogger(GlobalLogContext, new ConsoleOPALLogger(true, Warn))

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        val cpaProject = AnalysisModeConfigFactory.resetAnalysisMode(project, AnalysisModes.CPA)
        val cpaStore = cpaProject.get(PropertyStoreKey)

        val methodsCount: Double = project.methodsCount.toDouble
        def getPercentage(value: Double): String = "%1.2f" format (value / methodsCount * 100d)

        var cpaCCG: ComputedCallGraph = null
        PerformanceEvaluation.time {
            cpaCCG = cpaProject.get(org.opalj.fpcf.analyses.cg.cha.CHACallGraphKey)
        } { t ⇒ println("CPA-CHA computation time: "+t.toSeconds) }

        //        println("CPA (cbs resolution index): "+GlobalPerformanceEvaluation.getTime('cbs).toSeconds.toString(true))
        //        println("CPA (cbs analysis): "+GlobalPerformanceEvaluation.getTime('cbst).toSeconds.toString(true))
        //        println("CPA (entry points): "+GlobalPerformanceEvaluation.getTime('ep).toSeconds.toString(true))
        //        println("CPA (clientCallable): "+GlobalPerformanceEvaluation.getTime('callableByOthers).toSeconds.toString(true))
        //        println("CPA (method accessibility): "+GlobalPerformanceEvaluation.getTime('methodAccess).toSeconds.toString(true))
        //        println("CPA (instantiable classes index): "+GlobalPerformanceEvaluation.getTime('inst).toSeconds.toString(true))
        //        println("CPA (cg construction): "+GlobalPerformanceEvaluation.getTime('const).toSeconds.toString(true))
        //        println("CPA (invoke virtual): \t - "+GlobalPerformanceEvaluation.getTime('invokevirtual).toSeconds.toString(true))
        //        println("CPA (invoke interface): \t - "+GlobalPerformanceEvaluation.getTime('invokeinterface).toSeconds.toString(true))
        //        println("CPA (invoke special): \t - "+GlobalPerformanceEvaluation.getTime('invokespecial).toSeconds.toString(true))
        //        println("CPA (invoke static): \t - "+GlobalPerformanceEvaluation.getTime('invokestatic).toSeconds.toString(true))
        //        println("CPA (cg builder): \t - "+GlobalPerformanceEvaluation.getTime('cgbuilder).toSeconds.toString(true)+"\n\n")

        val execpetions = cpaCCG.constructionExceptions.map(_.toFullString).mkString("Construction Exception\n\n", "\n", "\n")
        println(execpetions)

        val newCpaCG = cpaCCG.callGraph

        val cpaEP = cpaStore.entities { (p: Property) ⇒ p == IsEntryPoint }

        BasicReport(
            s"#entry points    : ${cpaEP.size}\n"+
                s"percentage   : ${getPercentage(cpaEP.size.toDouble)}\n"+
                s"#call edges  : ${newCpaCG.callEdgesCount}\n"
        )
    }
}
