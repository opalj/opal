/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package ai

import reflect.ClassTag

/**
 * The values modules of a [[de.tud.cs.st.bat.resolved.ai.Domain]].
 *
 * Some of the value types are fixed and cannot be directly extended. This
 * part is required by BATAI to perform the abstract interpretation.
 */
class DefaultDomain extends AbstractDefaultDomain {

    type DomainValue = Value
    val DomainValueTag: ClassTag[DomainValue] = implicitly

    type DomainNoLegalValue = NoLegalValue
    def NoLegalValue(initialReason: String): DomainNoLegalValue =
        new NoLegalValue(initialReason)

    trait CTIntegerValue extends super.CTIntegerValue {

        def merge(value: DomainValue): Update[DomainValue] =
            value match { case CTIntegerValue() ⇒ NoUpdate }
    }
    object CTIntegerValue extends CTIntegerValue {
        private[this] val instance = new CTIntegerValue {}
        def apply() = instance
        def unapply(ctIntVal: CTIntegerValue): Boolean = ctIntVal ne null
    }

    case object SomeBooleanValue extends CTIntegerValue with TypedValue {
        final def valueType = BooleanType

        // Note, we cannot implement some general merging over here!
        // E.g., if the other value is of type CTIntegerValue and represents 
        // other values than 0 and 1 then some special handling needs to be performed!

        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case SomeBooleanValue         ⇒ NoUpdate
            case other: NoLegalValue      ⇒ MetaInformationUpdate(other)
            case other @ SomeByteValue    ⇒ StructuralUpdate(other)
            case other @ SomeShortValue   ⇒ StructuralUpdate(other)
            case other @ SomeIntegerValue ⇒ StructuralUpdate(other)
            case other @ SomeCharValue    ⇒ StructuralUpdate(other)
            case other: CTIntegerValue    ⇒ AIImplementationError(missingSupport(other))
            case other                    ⇒ StructuralUpdate(NoLegalValue(incompatibleValues(other)))
        }
    }
    case object SomeByteValue extends CTIntegerValue with TypedValue {
        final def valueType = ByteType

        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case SomeByteValue            ⇒ NoUpdate
            case other: NoLegalValue      ⇒ MetaInformationUpdate(other)
            case SomeBooleanValue         ⇒ NoUpdate
            case other @ SomeShortValue   ⇒ StructuralUpdate(other)
            case other @ SomeIntegerValue ⇒ StructuralUpdate(other)
            case other @ SomeCharValue    ⇒ StructuralUpdate(other)
            case other: CTIntegerValue    ⇒ AIImplementationError(missingSupport(other))
            case other                    ⇒ StructuralUpdate(NoLegalValue(incompatibleValues(other)))
        }
    }

    case object SomeShortValue extends CTIntegerValue with TypedValue {
        final def valueType = ShortType
        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case SomeShortValue           ⇒ NoUpdate
            case other: NoLegalValue      ⇒ MetaInformationUpdate(other)
            case SomeBooleanValue         ⇒ NoUpdate
            case SomeByteValue            ⇒ NoUpdate
            case other @ SomeIntegerValue ⇒ StructuralUpdate(other)
            case other @ SomeCharValue    ⇒ StructuralUpdate(SomeIntegerValue)
            case other: CTIntegerValue    ⇒ AIImplementationError(missingSupport(other))
            case other                    ⇒ StructuralUpdate(NoLegalValue(incompatibleValues(other)))
        }
    }

    case object SomeCharValue extends CTIntegerValue with TypedValue {
        final def valueType = CharType

        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case SomeCharValue            ⇒ NoUpdate
            case other: NoLegalValue      ⇒ MetaInformationUpdate(other)
            case SomeBooleanValue         ⇒ NoUpdate
            case SomeByteValue            ⇒ NoUpdate
            case other @ SomeShortValue   ⇒ StructuralUpdate(SomeIntegerValue)
            case other @ SomeIntegerValue ⇒ StructuralUpdate(other)
            case other: CTIntegerValue    ⇒ AIImplementationError(missingSupport(other))
            case other                    ⇒ StructuralUpdate(NoLegalValue(incompatibleValues(other)))
        }
    }
    case object SomeIntegerValue extends CTIntegerValue with TypedValue {

        final def valueType = ShortType

        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case SomeIntegerValue      ⇒ NoUpdate
            case other: NoLegalValue   ⇒ MetaInformationUpdate(other)
            case SomeCharValue         ⇒ NoUpdate
            case SomeBooleanValue      ⇒ NoUpdate
            case SomeByteValue         ⇒ NoUpdate
            case SomeShortValue        ⇒ NoUpdate
            // the following cases are just defined to catch errors/to help debug the AI
            case other: CTIntegerValue ⇒ AIImplementationError(missingSupport(other))
            case other                 ⇒ StructuralUpdate(NoLegalValue(incompatibleValues(other)))
        }
    }
    val IntegerConstant0 = SomeIntegerValue
    case object SomeFloatValue extends SomeFloatValue {
        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case SomeFloatValue      ⇒ NoUpdate
            case other: NoLegalValue ⇒ MetaInformationUpdate(other)
            case other               ⇒ StructuralUpdate(NoLegalValue(incompatibleValues(other)))
        }
    }

    /**
     * Abstracts over all values with computational type `reference`.
     */

    case object SomeReferenceValue extends ReferenceValue {
        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case other: ReferenceValue ⇒ NoUpdate
            case other: NoLegalValue   ⇒ MetaInformationUpdate(other)
            case other                 ⇒ StructuralUpdate(NoLegalValue(incompatibleValues(other)))
        }
    }

    case object NullValue extends ReferenceValue {
        override def merge(value: Value): Update[DomainValue] = value match {
            case NullValue                  ⇒ NoUpdate
            case other @ SomeReferenceValue ⇒ StructuralUpdate(other)
            case other: ReferenceValue      ⇒ StructuralUpdate(other)
            case other: NoLegalValue        ⇒ MetaInformationUpdate(other)
            case other                      ⇒ StructuralUpdate(NoLegalValue(incompatibleValues(other)))
        }
    }
    def nullValue = NullValue

    case class AReferenceValue(
        override val valueType: ReferenceType)
            extends ReferenceValue {

        // TODO [AI] We need some support to consult the domain to decide what we want to do.

        override def merge(value: DomainValue): Update[DomainValue] = value match {
            // What we do here is extremely simplistic, but this is basically all we can
            // do when we do not have the class hierarchy available.
            case AReferenceValue(`valueType`) ⇒ NoUpdate
            case other @ SomeReferenceValue   ⇒ StructuralUpdate(other)
            case NullValue                    ⇒ NoUpdate
            case other: ReferenceValue        ⇒ BATError("cannot yet merge reference values!")
            case other: NoLegalValue          ⇒ MetaInformationUpdate(other)
            case other                        ⇒ StructuralUpdate(NoLegalValue(incompatibleValues(other)))
        }

        override def equals(other: Any): Boolean = {
            if (other.isInstanceOf[AReferenceValue]) {
                this.valueType ==
                    other.asInstanceOf[AReferenceValue].valueType
            } else {
                false
            }
        }
        override def hashCode: Int = -valueType.hashCode()

        override def toString: String = "ReferenceTypeValue: "+valueType.toJava
    }

    def ReferenceValue(referenceType: ReferenceType): AReferenceValue =
        new AReferenceValue(referenceType)

    case object SomeLongValue extends SomeLongValue {
        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case SomeLongValue       ⇒ NoUpdate
            case other: NoLegalValue ⇒ MetaInformationUpdate(other)
            case other               ⇒ StructuralUpdate(NoLegalValue(incompatibleValues(other)))
        }
    }

    case object SomeDoubleValue extends SomeDoubleValue {
        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case SomeDoubleValue     ⇒ NoUpdate
            case other: NoLegalValue ⇒ MetaInformationUpdate(other)
            case other               ⇒ StructuralUpdate(NoLegalValue(incompatibleValues(other)))
        }
    }

    class ReturnAddressValue(
        val addresses: Set[Int])
            extends super.ReturnAddressValue {

        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case ReturnAddressValue(otherAddresses) ⇒ {
                if (otherAddresses subsetOf this.addresses)
                    NoUpdate
                else
                    StructuralUpdate(ReturnAddressValue(this.addresses ++ otherAddresses))
            }
            case other: NoLegalValue ⇒ MetaInformationUpdate(other)
            case other               ⇒ StructuralUpdate(NoLegalValue(incompatibleValues(other)))
        }
    }

    type DomainReturnAddressValue = ReturnAddressValue

    def ReturnAddressValue(addresses: Set[Int]): DomainReturnAddressValue =
        new ReturnAddressValue(addresses)

    def ReturnAddressValue(address: Int): DomainReturnAddressValue =
        new ReturnAddressValue(Set(address))

}





