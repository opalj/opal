/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi

/**
 * Common super class of the `reference_kind`s used by the constant pool's
 * CONSTANT_MethodHandle_info structure.
 *
 * @author Michael Eichberg
 */
sealed abstract class ReferenceKind {

    def referenceKind: Int

    def referenceKindName: String

}

/**
 * Factory for `ReferenceKind` objects.
 */
object ReferenceKind {

    private[this] val referenceKinds: Array[ReferenceKind] = Array(
        /* 0*/ null, // <=> Index 0 is not used
        /* 1*/ REF_getField,
        /* 2*/ REF_getStatic,
        /* 3*/ REF_putField,
        /* 4*/ REF_putStatic,
        /* 5*/ REF_invokeVirtual,
        /* 6*/ REF_invokeStatic,
        /* 7*/ REF_invokeSpecial,
        /* 8*/ REF_newInvokeSpecial,
        /* 9*/ REF_invokeInterface
    )

    def apply(referenceKindID: Int): ReferenceKind = referenceKinds(referenceKindID)
}

case object REF_getField extends ReferenceKind {
    final val referenceKind = 1
    final val referenceKindName = "REF_getField"
}

case object REF_getStatic extends ReferenceKind {
    final val referenceKind = 2
    final val referenceKindName = "REF_getStatic"
}

case object REF_putField extends ReferenceKind {
    final val referenceKind = 3
    final val referenceKindName = "REF_putField"
}

case object REF_putStatic extends ReferenceKind {
    final val referenceKind = 4
    final val referenceKindName = "REF_putStatic"
}

case object REF_invokeVirtual extends ReferenceKind {
    final val referenceKind = 5
    final val referenceKindName = "REF_invokeVirtual"
}

case object REF_invokeStatic extends ReferenceKind {
    final val referenceKind = 6
    final val referenceKindName = "REF_invokeStatic"
}

case object REF_invokeSpecial extends ReferenceKind {
    final val referenceKind = 7
    final val referenceKindName = "REF_invokeSpecial"
}

case object REF_newInvokeSpecial extends ReferenceKind {
    final val referenceKind = 8
    final val referenceKindName = "REF_newInvokeSpecial"
}

case object REF_invokeInterface extends ReferenceKind {
    final val referenceKind = 9
    final val referenceKindName = "REF_invokeInterface"
}

