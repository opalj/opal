/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package fixtures

/**
 * Models a basic property to "mark" something.
 *
 * @note Only intended to be used as a test fixture.
 */
object Marker {

    final val Key = {
        PropertyKey.create[Entity, MarkerProperty](
            "Marker",
            (ps: PropertyStore, reason: FallbackReason, e: Entity) => NotMarked
        )
    }

    sealed trait MarkerProperty extends Property {
        type Self = MarkerProperty
        def key: PropertyKey[MarkerProperty] = Marker.Key
    }

    case object IsMarked extends MarkerProperty
    case object NotMarked extends MarkerProperty
}
