package org.opalj
package fpcf
package analysis
package cg
package cha

import java.net.URL
import org.opalj.br.analyses.{BasicReport, CallBySignatureResolutionKey, DefaultOneStepAnalysis, Project, SourceElementsPropertyStoreKey}
import org.opalj.fpcf.Property
import org.opalj.fpcf.analysis.demo.AnalysisModeConfigFactory
import org.opalj.log.OPALLogger
import org.opalj.log.GlobalLogContext
import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.Warn

object CpaCHADemo extends DefaultOneStepAnalysis {

    override def title: String = "Test stuff."

    override def description: String =
        ""

    //OPALLogger.updateLogger(GlobalLogContext, new ConsoleOPALLogger(true, Warn))

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        val cpaProject = AnalysisModeConfigFactory.resetAnalysisMode(project, AnalysisModes.CPA)
        val cpaStore = cpaProject.get(SourceElementsPropertyStoreKey)

        val methodsCount: Double = project.methodsCount.toDouble
        def getPercentage(value: Int): String = "%1.2f" format (value.toDouble / methodsCount * 100d)

        // CALL GRAPH STUFF
        val newCpaCG = cpaProject.get(CHACallGraphKey).callGraph
        // CALL GRAPH STUFF

        val cpaEP = cpaStore.entities { (p: Property) ⇒
            p == IsEntryPoint
        }

        val cbs = project.get(CallBySignatureResolutionKey)
        println(cbs.statistics)

        BasicReport(
            s"#entry points    : ${cpaEP.size}\n"+
                s"percentage   : ${getPercentage(cpaEP.size)}\n"+
                s"#call edges  : ${newCpaCG.callEdgesCount}\n"
        )
    }
}