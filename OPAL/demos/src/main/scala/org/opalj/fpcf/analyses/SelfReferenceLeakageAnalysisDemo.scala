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
package analyses

import java.net.URL

import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.ClassFile
import org.opalj.fpcf.properties.SelfReferenceLeakage
import org.opalj.fpcf.properties.DoesNotLeakSelfReference

/**
 * Runs the default self-reference leakage analysis.
 *
 * @author Michael Eichberg
 */
object SelfReferenceLeakageAnalysisDemo extends DefaultOneStepAnalysis {

    override def title: String = "Analyses whether a class leaks it self-reference this"

    override def description: String = {
        "Determines if a class leaks its self reference, if not, then the method which instantiates the object has full control."
    }

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val projectStore = project.get(PropertyStoreKey)

        var analysisTime = Seconds.None
        time {
            projectStore.setupPhase(Set(SelfReferenceLeakage.Key))
            L0SelfReferenceLeakageAnalysis.start(project)
            projectStore.waitOnPhaseCompletion()
        } { t ⇒ analysisTime = t.toSeconds }

        val notLeakingEntities: Iterator[EPS[Entity, SelfReferenceLeakage]] =
            projectStore.entities(SelfReferenceLeakage.Key) filter { eps ⇒
                eps.lb == DoesNotLeakSelfReference
            }
        val notLeakingClasses = notLeakingEntities map { eps ⇒
            val classFile = eps.e.asInstanceOf[ClassFile]
            val classType = classFile.thisType
            val className = classFile.thisType.toJava
            if (project.classHierarchy.isInterface(classType).isYes)
                "interface "+className
            else
                "class "+className
        }

        val leakageInfo =
            notLeakingClasses.toList.sorted.mkString(
                "\nClasses not leaking self reference:\n",
                "\n",
                s"\nTotal: ${notLeakingEntities.size}\n"
            )
        BasicReport(leakageInfo + projectStore+"\nAnalysis time: "+analysisTime)
    }
}
