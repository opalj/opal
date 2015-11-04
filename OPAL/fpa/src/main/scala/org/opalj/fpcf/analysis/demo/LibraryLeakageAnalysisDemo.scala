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
package demo

import org.opalj.fpcf.MethodAnalysisDemo
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import java.net.URL
import org.opalj.log.OPALLogger
import org.opalj.log.GlobalLogContext
import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.Warn
import org.opalj.fpcf.Property
import org.opalj.fpcf.analysis.FPCFAnalysisManagerKey
import org.opalj.fpcf.analysis.Leakage
import org.opalj.fpcf.analysis.LibraryLeakageAnalysis
import org.opalj.fpcf.analysis.NoLeakage

/**
 * @author Michael Reif
 */
object LibraryLeakageAnalysisDemo extends MethodAnalysisDemo {

    override def title: String =
        "method leakage analysis"

    override def description: String =
        "determines if the method is exposed to the client via subclasses"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        OPALLogger.updateLogger(GlobalLogContext, new ConsoleOPALLogger(true, Warn))

        val propertyStore = project.get(SourceElementsPropertyStoreKey)
        val executer = project.get(FPCFAnalysisManagerKey)

        var analysisTime = org.opalj.util.Seconds.None
        org.opalj.util.PerformanceEvaluation.time {
            executer.run(LibraryLeakageAnalysis)
        } { t ⇒ analysisTime = t.toSeconds }

        //        val notLeakedMethods = entitiesByProperty(NoLeakage)(propertyStore)
        //        val notLeakedMethodsInfo = buildMethodInfo(notLeakedMethods)(project) filter { str ⇒ str.trim.startsWith("public java.") }
        //
        //        val nonOverriddenInfoString = finalReport(notLeakedMethodsInfo, "Found non-overridden methods")

        val leakedMethods = propertyStore.entities { (p: Property) ⇒
            p == Leakage
        }

        val notLeakedMethods2 = propertyStore.entities { (p: Property) ⇒
            p == NoLeakage
        }
        BasicReport(
            //            nonOverriddenInfoString +
            propertyStore.toString+
                "\nAnalysis time: "+analysisTime +
                s"\nleaked: ${leakedMethods.size}"+
                s"\n not leaked: ${notLeakedMethods2.size}"
        )
    }
}