/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.BootstrapMethod
import org.opalj.br.MethodDescriptor

/**
 * Most basic handling of method invocations that determines the value that is
 * put onto the operand stack/returned by a method call based on the called method's
 * return type.
 *
 * '''This implementation completely ignores exceptions and/or errors thrown by the method.'''
 *
 * (Linkage related exceptions are currently generally ignored.)
 *
 * @note By ignoring potentially thrown exceptions it may be the case that not all
 *      possible paths in a program are explored and that the overall analysis may not be
 *      sound.
 *
 * @author Michael Eichberg
 */
trait SimpleTypeLevelInvokeInstructions extends MethodCallsDomain {
    domain: ReferenceValuesDomain with ValuesFactory with Configuration =>

    protected[this] def handleInstanceBasedInvoke(
        pc:               Int,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult = {
        refIsNull(pc, operands.last) match {
            case Yes =>
                justThrows(VMNullPointerException(pc))
            case Unknown if throwNullPointerExceptionOnMethodCall =>
                val returnType = methodDescriptor.returnType
                val exceptionValue = Set(VMNullPointerException(pc))
                if (returnType.isVoidType)
                    ComputationWithSideEffectOrException(exceptionValue)
                else
                    ComputedValueOrException(TypedValue(pc, returnType), exceptionValue)
            case /*No or Unknown & DoNotThrowNullPointerException*/ _ =>
                val returnType = methodDescriptor.returnType
                if (returnType.isVoidType)
                    ComputationWithSideEffectOnly
                else
                    ComputedValue(TypedValue(pc, returnType))
        }
    }

    /*override*/ def invokevirtual(
        pc:               Int,
        declaringClass:   ReferenceType,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult = {
        handleInstanceBasedInvoke(pc, methodDescriptor, operands)
    }

    /*override*/ def invokeinterface(
        pc:               Int,
        declaringClass:   ObjectType,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult = {
        handleInstanceBasedInvoke(pc, methodDescriptor, operands)
    }

    /*override*/ def invokespecial(
        pc:               Int,
        declaringClass:   ObjectType,
        isInterface:      Boolean,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult = {
        handleInstanceBasedInvoke(pc, methodDescriptor, operands)
    }

    /*override*/ def invokestatic(
        pc:               Int,
        declaringClass:   ObjectType,
        isInterface:      Boolean,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult = {
        val returnType = methodDescriptor.returnType
        if (returnType.isVoidType)
            ComputationWithSideEffectOnly
        else
            ComputedValue(TypedValue(pc, returnType))
    }

    /*override*/ def invokedynamic(
        pc:               Int,
        bootstrapMethod:  BootstrapMethod,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): Computation[DomainValue, ExceptionValues] = {
        val returnType = methodDescriptor.returnType
        if (returnType.isVoidType)
            ComputationWithSideEffectOnly
        else
            ComputedValue(TypedValue(pc, returnType))
    }
}
