/* BSD 2Clause License:
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

import java.net.URL
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.br.Method
import org.opalj.bi.VisibilityModifier
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_PRIVATE
import org.opalj.bi.ACC_PROTECTED

/**
 * @author Michael Reif
 */
object ShadowingAnalysisDemo
        extends DefaultOneStepAnalysis {

    override def title: String =
        "entry point set computation"

    override def description: String =
        "determins the entry point set of an library by the configured analysis mode"

    override def doAnalyze(
        project: Project[URL],
        parameters: Seq[String],
        isInterrupted: () ⇒ Boolean): BasicReport = {

        val projectStore = project.get(SourceElementsPropertyStoreKey)

        var analysisTime = org.opalj.util.Seconds.None
        org.opalj.util.PerformanceEvaluation.time {
            StaticMethodAccessibilityAnalysisEx(project)
            projectStore.waitOnPropertyComputationCompletion( /*default: true*/ )
        } { t ⇒ analysisTime = t.toSeconds }

        val nonVisibleByClient: Traversable[(AnyRef, Property)] =
            projectStore(ProjectAccessibility.Key).filter { ep ⇒
                val shadowed = ep._2
                shadowed == PackageLocal
            }

        def methodVisibility(modifier: Option[VisibilityModifier]): String = {
            modifier match {
                case Some(ACC_PUBLIC)    ⇒ "+"
                case Some(ACC_PRIVATE)   ⇒ "-"
                case Some(ACC_PROTECTED) ⇒ "~"
                case None                ⇒ "#"
            }
        }

        def formatMethods(methods: Traversable[(AnyRef, Property)]): Traversable[String] = {

            methods.map { e ⇒
                val method = e._1.asInstanceOf[Method]
                val classFile = project.classFile(method)
                classFile.thisType.toJava+" "+methodVisibility(method.visibilityModifier)+" "+method.name
            }
        }

        //        val visibleMethods = formatMethods(visibleByClient)contains
        val nonVisibleMethods = formatMethods(nonVisibleByClient) //.filter { x ⇒ x.contains("java.util") }

        //        val visibleMethodInfo = visibleMethods.mkString(
        //            "\nMethods that are visible by the client:\n\n\t",
        //            "\n\t",
        //            s"\nTotal: ${visibleByClient.size}\n\n"
        //        )

        val nonVisibleMethodInfo = nonVisibleMethods.mkString(
            "\npackage local Methods that are not visible by the client:\n\n\t",
            "\n\t",
            s"\nTotal: ${nonVisibleByClient.size}\n\n"
        )

        BasicReport( //visibleMethodInfo +
            nonVisibleMethodInfo +
                projectStore+
                "\nAnalysis time: "+analysisTime)
    }

}