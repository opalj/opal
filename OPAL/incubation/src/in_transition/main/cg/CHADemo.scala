/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg
package cha

import java.net.URL

import org.opalj.br.analyses.{BasicReport, DefaultOneStepAnalysis, Project, PropertyStoreKey}
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.Method
import org.opalj.br.analyses.AnalysisModeConfigFactory
import org.opalj.util.PerformanceEvaluation
import org.opalj.fpcf.properties.IsEntryPoint
import org.opalj.ai.analyses.cg.CallGraphFactory
import org.opalj.ai.analyses.cg.CallGraph
import org.opalj.br.analyses.cg.InstantiableClassesKey

object CHADemo extends DefaultOneStepAnalysis {

    override def title: String = "Test stuff."

    override def description: String = ""

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val entryPointInfo = false
        val cgDiff = false
        val instantiatedClassesInfo = false

        val opaProject = AnalysisModeConfigFactory.resetAnalysisMode(project, AnalysisModes.OPA)
        val cpaProject = AnalysisModeConfigFactory.resetAnalysisMode(project, AnalysisModes.CPA)
        //        val appProject = AnalysisModeConfigFactory.updateProject(project, AnalysisModes.APP)
        // not supported yet

        val oldEntryPoints = CallGraphFactory.defaultEntryPointsForLibraries(project)
        val cpaStore = cpaProject.get(PropertyStoreKey)
        val opaStore = opaProject.get(PropertyStoreKey)
        //        val appStore = appProject.get(PropertyStoreKey)

        val methodsCount: Double = project.projectMethodsCount.toDouble
        def getPercentage(value: Int): String = "%1.2f" format (value.toDouble / methodsCount * 100d)

        var traditionalCG: CallGraph = null

        // CALL GRAPH STUFF
        PerformanceEvaluation.time {
            traditionalCG = project.get(CHACallGraphKey).callGraph
        } { t ⇒ println("naive CHA computation time: "+t.toSeconds) }

        //        println(GlobalPerformanceEvaluation.getTime('cbs).toSeconds.toString(true))
        //        println(GlobalPerformanceEvaluation.getTime('cbst).toSeconds.toString(true))

        var opaCG: CallGraph = null

        PerformanceEvaluation.time {
            opaCG = opaProject.get(org.opalj.fpcf.analyses.cg.cha.CHACallGraphKey).callGraph
        } { t ⇒ println("OPA-CHA computation time: "+t.toSeconds) }
        //
        //        println("OPA (cbs resolution index): "+GlobalPerformanceEvaluation.getTime('cbs).toSeconds.toString(true))
        //        println("OPA (cbs analysis): "+GlobalPerformanceEvaluation.getTime('cbst).toSeconds.toString(true))
        //        println("OPA (entry points): "+GlobalPerformanceEvaluation.getTime('ep).toSeconds.toString(true))
        //        println("OPA (clientCallable): "+GlobalPerformanceEvaluation.getTime('callableByOthers).toSeconds.toString(true))
        //        println("OPA (method accessibility): "+GlobalPerformanceEvaluation.getTime('methodAccess).toSeconds.toString(true))
        //        println("OPA (instantiable classes index): "+GlobalPerformanceEvaluation.getTime('inst).toSeconds.toString(true))
        //        println("OPA (cg construction): "+GlobalPerformanceEvaluation.getTime('const).toSeconds.toString(true))
        //        println("OPA (invoke virtual): \t - "+GlobalPerformanceEvaluation.getTime('invokevirtual).toSeconds.toString(true))
        //        println("OPA (invoke interface): \t - "+GlobalPerformanceEvaluation.getTime('invokeinterface).toSeconds.toString(true))
        //        println("OPA (invoke special): \t - "+GlobalPerformanceEvaluation.getTime('invokespecial).toSeconds.toString(true))
        //        println("OPA (invoke static): \t - "+GlobalPerformanceEvaluation.getTime('invokestatic).toSeconds.toString(true))
        //        println("OPA (cg builder): \t - "+GlobalPerformanceEvaluation.getTime('cgbuilder).toSeconds.toString(true)+"\n\n")
        //GlobalPerformanceEvaluation.resetAll()

