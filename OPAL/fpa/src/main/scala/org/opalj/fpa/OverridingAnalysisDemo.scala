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
package fpa

import org.opalj.fpa.demo.util.MethodAnalysisDemo
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import java.net.URL

/**
 * @author Michael Reif
 */
object OverridingAnalysisDemo extends MethodAnalysisDemo {

    override def title: String =
        "method override analysis"

    override def description: String =
        "determines the overriden methods of a library"

    override def doAnalyze(
        project: Project[URL],
        parameters: Seq[String],
        isInterrupted: () ⇒ Boolean): BasicReport = {

        val propertyStore = project.get(SourceElementsPropertyStoreKey)

        var analysisTime = org.opalj.util.Seconds.None
        org.opalj.util.PerformanceEvaluation.time {
            val fmat = new Thread(new Runnable { def run = OverridingAnalysis.analyze(project) });
            fmat.start
            fmat.join
            propertyStore.waitOnPropertyComputationCompletion( /*default: true*/ )
        } { t ⇒ analysisTime = t.toSeconds }

        val nonOverriddenMethods = entitiesByProperty(NonOverridden)(propertyStore)
        val nonOverriddenInfo = buildMethodInfo(nonOverriddenMethods)(project) filter { str ⇒ str.trim.startsWith("public java.") }

        //        val overriddenMethods = entitiesByProperty(IsOverridden)(propertyStore)
        //        val overriddenInfo = buildMethodInfo(overriddenMethods)(project) filter { str ⇒ str.trim.startsWith("java.") }

        val nonOverriddenInfoString = finalReport(nonOverriddenInfo, "Found non-overridden methods")
        //        val overriddenInfoString = finalReport(overriddenInfo, "Found overridden methods")

        BasicReport(
            //            overriddenInfoString +
            nonOverriddenInfoString +
                propertyStore+
                "\nAnalysis time: "+analysisTime
        )
    }
}