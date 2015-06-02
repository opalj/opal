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
package org.opalj.fp
package analyses

import scala.language.postfixOps

import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import java.net.URL
import org.opalj.br.Method

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
        project: Project[URL],
        parameters: Seq[String],
        isInterrupted: () ⇒ Boolean): BasicReport = {

        // The purity analysis requires information about the actual mutability
        // of private static non-final fields. Hence, we have to schedule the
        // respective analysis. (Technically, it would be possible to schedule
        // it afterwards, but that doesn't make sense.)
        println("Starting Mutability Analysis")
        MutablityAnalysis.analyze(project)
        println("Starting Purity Analysis")
        PurityAnalysis.analyze(project)

        val projectStore = project.get(SourceElementsPropertyStoreKey)
        projectStore.waitOnPropertyComputationCompletion()
        projectStore.terminateSuspendedComputations()

        val pureEntities: Traversable[(AnyRef, Property)] = projectStore(Purity.Key)
        //            projectStore(Purity.Key).filter { ep ⇒
        //                val purity = ep._2
        //                (purity == Pure || purity == ConditionallyPure)
        //            }
        val pureMethods: Traversable[(Method, Property)] =
            pureEntities.map(e ⇒ (e._1.asInstanceOf[Method], e._2))
        val pureMethodsAsStrings =
            pureMethods.map(m ⇒ m._2+" >> "+m._1.toJava(project.classFile(m._1)))

        BasicReport(
            pureMethodsAsStrings.
                toList.
                sorted.
                mkString(
                    "\nPure methods:\n",
                    "\n",
                    s"\nTotal: ${pureMethods.size}\n$projectStore")
        )
    }
}

