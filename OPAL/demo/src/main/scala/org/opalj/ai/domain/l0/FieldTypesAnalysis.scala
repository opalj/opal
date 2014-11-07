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
package ai
package domain
package l0

import java.net.URL
import org.opalj.br.analyses.{ OneStepAnalysis, AnalysisExecutor, BasicReport, Project }
import org.opalj.br.{ ClassFile, Method, Field, Code }
import org.opalj.br.{ ObjectType, FieldType }
import org.opalj.ai.Domain
import org.opalj.ai.Computation
import org.opalj.ai.ComputationWithSideEffectOnly
import org.opalj.ai.domain
import org.opalj.collection.immutable.UIDSet
import org.opalj.br.UpperTypeBound

/**
 * Demonstrates how to use OPAL's FieldTypesAnalysis.
 *
 * @author Michael Eichberg
 */
object FieldTypesAnalysis extends AnalysisExecutor {

    val analysis = new OneStepAnalysis[URL, BasicReport] {

        override def title: String =
            "Tries to derive more precise information about the fields of a class."

        override def description: String =
            "Identifies fields of a class where we can – statically – derive more precise type/value information."

        override def doAnalyze(
            theProject: Project[URL],
            parameters: Seq[String],
            isInterrupted: () ⇒ Boolean) = {
            import org.opalj.util.PerformanceEvaluation.{ time, ns2sec }

            val refinedFieldTypes =
                org.opalj.ai.analyses.FieldTypesAnalysis.doAnalyze(
                    theProject,
                    isInterrupted)

            BasicReport(
                refinedFieldTypes.seq.map { info ⇒
                    val (field, upperTypeBound) = info
                    val classFile = theProject.classFile(field)
                    classFile.thisType.toJava+"{ "+
                        field.name+":"+
                        {
                            if (upperTypeBound.isEmpty)
                                "null"
                            else
                                upperTypeBound.map { _.toJava }.mkString(" with ")
                        }+
                        " // Originaltype: "+field.fieldType.toJava+" }"
                }.mkString("\n")+
                    "\n"+
                    "Number of refined field types: "+refinedFieldTypes.size+"\n"
            )
        }
    }
}

