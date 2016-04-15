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
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.Method
import org.opalj.br.Field
import org.opalj.fpcf.analysis.fields.FieldMutability
import org.opalj.fpcf.analysis.fields.FieldMutabilityAnalysis
import org.opalj.fpcf.analysis.methods.PurityAnalysis
import org.opalj.fpcf.analysis.methods.Purity
import org.opalj.fpcf.analysis.methods.Pure

/**
 * Runs the purity analysis including all analyses that may improve the overall result.
 *
 * @author Michael Eichberg
 */
object PurityAnalysisDemo extends DefaultOneStepAnalysis {

    override def title: String = "determines those methods that are pure"

    override def description: String =
        "identifies method which are pure; i.e. which just operate on the passed parameters"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val projectStore = project.get(SourceElementsPropertyStoreKey)
        projectStore.debug = false

        // We immediately also schedule the purity analysis to improve the
        // parallelization!

        val manager = project.get(FPCFAnalysesManagerKey)

        var t = Seconds.None
        time { manager.runAll(FieldMutabilityAnalysis, PurityAnalysis) } { r ⇒ t = r.toSeconds }

        val effectivelyFinalEntities: Traversable[EP[Entity, FieldMutability]] =
            projectStore.entities(FieldMutability.key)

        val effectivelyFinalFields: Traversable[(Field, Property)] =
            effectivelyFinalEntities.map(ep ⇒ (ep.e.asInstanceOf[Field], ep.p))

        val effectivelyFinalFieldsAsStrings =
            effectivelyFinalFields.map(f ⇒ f._2+" >> "+f._1.toJava(project.classFile(f._1)))

        val pureEntities: Traversable[EP[Entity, Purity]] = projectStore.entities(Purity.key)
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
        BasicReport(
            fieldInfo +
                methodInfo +
                projectStore.toString(false)+
                "\nPure methods: "+pureMethods.filter(m ⇒ m._2 == Pure).size+
                "\nAnalysis time: "+t
        )
    }
}
