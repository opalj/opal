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

import java.net.URL
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.Method
import org.opalj.br.Field

/**
 * Demonstrates how to run the purity analysis.
 *
 * @author Michael Eichberg
 */
object PurityAnalysisDemo extends DefaultOneStepAnalysis {

    override def title: String =
        "determines those methods that are pure"

    override def description: String =
        "identifies method which are pure; i.e. which just operate on the passed parameters"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val projectStore = project.get(SourceElementsPropertyStoreKey)

        // We immediately also schedule the purity analysis to improve the
        // parallelization!

        val manager = project.get(FPCFAnalysisManagerKey)

        var analysisTime = org.opalj.util.Seconds.None
        org.opalj.util.PerformanceEvaluation.time {
            manager.runAll(MutabilityAnalysis, PurityAnalysis)
        } { t ⇒ analysisTime = t.toSeconds }

        val effectivelyFinalEntities: Traversable[(AnyRef, Property)] = projectStore(Mutability.key)
        val effectivelyFinalFields: Traversable[(Field, Property)] =
            effectivelyFinalEntities.map(e ⇒ (e._1.asInstanceOf[Field], e._2))
        val effectivelyFinalFieldsAsStrings =
            effectivelyFinalFields.map(f ⇒ f._2+" >> "+f._1.toJava(project.classFile(f._1)))

        val pureEntities: Traversable[(AnyRef, Property)] =
            projectStore(Purity.key)
        //            projectStore(Purity.Key).filter { ep ⇒
        //                val purity = ep._2
        //                (purity == Pure || purity == ConditionallyPure)
        //            }
        val pureMethods: Traversable[(Method, Property)] =
            pureEntities.map(e ⇒ (e._1.asInstanceOf[Method], e._2))
        val pureMethodsAsStrings =
            pureMethods.map(m ⇒ m._2+" >> "+m._1.toJava(project.classFile(m._1)))

        val fieldInfo =
            effectivelyFinalFieldsAsStrings.toList.sorted.mkString(
                "\nMutability of private static non-final fields:\n",
                "\n",
                s"\nTotal: ${effectivelyFinalFields.size}\n"
            )

        val methodInfo =
            pureMethodsAsStrings.toList.sorted.mkString(
                "\nPure methods:\n",
                "\n",
                s"\nTotal: ${pureMethods.size}\n"
            )
        BasicReport(fieldInfo + methodInfo + projectStore+"\nAnalysis time: "+analysisTime)
    }
}

