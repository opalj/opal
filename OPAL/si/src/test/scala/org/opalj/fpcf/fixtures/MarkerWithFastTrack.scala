/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package fixtures

import scala.collection.mutable.WeakHashMap

/**
 * Models a basic property to "mark" something. Counts how often the fast-track was used.
 *
 * @note Only intended to be used as a test fixture.
 */
object MarkerWithFastTrack {

    private[this] final val fastTrackEvaluationsCount = new WeakHashMap[PropertyStore, Int]

    def fastTrackEvaluationsCount(ps: PropertyStore): Int = {
        fastTrackEvaluationsCount.getOrElse(ps, 0)
    }

    final val MarkerWithFastTrackKey =
        PropertyKey.create[Entity, MarkerWithFastTrackProperty](
            "MarkerWithFastTrack",
            (_: PropertyStore, _: FallbackReason, _: Entity) ⇒ NotMarked,
            (ps: PropertyStore, _: Entity) ⇒ {
                Thread.sleep(System.nanoTime() % 50)
                fastTrackEvaluationsCount.synchronized {
                    val oldCount: Int = fastTrackEvaluationsCount.getOrElse(ps, 0)
                    fastTrackEvaluationsCount.put(ps, oldCount + 1)
                }
                Some(IsMarked)
            }
        )

    sealed trait MarkerWithFastTrackProperty extends Property {
        type Self = MarkerWithFastTrackProperty
        def key: PropertyKey[MarkerWithFastTrackProperty] = MarkerWithFastTrackKey
    }
    case object IsMarked extends MarkerWithFastTrackProperty
    case object NotMarked extends MarkerWithFastTrackProperty
}

