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

import org.opalj.br.ObjectType

sealed trait TypeImmutabilityPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = TypeImmutability
}

/**
 * Specifies if all instances of a respective type (this includes the instances of the
 * type's subtypes) are (conditionally) immutable. Conditionally immutable means that only the
 * instance of the type itself is guaranteed to be immutable, but not all reachable objects.
 * In general, all -- so called -- immutable collections are only conditionally immutable. I.e.,
 * the collection as a whole is only immutable if only immutable objects are stored in the
 * collection. If this is not the case, the collection is only conditionally immutable.
 *
 * This property is of particular interest if the precise type cannot be computed statically. This
 * property basically depends on the [[org.opalj.br.analyses.cg.TypeExtensibilityKey]] and
 * [[ClassImmutability]].
 *
 * @author Michael Eichberg
 */
sealed trait TypeImmutability extends OrderedProperty with TypeImmutabilityPropertyMetaInformation {

    /**
     * Returns the key used by all `TypeImmutability` properties.
     */
    final def key = TypeImmutability.key

    def isImmutable: Boolean
    def isImmutableContainer: Boolean
    /** `true` if the mutability is unknown or if the type is mutable.*/
    def isMutable: Boolean

    def meet(other: TypeImmutability): TypeImmutability
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
        // The  property that will be used if no analysis is able
        // to (directly) compute the respective property.
        MutableType,
        // When we have a cycle all properties are necessarily at least conditionally
        // immutable hence, we can leverage the "immutability" of one of the members of
        // the cycle and wait for the automatic propagation...
        (_: PropertyStore, eps: EPS[ObjectType, TypeImmutability]) ⇒ eps.toUBEP
    )
}

/**
 * An instance of the respective class is effectively immutable. I.e., after creation it is not
 * possible for a client to set a field or to call a method that updates the internal state
 */
case object ImmutableType extends TypeImmutability {

    override def isImmutable: Boolean = true
    override def isImmutableContainer: Boolean = false
    override def isMutable: Boolean = false

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {}

    def meet(that: TypeImmutability): TypeImmutability = if (this == that) this else that

}

case object ImmutableContainerType extends TypeImmutability {

    override def isImmutable: Boolean = false
    override def isImmutableContainer: Boolean = true
    override def isMutable: Boolean = false

    def meet(that: TypeImmutability): TypeImmutability = if (that == MutableType) that else this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other == ImmutableType) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this");
        }
    }
}

case object MutableType extends TypeImmutability {

    override def isImmutable: Boolean = false
    override def isImmutableContainer: Boolean = false
    override def isMutable: Boolean = true

    def meet(other: TypeImmutability): this.type = this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other != MutableType) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this");
        }
    }
}

