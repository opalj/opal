/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package analyses
package cg
package cha

import java.net.URL
import org.opalj.ai.analyses.cg.ComputedCallGraph
import org.opalj.br.analyses.{BasicReport, DefaultOneStepAnalysis, Project, PropertyStoreKey}
import org.opalj.br.analyses.AnalysisModeConfigFactory
import org.opalj.util.PerformanceEvaluation
import org.opalj.fpcf.properties.IsEntryPoint

object CpaCHADemo extends DefaultOneStepAnalysis {

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
