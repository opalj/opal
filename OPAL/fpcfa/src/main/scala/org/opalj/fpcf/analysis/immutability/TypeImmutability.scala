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
package fpcf
package analysis
package immutability

sealed trait TypeImmutabilityPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = TypeImmutability
}

/**
 * Specified if all instances of a respective type (this includes the instances of the
 * type's subtypes) are (conditionally) immutable.
 *
 * This property is of particular interest if the precise type cannot be computed statically and
 * basically depends on the [[AnalysisMode]] and [[ObjectImmutability]].
 *
 * @author Michael Eichberg
 */
sealed trait TypeImmutability
        extends Property
        with TypeImmutabilityPropertyMetaInformation
        with scala.math.Ordered[TypeImmutability] {

    /**
     * Returns the key used by all `TypeImmutability` properties.
     */
    final def key = TypeImmutability.key

    def isMutable: Boolean

    def isConditionallyImmutable: Boolean

    def join(other: TypeImmutability): TypeImmutability
}
/**
 * Common constants use by all [[TypeImmutability]] properties associated with methods.
 */
object TypeImmutability extends TypeImmutabilityPropertyMetaInformation {

    /**
     * The key associated with every [[TypeImmutability]] property.
     */
    final val key: PropertyKey[TypeImmutability] = PropertyKey.create(
        "TypeImmutability",
        // The default property that will be used if no analysis is able
        // to (directly) compute the respective property.
        MutableType,
        // When we have a cycle all properties are necessarily at least conditionally
        // immutable hence, we can leverage the "immutability" of one of the members of
        // the cycle and wait for the automatic propagation...
        ImmutableType
    )
}

/**
 * An instance of the respective class is effectively immutable. I.e., after creation it is not
 * possible for a client to set a field or to call a method that updates the internal state
 *
 */
case object ImmutableType extends TypeImmutability {
    final val isRefineable = false
    final val isMutable = false
    final val isConditionallyImmutable = false

    def join(that: TypeImmutability): TypeImmutability = {
        if (this == that) this else that
    }

    def compare(that: TypeImmutability) = {
        if (this == that) 0 else 1
    }
}

case object UnknownTypeImmutability extends TypeImmutability {
    final val isRefineable = true
    final val isMutable = false
    final val isConditionallyImmutable = false

    def join(that: TypeImmutability): TypeImmutability = {
        if (that == MutableType) MutableType else this
    }

    def compare(that: TypeImmutability) = {
        if (this == that) 0 else -1
    }
}

case object ConditionallyImmutableType extends TypeImmutability {
    final val isRefineable = false
    final val isMutable = false
    final val isConditionallyImmutable = true

    def join(that: TypeImmutability): TypeImmutability = {
        that match {
            case MutableType | UnknownTypeImmutability ⇒ that
            case _                                     ⇒ this
        }
    }

    def compare(that: TypeImmutability) = {
        if (this == that)
            0
        else if (that == UnknownTypeImmutability || that == AtLeastConditionallyImmutableType)
            1
        else
            -1
    }

}

case object AtLeastConditionallyImmutableType extends TypeImmutability {
    final val isRefineable = true
    final val isMutable = false
    final val isConditionallyImmutable = false

    def join(other: TypeImmutability): TypeImmutability = {
        other match {
            case MutableType | UnknownTypeImmutability | ConditionallyImmutableType ⇒ other
            case _ ⇒ this
        }
    }

    def compare(that: TypeImmutability) = {
        if (this == that) 0 else if (that == UnknownTypeImmutability) 1 else -1
    }

}

case object MutableType extends TypeImmutability {
    final val isRefineable = false
    final val isMutable = true
    final val isConditionallyImmutable = false

    def join(other: TypeImmutability): this.type = this

    def compare(that: TypeImmutability) = {
        if (this == that) 0 else throw new IllegalArgumentException
    }

}
