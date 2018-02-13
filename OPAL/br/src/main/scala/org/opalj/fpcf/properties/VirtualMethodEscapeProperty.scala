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
package fpcf
package properties

sealed trait VirtualMethodEscapePropertyMetaInformation extends PropertyMetaInformation {
    final type Self = VirtualMethodEscapeProperty
}

sealed case class VirtualMethodEscapeProperty(
        escapeProperty: EscapeProperty
) extends Property with VirtualMethodEscapePropertyMetaInformation {
    final def key: PropertyKey[VirtualMethodEscapeProperty] = VirtualMethodEscapeProperty.key

    final override def isRefinable: Boolean = escapeProperty.isRefinable

    def meet(other: VirtualMethodEscapeProperty) =
        VirtualMethodEscapeProperty(escapeProperty meet other.escapeProperty)
}

object VirtualMethodEscapeProperty extends VirtualMethodEscapePropertyMetaInformation {

    def apply(
        escapeProperty: EscapeProperty
    ): VirtualMethodEscapeProperty = escapeProperty match {
        case GlobalEscape ⇒ VGlobalEscape
        case EscapeViaStaticField ⇒ VEscapeViaStaticField
        case EscapeViaHeapObject ⇒ VEscapeViaHeapObject

        case NoEscape ⇒ VNoEscape
        case EscapeInCallee ⇒ VEscapeInCallee
        case EscapeViaParameter ⇒ VEscapeViaParameter
        case EscapeViaReturn ⇒ VEscapeViaReturn
        case EscapeViaAbnormalReturn ⇒ VEscapeViaAbnormalReturn
        case EscapeViaParameterAndReturn ⇒ VEscapeViaParameterAndReturn
        case EscapeViaParameterAndAbnormalReturn ⇒ VEscapeViaParameterAndAbnormalReturn
        case EscapeViaNormalAndAbnormalReturn ⇒ VEscapeViaNormalAndAbnormalReturn
        case EscapeViaParameterAndNormalAndAbnormalReturn ⇒ VEscapeViaParameterAndNormalAndAbnormalReturn

        case AtMost(NoEscape) ⇒ VAtMostNoEscape
        case AtMost(EscapeInCallee) ⇒ VAtMostEscapeInCallee
        case AtMost(EscapeViaParameter) ⇒ VAtMostEscapeViaParameter
        case AtMost(EscapeViaReturn) ⇒ VAtMostEscapeViaReturn
        case AtMost(EscapeViaAbnormalReturn) ⇒ VAtMostEscapeViaAbnormalReturn
        case AtMost(EscapeViaParameterAndReturn) ⇒ VAtMostEscapeViaParameterAndReturn
        case AtMost(EscapeViaParameterAndAbnormalReturn) ⇒ VAtMostEscapeViaParameterAndAbnormalReturn
        case AtMost(EscapeViaNormalAndAbnormalReturn) ⇒ VAtMostEscapeViaNormalAndAbnormalReturn
        case AtMost(EscapeViaParameterAndNormalAndAbnormalReturn) ⇒ VAtMostEscapeViaParameterAndNormalAndAbnormalReturn

        case Conditional(NoEscape) ⇒ VCondNoEscape
        case Conditional(EscapeInCallee) ⇒ VCondEscapeInCallee
        case Conditional(EscapeViaParameter) ⇒ VCondEscapeViaParameter
        case Conditional(EscapeViaReturn) ⇒ VCondEscapeViaReturn
        case Conditional(EscapeViaAbnormalReturn) ⇒ VCondEscapeViaAbnormalReturn
        case Conditional(EscapeViaParameterAndReturn) ⇒ VCondEscapeViaParameterAndReturn
        case Conditional(EscapeViaParameterAndAbnormalReturn) ⇒ VCondEscapeViaParameterAndAbnormalReturn
        case Conditional(EscapeViaNormalAndAbnormalReturn) ⇒ VCondEscapeViaNormalAndAbnormalReturn
        case Conditional(EscapeViaParameterAndNormalAndAbnormalReturn) ⇒ VCondEscapeViaParameterAndNormalAndAbnormalReturn
        case Conditional(AtMost(NoEscape)) ⇒ VAtMostCondNoEscape
        case Conditional(AtMost(EscapeInCallee)) ⇒ VAtMostCondEscapeInCallee
        case Conditional(AtMost(EscapeViaParameter)) ⇒ VAtMostCondEscapeViaParameter
        case Conditional(AtMost(EscapeViaReturn)) ⇒ VAtMostCondEscapeViaReturn
        case Conditional(AtMost(EscapeViaAbnormalReturn)) ⇒ VAtMostCondEscapeViaAbnormalReturn
        case Conditional(AtMost(EscapeViaParameterAndReturn)) ⇒ VAtMostCondEscapeViaParameterAndReturn
        case Conditional(AtMost(EscapeViaParameterAndAbnormalReturn)) ⇒ VAtMostCondEscapeViaParameterAndAbnormalReturn
        case Conditional(AtMost(EscapeViaNormalAndAbnormalReturn)) ⇒ VAtMostCondEscapeViaNormalAndAbnormalReturn
        case Conditional(AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)) ⇒ VAtMostCondEscapeViaParameterAndNormalAndAbnormalReturn

