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
import org.opalj.fpcf.properties.Pure
import org.opalj.fpcf.properties.Purity

/**
 * Runs the purity analysis including all analyses that may improve the overall result.
 *
 * @author Michael Eichberg
 */
object PurityAnalysisRunner extends DefaultOneStepAnalysis {

    override def title: String = "assess the purity of methods"

    override def description: String = { "assess the purity of some methods" }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val projectStore = project.get(PropertyStoreKey)

        org.opalj.fpcf.analysis.FieldMutabilityAnalysis.start(project, projectStore)

        projectStore.debug = true
        org.opalj.fpcf.analysis.PurityAnalysis.start(project, projectStore)

        projectStore.waitOnPropertyComputationCompletion(true)

        val pureEntities: Traversable[EP[Entity, Purity]] = projectStore.entities(Purity.key)
        val pureMethods: Traversable[(Method, Property)] =
            pureEntities.map(e ⇒ (e._1.asInstanceOf[Method], e._2))
        val pureMethodsAsStrings =
            pureMethods.map(m ⇒ m._2+" >> "+m._1.toJava(project.classFile(m._1)))

        val methodInfo =
            pureMethodsAsStrings.toList.sorted.mkString(
                "\nPure methods:\n",
                "\n",
                s"\nTotal: ${pureMethods.size}\n"
            )

        val result = methodInfo +
            projectStore.toString(false)+
            "\nPure methods: "+pureMethods.filter(m ⇒ m._2 == Pure).size

        BasicReport(result)
    }
}
