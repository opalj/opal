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
package ai
package domain
package l1

import java.net.URL

import scala.Console.BLUE
import scala.Console.BOLD
import scala.Console.GREEN
import scala.Console.RESET
import scala.Iterable
import org.opalj.ai.domain
import org.opalj.value.TypeOfReferenceValue
import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet1
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.ai.CorrelationalDomain
import org.opalj.ai.Domain
import org.opalj.ai.InterruptableAI

/**
 * A shallow analysis that tries to refine the return types of methods.
 *
 * @author Michael Eichberg
 */
object MethodReturnValuesAnalysis extends DefaultOneStepAnalysis {

    class AnalysisDomain(
            override val project: Project[java.net.URL],
            val ai:               InterruptableAI[_],
            val method:           Method
    ) extends CorrelationalDomain
        with domain.DefaultDomainValueBinding
        with domain.ThrowAllPotentialExceptionsConfiguration
        with domain.l0.DefaultTypeLevelIntegerValues
        with domain.l0.DefaultTypeLevelLongValues
        with domain.l0.TypeLevelPrimitiveValuesConversions
        with domain.l0.TypeLevelLongValuesShiftOperators
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l0.TypeLevelFieldAccessInstructions
        with domain.l0.TypeLevelInvokeInstructions
        with domain.l1.DefaultReferenceValuesBinding
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization
        with domain.TheProject
        with domain.TheMethod
        with domain.RecordReturnedValuesInfrastructure {

        type ReturnedValue = DomainValue

        private[this] val originalReturnType: ReferenceType =
            method.descriptor.returnType.asReferenceType

        private[this] var theReturnedValue: DomainValue = null

        // e.g., a method that always throws an exception...
        def returnedValue: Option[DomainValue] = Option(theReturnedValue)

        protected[this] def doRecordReturnedValue(pc: Int, value: DomainValue): Unit = {
            if (theReturnedValue == null)
                theReturnedValue = value.summarize(Int.MinValue)
            else
                theReturnedValue = summarize(Int.MinValue, Iterable(theReturnedValue, value))

            theReturnedValue match {
                case rv @ TypeOfReferenceValue(UIDSet1(`originalReturnType`)) if (
                    rv.isNull.isUnknown && !rv.isPrecise
                ) ⇒
                    // the return type will not be more precise than the original type
                    ai.interrupt()
                case _ ⇒ /*go on*/
            }
        }
    }

    override def title: String = "Derives Information About Returned Values"

    override def description: String =
        "Identifies methods where we can – statically – derive more precise return type/value information."

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        val methodsWithRefinedReturnTypes = time {
            for {
                classFile ← theProject.allClassFiles.par
                method ← classFile.methods
                if method.body.isDefined
                originalType = method.returnType
                if method.returnType.isReferenceType
                ai = new InterruptableAI[Domain]
                domain = new AnalysisDomain(theProject, ai, method)
                result = ai(method, domain)
                if !result.wasAborted
                returnedValue = domain.returnedValue
                if returnedValue.isEmpty ||
                    returnedValue.get.asDomainReferenceValue.upperTypeBound != UIDSet(originalType)
            } yield {
                RefinedReturnType(method, domain.returnedValue)
            }
        } { ns ⇒ println(s"the analysis took ${ns.toSeconds}") }

        BasicReport(
            methodsWithRefinedReturnTypes.mkString(
                "Methods with refined return types ("+methodsWithRefinedReturnTypes.size+"): \n", "\n", "\n"
            )
        )
    }

}

case class RefinedReturnType(method: Method, refinedType: Option[Domain#DomainValue]) {

    override def toString(): String = {
        import Console._
        "Refined the return type of "+BOLD + BLUE + method.toJava+" => "+
            GREEN + refinedType.getOrElse("\"NONE\" (the method does not return normally)") + RESET
    }

}
