/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package fixtures

/**
 * Models a basic purity property.
 *
 * @note Only intended to be used as a test fixture.
 */
sealed trait Purity extends OrderedProperty {
    final type Self = Purity
    final def key: PropertyKey[Purity] = Purity.Key
}

object Purity {
    final val Key = PropertyKey.create[Entity, Purity]("Purity", Impure)
}

case object Pure extends Purity {
    def checkIsEqualOrBetterThan(e: Entity, other: Purity): Unit = { /* always succeeds */ }
}

case object Impure extends Purity {
    def checkIsEqualOrBetterThan(e: Entity, other: Purity): Unit = {
        if (other != Impure) {
            throw new IllegalArgumentException(s"$e: $this is not equal or better than $other")
        }
    }
}
