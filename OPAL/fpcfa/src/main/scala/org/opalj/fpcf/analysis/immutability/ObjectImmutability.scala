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

sealed trait ObjectImmutabilityPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = ObjectImmutability

}

/**
 * Specifies the mutability of instances of a specific class.
 * The highest rating is "Immutable", then "Conditionally Immutable", then "Mutable".
 *
 * An instance of a class is rated as immutable if the state of does not change after
 * initialization in a client visible manner! This includes all classes referenced by the instances
 * (transitive hull). However, fields that are lazily initialized, but don't change after that
 * do not impede immutability.
 * Conditionally immutable means that the state of the instance of the respective class
 * cannot be mutated, but objects referenced by it can be mutated (so called
 * immutable collections are typically rated as "conditionally immutable").
 * Mutable means that a client can mutate (directly or indirectly)
 * the state of respective objects. In general the state of a class is determined w.r.t.
 * the declared fields. I.e., a method that has, e.g., a call time dependent behavior,
 * but which does not mutate the state of the class does not affect the mutability rating.
 *
 * The mutability assessment is by default done on a per class basis and only includes
 * the super classes of a class. A rating that is based on all usages is only meaningful
 * if we analyze an application.
 *
 * ==Thread-safe Lazily Initialized Fields==
 * A field that is initialized lazily in a thread-safe manner; i.e.,
 * which is set at most once after construction and which is always set to the
 * same value independent of the time of (lazy) initialization, may not affect the
 * mutability rating. However, an analysis may rate such a class as mutable. An
 * example of such a field is the field that stores the lazily calculated hashCode of
 * a `String` object.
 *
 * ==Inheritance==
 *  - Instances of `java.lang.Object` are immutable. However, if a class defines a
 * constructor which has a parameter of type object and which assigns the respective
 * parameter value to a field will at-most be conditionally immutable (instances of the
 * class object are immutable, but instances of the type (which includes all subtypes) are
 * not immutable; in general
 * we must assume that the referenced object may be (at runtime) some mutable object.
 *  - In general, only classes that inherit from (conditionally) immutable class can be
 * (conditionally) immutable; if a class is mutable, all subclasses are also
 * considered to be mutable. I.e., a subclass can never have a higher mutability rating
 * than a superclass.
 *  - All classes for which the superclasstype information is not complete are rated
 * as unknown. (Interfaces are generally ignored as they are always immutable.)
 *
 * ==Native Methods==
 * Unknown native methods are considered as mutating the state unless all state is
 * explicitly final; however, this is already handled by the
 * [[org.opalj.fpcf.analysis.fields.FieldMutabilityAnalysis]].
 *
 * ==Identifying Immutable Objects in Practice==
 * Identifying real world immutable classes as such by means of an analysis is in general a
 * challenging task. For example, to
 * identify the well known immutable class "java.lang.String" as such requires:
 *  - Identifying that the field hash is effectively immutable though the field is only lazily
 *    initialized (in a thread-safe manner).
 *  - Determing that all calls to the package-private constructor java.lang.String(byte[] buf,
 *    Boolean shared) are actually passed an array that is not shared afterwards. I.e., the
 *    ownership is in all cases effectively transfered to the class java.lang.String.
 *
 * ==Interfaces==
 * Are not considered during the analysis as they are always immutable. (All fields are (implicitly)
 * `static` and `final`.)
 *
 * @author Andre Pacak
 * @author Michael Eichberg
 */
sealed trait ObjectImmutability
        extends OrderedProperty
        with ObjectImmutabilityPropertyMetaInformation {

    /**
     * Returns the key used by all `ObjectImmutability` properties.
     */
    final def key = ObjectImmutability.key

    def correspondingTypeImmutability: TypeImmutability

    def isMutable: Answer
}
/**
 * Common constants use by all [[ObjectImmutability]] properties associated with methods.
 */
object ObjectImmutability extends ObjectImmutabilityPropertyMetaInformation {

    /**
     * The key associated with every [[ObjectImmutability]] property.
     */
    final val key: PropertyKey[ObjectImmutability] = PropertyKey.create(
        "ObjectImmutability",
        // The default property that will be used if no analysis is able
        // to (directly) compute the respective property.
        MutableObjectDueToUnresolvableDependency,
        // When we have a cycle all properties are necessarily at least conditionally
        // immutable (type and object wise) hence, we can leverage the "immutability"
        ImmutableObject
    )
}

case object UnknownObjectImmutability extends ObjectImmutability {
    final val isRefineable = true
    final val correspondingTypeImmutability = UnknownTypeImmutability

    def isValidSuccessorOf(other: OrderedProperty): Option[String] = {
        if (other == UnknownObjectImmutability)
            None
        else
            Some(s"impossible refinement of $other to $this")
    }

    def isMutable: Answer = Unknown
}

/**
 * An instance of the respective class is effectively immutable
 * and also all reference objects. I.e., after creation it is not
 * possible for a client to set a field or to call a method that updates the internal state
 * of the instance or an object
 * referred to by the instance in such a way that the client can observe the state change.
 *
 */
case object ImmutableObject extends ObjectImmutability {
    final val isRefineable = false
    final val correspondingTypeImmutability = ImmutableType

    def isValidSuccessorOf(other: OrderedProperty): Option[String] = {
        if (other.isRefineable)
            None
        else
            Some(s"impossible refinement of $other to $this")
    }

    final def isMutable: Answer = No
}

/**
 * An instance of the respective class is effectively immutable. I.e., after creation it is not
 * possible for a client to set a field or to call a method that updates the internal state
 *
 */
case object ConditionallyImmutableObject extends ObjectImmutability {
    final val isRefineable = false
    final val correspondingTypeImmutability = ConditionallyImmutableType

    def isValidSuccessorOf(other: OrderedProperty): Option[String] = {
        if (other.isRefineable)
            None
        else
            Some(s"impossible refinement of $other to $this")
    }

    final def isMutable: Answer = No
}

case object AtLeastConditionallyImmutableObject extends ObjectImmutability {
    final val isRefineable = true
    final val correspondingTypeImmutability = AtLeastConditionallyImmutableType

    def isValidSuccessorOf(other: OrderedProperty): Option[String] = {
        if (other.isRefineable)
            None
        else
            Some(s"impossible refinement of $other to $this")
    }

    final def isMutable: Answer = No
}

sealed trait MutableObject extends ObjectImmutability {
    final val isRefineable = false
    val reason: String
    final val correspondingTypeImmutability = MutableType

    def isValidSuccessorOf(other: OrderedProperty): Option[String] = {
        if (other.isRefineable)
            None
        else
            Some(s"impossible refinement of $other to $this")
    }

    final def isMutable: Answer = Yes
}

case object MutableObjectByAnalysis extends MutableObject {
    final val reason = "determined by analysis"
}

case object MutableObjectDueToUnknownSupertypes extends MutableObject {
    final val reason = "the type hierarchy is upwards incomplete"
}

case object MutableObjectDueToUnresolvableDependency extends MutableObject {
    final val reason = "a dependency cannot be resolved"
}
