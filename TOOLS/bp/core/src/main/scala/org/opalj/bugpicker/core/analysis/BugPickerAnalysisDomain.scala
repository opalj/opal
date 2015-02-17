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
package bugpicker
package core
package analysis

import java.net.URL
import org.opalj.br.analyses.Project
import org.opalj.br.Method
import org.opalj.br.PC
import org.opalj.ai.CorrelationalDomain
import org.opalj.ai.domain
import org.opalj.ai.analyses.FieldValueInformation
import org.opalj.ai.analyses.MethodReturnValueInformation
import org.opalj.br.MethodSignature
import org.opalj.ai.analyses.cg.CallGraphCache
import org.opalj.ai.domain.la.PerformInvocationsWithBasicVirtualMethodCallResolution
import org.opalj.ai.domain.l2.PerformInvocationsWithRecursionDetection
import org.opalj.br.ClassFile
import org.opalj.ai.AI
import org.opalj.ai.TheAI
import org.opalj.ai.domain.l2.CalledMethodsStore
import org.opalj.ai.TheMemoryLayout

trait BaseBugPickerAnalysisDomain
        extends CorrelationalDomain
        with domain.TheProject
        with domain.TheMethod
        with domain.DefaultDomainValueBinding
        with domain.ThrowAllPotentialExceptionsConfiguration
        //with domain.l0.TypeLevelFieldAccessInstructions
        with domain.la.RefinedTypeLevelFieldAccessInstructions
        with domain.l0.TypeLevelInvokeInstructions
        with domain.la.RefinedTypeLevelInvokeInstructions
        with domain.SpecialMethodsHandling
        with domain.l1.DefaultIntegerRangeValues
        //with domain.l1.DefaultIntegerSetValues
        with domain.l1.ConstraintsBetweenIntegerValues
        with domain.l1.DefaultLongSetValues
        with domain.l1.LongSetValuesShiftOperators
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l1.ConcretePrimitiveValuesConversions
        //with domain.l1.DefaultReferenceValuesBinding
        //with domain.l1.DefaultStringValuesBinding
        with domain.l1.NullPropertyRefinement
        with domain.l1.MaxArrayLengthRefinement
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization {
    // we wan to get the special treatment of calls on "Class" objects
    // and do not want to perform invocations in this case;
    // hence, we have to mix in this domain AFTER the PerformInvocations domain!
    this: domain.l1.DefaultClassValuesBinding ⇒
}

class FallbackBugPickerAnalysisDomain(
    val project: Project[URL],
    val fieldValueInformation: FieldValueInformation,
    val methodReturnValueInformation: MethodReturnValueInformation,
    val cache: CallGraphCache[MethodSignature, scala.collection.Set[Method]],
    val method: Method,
    override val maxCardinalityOfIntegerRanges: Long)
        extends BaseBugPickerAnalysisDomain
        with domain.l1.DefaultClassValuesBinding
        with domain.RecordCFG
        with domain.l1.RecordAllThrownExceptions

trait BasePerformInvocationBugPickerAnalysisDomain
        extends BaseBugPickerAnalysisDomain
        with PerformInvocationsWithBasicVirtualMethodCallResolution
        with PerformInvocationsWithRecursionDetection
        with domain.l1.DefaultClassValuesBinding {

    val maxCallChainLength: Int

    def currentCallChainLength: Int

    def shouldInvocationBePerformed(
        definingClass: ClassFile,
        method: Method): Boolean =
        maxCallChainLength > currentCallChainLength &&
            !method.returnType.isVoidType &&
            method.parametersCount > 0
}

/**
 * The domain that is used to identify the issues.
 *
 * @author Michael Eichberg
 */
class RootBugPickerAnalysisDomain(
    val project: Project[URL],
    val fieldValueInformation: FieldValueInformation,
    val methodReturnValueInformation: MethodReturnValueInformation,
    val cache: CallGraphCache[MethodSignature, scala.collection.Set[Method]],
    val classFile: ClassFile,
    val method: Method,
    override val maxCardinalityOfIntegerRanges: Long,
    val maxCallChainLength: Int)
        extends BasePerformInvocationBugPickerAnalysisDomain
        with TheAI[BaseBugPickerAnalysisDomain]
        with TheMemoryLayout // required to extract the initial operands
        // the following two are required to detect instructions that always throw
        // an exception (such as div by zero, a failing checkcast, a method call that
        // always fails etc.)
        with domain.RecordCFG
        with domain.l1.RecordAllThrownExceptions {
    callingDomain ⇒

    final def currentCallChainLength: Int = 0

    // the called methods store is always only required at analysis time, at this point
    // in time the initial operands are available!
    lazy val calledMethodsStore: CalledMethodsStore = {
        val store = new CalledMethodsStore(this, /*Frequent Evaluation Warning=*/ 256)
        val operands =
            localsArray(0).foldLeft(List.empty[DomainValue])((l, n) ⇒
                if (n ne null) n :: l else l
            )
        // we want to add this method to avoid useless recursions;
        // it is (at this moment) definitively not recursive...
        store.isRecursive(classFile, method, operands)
        store
    }

    def invokeExecutionHandler(
        pc: PC,
        definingClass: ClassFile,
        method: Method,
        operands: Operands): InvokeExecutionHandler = {
        assert(method ne null)

        new InvokeExecutionHandler {

            override val domain =
                new InvocationBugPickerAnalysisDomain(
                    callingDomain.ai,
                    project,
                    fieldValueInformation,
                    methodReturnValueInformation,
                    cache,
                    method,
                    maxCardinalityOfIntegerRanges,
                    maxCallChainLength, currentCallChainLength + 1) {
                    val calledMethodsStore: RootBugPickerAnalysisDomain.this.calledMethodsStore.type =
                        RootBugPickerAnalysisDomain.this.calledMethodsStore
                }

            def ai: AI[_ >: this.domain.type] = callingDomain.ai
        }
    }
}

abstract class InvocationBugPickerAnalysisDomain(
    val ai: AI[BaseBugPickerAnalysisDomain],
    val project: Project[URL],
    val fieldValueInformation: FieldValueInformation,
    val methodReturnValueInformation: MethodReturnValueInformation,
    val cache: CallGraphCache[MethodSignature, scala.collection.Set[Method]],
    val method: Method,
    override val maxCardinalityOfIntegerRanges: Long,
    val maxCallChainLength: Int = 0,
    val currentCallChainLength: Int)
        extends BasePerformInvocationBugPickerAnalysisDomain
        with domain.RecordMethodCallResults
        with domain.RecordLastReturnedValues
        with domain.RecordAllThrownExceptions
        with PerformInvocationsWithBasicVirtualMethodCallResolution {
    callingDomain ⇒

    def invokeExecutionHandler(
        pc: PC,
        definingClass: ClassFile,
        method: Method,
        operands: Operands): InvokeExecutionHandler = {
        assert(method ne null)

        new InvokeExecutionHandler {

            override val domain =
                new InvocationBugPickerAnalysisDomain(
                    callingDomain.ai,
                    project,
                    fieldValueInformation,
                    methodReturnValueInformation,
                    cache,
                    method,
                    maxCardinalityOfIntegerRanges,
                    maxCallChainLength, currentCallChainLength + 1) {
                    val calledMethodsStore: InvocationBugPickerAnalysisDomain.this.calledMethodsStore.type =
                        InvocationBugPickerAnalysisDomain.this.calledMethodsStore
                }

            def ai: AI[_ >: this.domain.type] = callingDomain.ai
        }
    }
}

