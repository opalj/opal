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
package br
package analyses
package fp

import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import java.net.URL
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.fp.Property
import org.opalj.br.Method

/**
 * @author Michael Reif
 */
object FactoryAnalysisDemo extends DefaultOneStepAnalysis {

    override def title: String =
        "factory method computation"

    override def description: String =
        "determines the factory methods of a library"

    override def doAnalyze(
        project: Project[URL],
        parameters: Seq[String],
        isInterrupted: () ⇒ Boolean): BasicReport = {

        // RECOMMENDED
        // The factory method analysis requires information about the accessibility
        // of static methods. Hence, we have to schedule the
        // respective analysis. (Technically, it would be possible to schedule
        // it afterwards, but that doesn't make sense.)
        ShadowingAnalysis.analyze(project)

        val projectStore = project.get(SourceElementsPropertyStoreKey)

        var analysisTime = org.opalj.util.Seconds.None
        org.opalj.util.PerformanceEvaluation.time {
            val fmat = new Thread(new Runnable { def run = FactoryMethodAnalysis.analyze(project) });
            fmat.start
            fmat.join
            projectStore.waitOnPropertyComputationCompletion( /*default: true*/ )
        } { t ⇒ analysisTime = t.toSeconds }

        val factoryMethods: Traversable[(AnyRef, Property)] =
            projectStore(FactoryMethod.Key).filter { ep ⇒
                val isFactoryMethod = ep._2
                isFactoryMethod == IsFactoryMethod
            }

        val factoryMethodsInfo = factoryMethods.map { e ⇒
            val method = e._1.asInstanceOf[Method]
            val classFile = project.classFile(method)
            classFile.thisType.toJava+" "+method.name
        }

        BasicReport(factoryMethodsInfo.mkString(
            "\nfactory methods:\n\n\t",
            "\n\t",
            s"\nTotal: ${factoryMethods.size}\n\n") +
            projectStore+
            "\nAnalysis time: "+analysisTime

        )
    }
}