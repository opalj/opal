/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }
import analyses.{ Project, ClassHierarchy }
import de.tud.cs.st.bat.resolved.ai.IsReferenceValue
import de.tud.cs.st.bat.resolved.ai.ComputedValueAndException

trait MethodReturnInformation { this: Domain[_] ⇒

    def returnedValues(result: AIResult[this.type]): Seq[DomainValue]

    def thrownExceptions(result: AIResult[this.type]): Seq[DomainValue]

}

trait DefaultMethodReturnInformation extends MethodReturnInformation { this: Domain[_] ⇒

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

    def returnedValues(result: AIResult[this.type]): Seq[DomainValue] = {
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

    def thrownExceptions(result: AIResult[this.type]): Seq[DomainValue] = {
        _throwInstructions.toSeq.map(result.operandsArray(_).head)
    }

}

trait PerformInvocations[+I, Source]
        extends Domain[I]
        with TypeLevelInvokeInstructions { thisDomain ⇒

    def project: Project[Source]

    private[this] def classHierarchy: ClassHierarchy = project.classHierarchy

    def Operands(operands: Iterable[DomainValue]): DomainValues[thisDomain.type] =
        DomainValues(thisDomain)(operands)

    // the function to identify recursive calls
    def isRecursive(
        definingClass: ClassFile,
        method: Method,
        operands: DomainValues[thisDomain.type]): Boolean

    trait InvokeExecutionHandler[D <: Domain[_ >: I] with MethodReturnInformation] {

        // the domain to use
        val domain: D

        // the abstract interpreter
        val ai: AI[_ >: domain.type]

        // the function to transform the result
        def transformResult(callerPC: PC, result: AIResult[domain.type]): OptionalReturnValueOrExceptions = {
            val returnedValues = result.domain.returnedValues(result)
            val computedValue =
                if (returnedValues.isEmpty)
                    None
                else {
                    val summarizedValue = result.domain.summarize(callerPC, returnedValues)
                    Some(summarizedValue.adapt(thisDomain, callerPC))
                }
            val thrownExceptions = result.domain.thrownExceptions(result)
            ComputedValueAndException(
                computedValue,
                thrownExceptions.map(_.adapt(thisDomain, callerPC)).toSet)

        }

        def perform(
            pc: PC,
            definingClass: ClassFile,
            method: Method,
            parameters: Array[domain.type#DomainValue]) = {
            transformResult(pc, ai.perform(definingClass, method, domain)(Some(parameters)))
        }
    }

    def invokeExecutionHandler(
        pc: PC,
        definingClass: ClassFile,
        method: Method,
        operands: List[DomainValue]): InvokeExecutionHandler[_ <: Domain[I]]

    override def invokevirtual(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): OptionalReturnValueOrExceptions =
        ComputedValue(asTypedValue(pc, methodDescriptor.returnType))

    override def invokeinterface(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): OptionalReturnValueOrExceptions =
        ComputedValue(asTypedValue(pc, methodDescriptor.returnType))

    override def invokespecial(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): OptionalReturnValueOrExceptions =
        ComputedValue(asTypedValue(pc, methodDescriptor.returnType))

    final override def invokestatic(
        pc: PC,
        declaringClass: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): OptionalReturnValueOrExceptions = {

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
                case Some((classFile, method)) if !method.isNative ⇒
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
        operands: List[DomainValue]): OptionalReturnValueOrExceptions = {
        super.invokestatic(pc, declaringClass, name, methodDescriptor, operands)
    }

    def invokestatic(
        pc: PC,
        definingClass: ClassFile,
        method: Method,
        operands: List[DomainValue]): OptionalReturnValueOrExceptions = {

        val executionHandler = invokeExecutionHandler(pc, definingClass, method, operands)
        val parameters = operands.reverse.zipWithIndex.map { operand_index ⇒
            val (operand, index) = operand_index
            operand.adapt(executionHandler.domain, -(index + 1))
        }.toArray(executionHandler.domain.DomainValueTag)
        executionHandler.perform(pc, definingClass, method, parameters)
    }
}


