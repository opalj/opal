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
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.ClassFile
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.fpcf.Property
import org.opalj.fpcf.analysis.FPCFAnalysisManagerKey
import org.opalj.fpcf.analysis.Instantiability
import org.opalj.fpcf.analysis.Instantiable
import org.opalj.fpcf.analysis.SimpleInstantiabilityAnalysis

/**
 * @author Michael Reif
 */
object SimpleInstantiabilityAnalysisDemo extends DefaultOneStepAnalysis {

    override def title: String = "class instantiablility computation"

    override def description: String = "determines the instantiable classes of a library/application"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val propertyStore = project.get(SourceElementsPropertyStoreKey)
        val executer = project.get(FPCFAnalysisManagerKey)
        var analysisTime = org.opalj.util.Seconds.None

        org.opalj.util.PerformanceEvaluation.time {

            executer.run(SimpleInstantiabilityAnalysis)

        } { t ⇒ analysisTime = t.toSeconds }

        val instantiableClasses: Traversable[(AnyRef, Property)] =
            propertyStore(Instantiability.Key).filter { ep ⇒
                val isInstantiable = ep._2
                isInstantiable == Instantiable
            }

        val classInfo = instantiableClasses.map { e ⇒
            val classFile = e._1.asInstanceOf[ClassFile]
            classFile.thisType.toJava
        }

        BasicReport(classInfo.mkString(
            "\ninstantiable classes:\n\n\t",
            "\n\t",
            s"\n# instantiable classes: ${instantiableClasses.size}\n"
        ) +
            s"\n #classes: ${project.classFilesCount}\n"+
            "\nanalysis time: "+analysisTime)
    }
}