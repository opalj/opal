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
 * signature.
 *
 * (Linkage related exceptions are currently generally ignored.)
 *
 * @author Michael Eichberg
 */
trait TypeLevelInvokeInstructions extends MethodCallsHandling {
    domain: ReferenceValuesDomain with TypedValuesFactory with Configuration with TheCode =>

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
        handleInstanceBasedInvoke(pc, methodDescriptor, receiverIsNull = No)
    }

    /*override*/ def invokestatic(
        pc:               Int,
        declaringClass:   ObjectType,
        isInterface:      Boolean,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult = {
        handleInvoke(pc, methodDescriptor)
    }

    /*override*/ def invokedynamic(
        pc:               Int,
        bootstrapMethod:  BootstrapMethod,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult = {
        handleInvoke(pc, methodDescriptor)
    }

}
