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

import org.opalj.collection.immutable.:&:
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.VoidType
import org.opalj.br.IntegerType
import org.opalj.ai.Configuration
import org.opalj.ai.IntegerValuesDomain
import org.opalj.ai.ReferenceValuesDomain

/**
 * Hard-codes some part of the semantics of some very high-profile (native) methods of the JDK
 * (for example, `System.arraycopy`).
 *
 * ==Mixin Order==
 *  This method should be mixed in (lexically) after those domains that provide the
 *  basic handling of unresolved methods (e.g., using "just" type information) but before
 *  those that actually invoke a method!
 *
 * @author Michael Eichberg
 */
trait SpecialMethodsHandling extends MethodCallsHandling {
    callingDomain: ValuesFactory with ReferenceValuesDomain with IntegerValuesDomain with Configuration with TheCode ⇒

    import SpecialMethodsHandling._

    abstract override def invokestatic(
        pc:            Int,
        declaringType: ObjectType,
        isInterface:   Boolean,
        name:          String,
        descriptor:    MethodDescriptor,
        operands:      Operands
    ): MethodCallResult = {

        if (!(
            (declaringType eq ObjectType.System) &&
            name == "arraycopy" && descriptor == SystemArraycopyDescriptor
        )) {
            return super.invokestatic(pc, declaringType, isInterface, name, descriptor, operands);
        }

        // ON THE USAGE OF IMMEDIATE_VM_EXCEPTIONS
        // Please note that we use "VM exceptions" in the following because System.arraycopy
        // is native (implemented by the JVM) AND the thrown exceptions have never escaped in any
        // way. Hence, the exceptions are fresh, the type is precise and the value is properly
        // initialized in a well-defined(fixed) manner.

        val length :&: destPos :&: dest :&: sourcePos :&: source :&: _ = operands
        val sourceIsNull = refIsNull(pc, source)
        val destIsNull = refIsNull(pc, dest)
        if (sourceIsNull.isYes || destIsNull.isYes) {
            return justThrows(VMNullPointerException(pc)); // <=== early return
        }

        var exceptions: List[ExceptionValue] = List.empty
        if (sourceIsNull.isUnknown || destIsNull.isUnknown)
            exceptions ::= VMNullPointerException(pc)

        // IMPROVE The support for identifying ArrayStoreExceptions for System.arraycopy methods
        exceptions ::= VMArrayStoreException(pc)

        exceptions ::=
            InitializedObjectValue(
                ValueOriginForImmediateVMException(pc),
                ObjectType.IndexOutOfBoundsException
            )
        if (intIsSomeValueInRange(pc, sourcePos, 0, Int.MaxValue).isNo ||
            intIsSomeValueInRange(pc, destPos, 0, Int.MaxValue).isNo ||
            intIsSomeValueInRange(pc, length, 0, Int.MaxValue).isNo)
            ThrowsException(exceptions);
        else
            MethodCallResult(exceptions)
    }
}

object SpecialMethodsHandling {

    final val SystemArraycopyDescriptor = {
        MethodDescriptor(
            IndexedSeq(ObjectType.Object, IntegerType, ObjectType.Object, IntegerType, IntegerType),
            VoidType
        )
    }

}
