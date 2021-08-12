/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * Specifies the type of an update. The type hierarchies of [[Update]] and [[UpdateType]]
 * are aligned and it is possible to conveniently switch between them. Contrary to
 * an `Update` object an `UpdateType` object never has any payload, it just characterizes
 * an update. However, by passing a value to an `UpdateType` the `UpdateType`
 * is turned into a corresponding [[org.opalj.ai.Update]] object.
 *
 * ==Example==
 * {{{
 * val updateType : UpdateType = ...
 * val update : Update = updateType(<someValue>)
 * }}}
 *
 * @author Michael Eichberg
 */
sealed abstract class UpdateType {
    /**
     * Lift this update type to an [[Update]] of the corresponding type which contains
     * the given value.
     */
    def apply[V](value: => V): Update[V]

    /**
     * Returns `true` if `this` `UpdateType` represents the `NoUpdateType`.
     */
    def noUpdate: Boolean

    /**
     * Returns `true` if `this` `UpdateType` is a [[MetaInformationUpdateType]].
     */
    def isMetaInformationUpdate: Boolean

    /**
     * Merges this `UpdateType` with the given one. That is, it is determined which
     * type is the more qualified one (`NoUpdateType` < `MetaInformationUpdateType` <
     * `StructuralUpdateType`) and that one is returned.
     */
    def &:(updateType: UpdateType): UpdateType

    /**
     * Merges this `UpdateType` with the given `Update` object and returns an `UpdateType`
     * object that characterizes the update.
     */
    def &:(update: Update[_]): UpdateType

}

case object NoUpdateType extends UpdateType {

    override def apply[V](value: => V): Update[V] = NoUpdate

    override def noUpdate: Boolean = true

    override def isMetaInformationUpdate: Boolean = false

    override def &:(updateType: UpdateType): UpdateType = updateType

    override def &:(update: Update[_]): UpdateType = update.updateType

}

// TODO Rename to HeapShapeUpdate
sealed trait MetaInformationUpdateType extends UpdateType {

    override def apply[V](value: => V): Update[V] = MetaInformationUpdate(value)

    override def noUpdate: Boolean = false

    override def isMetaInformationUpdate: Boolean = true

    override def &:(updateType: UpdateType): UpdateType = {
        if (updateType == StructuralUpdateType)
            StructuralUpdateType
        else
            this
    }

    override def &:(update: Update[_]): UpdateType = update.updateType &: this
}
case object MetaInformationUpdateType extends MetaInformationUpdateType

case object StructuralUpdateType extends UpdateType {

    override def apply[V](value: => V): Update[V] = StructuralUpdate(value)

    override def noUpdate: Boolean = false

    override def isMetaInformationUpdate: Boolean = false

    override def &:(updateType: UpdateType): UpdateType = StructuralUpdateType

    override def &:(update: Update[_]): UpdateType = StructuralUpdateType
}
