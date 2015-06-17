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
import org.opalj.ai.mapOperands
import org.opalj.ai.TheAI
import org.opalj.ai.domain.l2.CalledMethodsStore
import org.opalj.ai.TheMemoryLayout
import org.opalj.ai.common.XHTML
import org.opalj.br.ReferenceType
import org.opalj.br.MethodDescriptor
import org.opalj.ai.domain.l2.ChildPerformInvocationsWithRecursionDetection
import org.opalj.ai.AIResult
import org.opalj.ai.domain.l2.CalledMethodsStore

/**
 * The base domain that is shared by all domains that are used to perform abstract
 * interpretations of methods.
 */
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
        // with domain.l1.DefaultIntegerSetValues
        // [CURRENTLY ONLY A WASTE OF RESOURCES] with domain.l1.ConstraintsBetweenIntegerValues
        with domain.l1.DefaultLongSetValues
        with domain.l1.LongSetValuesShiftOperators
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l1.ConcretePrimitiveValuesConversions
        //with domain.l1.DefaultReferenceValuesBinding [implicitly mixed in via StringValuesBinding]
        //with domain.l1.DefaultStringValuesBinding [implicitly mixed in via ClassValuesBinding]
        with domain.l1.NullPropertyRefinement
        with domain.l1.MaxArrayLengthRefinement
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization {
    // We want to get the special treatment of calls on "Class" objects
    // and do not want to perform invocations in this case;
    // hence, we have to mix in this domain AFTER the PerformInvocations domain!
    this: domain.l1.DefaultClassValuesBinding ⇒
}

/**
 * This is the fall back domain that is used to perform an abstract interpretation of
 * a method without invoking called methods.
 */
class FallbackBugPickerAnalysisDomain(
    val project: Project[URL],
    val fieldValueInformation: FieldValueInformation,
    val methodReturnValueInformation: MethodReturnValueInformation,
    val cache: CallGraphCache[MethodSignature, scala.collection.Set[Method]],
    override val maxCardinalityOfIntegerRanges: Long,
    override val maxCardinalityOfLongSets: Int,
    val /*current*/ method: Method)
        extends BaseBugPickerAnalysisDomain
        with domain.l1.DefaultClassValuesBinding
        with domain.l1.RecordAllThrownExceptions
        with domain.RecordCFG

/**
 * The base domain that is shared by all domains that are used to perform abstract
 * interpretations of methods where methods are potentially called.
 */
