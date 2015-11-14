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
import org.opalj.fpcf.Property
import org.opalj.fpcf.analysis.demo.AnalysisModeConfigFactory
import org.opalj.log.OPALLogger
import org.opalj.log.GlobalLogContext
import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.Warn

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
        val cpaStore = cpaProject.get(SourceElementsPropertyStoreKey)

        val methodsCount: Double = project.methodsCount.toDouble
        def getPercentage(value: Double): String = "%1.2f" format (value / methodsCount * 100d)

        val newCpaCG = cpaProject.get(CHACallGraphKey).callGraph

        val cpaEP = cpaStore.entities { (p: Property) ⇒ p == IsEntryPoint }

        val cbs = project.get(CallBySignatureResolutionKey)
        println(cbs.statistics)

        BasicReport(
            s"#entry points    : ${cpaEP.size}\n"+
                s"percentage   : ${getPercentage(cpaEP.size.toDouble)}\n"+
                s"#call edges  : ${newCpaCG.callEdgesCount}\n"
        )
    }
}