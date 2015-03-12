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
package cg

import scala.collection.Set
import org.opalj.ai.CorrelationalDomain
import org.opalj.br.analyses.Project
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.MethodSignature
import org.opalj.br.analyses.Project
import org.opalj.ai.domain.DefaultDomainValueBinding
import org.opalj.ai.domain.DefaultHandlingOfMethodResults
import org.opalj.ai.domain.IgnoreSynchronization
import org.opalj.ai.domain.ProjectBasedClassHierarchy
import org.opalj.ai.domain.TheClassFile
import org.opalj.ai.domain.TheMethod
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.ThrowAllPotentialExceptionsConfiguration
import org.opalj.ai.domain.SpecialMethodsHandling
import org.opalj.ai.domain.l0
import org.opalj.ai.domain.l1
import org.opalj.ai.domain.la
import org.opalj.ai.analyses.FieldValueInformation
import org.opalj.ai.analyses.MethodReturnValueInformation
import org.opalj.ai.domain.DefaultRecordMethodCallResults
import org.opalj.ai.domain.la.PerformInvocationsWithBasicVirtualMethodCallResolution

/**
 * Domain object which can used to calculate a context-sensitive call graph.
 * This domain uses advanced domains for tracking primitive values to rule out
 * potential dead branches/method calls on dead branches.
 *
 * @author Michael Eichberg
 */
class CFACallGraphDomain[Source](
    val k: Int, // the maximum length of the call chain
    val project: Project[Source],
    val fieldValueInformation: FieldValueInformation,
    val methodReturnValueInformation: MethodReturnValueInformation,
    val cache: CallGraphCache[MethodSignature, Set[Method]],
    val classFile: ClassFile,
    val method: Method,
    val calledMethods: Set[Method] = Set.empty)
        extends CorrelationalDomain
        with DefaultDomainValueBinding
        with ThrowAllPotentialExceptionsConfiguration
        with TheProject
        with ProjectBasedClassHierarchy
        with TheClassFile
        with TheMethod
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with l1.DefaultReferenceValuesBinding
        with l1.NullPropertyRefinement
        with l1.DefaultIntegerRangeValues
        //with l0.DefaultTypeLevelIntegerValues
        // [CURRENTLY ONLY A WASTE OF RESOURCES] with l1.ConstraintsBetweenIntegerValues
        with l0.DefaultTypeLevelLongValues
        //with l1.LongSetValuesShiftOperators
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelLongValuesShiftOperators
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l1.MaxArrayLengthRefinement
        with l0.TypeLevelInvokeInstructions // the foundation
        with la.RefinedTypeLevelInvokeInstructions
        with SpecialMethodsHandling
        with la.RefinedTypeLevelFieldAccessInstructions
        with PerformInvocationsWithBasicVirtualMethodCallResolution
        with DefaultRecordMethodCallResults {
    callingDomain ⇒

    // we just want to be able to track "booleans"
    override protected def maxCardinalityOfIntegerRanges: Long = 2l

    def shouldInvocationBePerformed(
        definingClass: ClassFile,
        method: Method): Boolean =
        // we only call methods where we have a chance that the return value
        // actually depends on the calling context and may directly affect
        // subsequent calls (i.e., if the return value is a primitive value
        // then we are not interested.) The return value of methods where the
        // return value does not depend on the calling context should
        // be precise based on the results of the pre-analyses.
        calledMethods.size < k && !calledMethods.contains(method) &&
            method.descriptor.parametersCount > 0 &&
            !method.returnType.isVoidType &&
            (
                !method.returnType.isObjectType ||
                classHierarchy.hasSubtypes(method.returnType.asObjectType).isYes
            )

    type CalledMethodDomain = CFACallGraphDomain[Source]

    override def calledMethodDomain(classFile: ClassFile, method: Method) =
        new CFACallGraphDomain(
            k,
            project,
            fieldValueInformation,
            methodReturnValueInformation,
            cache,
            classFile,
            method,
            calledMethods + callingDomain.method)

    def calledMethodAI: BaseAI.type = BaseAI

    override protected[this] def doInvoke(
        pc: PC,
        definingClass: ClassFile,
        method: Method,
        operands: Operands,
        fallback: () ⇒ MethodCallResult): MethodCallResult = {

        val result = super.doInvoke(pc, definingClass, method, operands, fallback)
        //        if (calledMethods.isEmpty)
        //            println(s"[info - call graph] ${callingDomain.method.toJava(callingDomain.classFile)}:$pc the result of calling ${method.toJava(definingClass)} is $result")
        result
    }
}

