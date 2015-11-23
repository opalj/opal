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

import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.VoidType
import org.opalj.br.IntegerType
import org.opalj.ai.Configuration
import org.opalj.ai.IntegerValuesDomain
import org.opalj.ai.ReferenceValuesDomain
import org.opalj.br.ObjectType.Object

/**
 * ==Mixin Order==
 *  This method should be mixed in after those domains that provide the
 *  basic handling of unresolved methods (e.g., using "just" type information).
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 */
trait SpecialMethodsHandling extends MethodCallsHandling {
    callingDomain: ValuesFactory with ReferenceValuesDomain with IntegerValuesDomain with Configuration with TheCode ⇒

    import SpecialMethodsHandling._

    abstract override def invokestatic(
        pc:                 PC,
        declaringClassType: ObjectType,
        name:               String,
        methodDescriptor:   MethodDescriptor,
        operands:           Operands
    ): MethodCallResult = {

        if (!(
            (declaringClassType eq ObjectType.System) &&
            name == "arraycopy" && methodDescriptor == arraycopyDescriptor
        ))
            return super.invokestatic(pc, declaringClassType, name, methodDescriptor, operands);

        val List(length, destPos, dest, sourcePos, source) = operands
        val sourceIsNull = refIsNull(pc, source)
        val destIsNull = refIsNull(pc, dest)
        if (sourceIsNull.isYes || destIsNull.isYes) {
            return justThrows(NullPointerException(pc)); // <=== early return
        }

        var exceptions: List[ExceptionValue] = List.empty
        if (sourceIsNull.isUnknown || destIsNull.isUnknown)
            exceptions ::= NullPointerException(pc)

        // IMPROVE The support for identifying ArrayStoreExceptions for System.arraycopy methods
        exceptions ::= ArrayStoreException(pc)

        exceptions ::= InitializedObjectValue(pc, ObjectType.IndexOutOfBoundsException)
        if (intIsSomeValueInRange(pc, sourcePos, 0, Int.MaxValue).isNo ||
            intIsSomeValueInRange(pc, destPos, 0, Int.MaxValue).isNo ||
            intIsSomeValueInRange(pc, length, 0, Int.MaxValue).isNo)
            ThrowsException(exceptions);
        else
            MethodCallResult(exceptions)
    }
}

object SpecialMethodsHandling {

    import org.opalj.br.ObjectType.Object

    final val arraycopyDescriptor = {
        MethodDescriptor(
            IndexedSeq(Object, IntegerType, Object, IntegerType, IntegerType),
            VoidType
        )
    }

}
