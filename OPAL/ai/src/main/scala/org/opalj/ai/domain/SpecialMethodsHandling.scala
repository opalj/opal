/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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

import scala.collection.immutable.ArraySeq

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
    callingDomain: ValuesFactory with ReferenceValuesDomain with IntegerValuesDomain with Configuration with TheCode =>

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

        val length :: destPos :: dest :: sourcePos :: source :: _ = operands
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
            ArraySeq(ObjectType.Object, IntegerType, ObjectType.Object, IntegerType, IntegerType),
            VoidType
        )
    }

}
