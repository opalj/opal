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
package l2

import org.opalj.util.{ Answer, Yes, No, Unknown }

import br._
import br.instructions._
import br.analyses.{ Project, ClassHierarchy }

/**
 * @author Michael Eichberg
 */
trait PerformInvocations[Source]
        extends Domain
        with l0.TypeLevelInvokeInstructions
        with ProjectBasedClassHierarchy[Source] { theDomain ⇒

    def Operands(operands: Iterable[DomainValue]): DomainValues =
        DomainValues(theDomain)(operands)

    // the function to identify recursive calls
    def isRecursive(
        definingClass: ClassFile,
        method: Method,
        operands: DomainValues): Boolean

    trait InvokeExecutionHandler {

        // the domain to use
        val domain: Domain with RecordReturnFromMethodInstructions with RecordReturnVoidInstructions with DefaultRecordThrownExceptions

        // the abstract interpreter
        val ai: AI[_ >: domain.type]

        def perform(
            pc: PC,
            definingClass: ClassFile,
            method: Method,
            parameters: Array[domain.DomainValue]): MethodCallResult = {

            val aiResult = ai.perform(definingClass, method, domain)(Some(parameters))
            transformResult(pc, method, aiResult)
        }

        // the function to transform the result
        protected[this] def transformResult(
            callerPC: PC,
            calledMethod: Method,
            result: AIResult { val domain: InvokeExecutionHandler.this.domain.type }): MethodCallResult = {
            val domain = result.domain
            val operandsArray = result.operandsArray
            val thrownExceptions = domain.allThrownExceptions.values map { _.adapt(theDomain, callerPC) }

            if (calledMethod.descriptor.returnType eq VoidType) {
                if (domain.allReturnVoidInstructions.isEmpty) {
                    // The method must have returned with an exception.
                    ThrowsException(thrownExceptions)
                } else if (thrownExceptions.nonEmpty) {
                    ComputationWithSideEffectOrException(thrownExceptions)
                } else {
                    ComputationWithSideEffectOnly
                }
            } else {
                // The method potentially returns some value...
                val returnInstructions = domain.allReturnInstructions
                if (returnInstructions.nonEmpty) {
                    val returnedValue =
                        Some(
                            result.domain.summarize(
                                callerPC,
                                domain.allReturnInstructions mapToList { pc ⇒ operandsArray(pc).head }
                            ).adapt(theDomain, callerPC)
                        )
                    if (thrownExceptions.nonEmpty) {
                        ComputedValueAndException(returnedValue, thrownExceptions)
                    } else {
                        ComputedValue(returnedValue)
                    }
                } else {
                    // The method must have returned with an exception.
                    ThrowsException(thrownExceptions)
                }
            }
        }
    }

    def invokeExecutionHandler(
        pc: PC,
        definingClass: ClassFile,
        method: Method,
        operands: List[DomainValue]): InvokeExecutionHandler

    override def invokevirtual(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult =
        ComputedValue(asTypedValue(pc, methodDescriptor.returnType))

    override def invokeinterface(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult =
        ComputedValue(asTypedValue(pc, methodDescriptor.returnType))

    override def invokespecial(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult =
        ComputedValue(asTypedValue(pc, methodDescriptor.returnType))

    final override def invokestatic(
        pc: PC,
        declaringClass: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult = {

        def fallback() =
            baseInvokestatic(pc, declaringClass, methodName, methodDescriptor, operands)

        if (declaringClass.isArrayType)
            // given that arrays (up until Java 7) do not have any static methods, we 
            // should not encounter this situation...
            return fallback()

        classHierarchy.resolveMethodReference(
            declaringClass.asObjectType,
            methodName,
            methodDescriptor,
            project) match {
                case Some(method) if !method.isNative ⇒
                    val classFile = project.classFile(method)
                    if (isRecursive(classFile, method, Operands(operands)))
                        fallback()
                    else
                        invokestatic(pc, classFile, method, operands)
                case _ ⇒
                    fallback()
            }
    }

    def baseInvokestatic(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult = {
        super.invokestatic(pc, declaringClass, name, methodDescriptor, operands)
    }

    def invokestatic(
        pc: PC,
        definingClass: ClassFile,
        method: Method,
        operands: List[DomainValue]): MethodCallResult = {

        val executionHandler = invokeExecutionHandler(pc, definingClass, method, operands)
        val parameters = executionHandler.domain.DomainValueTag.newArray(method.body.get.maxLocals)
        var localVariableIndex = 0
        for ((operand, index) ← operands.view.reverse.zipWithIndex) {
            parameters(localVariableIndex) =
                operand.adapt(executionHandler.domain, -(index + 1))
            localVariableIndex += operand.computationalType.operandSize
        }
        executionHandler.perform(pc, definingClass, method, parameters)
    }
}


