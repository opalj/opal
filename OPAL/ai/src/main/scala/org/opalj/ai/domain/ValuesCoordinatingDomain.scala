/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.opalj.br.FieldType
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.BootstrapMethod
import org.opalj.br.MethodDescriptor

/**
 * Can be mixed in to create a `Domain` that is intended to be used to coordinate the
 * exchange of values between different domains.
 *
 * This domain does not prescribe the semantics of any values, but
 * instead implements methods that perform computations.
 *
 * This domain directly inherits from [[Domain]] and can, thus, directly be used
 * to create a final domain.
 *
 * ==Core Properties==
 *  - Concrete base implementation of the following domains:
 *      - [[Configuration]]
 *      - [[MethodCallsDomain]]
 *      - [[FieldAccessesDomain]]
 *      - [[MonitorInstructionsDomain]]
 *  - Thread safe.
 *  - Reusable.
 *
 * @author Michael Eichberg
 */
trait ValuesCoordinatingDomain extends CorrelationalDomain with Configuration {

    def throwClassCastException: Boolean = true
    def throwNullPointerExceptionOnThrow: Boolean = true
    def abortProcessingExceptionsOfCalledMethodsOnUnknownException: Boolean = false
    def abortProcessingThrownExceptionsOnUnknownException: Boolean = false
    def throwExceptionsOnMethodCall: ExceptionsRaisedByCalledMethod = {
        ExceptionsRaisedByCalledMethods.Any
    }
    def throwNullPointerExceptionOnMethodCall: Boolean = true
    def throwNullPointerExceptionOnFieldAccess: Boolean = true
    def throwArithmeticExceptions: Boolean = true
    def throwNullPointerExceptionOnMonitorAccess: Boolean = true
    def throwIllegalMonitorStateException: Boolean = true
    def throwNullPointerExceptionOnArrayAccess: Boolean = true
    def throwArrayIndexOutOfBoundsException: Boolean = true
    def throwArrayStoreException: Boolean = true
    def throwNegativeArraySizeException: Boolean = true
    def throwClassNotFoundException: Boolean = true

    /*override*/ def invokevirtual(
        pc:               Int,
        declaringClass:   ReferenceType,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult = {
        throw new UnsupportedOperationException
    }

    /*override*/ def invokeinterface(
        pc:               Int,
        declaringClass:   ObjectType,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult = {
        throw new UnsupportedOperationException
    }

    /*override*/ def invokespecial(
        pc:               Int,
        declaringClass:   ObjectType,
        isInterface:      Boolean,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult = {
        throw new UnsupportedOperationException
    }

    /*override*/ def invokestatic(
        pc:               Int,
        declaringClass:   ObjectType,
        isInterface:      Boolean,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): MethodCallResult = {
        throw new UnsupportedOperationException
    }

    /*override*/ def invokedynamic(
        pc:               Int,
        bootstrapMethod:  BootstrapMethod,
        name:             String,
        methodDescriptor: MethodDescriptor,
        operands:         Operands
    ): Computation[DomainValue, ExceptionValues] = {
        throw new UnsupportedOperationException
    }

    /* override*/ def getfield(
        pc:             Int,
        objectref:      DomainValue,
        declaringClass: ObjectType,
        name:           String,
        fieldType:      FieldType
    ): Computation[DomainValue, ExceptionValue] = {
        throw new UnsupportedOperationException
    }

    /*override*/ def getstatic(
        pc:             Int,
        declaringClass: ObjectType,
        name:           String,
        fieldType:      FieldType
    ): Computation[DomainValue, Nothing] = {
        throw new UnsupportedOperationException
    }

    /*override*/ def putfield(
        pc:             Int,
        objectref:      DomainValue,
        value:          DomainValue,
        declaringClass: ObjectType,
        name:           String,
        fieldType:      FieldType
    ): Computation[Nothing, ExceptionValue] = {
        throw new UnsupportedOperationException
    }

    /*override*/ def putstatic(
        pc:             Int,
        value:          DomainValue,
        declaringClass: ObjectType,
        name:           String,
        fieldType:      FieldType
    ): Computation[Nothing, Nothing] = {
        throw new UnsupportedOperationException
    }

    /*override*/ def monitorenter(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        throw new UnsupportedOperationException
    }

    /*override*/ def monitorexit(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValues] = {
        throw new UnsupportedOperationException
    }

    /*override*/ def returnVoid(pc: Int): Computation[Nothing, ExceptionValue] =
        throw new UnsupportedOperationException

    /*override*/ def abruptMethodExecution(pc: Int, exception: ExceptionValue): Unit =
        throw new UnsupportedOperationException

    /*override*/ def areturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] =
        throw new UnsupportedOperationException

    /*override*/ def dreturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] =
        throw new UnsupportedOperationException

    /*override*/ def freturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] =
        throw new UnsupportedOperationException

    /*override*/ def ireturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] =
        throw new UnsupportedOperationException

    /*override*/ def lreturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] =
        throw new UnsupportedOperationException

    /*override*/ def l2d(pc: Int, value: DomainValue): DomainValue =
        throw new UnsupportedOperationException
    /*override*/ def l2f(pc: Int, value: DomainValue): DomainValue =
        throw new UnsupportedOperationException
    /*override*/ def l2i(pc: Int, value: DomainValue): DomainValue =
        throw new UnsupportedOperationException

    /*override*/ def i2d(pc: Int, value: DomainValue): DomainValue =
        throw new UnsupportedOperationException
    /*override*/ def i2f(pc: Int, value: DomainValue): DomainValue =
        throw new UnsupportedOperationException
    /*override*/ def i2l(pc: Int, value: DomainValue): DomainValue =
        throw new UnsupportedOperationException

    /*override*/ def f2d(pc: Int, value: DomainValue): DomainValue =
        throw new UnsupportedOperationException
    /*override*/ def f2i(pc: Int, value: DomainValue): DomainValue =
        throw new UnsupportedOperationException
    /*override*/ def f2l(pc: Int, value: DomainValue): DomainValue =
        throw new UnsupportedOperationException

    /*override*/ def d2f(pc: Int, value: DomainValue): DomainValue =
        throw new UnsupportedOperationException
    /*override*/ def d2i(pc: Int, value: DomainValue): DomainValue =
        throw new UnsupportedOperationException
    /*override*/ def d2l(pc: Int, value: DomainValue): DomainValue =
        throw new UnsupportedOperationException

    /*override*/ def lshl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        throw new UnsupportedOperationException

    /*override*/ def lshr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        throw new UnsupportedOperationException

    /*override*/ def lushr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        throw new UnsupportedOperationException
}
