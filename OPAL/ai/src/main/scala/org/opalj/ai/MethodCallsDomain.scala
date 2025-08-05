/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.br.BootstrapMethod
import org.opalj.br.ClassType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ReferenceType

/**
 * Defines all methods related to the invocation of other methods.
 *
 * @author Michael Eichberg
 */
trait MethodCallsDomain { this: ValuesDomain =>

    type MethodCallResult = Computation[DomainValue, ExceptionValues]

    def invokevirtual(
        pc:               Int,
        declaringClass:   ReferenceType, // e.g., Array[] x = ...; x.clone()
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult

    def invokeinterface(
        pc:               Int,
        declaringClass:   ClassType,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult

    def invokespecial(
        pc:               Int,
        declaringClass:   ClassType,
        isInterface:      Boolean,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult

    def invokestatic(
        pc:               Int,
        declaringClass:   ClassType,
        isInterface:      Boolean,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult

    def invokedynamic(
        pc:               Int,
        bootstrapMethod:  BootstrapMethod,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult

}
