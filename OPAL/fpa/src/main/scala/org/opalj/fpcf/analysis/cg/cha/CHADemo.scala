package org.opalj
package fpcf
package analysis
package cg
package cha

import java.net.URL

import org.opalj.br.analyses.{BasicReport, CallBySignatureResolutionKey, DefaultOneStepAnalysis, Project, SourceElementsPropertyStoreKey}
import org.opalj.fpcf.Property

object CHADemo extends DefaultOneStepAnalysis {

    override def title: String = "Test stuff."

    override def description: String =
        ""

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val oldEntryPoints = CallGraphFactory.defaultEntryPointsForLibraries(project)
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

        val difference = entryPoints -- oldEntryPoints

        println("\n\n SIZE: "+difference.size+"\n\n")

        println(difference.collect {
            case m: org.opalj.br.Method ⇒
                val cf = project.classFile(m)
                cf.thisType.toJava+" with method: "+m.descriptor.toJava(m.name)
        }.mkString("\n\n", "\n", "\n\n"))

        val outputTable = s"\n\n#methods: ${project.methodsCount}\n"+
            s"#entry points: | ${oldEntryPoints.size} (old)     | ${entryPoints.size} (new)\n"+
            s"percentage:    | ${getPercentage(oldEntryPoints.size)}% (old)     | ${getPercentage(entryPoints.size)}% (new)\n"+
            s"#call edges:   | ${traditionalCG.callEdgesCount} (old)     | ${newCG.callEdgesCount} (new)| ${newCG.callEdgesCount - traditionalCG.callEdgesCount} (new - old)"+
            s" | ${wrongCG.callEdgesCount} (old graph with new entry points)\n"

        BasicReport(
            outputTable
        )
    }
}