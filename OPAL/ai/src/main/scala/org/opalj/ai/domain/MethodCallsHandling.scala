/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import scala.collection.Set
import scala.collection.mutable

import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType

/**
 * Provides support for handling method invocations, but does not handle any invocations directly.
 *
 * @author Michael Eichberg
 */
trait MethodCallsHandling extends MethodCallsDomain {
    domain: ReferenceValuesDomain with TypedValuesFactory with Configuration with TheCode =>

    protected[this] def getPotentialExceptions(pc: Int): List[ExceptionValue] = {
        val exceptionTypes: mutable.Set[ObjectType] = mutable.Set.empty
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
            case ExceptionsRaisedByCalledMethods.Any =>
                add(ObjectType.Throwable)

            case ExceptionsRaisedByCalledMethods.AllExplicitlyHandled =>
                code.handlersFor(pc) foreach { h =>
                    h.catchType match {
                        case None     => add(ObjectType.Throwable)
                        case Some(ex) => add(ex)
                    }
                }
            case ExceptionsRaisedByCalledMethods.Known =>
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
                case Yes =>
                    // That's it!
                    return justThrows(VMNullPointerException(pc));

                case Unknown if throwNullPointerExceptionOnMethodCall =>
                    VMNullPointerException(pc) :: getPotentialExceptions(pc)

                case /*No or Unknown & DoNotThrowNullPointerException*/ _ =>
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
