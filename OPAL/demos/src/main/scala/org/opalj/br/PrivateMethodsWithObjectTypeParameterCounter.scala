/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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

import org.opalj.br.analyses.Project
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses.BasicReport
import java.net.URL

/**
 * Counts the number of private methods that have at least one parameter with
 * a reference type.
 *
 * @author Michael Eichberg
 */
object PrivateMethodsWithObjectTypeParameterCounter extends AnalysisExecutor with OneStepAnalysis[URL, BasicReport] {

    val analysis = this

    override def description: String =
        "Counts the number of package private and private methods with a body with at least one parameter that is an object type."

    def doAnalyze(project: Project[URL], params: Seq[String], isInterrupted: () ⇒ Boolean) = {
        val overallPotential = new java.util.concurrent.atomic.AtomicInteger(0)
        val methods = (
            for {
                classFile ← project.allClassFiles.par
                method ← classFile.methods
                if method.isPrivate //|| method.isPackagePrivate
                if method.name != "readObject" && method.name != "writeObject"
                potential = (method.descriptor.parameterTypes.collect {
                    case ot: ObjectType ⇒
                        project.classHierarchy.allSubtypes(ot, false).size
                    case _ ⇒
                        0
                }).sum
                if potential >= 5
            } yield {
                overallPotential.addAndGet(potential)
                classFile.thisType.toJava+
                    "{ "+
                    (if (method.isPrivate) "private " else "") + method.toJava+
                    " /* Potential: "+potential+" */ "+
                    "}"
            }
        ).seq

        BasicReport(
            methods.mkString(
                "Methods:\n\t",
                "\n\t",
                s"\n\t${methods.size} methods found with an overall refinement potential of ${overallPotential.get}.\n"
            )
        )
    }
}
