/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.Entity
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait ClassImmutabilityPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = ClassImmutability

}

/**
 * Specifies the (im)mutability of instances of a specific class.
 * The highest rating is "Immutable", then "Conditionally Immutable", then "Mutable".
 *
 * An instance of a class is rated as immutable if the state of the object does not change after
 * initialization in a client visible manner! This includes all objects referenced by the instances
 * (transitive hull). However, fields that are lazily initialized (in a thread-safe manner) and
 * which don't change after that do not impede immutability.
 * Conditionally immutable means that the state of the instance of the respective class
 * cannot be mutated, but objects referenced by it can be mutated (so called
 * immutable collections are typically rated as "conditionally immutable").
 * Mutable means that a client can mutate (directly or indirectly)
 * the state of respective objects. In general the state of a class is determined w.r.t.
 * the declared fields. I.e., an impure method which has, e.g., a call time dependent behavior
 * because it uses the current time, but which does not mutate the state of the class does not affect
 * the mutability rating. The same is true for methods with side-effects related to the state of
 * other types of object.
 *
 * The mutability assessment is by default done on a per class basis and only directly depends on the
 * super class of the analyzed class. A rating that is based on all actual usages is only meaningful
 * if we analyze an application. E.g., imagine a simple mutable data container class where no field
 * – in the concrete context of a specific application – is ever updated.
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
 * [[org.opalj.br.fpcf.analyses.L0FieldMutabilityAnalysis]].
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
sealed trait ClassImmutability
    extends OrderedProperty
    with ClassImmutabilityPropertyMetaInformation {

    final def key: PropertyKey[ClassImmutability] = ClassImmutability.key

    def correspondingTypeImmutability: TypeImmutability

    /** `true` if instances of the class are mutable. */
    def isMutable: Boolean
}
/**
 * Common constants use by all [[ClassImmutability]] properties associated with methods.
 */
object ClassImmutability extends ClassImmutabilityPropertyMetaInformation {

    /**
     * The key associated with every [[ClassImmutability]] property.
     */
    final val key: PropertyKey[ClassImmutability] = PropertyKey.create(
        "opalj.ClassImmutability",
        MutableObjectDueToUnresolvableDependency
    )
}

/**
 * An instance of the respective class is effectively immutable
 * and also all (transitively) referenced objects. I.e., after creation it is not
 * possible for a client to set a field or to call a method that updates the internal state
 * of the instance or an object referred to by the instance in such a way that the client
 * can observe the state change.
 *
 */
case object ImmutableObject extends ClassImmutability {

    final val correspondingTypeImmutability = ImmutableType

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {}

    final def isMutable: Boolean = false
}

/**
 * An instance of the respective class is (at least) effectively immutable. I.e., after creation
 * it is not possible for a client to set a field or to call a method that updates the direct
 * internal state; changing the transitive state may be possible.
 */
case object ImmutableContainer extends ClassImmutability {

    final val correspondingTypeImmutability = ImmutableContainerType

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other == ImmutableObject) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this");
        }
    }

    final def isMutable: Boolean = false
}

sealed trait MutableObject extends ClassImmutability {

    def reason: String
    final val correspondingTypeImmutability = MutableType

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other == ImmutableObject || other == ImmutableContainer) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }

    final def isMutable: Boolean = true

    final override def toString: String = s"MutableObject(reason=$reason)"
}

case object MutableObjectDueToIncompleteAnalysis extends MutableObject {
    final def reason = "analysis has not yet completed"
}

case object MutableObjectByAnalysis extends MutableObject {
    final def reason = "determined by analysis"
}

case object MutableObjectDueToUnknownSupertypes extends MutableObject {
    final def reason = "the type hierarchy is upwards incomplete"
}

case object MutableObjectDueToUnresolvableDependency extends MutableObject {
    final def reason = "a dependency cannot be resolved"
}