        var cpaCG: CallGraph = null
        PerformanceEvaluation.time {
            cpaCG = cpaProject.get(CHACallGraphKey).callGraph
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

        //        val execpetions = opaCG.constructionExceptions.map(_.toFullString).mkString("Construction Exception\n\n", "\n", "\n")
        //        println(execpetions)

        // ENTRY POINT INFO

        val cpaEP = cpaStore.entities(IsEntryPoint).collect { case m: Method if m.body.isDefined ⇒ m }.toSet

        val opaEP = opaStore.entities(IsEntryPoint).collect { case m: Method if m.body.isDefined ⇒ m }.toSet

        if (entryPointInfo) {

            val differenceCpa = cpaEP -- oldEntryPoints
            val differenceOpa = opaEP -- oldEntryPoints
            println("\n\nEntryPoints not detected by the old appoach. (see CallGraphFactory for details)")
            println("\n SIZE: "+differenceCpa.size)
            println(differenceCpa.collect {
                case m: org.opalj.br.Method ⇒
                    val cf = m.classFile
                    cf.thisType.toJava+" with method: "+m.descriptor.toJava(m.name)
            }.mkString("\n\n", "\n", "\n\n"))

            println("\n\nEntryPoints difference between old and OPA EntryPoints."+
                "\n these are in the OPA set but not in the old one! (see CallGraphFactory for details)")
            println("\n SIZE: "+differenceOpa.size)
            println(differenceOpa.collect {
                case m: org.opalj.br.Method ⇒
                    val cf = m.classFile
                    cf.thisType.toJava+" with method: "+m.descriptor.toJava(m.name)
            }.mkString("\n\n", "\n", "\n\n"))

            println("\n-------------------- END OF ENTRY POINT INFORMATION ------------------------\n")
        }

        // CALL GRAPH DIFF

        if (cgDiff) {
            val (unexpected, expected) = org.opalj.ai.analyses.cg.CallGraphComparison(project, opaCG, opaCG)

            println(unexpected.mkString("\n\nUNEXPECTED:", "\n\n", "\n\n"))
            //            println(additional.mkString("\n\nUNEXPECTED:", "\n", "\n\n"))
            println(expected.mkString("\n\nADDITIONAL:", "\n\n", "\n\n"))
        }

        //        println(s"\n\nOPA: ${newOpaCG.callBySignatureCount}\n")
        //        println(s"CPA: ${newCpaCG.callBySignatureCount}\n\n")

        if (instantiatedClassesInfo) {
            val opaNon = opaProject.get(InstantiableClassesKey).notInstantiable
            val cpaNon = cpaProject.get(InstantiableClassesKey).notInstantiable
            println("nonInstClasses(OPA): "+opaNon.size)
            println("nonInstClasses(CPA): "+cpaNon.size)
            println((cpaNon -- opaNon).mkString("\n"))
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
                (pc, instruction) ← m.body.get
            } {
                instruction.opcode match {
                    case INVOKEVIRTUAL.opcode   ⇒ incCallNum(0)
                    case INVOKEINTERFACE.opcode ⇒ incCallNum(1)
                    case INVOKESPECIAL.opcode   ⇒ incCallNum(2)
                    case INVOKESTATIC.opcode    ⇒ incCallNum(3)
                    case _                      ⇒
                }
            }

            println("0 = virtual, 1 = interface, 2 = special, 3 = static")
            println(map.toSeq.sortBy(_._1).mkString("\n\ninvocation counts:\n\n", "\n", "\n\n"))
        }

        val outputTable = s"\n\n#methods: ${methodsCount}\n"+
            s"#entry points: | ${oldEntryPoints.size} (old)     | ${opaEP.size} (opa) v     | ${cpaEP.size} (cpa)\n"+
            s"percentage:    | ${getPercentage(oldEntryPoints.size)}% (old)     | ${getPercentage(opaEP.size)}% (opa) | ${getPercentage(cpaEP.size)}% (cpa)\n"+
            s"#call edges:   | ${traditionalCG.callEdgesCount} (old)     | ${opaCG.callEdgesCount} (opa) | ${cpaCG.callEdgesCount} (cpa)| ${opaCG.callEdgesCount - traditionalCG.callEdgesCount} (new - old)" //+
        //s" | ${wrongCG.callEdgesCount} (old graph with new entry points)\n"

        BasicReport(
            outputTable
        )
    }
}
