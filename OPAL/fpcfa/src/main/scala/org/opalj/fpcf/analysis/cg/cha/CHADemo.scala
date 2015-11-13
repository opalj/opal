package org.opalj
package fpcf
package analysis
package cg
package cha

import java.net.URL
import org.opalj.br.analyses.{BasicReport, CallBySignatureResolutionKey, DefaultOneStepAnalysis, Project, SourceElementsPropertyStoreKey}
import org.opalj.fpcf.analysis.demo.AnalysisModeConfigFactory
import org.opalj.br.Method
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC

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
        val cgDiff = false
        val instantiatedCLassesInfo = false

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

        val traditionalCG = project.get(org.opalj.ai.analyses.cg.CHACallGraphKey).callGraph
        val newCpaCG = cpaProject.get(org.opalj.fpcf.analysis.cg.cha.CHACallGraphKey).callGraph.asInstanceOf[CallBySignatureCallGraph]
        val newOpaCG = opaProject.get(org.opalj.fpcf.analysis.cg.cha.CHACallGraphKey).callGraph.asInstanceOf[CallBySignatureCallGraph]

        // ENTRY POINT INFO

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

        // CALL GRAPH DIFF

        if (cgDiff) {
            val (less, additional) = org.opalj.ai.analyses.cg.CallGraphComparison(project, traditionalCG, newOpaCG)

            println(less.mkString("\n\nEXPECTED:", "\n", "\n\n"))
            //            println(additional.mkString("\n\nUNEXPECTED:", "\n", "\n\n"))
            additional.mkString("\n\nUNEXPECTED:", "\n", "\n\n")
        }

        println(s"\n\nOPA: ${newOpaCG.callBySignatureCount}\n")
        println(s"CPA: ${newCpaCG.callBySignatureCount}\n\n")

        if (instantiatedCLassesInfo) {
            val opaNon = opaProject.get(InstantiableClassesIndexKey).notInstantiable
            val cpaNon = cpaProject.get(InstantiableClassesIndexKey).notInstantiable
            println("nonInstClasses(OPA): "+opaNon.size)
            println("nonInstClasses(CPA): "+cpaNon.size)
            println((cpaNon -- opaNon).mkString("\n"))
        }

        val callBySignaturePossibilities = true
        if (callBySignaturePossibilities) {
            println("OPA STATISTICS\n\n"+opaProject.get(CallBySignatureResolutionKey).statistics()+"\n\n")
            println("CPA STATISTICS\n\n"+cpaProject.get(CallBySignatureResolutionKey).statistics()+"\n\n")
        }

        val callSiteInfo = true
        if (callSiteInfo) {
            // 0 = virtual, 1 = interface, 2 = special, 3 = static 
            val map = scala.collection.mutable.Map[Int, Int](0 → 0, 1 → 0, 2 → 0, 3 → 4)
            def incCallNum(callType: Int): Unit = {
                map.get(callType) match {
                    case Some(count) ⇒ map.update(callType, count + 1)
                    case _           ⇒
                }
            }

            for {
                cf ← project.allClassFiles
                m ← cf.methods if m.body.isDefined
            } {
                m.body.get.foreach { (pc, instruction) ⇒
                    instruction.opcode match {
                        case INVOKEVIRTUAL.opcode   ⇒ incCallNum(0)
                        case INVOKEINTERFACE.opcode ⇒ incCallNum(1)
                        case INVOKESPECIAL.opcode   ⇒ incCallNum(2)
                        case INVOKESTATIC.opcode    ⇒ incCallNum(3)
                        case _                      ⇒
                    }
                }
            }

            println("0 = virtual, 1 = interface, 2 = special, 3 = static")
            println(map.toSeq.sortBy(_._1).mkString("\n\ninvocation counts:\n\n", "\n", "\n\n"))
        }

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