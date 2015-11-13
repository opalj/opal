/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
import org.opalj.br.MethodWithBody

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
        val cpaStore = cpaProject.get(SourceElementsPropertyStoreKey)
        val opaStore = opaProject.get(SourceElementsPropertyStoreKey)
        //        val appStore = appProject.get(SourceElementsPropertyStoreKey)

        val methodsCount: Double = project.projectMethodsCount.toDouble
        def getPercentage(value: Int): String = "%1.2f" format (value.toDouble / methodsCount * 100d)

        // CALL GRAPH STUFF

        val traditionalCG = project.get(org.opalj.ai.analyses.cg.CHACallGraphKey).callGraph

        val cpaCCG = cpaProject.get(org.opalj.fpcf.analysis.cg.cha.CHACallGraphKey)
        val opaCCG = opaProject.get(org.opalj.fpcf.analysis.cg.cha.CHACallGraphKey)

        val execpetions = opaCCG.constructionExceptions.map(_.toFullString).mkString("Construction Exception\n\n", "\n", "\n")
        println(execpetions)
        val newCpaCG = cpaCCG.callGraph.asInstanceOf[CallBySignatureCallGraph]
        val newOpaCG = opaCCG.callGraph.asInstanceOf[CallBySignatureCallGraph]

        // ENTRY POINT INFO

        val cpaEP = cpaStore.collect { case (m @ MethodWithBody(_), IsEntryPoint) ⇒ m }.toSet

        val opaEP = opaStore.collect { case (m @ MethodWithBody(_), IsEntryPoint) ⇒ m }.toSet

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
            val (unexpected, expected) = org.opalj.ai.analyses.cg.CallGraphComparison(project, newOpaCG, newCpaCG)

            println(unexpected.mkString("\n\nUNEXPECTED:", "\n\n", "\n\n"))
            //            println(additional.mkString("\n\nUNEXPECTED:", "\n", "\n\n"))
            println(expected.mkString("\n\nADDITIONAL:", "\n\n", "\n\n"))
        }

        println(s"\n\nOPA: ${newOpaCG.callBySignatureCount}\n")
        println(s"CPA: ${newCpaCG.callBySignatureCount}\n\n")

        if (instantiatedClassesInfo) {
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