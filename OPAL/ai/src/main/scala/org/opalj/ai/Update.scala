/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * Encapsulates an updated value and qualifies the type of the update.
 *
 * In general OPAL distinguishes between updates to a value that are relevant w.r.t.
 * the abstract interpretation and those updates that just update some meta-information
 * and which do not affect the abstract interpretation and – in particular – do not
 * force the framework to continue the abstract interpretation.
 *
 * @author Michael Eichberg
 */
sealed abstract class Update[+V] {

    /**
     * Merges a given `updateType` value with the type of this update and returns a
     * new `UpdateType` value.
     *
     * @see [[org.opalj.ai.UpdateType]] for further details.
     */
    def &:(updateType: UpdateType): UpdateType

    def isStructuralUpdate: Boolean = false

    def isMetaInformationUpdate: Boolean = false

    def isSomeUpdate: Boolean

    def isNoUpdate: Boolean

    /**
     * The ''type'' of this update.
     */
    def updateType: UpdateType

    /**
     * The updated value; if available.
     */
    def value: V

    /**
     * Creates a new `Update` object of the same type with the given value.
     */
    def updateValue[NewV](newValue: NewV): Update[NewV]
}

/**
 * Identifies updates where something was updated without further qualifying the update.
 *
 * ==Usage==
 * This class (and its companion object) are primarily used for pattern matching purposes.
 */
sealed abstract class SomeUpdate[V] extends Update[V] {

    override def isNoUpdate: Boolean = false

    override def isSomeUpdate: Boolean = true

}

/**
 * Facilitates matching against updates that actually encapsulate an updated value.
 */
object SomeUpdate {

    def unapply[V](update: SomeUpdate[V]): Some[V] = Some(update.value)

}

// TODO Replace Structural-  and MetaInformation Update by qualified update - where the qualification is specified using an INT value.
// Predefined int masks:
//  0               => no update         <=> nothing has changed...
//
//  Int.MinValue    => illegal update    <=> the update resulted in an illegal value.
//  (100000....000)
//
//
//  In general, the AI will only continue interpretation iff a positive update value is used.
//
// To make the information composable, only values 2^x have to be used respectively
// (Int.MinValue+4).toBinaryString if the update type should not (directly) force the continuation
// of the abstract interpretation.
//  1               => heap shape update <=> the structure of the heap was updated
//                     (timestamp update)
//
//  2               => type update       <=> the (upper) type bound was updated
//
//  4               => value update      <=> the represented value was updated

/**
 * Characterizes updates where the abstract state was updated such that it is required to
 * continue the abstract interpretation.
 */
final case class StructuralUpdate[V](value: V) extends SomeUpdate[V] {

    override def isStructuralUpdate: Boolean = true

    override def updateType: UpdateType = StructuralUpdateType

    override def &:(updateType: UpdateType): UpdateType = StructuralUpdateType

    override def updateValue[NewV](newValue: NewV): StructuralUpdate[NewV] = {
        StructuralUpdate(newValue)
    }
}
/**
 * Characterizes an update that did not affect the abstract state but instead just
 * updated some meta information.
 *
 * In general, the abstract interpretation framework handles `NoUpdate`s and
 * `MetaInformationUpdate`s in the same way.
 *
 * ==Example==
 * If two values are merged that are seen on two different paths, but which represent the
 * same abstract value, we may want to update the meta-information about
 * the origin of the current value, but this information may not be part of the abstract
 * state and hence, is not relevant for the abstract interpreter. In this case the
 * interpreter will not reschedule subsequent instructions. ''However, whether or not
 * the information about the origin of a value is considered to be part of the abstract
 * state is a decision of the domain.''
 */
final case class MetaInformationUpdate[V](value: V) extends SomeUpdate[V] {

    override def isMetaInformationUpdate: Boolean = true

    override def updateType: UpdateType = MetaInformationUpdateType

    override def &:(updateType: UpdateType): UpdateType = {
        if (updateType == StructuralUpdateType)
            updateType
        else
            MetaInformationUpdateType
    }

    override def updateValue[NewV](newValue: NewV): MetaInformationUpdate[NewV] = {
        MetaInformationUpdate(newValue)
    }
}

/**
 * Indicates that the (given) structure was not updated.
 *
 * @note The abstract interpretation framework itself does not distinguish between a
 *      `NoUpdate` and a `MetaInformationUpdate`; the abstract interpretation will not
 *      be continued in both cases.
 */
case object NoUpdate extends Update[Nothing] {

    type ThisType[V] = NoUpdate.type

    override def isNoUpdate: Boolean = true

    override def isSomeUpdate: Boolean = false

    override def updateType: UpdateType = NoUpdateType

    override def &:(updateType: UpdateType): UpdateType = updateType

    override def value: Nothing = throw DomainException("a NoUpdate contains no value")

    override def updateValue[NewV](newValue: NewV): Nothing = {
        throw DomainException("cannot update the value of a NoUpdate")
    }
}
