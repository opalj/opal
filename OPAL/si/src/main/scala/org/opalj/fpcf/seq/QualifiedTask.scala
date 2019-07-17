/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package seq

/**
 * Encapsulates different tasks.
 *
 * @author Michael Eichberg
 */
sealed abstract class QualifiedTask extends (() ⇒ Unit) {

    /**
     * Returns `true` if and only if this task was definitively triggered
     * by a final property.
     */
    def isTriggeredByFinalProperty: Boolean

    def isEntityBasedTask: Boolean = false
    def asEntityBasedTask: EntityBasedQualifiedTask = throw new ClassCastException
}

final case class HandleResultTask[E <: Entity, P <: Property](
        ps: PropertyStore,
        r:  PropertyComputationResult
) extends QualifiedTask {

    override def apply(): Unit = {
        // Get the most current property when the depender is eventually evaluated.
        ps.handleResult(r)
    }

    override def isTriggeredByFinalProperty: Boolean = {
        r match {
            case NoResult | _: FinalPropertyComputationResult ⇒ true
            case _                                            ⇒ false
        }
    }
}

sealed abstract class EntityBasedQualifiedTask extends QualifiedTask {

    /** The entity which will be analyzed. */
    def e: Entity
    final override def isEntityBasedTask: Boolean = true
    final override def asEntityBasedTask: EntityBasedQualifiedTask = this
}

final case class PropertyComputationTask[E <: Entity](
        ps: PropertyStore,
        e:  E,
        pc: PropertyComputation[E]
) extends EntityBasedQualifiedTask {

    override def apply(): Unit = ps.handleResult(pc(e))

    override def isTriggeredByFinalProperty: Boolean = false
}

final case class OnFinalUpdateComputationTask[E <: Entity, P <: Property](
        ps: PropertyStore,
        r:  FinalEP[E, P],
        c:  OnUpdateContinuation
) extends EntityBasedQualifiedTask {

    override def e: Entity = r.e

    override def apply(): Unit = {
        ps.handleResult(c(r))
    }

    override def isTriggeredByFinalProperty: Boolean = true
}

final case class OnUpdateComputationTask[E <: Entity, P <: Property](
        ps:  PropertyStore,
        epk: EPK[E, P],
        c:   OnUpdateContinuation
) extends EntityBasedQualifiedTask {

    override def e: Entity = epk.e

    override def apply(): Unit = {
        // Get the most current property when the depender is eventually evaluated.
        ps.handleResult(c(ps(epk).asEPS))
    }

    override def isTriggeredByFinalProperty: Boolean = false

}
