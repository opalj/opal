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

object OpaCHADemo extends ProjectAnalysisApplication {

    override def title: String = "Test stuff."

    override def description: String =
        ""

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        val opaProject = AnalysisModeConfigFactory.resetAnalysisMode(project, AnalysisModes.OPA)
        val opaStore = opaProject.get(PropertyStoreKey)

        val methodsCount: Double = project.methodsCount.toDouble
        def getPercentage(value: Int): String = "%1.2f" format (value.toDouble / methodsCount * 100d)

        var opaCCG: ComputedCallGraph = null
        PerformanceEvaluation.time {
            opaCCG = opaProject.get(org.opalj.fpcf.analyses.cg.cha.CHACallGraphKey)
        } { t ⇒ println("OPA-CHA computation time: "+t.toSeconds) }

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

        val execpetions = opaCCG.constructionExceptions.map(_.toFullString).mkString("Construction Exception\n\n", "\n", "\n")
        println(execpetions)

        val newOpaCG = opaCCG.callGraph

        val opaEP = opaStore.entities { (p: Property) ⇒ p == IsEntryPoint }

        BasicReport(
            s"#methods:  ${methodsCount}\n"+
                s"#entry points: ${opaEP.size}\n"+
                s"percentage   : ${getPercentage(opaEP.size)}\n"+
                s"#call edges  : ${newOpaCG.callEdgesCount}"
        )
    }
}
