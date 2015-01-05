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

import scala.language.existentials

import java.net.URL

import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.ai.analyses.{ MethodReturnValuesAnalysis ⇒ TheMethodReturValuesAnalysis }
import org.opalj.ai.analyses.FieldValuesKey
import org.opalj.ai.analyses.BaseMethodReturnValuesAnalysisDomain
import org.opalj.ai.analyses.cg.CallGraphCache
import org.opalj.br.MethodSignature
import org.opalj.ai.analyses.FPMethodReturnValuesAnalysisDomain
import org.opalj.ai.analyses.FPFieldValuesAnalysisDomain
import org.opalj.ai.analyses.FieldValuesAnalysis
import org.opalj.ai.domain.RecordReturnedValues
import org.opalj.ai.domain.RecordLastReturnedValues
import org.opalj.ai.domain.Origin
import org.opalj.ai.domain.Origins

/**
 * A very small analysis that identifies those methods that always return a value
 * that was passed as a parameter to the method; the self reference `this` is also treated
 * as a(n implicit) parameter.
 *
 * @author Michael Eichberg
 */
object MethodsThatAlwaysReturnAPassedParameter
        extends AnalysisExecutor
        with OneStepAnalysis[URL, BasicReport] {

    val analysis = this

    override def title: String =
        "identify methods that always return a given parameter"

    override def description: String =
        "identifies methods that either always throw an exception or return a given parameter"

    override def doAnalyze(
        theProject: Project[URL],
        parameters: Seq[String],
        isInterrupted: () ⇒ Boolean) = {

        val methods = for {
            classFile ← theProject.classFiles.par
            method ← classFile.methods
            if method.body.isDefined
            if method.descriptor.returnType.isReferenceType
            if (
                method.descriptor.parametersCount > 0 &&
                method.descriptor.parameterTypes.exists(_.isReferenceType)
            ) || !method.isStatic
            result = BaseAI(
                classFile,
                method,
                // "in real code" a specially tailored domain should be used.
                new domain.l1.DefaultDomain(
                    theProject,
                    classFile, method
                ) with RecordLastReturnedValues)
            if result.domain.allReturnedValues.forall(_ match {
                case (_, Origin(o)) if o < 0              ⇒ true
                case (_, Origins(os)) if os.forall(_ < 0) ⇒ true
                case _                                    ⇒ false
            })
        } yield {
            // collect the origin information
            val origins =
                result.domain.allReturnedValues.values.
                    map(result.domain.origin(_).toSet).flatten.toSet

            method.toJava(classFile) + (
                if (origins.nonEmpty)
                    "; returned values: "+origins.mkString(",")
                else
                    "; throws an exception"
            )
        }

        val (throwsException, returnsParameter) = methods.partition { _.endsWith("exception") }
        val returnsParameterResult =
            returnsParameter.toList.sorted.mkString(
                s"Found ${returnsParameter.size} methods which always return a passed parameter (including this):\n",
                "\n",
                "\n")
        val throwsExceptionResult =
            throwsException.toList.sorted.mkString(
                s"Found ${throwsException.size} methods which always throw an exception:\n",
                "\n",
                "\n")
        BasicReport(throwsExceptionResult + returnsParameterResult)

    }
}

