/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait VirtualMethodEscapePropertyMetaInformation extends PropertyMetaInformation {
    final type Self = VirtualMethodEscapeProperty
}

sealed case class VirtualMethodEscapeProperty(
        escapeProperty: EscapeProperty
) extends Property with VirtualMethodEscapePropertyMetaInformation {
    final def key: PropertyKey[VirtualMethodEscapeProperty] = VirtualMethodEscapeProperty.key

    def meet(other: VirtualMethodEscapeProperty) =
        VirtualMethodEscapeProperty(escapeProperty meet other.escapeProperty)
}

/**
 * Represents the [[EscapeProperty]] for [[org.opalj.br.analyses.VirtualFormalParameter]] objects
 * as entity. The underlying property is an aggregation of escape properties of the underlying
 * parameters of the concrete methods.
 *
 * @author Florian Kuebler
 */
object VirtualMethodEscapeProperty extends VirtualMethodEscapePropertyMetaInformation {

    def apply(
        escapeProperty: EscapeProperty
    ): VirtualMethodEscapeProperty = escapeProperty match {
        case GlobalEscape => VGlobalEscape
        case EscapeViaStaticField => VEscapeViaStaticField
        case EscapeViaHeapObject => VEscapeViaHeapObject

        case NoEscape => VNoEscape
        case EscapeInCallee => VEscapeInCallee
        case EscapeViaParameter => VEscapeViaParameter
        case EscapeViaReturn => VEscapeViaReturn
        case EscapeViaAbnormalReturn => VEscapeViaAbnormalReturn
        case EscapeViaParameterAndReturn => VEscapeViaParameterAndReturn
        case EscapeViaParameterAndAbnormalReturn => VEscapeViaParameterAndAbnormalReturn
        case EscapeViaNormalAndAbnormalReturn => VEscapeViaNormalAndAbnormalReturn
        case EscapeViaParameterAndNormalAndAbnormalReturn => VEscapeViaParameterAndNormalAndAbnormalReturn

        case AtMost(NoEscape) => VAtMostNoEscape
        case AtMost(EscapeInCallee) => VAtMostEscapeInCallee
        case AtMost(EscapeViaParameter) => VAtMostEscapeViaParameter
        case AtMost(EscapeViaReturn) => VAtMostEscapeViaReturn
        case AtMost(EscapeViaAbnormalReturn) => VAtMostEscapeViaAbnormalReturn
        case AtMost(EscapeViaParameterAndReturn) => VAtMostEscapeViaParameterAndReturn
        case AtMost(EscapeViaParameterAndAbnormalReturn) => VAtMostEscapeViaParameterAndAbnormalReturn
        case AtMost(EscapeViaNormalAndAbnormalReturn) => VAtMostEscapeViaNormalAndAbnormalReturn
        case AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) => VAtMostEscapeViaParameterAndNormalAndAbnormalReturn

        case _ => throw new RuntimeException(s"Unsupported property: $escapeProperty")

    }

    final val VGlobalEscape = new VirtualMethodEscapeProperty(GlobalEscape)
    final val VEscapeViaHeapObject = new VirtualMethodEscapeProperty(EscapeViaHeapObject)
    final val VEscapeViaStaticField = new VirtualMethodEscapeProperty(EscapeViaStaticField)

    final val VNoEscape = new VirtualMethodEscapeProperty(NoEscape)
    final val VEscapeInCallee = new VirtualMethodEscapeProperty(EscapeInCallee)

    final val VEscapeViaParameter = new VirtualMethodEscapeProperty(EscapeViaParameter)
    final val VEscapeViaReturn = new VirtualMethodEscapeProperty(EscapeViaReturn)
    final val VEscapeViaAbnormalReturn = new VirtualMethodEscapeProperty(EscapeViaAbnormalReturn)

    final val VEscapeViaNormalAndAbnormalReturn = new VirtualMethodEscapeProperty(EscapeViaNormalAndAbnormalReturn)
    final val VEscapeViaParameterAndReturn = new VirtualMethodEscapeProperty(EscapeViaParameterAndReturn)
    final val VEscapeViaParameterAndAbnormalReturn = new VirtualMethodEscapeProperty(EscapeViaParameterAndAbnormalReturn)
    final val VEscapeViaParameterAndNormalAndAbnormalReturn = new VirtualMethodEscapeProperty(EscapeViaParameterAndNormalAndAbnormalReturn)

    final val VAtMostNoEscape = new VirtualMethodEscapeProperty(AtMost(NoEscape))
    final val VAtMostEscapeInCallee = new VirtualMethodEscapeProperty(AtMost(EscapeInCallee))

    final val VAtMostEscapeViaParameter = new VirtualMethodEscapeProperty(AtMost(EscapeViaParameter))
    final val VAtMostEscapeViaReturn = new VirtualMethodEscapeProperty(AtMost(EscapeViaReturn))
    final val VAtMostEscapeViaAbnormalReturn = new VirtualMethodEscapeProperty(AtMost(EscapeViaAbnormalReturn))

    final val VAtMostEscapeViaNormalAndAbnormalReturn = new VirtualMethodEscapeProperty(AtMost(EscapeViaNormalAndAbnormalReturn))
    final val VAtMostEscapeViaParameterAndReturn = new VirtualMethodEscapeProperty(AtMost(EscapeViaParameterAndReturn))
    final val VAtMostEscapeViaParameterAndAbnormalReturn = new VirtualMethodEscapeProperty(AtMost(EscapeViaParameterAndAbnormalReturn))
    final val VAtMostEscapeViaParameterAndNormalAndAbnormalReturn = new VirtualMethodEscapeProperty(AtMost(EscapeViaParameterAndNormalAndAbnormalReturn))

    final val key: PropertyKey[VirtualMethodEscapeProperty] = PropertyKey.create[DeclaredMethod, VirtualMethodEscapeProperty](
        "VirtualMethodEscapeProperty",
        VAtMostNoEscape
    )

}
