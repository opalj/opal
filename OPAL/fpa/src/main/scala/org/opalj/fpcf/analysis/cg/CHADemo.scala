package org.opalj
package fpcf
package analysis
package cg

import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.ai.analyses.cg.CallGraphFactory
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.ai.analyses.cg.CHACallGraphKey
import java.net.URL
import org.opalj.br.analyses.CallBySignatureResolutionKey

object CHADemo extends DefaultOneStepAnalysis {

    override def title: String = "Test stuff."

    override def description: String =
        ""

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val oldEntryPoints = CallGraphFactory.defaultEntryPointsForLibraries(project).size
        val propertyStore = project.get(SourceElementsPropertyStoreKey)

        val methodsCount: Double = project.methodsCount.toDouble
        def getPercentage(value: Int): String = "%1.2f" format (value.toDouble / methodsCount * 100d)

        // CALL GRAPH STUFF

        val traditionalCG = project.get(CHACallGraphKey).callGraph
        val newCG = project.get(LibraryCHACallGraphKey).callGraph
        val wrongCG = project.get(ExpLibraryCHACallGraphKey).callGraph
        println("FINISHED CG CONSTRUCTION")
        // CALL GRAPH STUFF

        val entryPoints = propertyStore.entities { (p: Property) ⇒
            p == IsEntryPoint
        }

        val cbs = project.get(CallBySignatureResolutionKey)

        println(cbs.statistics)

        val outputTable = s"\n\n#methods: ${project.methodsCount}\n"+
            s"#entry points: | $oldEntryPoints (old)     | ${entryPoints.size} (new)\n"+
            s"percentage:    | ${getPercentage(oldEntryPoints)}% (old)     | ${getPercentage(entryPoints.size)}% (new)\n"+
            s"#call edges:   | ${traditionalCG.callEdgesCount} (old)     | ${newCG.callEdgesCount} (new)| ${traditionalCG.callEdgesCount - newCG.callEdgesCount} (old - new) | ${wrongCG.callEdgesCount}\n"

        BasicReport(
            outputTable
        )
    }
}