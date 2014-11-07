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
package analyses

import java.net.URL
import org.opalj.collection.immutable.UIDSet
import org.opalj.br.analyses.{ Analysis, OneStepAnalysis, AnalysisExecutor, BasicReport, SomeProject }
import org.opalj.br.{ ClassFile, Method }
import org.opalj.br.{ ReferenceType }
import org.opalj.ai.Domain
import org.opalj.ai.domain._
import org.opalj.ai.InterruptableAI
import org.opalj.ai.IsAReferenceValue
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.ai.NoUpdate
import org.opalj.ai.SomeUpdate
import org.opalj.br.analyses.Project

/**
 * A shallow analysis that tries to refine the return types of methods.
 *
 * @author Michael Eichberg
 */
class MethodReturnValuesAnalysisDomain(
    override val project: SomeProject,
    val ai: InterruptableAI[_],
    val method: Method)
        extends Domain
        with TheProject
        with ProjectBasedClassHierarchy
        with TheMethod
        with DefaultDomainValueBinding
        with ThrowAllPotentialExceptionsConfiguration
        with l0.DefaultTypeLevelIntegerValues
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultPrimitiveValuesConversions
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelInvokeInstructions
        with l0.DefaultReferenceValuesBinding
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with RecordReturnedValuesInfrastructure {

    type ReturnedValue = DomainValue

    private[this] val originalReturnType: ReferenceType =
        method.descriptor.returnType.asReferenceType

    private[this] var theReturnedValue: DomainValue = null

    // e.g., a method that always throws an exception...
    def returnedValue: Option[DomainValue] = Option(theReturnedValue)

    protected[this] def doRecordReturnedValue(pc: PC, value: DomainValue): Unit = {
        if (theReturnedValue eq value)
            return ;

        if (theReturnedValue == null) {
            theReturnedValue = value
            return ;
        }

        theReturnedValue.join(Int.MinValue, value) match {
            case SomeUpdate(newValue) ⇒
                typeOfValue(newValue) match {
                    case IsAReferenceValue(utb) if (utb.size == 1) && (utb.first eq originalReturnType) ⇒
                        // the return type will not be more precise than the original type
                        ai.interrupt()
                    case _ ⇒
                        theReturnedValue = newValue
                }

            case NoUpdate ⇒
            /* Nothing to do. */
        }

    }
}

