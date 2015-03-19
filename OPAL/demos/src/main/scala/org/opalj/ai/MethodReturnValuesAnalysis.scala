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

import org.opalj.util.PerformanceEvaluation.time
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.br.MethodSignature
import org.opalj.ai.analyses.{ MethodReturnValuesAnalysis ⇒ TheMethodReturValuesAnalysis }
import org.opalj.ai.analyses.FieldValuesKey
import org.opalj.ai.analyses.BaseMethodReturnValuesAnalysisDomain
import org.opalj.ai.analyses.FPMethodReturnValuesAnalysisDomain
import org.opalj.ai.analyses.MethodReturnValueInformation
import org.opalj.ai.analyses.FPFieldValuesAnalysisDomain
import org.opalj.ai.analyses.FieldValuesAnalysis
import org.opalj.ai.analyses.cg.CallGraphCache

/**
 * A shallow analysis that tries to refine the return types of methods.
 *
 * @author Michael Eichberg
 */
object MethodReturnValuesAnalysis extends DefaultOneStepAnalysis {

    override def title: String = TheMethodReturValuesAnalysis.title

    override def description: String = TheMethodReturValuesAnalysis.description

    override def doAnalyze(
        theProject: Project[URL],
        parameters: Seq[String],
        isInterrupted: () ⇒ Boolean) = {

        var fieldValueInformation = theProject.get(FieldValuesKey)

        var methodReturnValueInformation: MethodReturnValueInformation = time {
            def createDomain(ai: InterruptableAI[Domain], method: Method) = {
                new BaseMethodReturnValuesAnalysisDomain(
                    theProject, fieldValueInformation, ai, method
                )
            }
            TheMethodReturValuesAnalysis.doAnalyze(theProject, isInterrupted, createDomain)
        } { t ⇒ println(s"Analysis time: $t") }
        println("number of methods with refined returned types "+methodReturnValueInformation.size)

        var continueAnalysis = true
        val cache = new CallGraphCache[MethodSignature, scala.collection.Set[Method]](theProject)
        do {

            val mrva = time {
                def createDomain(ai: InterruptableAI[Domain], method: Method) = {
                    new FPMethodReturnValuesAnalysisDomain(
                        theProject,
                        fieldValueInformation, methodReturnValueInformation,
                        cache,
                        ai, method
                    )
                }
                TheMethodReturValuesAnalysis.doAnalyze(theProject, isInterrupted, createDomain)
            } { t ⇒ println(s"Method return values analysis time: $t") }

            val fvta = time {
                def createDomain(project: SomeProject, classFile: ClassFile) = {
                    new FPFieldValuesAnalysisDomain(
                        theProject,
                        fieldValueInformation, methodReturnValueInformation,
                        cache,
                        classFile
                    )
                }
                FieldValuesAnalysis.doAnalyze(theProject, createDomain, isInterrupted)
            } { t ⇒ println(s"Field value information analysis time: $t.") }

            println("number of fields with refined types "+fieldValueInformation.size)
            println("number of methods with refined returned types "+methodReturnValueInformation.size)

            continueAnalysis =
                mrva.size > methodReturnValueInformation.size ||
                    fvta.size > fieldValueInformation.size

            methodReturnValueInformation = mrva
            fieldValueInformation = fvta
        } while (continueAnalysis)

        val results =
            methodReturnValueInformation.map { result ⇒
                val (method, value) = result
                RefinedReturnType[Domain](
                    theProject,
                    theProject.classFile(method),
                    method, value)
            }

        BasicReport(
            results.map(_.toString()).toSeq.sorted.mkString(
                "Methods with refined return types ("+results.size+"): \n", "\n", "\n"))
    }
}

case class RefinedReturnType[D <: Domain](
        project: SomeProject,
        classFile: ClassFile,
        method: Method,
        refinedType: Option[D#DomainValue]) {

    override def toString = {

        val returnType = method.descriptor.returnType
        val additionalInfo =
            refinedType match {
                case value @ IsAReferenceValue(utb) ⇒
                    if (returnType.isReferenceType && value.isValueSubtypeOf(returnType.asReferenceType).isNoOrUnknown)
                        s"the $refinedType is not a subtype of the declared type $returnType"
                    else if (returnType.isObjectType && project.classHierarchy.hasSubtypes(returnType.asObjectType).isNoOrUnknown)
                        s"the $returnType has no subtypes, but we were still able to refine the value $refinedType"
                case _ ⇒ ""
            }

        import Console._
        val declaringClassOfMethod = classFile.thisType.toJava

        "Refined the return type of "+BOLD + BLUE +
            declaringClassOfMethod+"{ "+method.toJava+" }"+
            " => "+GREEN +
            refinedType.getOrElse("\"NONE\" (the method never returns normally)") +
            RESET + RED + additionalInfo + RESET
    }

}

