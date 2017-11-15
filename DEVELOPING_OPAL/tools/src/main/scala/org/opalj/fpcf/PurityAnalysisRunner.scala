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

import java.net.URL

import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.Method
import org.opalj.fpcf.analyses.AdvancedFieldMutabilityAnalysis
import org.opalj.fpcf.analyses.ClassImmutabilityAnalysis
import org.opalj.fpcf.analyses.TypeImmutabilityAnalysis
import org.opalj.fpcf.analyses.MethodPurityAnalysis
import org.opalj.fpcf.analyses.FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.PurityAnalysis
import org.opalj.fpcf.properties.Pure
import org.opalj.fpcf.properties.Purity
import org.opalj.fpcf.properties.SideEffectFree

/**
 * Runs the purity analysis including all analyses that may improve the overall result.
 *
 * @author Michael Eichberg
 */
object PurityAnalysisRunner extends DefaultOneStepAnalysis {

    final val MPAParam = "-analysis=MethodPurityAnalysis"

    override def title: String = "assess the purity of methods"

    override def description: String = { "assess the purity of some methods" }

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Traversable[String] = {
        super.checkAnalysisSpecificParameters(parameters.filter(_ != MPAParam))
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val propertyStore = project.get(PropertyStoreKey)

        if (parameters.contains(MPAParam)) {
            ClassImmutabilityAnalysis.start(project, propertyStore)
            TypeImmutabilityAnalysis.start(project, propertyStore)
            AdvancedFieldMutabilityAnalysis.start(project, propertyStore)
            MethodPurityAnalysis.start(project, propertyStore)
        } else {
            FieldMutabilityAnalysis.start(project, propertyStore)
            PurityAnalysis.start(project, propertyStore)
        }

        propertyStore.waitOnPropertyComputationCompletion(true)

        val pureEntities: Traversable[EP[Entity, Purity]] = propertyStore.entities(Purity.key)
        val pureMethods: Traversable[(Method, Property)] =
            pureEntities.map(e ⇒ (e._1.asInstanceOf[Method], e._2))
        val pureMethodsAsStrings = pureMethods.map(m ⇒ m._2+" >> "+m._1.toJava)

        val methodInfo =
            pureMethodsAsStrings.toList.sorted.mkString(
                "\nPure methods:\n",
                "\n",
                s"\nTotal: ${pureMethods.size}\n"
            )

        val result = methodInfo +
            propertyStore.toString(false)+
            "\nPure methods:             "+pureMethods.count(m ⇒ m._2 == Pure)+
            "\nSide-effect free methods: "+pureMethods.count(m ⇒ m._2 == SideEffectFree)

        BasicReport(result)
    }
}
