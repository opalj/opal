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

import org.opalj.util.{ Answer, Yes, No, Unknown }

import org.opalj.br.{ Type, ObjectType, ReferenceType, FieldType }
import org.opalj.br.MethodDescriptor
import org.opalj.br.BootstrapMethod
import org.opalj.br.analyses.{ Project, ClassHierarchy }

/**
 * A `Domain` that is intended to be used to coordinate the exchange of values between
 * different domains. This domain does not prescribe the semantics of any values, but
 * instead implements all methods that perform computations.
 *
 * @author Michael Eichberg
 */
trait ValuesCoordinatingDomain
        extends Domain
        with ThrowAllPotentialExceptionsConfiguration /*ACTUALLY NOT RELEVANT*/ {

    /*override*/ def invokevirtual(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult =
        throw new UnsupportedOperationException

    /*override*/ def invokeinterface(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult =
        throw new UnsupportedOperationException

    /*override*/ def invokespecial(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult =
        throw new UnsupportedOperationException

    /*override*/ def invokestatic(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult =
        throw new UnsupportedOperationException

    /*override*/ def invokedynamic(
        pc: PC,
        bootstrapMethod: BootstrapMethod,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): Computation[DomainValue, ExceptionValues] =
        throw new UnsupportedOperationException

    /* override*/ def getfield(
        pc: PC,
        objectref: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[DomainValue, ExceptionValue] =
        throw new UnsupportedOperationException

    /*override*/ def getstatic(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[DomainValue, Nothing] =
        throw new UnsupportedOperationException

    /*override*/ def putfield(
        pc: PC,
        objectref: DomainValue,
        value: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[Nothing, ExceptionValue] =
        throw new UnsupportedOperationException

    /*override*/ def putstatic(
        pc: PC,
        value: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[Nothing, Nothing] =
        throw new UnsupportedOperationException

    /*override*/ def monitorenter(
        pc: PC,
        value: DomainValue): Computation[Nothing, ExceptionValue] =
        throw new UnsupportedOperationException

    /*override*/ def monitorexit(
        pc: PC,
        value: DomainValue): Computation[Nothing, ExceptionValue] =
        throw new UnsupportedOperationException

    /*override*/ def returnVoid(pc: PC): Unit =
        throw new UnsupportedOperationException

    /*override*/ def abruptMethodExecution(pc: PC, exception: DomainValue): Unit =
        throw new UnsupportedOperationException

    /*override*/ def areturn(pc: PC, value: DomainValue): Unit =
        throw new UnsupportedOperationException

    /*override*/ def dreturn(pc: PC, value: DomainValue): Unit =
        throw new UnsupportedOperationException

    /*override*/ def freturn(pc: PC, value: DomainValue): Unit =
        throw new UnsupportedOperationException

    /*override*/ def ireturn(pc: PC, value: DomainValue): Unit =
        throw new UnsupportedOperationException

    /*override*/ def lreturn(pc: PC, value: DomainValue): Unit =
        throw new UnsupportedOperationException
}