        case _ ⇒ throw new RuntimeException(s"Unsupported property: $escapeProperty")

    }

    final val VGlobalEscape = new VirtualMethodEscapeProperty(GlobalEscape)
    final val VEscapeViaHeapObject = new VirtualMethodEscapeProperty(EscapeViaHeapObject)
    final val VEscapeViaStaticField = new VirtualMethodEscapeProperty(EscapeViaStaticField)

    final val VNoEscape = new VirtualMethodEscapeProperty(NoEscape)
    final val VEscapeInCallee = new VirtualMethodEscapeProperty(EscapeInCallee)

    final val VEscapeViaParameter = new VirtualMethodEscapeProperty(EscapeViaParameter)
    final val VEscapeViaReturn = new VirtualMethodEscapeProperty(EscapeViaReturn)
    final val VEscapeViaAbnormalReturn = new VirtualMethodEscapeProperty(EscapeViaAbnormalReturn)

    final val VEscapeViaNormalAndAbnormalReturn = VirtualMethodEscapeProperty(EscapeViaNormalAndAbnormalReturn)
    final val VEscapeViaParameterAndReturn = new VirtualMethodEscapeProperty(EscapeViaParameterAndReturn)
    final val VEscapeViaParameterAndAbnormalReturn = new VirtualMethodEscapeProperty(EscapeViaParameterAndAbnormalReturn)
    final val VEscapeViaParameterAndNormalAndAbnormalReturn = new VirtualMethodEscapeProperty(EscapeViaParameterAndNormalAndAbnormalReturn)

    final val VAtMostNoEscape = new VirtualMethodEscapeProperty(AtMost(NoEscape))
    final val VAtMostEscapeInCallee = new VirtualMethodEscapeProperty(AtMost(EscapeInCallee))

    final val VAtMostEscapeViaParameter = new VirtualMethodEscapeProperty(AtMost(EscapeViaParameter))
    final val VAtMostEscapeViaReturn = new VirtualMethodEscapeProperty(AtMost(EscapeViaReturn))
    final val VAtMostEscapeViaAbnormalReturn = new VirtualMethodEscapeProperty(AtMost(EscapeViaAbnormalReturn))

    final val VAtMostEscapeViaNormalAndAbnormalReturn = VirtualMethodEscapeProperty(AtMost(EscapeViaNormalAndAbnormalReturn))
    final val VAtMostEscapeViaParameterAndReturn = new VirtualMethodEscapeProperty(AtMost(EscapeViaParameterAndReturn))
    final val VAtMostEscapeViaParameterAndAbnormalReturn = new VirtualMethodEscapeProperty(AtMost(EscapeViaParameterAndAbnormalReturn))
    final val VAtMostEscapeViaParameterAndNormalAndAbnormalReturn = new VirtualMethodEscapeProperty(AtMost(EscapeViaParameterAndNormalAndAbnormalReturn))

    final val VCondNoEscape = new VirtualMethodEscapeProperty(Conditional(NoEscape))
    final val VCondEscapeInCallee = new VirtualMethodEscapeProperty(Conditional(EscapeInCallee))

    final val VCondEscapeViaParameter = new VirtualMethodEscapeProperty(Conditional(EscapeViaParameter))
    final val VCondEscapeViaReturn = new VirtualMethodEscapeProperty(Conditional(EscapeViaReturn))
    final val VCondEscapeViaAbnormalReturn = new VirtualMethodEscapeProperty(Conditional(EscapeViaAbnormalReturn))

    final val VCondEscapeViaNormalAndAbnormalReturn = VirtualMethodEscapeProperty(Conditional(EscapeViaNormalAndAbnormalReturn))
    final val VCondEscapeViaParameterAndReturn = new VirtualMethodEscapeProperty(Conditional(EscapeViaParameterAndReturn))
    final val VCondEscapeViaParameterAndAbnormalReturn = new VirtualMethodEscapeProperty(Conditional(EscapeViaParameterAndAbnormalReturn))
    final val VCondEscapeViaParameterAndNormalAndAbnormalReturn = new VirtualMethodEscapeProperty(Conditional(EscapeViaParameterAndNormalAndAbnormalReturn))

    final val VAtMostCondNoEscape = new VirtualMethodEscapeProperty(Conditional(AtMost(NoEscape)))
    final val VAtMostCondEscapeInCallee = new VirtualMethodEscapeProperty(Conditional(AtMost(EscapeInCallee)))

    final val VAtMostCondEscapeViaParameter = new VirtualMethodEscapeProperty(Conditional(AtMost(EscapeViaParameter)))
    final val VAtMostCondEscapeViaReturn = new VirtualMethodEscapeProperty(Conditional(AtMost(EscapeViaReturn)))
    final val VAtMostCondEscapeViaAbnormalReturn = new VirtualMethodEscapeProperty(Conditional(AtMost(EscapeViaAbnormalReturn)))

    final val VAtMostCondEscapeViaNormalAndAbnormalReturn = VirtualMethodEscapeProperty(Conditional(AtMost(EscapeViaNormalAndAbnormalReturn)))
    final val VAtMostCondEscapeViaParameterAndReturn = new VirtualMethodEscapeProperty(Conditional(AtMost(EscapeViaParameterAndReturn)))
    final val VAtMostCondEscapeViaParameterAndAbnormalReturn = new VirtualMethodEscapeProperty(Conditional(AtMost(EscapeViaParameterAndAbnormalReturn)))
    final val VAtMostCondEscapeViaParameterAndNormalAndAbnormalReturn = new VirtualMethodEscapeProperty(Conditional(AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)))

    final val key: PropertyKey[VirtualMethodEscapeProperty] = PropertyKey.create[VirtualMethodEscapeProperty](
        "VirtualMethodEscapeProperty",
        VAtMostNoEscape,
        EscapeProperty.cycleResolutionStrategy
    )

}