trait BasePerformInvocationBugPickerAnalysisDomain
        extends BaseBugPickerAnalysisDomain
        with PerformInvocationsWithRecursionDetection
        with PerformInvocationsWithBasicVirtualMethodCallResolution
        with domain.l1.DefaultClassValuesBinding { callingDomain ⇒

    def debug: Boolean

    type CalledMethodDomain = InvocationBugPickerAnalysisDomain

    override protected[this] def doInvoke(
        method: Method,
        calledMethodDomain: CalledMethodDomain)(
            parameters: calledMethodDomain.Locals): AIResult { val domain: calledMethodDomain.type } = {
        val result = super.doInvoke(method, calledMethodDomain)(parameters)
        if (debug) {
            org.opalj.io.writeAndOpen(XHTML.dump(
                Some(project.classFile(method)),
                Some(method),
                method.body.get,
                Some(
                    "Created: "+(new java.util.Date).toString+"<br>"+
                        "Domain: "+result.domain.getClass.getName+"<br>"+
                        XHTML.evaluatedInstructionsToXHTML(result.evaluated)),
                result.domain)(
                    result.operandsArray,
                    result.localsArray),
                "AIResult",
                ".html"
            )
        }
        result
    }

    override def doInvoke(
        pc: PC,
        definingClass: ClassFile,
        method: Method,
        operands: callingDomain.Operands,
        fallback: () ⇒ MethodCallResult): MethodCallResult = {
        val result = super.doInvoke(pc, definingClass, method, operands, fallback)
        if (debug) {
            println("the result of calling "+method.toJava(definingClass)+" is "+result)
        }
        result
    }

    val maxCallChainLength: Int

    def currentCallChainLength: Int

    def shouldInvocationBePerformed(
        definingClass: ClassFile,
        calledMethod: Method): Boolean = {
        val result =
            maxCallChainLength > currentCallChainLength &&
                !(calledMethod.isPrivate && calledMethod.parametersCount == 1)
        if (debug) {
            val i = if (result) " invokes " else " does not invoke "
            println(s"[$currentCallChainLength]"+
                method.toJava(project.classFile(method)) +
                i +
                calledMethod.toJava(definingClass))
        }
        result
    }

    abstract override def invokevirtual(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        descriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {

        val result = super.invokevirtual(pc, declaringClass, name, descriptor, operands)
        if (debug) {
            println(s"[$currentCallChainLength] call result of "+
                declaringClass.toJava+" "+descriptor.toJava(name) + result)
        }
        result
    }
}

class InvocationBugPickerAnalysisDomain(
    val project: Project[URL],
    val fieldValueInformation: FieldValueInformation,
    val methodReturnValueInformation: MethodReturnValueInformation,
    val cache: CallGraphCache[MethodSignature, scala.collection.Set[Method]],
    override val maxCardinalityOfIntegerRanges: Long,
    override val maxCardinalityOfLongSets: Int,
    val maxCallChainLength: Int,
    val callerDomain: BasePerformInvocationBugPickerAnalysisDomain,
    val /*current*/ method: Method,
    val currentCallChainLength: Int,
    val debug: Boolean)
        extends BasePerformInvocationBugPickerAnalysisDomain
        with domain.RecordMethodCallResults
        with domain.RecordLastReturnedValues
        with domain.RecordAllThrownExceptions
        with ChildPerformInvocationsWithRecursionDetection {
    callingDomain ⇒

    override def calledMethodDomain(classFile: ClassFile, method: Method) =
        new InvocationBugPickerAnalysisDomain(
            project,
            fieldValueInformation,
            methodReturnValueInformation,
            cache,
            maxCardinalityOfIntegerRanges,
            maxCardinalityOfLongSets,
            maxCallChainLength,
            callingDomain,
            method, currentCallChainLength + 1,
            debug) {
        }

    def calledMethodAI = callerDomain.calledMethodAI

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
    override val maxCardinalityOfIntegerRanges: Long,
    override val maxCardinalityOfLongSets: Int,
    val maxCallChainLength: Int,
    val classFile: ClassFile,
    val /*current*/ method: Method,
    val debug: Boolean,
    val frequentEvaluationWarningLevel: Int = 256)
        extends BasePerformInvocationBugPickerAnalysisDomain
        with TheAI[BaseBugPickerAnalysisDomain]
        with TheMemoryLayout // required to extract the initial operands
        // the following two are required to detect instructions that always throw
        // an exception (such as div by zero, a failing checkcast, a method call that
        // always fails etc.)
        with domain.l1.RecordAllThrownExceptions
        with domain.RecordCFG { callingDomain ⇒

    final def currentCallChainLength: Int = 0
    final def calledMethodAI = ai
    final val coordinatingDomain = this

    // The called methods store is always only required at analysis time, at this point
    // in time the initial operands are available!
    lazy val calledMethodsStore: CalledMethodsStore { val domain: coordinatingDomain.type } = {
        val operands =
            localsArray(0).foldLeft(List.empty[DomainValue])((l, n) ⇒
                if (n ne null) n :: l else l
            )
        CalledMethodsStore(coordinatingDomain, frequentEvaluationWarningLevel)(
            method, mapOperands(operands, coordinatingDomain))
    }

    override def calledMethodDomain(classFile: ClassFile, method: Method) =
        new InvocationBugPickerAnalysisDomain(
            project,
            fieldValueInformation,
            methodReturnValueInformation,
            cache,
            maxCardinalityOfIntegerRanges,
            maxCardinalityOfLongSets,
            maxCallChainLength,
            callingDomain,
            method,
            currentCallChainLength + 1,
            debug)

}

