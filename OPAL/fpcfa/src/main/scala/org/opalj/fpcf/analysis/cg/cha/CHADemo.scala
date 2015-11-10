package org.opalj
package fpcf
package analysis
package cg
package cha

import java.net.URL
import org.opalj.br.analyses.{BasicReport, CallBySignatureResolutionKey, DefaultOneStepAnalysis, Project, SourceElementsPropertyStoreKey}
import org.opalj.fpcf.analysis.demo.AnalysisModeConfigFactory
import org.opalj.br.Method

object CHADemo extends DefaultOneStepAnalysis {

    override def title: String = "Test stuff."

    override def description: String =
        ""

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val entryPointInfo = false

        val opaProject = AnalysisModeConfigFactory.resetAnalysisMode(project, AnalysisModes.OPA)
        val cpaProject = AnalysisModeConfigFactory.resetAnalysisMode(project, AnalysisModes.CPA)
        //        val appProject = AnalysisModeConfigFactory.updateProject(project, AnalysisModes.APP)
        // not supported yet

        val oldEntryPoints = CallGraphFactory.defaultEntryPointsForLibraries(project)
        val cpaStore = cpaProject.get(SourceElementsPropertyStoreKey)
        val opaStore = opaProject.get(SourceElementsPropertyStoreKey)
        //        val appStore = appProject.get(SourceElementsPropertyStoreKey)

        val methodsCount: Double = project.projectMethodsCount.toDouble
        def getPercentage(value: Int): String = "%1.2f" format (value.toDouble / methodsCount * 100d)

        // CALL GRAPH STUFF

        println(" Started Construction of OLD CHA call graph")
        val traditionalCG = project.get(org.opalj.ai.analyses.cg.CHACallGraphKey).callGraph
        println(" Finished Construction of OLD CHA call graph")
        println(" Started Construction of CHA call graph under CPA")
        val newCpaCG = cpaProject.get(org.opalj.fpcf.analysis.cg.cha.CHACallGraphKey).callGraph.asInstanceOf[CallBySignatureCallGraph]
        println(" Finished Construction of CHA call graph under CPA")
        println(" Started Construction of CHA call graph under OPA")
        val newOpaCG = opaProject.get(org.opalj.fpcf.analysis.cg.cha.CHACallGraphKey).callGraph.asInstanceOf[CallBySignatureCallGraph]
        println(" Finished Construction of CHA call graph under OPA")
        //        val wrongCG = appProject.get(LibraryCHACallGraphKey).callGraph
        // CALL GRAPH STUFF

        val cpaEP = cpaStore.collect { case (m: Method, IsEntryPoint) if m.body.nonEmpty ⇒ m }.toSet

        val opaEP = opaStore.collect { case (m: Method, IsEntryPoint) if m.body.nonEmpty ⇒ m }.toSet

        val cbs = project.get(CallBySignatureResolutionKey)

        println(cbs.statistics)
        val differenceCpa = cpaEP -- oldEntryPoints
        val differenceOpa = opaEP -- oldEntryPoints

        if (entryPointInfo) {
            println("\n\nEntryPoints not detected by the old appoach. (see CallGraphFactory for details)")
            println("\n SIZE: "+differenceCpa.size)
            println(differenceCpa.collect {
                case m: org.opalj.br.Method ⇒
                    val cf = project.classFile(m)
                    cf.thisType.toJava+" with method: "+m.descriptor.toJava(m.name)
            }.mkString("\n\n", "\n", "\n\n"))

            println("\n\nEntryPoints difference between old and OPA EntryPoints."+
                "\n these are in the OPA set but not in the old one! (see CallGraphFactory for details)")
            println("\n SIZE: "+differenceOpa.size)
            println(differenceOpa.collect {
                case m: org.opalj.br.Method ⇒
                    val cf = project.classFile(m)
                    cf.thisType.toJava+" with method: "+m.descriptor.toJava(m.name)
            }.mkString("\n\n", "\n", "\n\n"))

            println("\n-------------------- END OF ENTRY POINT INFORMATION ------------------------\n")
        }

        println(s"\n\nOPA: ${newOpaCG.callBySignatureCount}\n")
        println(s"CPA: ${newCpaCG.callBySignatureCount}\n\n")

        val outputTable = s"\n\n#methods: ${methodsCount}\n"+
            s"#entry points: | ${oldEntryPoints.size} (old)     | ${opaEP.size} (opa) v     | ${cpaEP.size} (cpa)\n"+
            s"percentage:    | ${getPercentage(oldEntryPoints.size)}% (old)     | ${getPercentage(opaEP.size)}% (opa) | ${getPercentage(cpaEP.size)}% (cpa)\n"+
            s"#call edges:   | ${traditionalCG.callEdgesCount} (old)     | ${newOpaCG.callEdgesCount} (opa) | ${newCpaCG.callEdgesCount} (cpa)| ${newCpaCG.callEdgesCount - traditionalCG.callEdgesCount} (new - old)" //+
        //s" | ${wrongCG.callEdgesCount} (old graph with new entry points)\n"

        BasicReport(
            outputTable
        )
    }
}