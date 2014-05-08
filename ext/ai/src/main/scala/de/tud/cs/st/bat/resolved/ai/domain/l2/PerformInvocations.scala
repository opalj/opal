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
package de.tud.cs.st
package bat
package resolved
package ai
package domain
package l2

import instructions._
import analyses.{ Project, ClassHierarchy }

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

trait MethodReturnInformation { theDomain: Domain ⇒

    def returnedValues(result: AIResult { val domain: theDomain.type }): Seq[DomainValue]

    def thrownExceptions(result: AIResult { val domain: theDomain.type }): Seq[DomainValue]

}

trait DefaultMethodReturnInformation extends MethodReturnInformation { theDomain: Domain ⇒

    def returnVoid(pc: PC): Unit = { /* Do nothing. */ }

    //
    // HANDLING NORMAL RETURNS
    //

    private[this] var _returnValueInstructions: Set[PC] = Set.empty

    def returnValueInstructions = _returnValueInstructions

    def areturn(pc: PC, value: DomainValue): Unit = { _returnValueInstructions += pc }

    def dreturn(pc: PC, value: DomainValue): Unit = { _returnValueInstructions += pc }

    def freturn(pc: PC, value: DomainValue): Unit = { _returnValueInstructions += pc }

    def ireturn(pc: PC, value: DomainValue): Unit = { _returnValueInstructions += pc }

    def lreturn(pc: PC, value: DomainValue): Unit = { _returnValueInstructions += pc }

    def returnedValues(result: AIResult { val domain: theDomain.type }): Seq[DomainValue] = {
        _returnValueInstructions.toSeq.map(result.operandsArray(_).head)
    }

    //
    // HANDLING ABNORMAL RETURNS (EXCEPTIONS)
    //

    private[this] var _throwInstructions: Set[PC] = Set.empty

    def throwInstructions = _throwInstructions

    def abruptMethodExecution(pc: PC, exception: DomainValue): Unit = {
        _throwInstructions += pc
    }

    def thrownExceptions(result: AIResult { val domain: theDomain.type }): Seq[DomainValue] = {
        _throwInstructions.toSeq.map(result.operandsArray(_).head)
    }

}

trait PerformInvocations[Source]
        extends Domain
        with l0.TypeLevelInvokeInstructions { theDomain ⇒

    def project: Project[Source]

    private[this] def classHierarchy: ClassHierarchy = project.classHierarchy

    def Operands(operands: Iterable[DomainValue]): DomainValues =
        DomainValues(theDomain)(operands)

    // the function to identify recursive calls
    def isRecursive(
        definingClass: ClassFile,
        method: Method,
        operands: DomainValues): Boolean

    trait InvokeExecutionHandler {

        // the domain to use
        val domain: Domain with MethodReturnInformation

        // the abstract interpreter
        val ai: AI[_ >: domain.type]

        def perform(
            pc: PC,
            definingClass: ClassFile,
            method: Method,
            parameters: Array[domain.DomainValue]): MethodCallResult = {
            val aiResult = ai.perform(definingClass, method, domain)(Some(parameters))
            transformResult(pc, aiResult)
        }

        // the function to transform the result
        def transformResult(
            callerPC: PC,
            result: AIResult { val domain: InvokeExecutionHandler.this.domain.type }): MethodCallResult = {
            val returnedValues = result.domain.returnedValues(result)
            val computedValue =
                if (returnedValues.isEmpty)
                    None
                else {
                    val summarizedValue = result.domain.summarize(callerPC, returnedValues)
                    Some(summarizedValue.adapt(theDomain, callerPC))
                }
            val thrownExceptions = result.domain.thrownExceptions(result)
            ComputedValueAndException(
                computedValue,
                thrownExceptions.map(_.adapt(theDomain, callerPC)).toSet)
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


