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

object OpaCHADemo extends DefaultOneStepAnalysis {

    override def title: String = "Test stuff."

    override def description: String =
        ""

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        val opaProject = AnalysisModeConfigFactory.updateProject(project, AnalysisModes.OPA)
        val opaStore = opaProject.get(SourceElementsPropertyStoreKey)

        OPALLogger.updateLogger(opaProject.logContext, new ConsoleOPALLogger(true, Warn))

        val methodsCount: Double = project.methodsCount.toDouble
        def getPercentage(value: Int): String = "%1.2f" format (value.toDouble / methodsCount * 100d)

        // CALL GRAPH STUFF
        val newOpaCG = opaProject.get(CHACallGraphKey).callGraph
        // CALL GRAPH STUFF

        val opaEP = opaStore.entities { (p: Property) ⇒
            p == IsEntryPoint
        }

        val cbs = project.get(CallBySignatureResolutionKey)
        println(cbs.statistics)

        BasicReport(
            s"#entry points: ${opaEP.size}\n"+
                s"percentage   : ${getPercentage(opaEP.size)}"+
                s"#call edges  : ${newOpaCG.callEdgesCount}"
        )
    }
}