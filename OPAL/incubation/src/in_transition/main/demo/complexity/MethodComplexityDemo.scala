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
package complexity

import java.net.URL

import org.opalj.br.analyses.Project
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.fpcf.properties.MethodComplexity

/**
 * Demonstrates how to use an analysis that was developed using the FPCF framework.
 *
 * @author Michael Eichberg
 */
object MethodComplexityDemo extends DefaultOneStepAnalysis {

    override def title: String = "assesses the complexity of methods"

    override def description: String =
        """|a very simple assessment of a method's complexity that primarily serves
           |the goal to make decisions about those methods that may be inlined""".stripMargin('|')

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        implicit val theProject = project
        implicit val propertyStore = theProject.get(PropertyStoreKey)

        val analysis = new MethodComplexityAnalysis
        propertyStore.scheduleEagerComputationsForEntities(project.allMethodsWithBody) { m ⇒ Result(m, analysis(m)) }
        propertyStore.waitOnPropertyComputationCompletion(true)
        println(propertyStore.toString)
        val ratings = propertyStore.entities { p ⇒
            p match {
                case MethodComplexity(c) if c < Int.MaxValue ⇒ true
                case _                                       ⇒ false

            }
        }
        BasicReport(ratings.mkString("\n", "\n", s"\n${ratings.size} simple methods found - Done."))
    }
}
