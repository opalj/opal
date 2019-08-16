/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package fixtures

/**
 * @note Only intended to be used as a test fixture.
 */
object ReachableEntities {

    /**
     * A collection of entities.
     */
    final val ReachedEntitiesKey = {
        PropertyKey.create[String, ReachedEntities]("ReachedEntities")
    }

    case class ReachedEntities(entities: Set[AnyRef]) extends Property {
        type Self = ReachedEntities
        def key: PropertyKey[ReachedEntities] = ReachedEntitiesKey
    }
}
