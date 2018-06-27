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

import scala.collection.Set

import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.Method

/**
 * Provides support for handling method invocations, but does not handle any invocations directly.
 *
 * @author Michael Eichberg
 */
trait MethodCallsHandling extends MethodCallsDomain {
    domain: ReferenceValuesDomain with TypedValuesFactory with Configuration with TheCode ⇒

    protected[this] def getPotentialExceptions(pc: Int): List[ExceptionValue] = {
        var exceptionTypes: Set[ObjectType] = Set.empty
        var exceptionValues: List[ExceptionValue] = List.empty

        def add(exceptionType: ObjectType): Unit = {

            if (!exceptionTypes.contains(exceptionType)) {
                exceptionTypes += exceptionType
                // We don't know the true type of the exception, we just know the upper bound!
                exceptionValues ::=
                    NonNullObjectValue(ValueOriginForMethodExternalException(pc), exceptionType)
            }
        }

        throwExceptionsOnMethodCall match {
            case ExceptionsRaisedByCalledMethods.Any ⇒
                add(ObjectType.Throwable)

            case ExceptionsRaisedByCalledMethods.AllExplicitlyHandled ⇒
                code.handlersFor(pc) foreach { h ⇒
                    h.catchType match {
                        case None     ⇒ add(ObjectType.Throwable)
                        case Some(ex) ⇒ add(ex)
                    }
                }
            case ExceptionsRaisedByCalledMethods.Known ⇒
            // we basically know nothing..
        }

        // The list of exception values is in reverse order when compared to the handlers!
        // This is by purpose to foster a faster overall evaluation. (I.e., we want
        // to perform the abstract interpretation using more abstract values first (<=>
        // exceptions with types higher-up in the type hierarchy).
        exceptionValues
    }

    /** Factory method called to create a [[MethodCallResult]]. */
    protected[this] def MethodCallResult(
        returnValue: DomainValue,
        exceptions:  Iterable[ExceptionValue]
    ): MethodCallResult = {
        if (exceptions.isEmpty)
            ComputedValue(returnValue)
        else
            ComputedValueOrException(returnValue, exceptions)
    }

    /** Factory method called to create a [[MethodCallResult]]. */
    protected[this] def MethodCallResult(
        potentialExceptions: Iterable[ExceptionValue]
    ): MethodCallResult = {
        if (potentialExceptions.isEmpty)
            ComputationWithSideEffectOnly
        else
            ComputationWithSideEffectOrException(potentialExceptions)
    }

    protected[this] def handleInvoke(
        pc:               Int,
        methodDescriptor: MethodDescriptor
    ): MethodCallResult = {
        val returnType = methodDescriptor.returnType
        val potentialExceptions = getPotentialExceptions(pc)
        if (returnType.isVoidType) {
            MethodCallResult(potentialExceptions)
        } else {
            MethodCallResult(TypedValue(pc, returnType), potentialExceptions)
        }
    }

    protected[this] def handleInstanceBasedInvoke(
        pc:               Int,
        methodDescriptor: MethodDescriptor,
        receiverIsNull:   Answer
    ): MethodCallResult = {
        val potentialExceptions =
            receiverIsNull match {
                case Yes ⇒
                    // That's it!
                    return justThrows(VMNullPointerException(pc));

                case Unknown if throwNullPointerExceptionOnMethodCall ⇒
                    VMNullPointerException(pc) :: getPotentialExceptions(pc)

                case /*No or Unknown & DoNotThrowNullPointerException*/ _ ⇒
                    getPotentialExceptions(pc)
            }
        val returnType = methodDescriptor.returnType
        if (returnType.isVoidType)
            MethodCallResult(potentialExceptions)
        else
            MethodCallResult(TypedValue(pc, returnType), potentialExceptions)
    }

    protected[this] def handleInstanceBasedInvoke(
        pc:               Int,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult = {
        val receiverIsNull = refIsNull(pc, operands.last)
        handleInstanceBasedInvoke(pc, methodDescriptor, receiverIsNull)
    }

    /**
     * @param methodDescriptor The method descriptor as specified by the invoke
     *      instruction.
     *      In case of the invocation of a signature polymorphic method using
     *      [[org.opalj.br.instructions.INVOKEVIRTUAL]] the descriptor of the
     *      invoked method may differ from the descriptor used by the method. Nevertheless,
     *      the [[MethodCallResult]] has to satisfy the requirements of the caller. In
     *      particular regarding the return type.
     */
    protected[this] def handleInstanceBasedInvoke(
        pc:               Int,
        methodDescriptor: MethodDescriptor,
        targetMethods:    Set[Method],
        receiverIsNull:   Answer,
        operands:         Operands
    ): MethodCallResult = {
        handleInstanceBasedInvoke(pc, methodDescriptor, receiverIsNull)
    }

}
