/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package seq

/**
 * Encapsulates different tasks.
 *
 * @author Michael Eichberg
 */
private[seq] sealed abstract class QualifiedTask extends (() ⇒ Unit) {

    /**
     * Returns `true` if and only if this task was definitively triggered 
     * by a final property.
     */
    def istriggeredByFinalProperty : Boolean

}

private[seq] final case class PropertyComputationTask[E <: Entity](
        ps: PropertyStore,
        e:  E,
        pc: PropertyComputation[E]
) extends QualifiedTask {

    override def apply(): Unit = ps.handleResult(pc(e))

    override def istriggeredByFinalProperty : Boolean = false
}

private[seq] final case class OnFinalUpdateComputationTask[E <: Entity, P <: Property](
        ps: PropertyStore,
        r:  FinalEP[E, P],
        c:  OnUpdateContinuation
) extends QualifiedTask {

    override def apply(): Unit = ps.handleResult(c(r))

    override def istriggeredByFinalProperty : Boolean = true
}

private[seq] final case class OnUpdateComputationTask[E <: Entity, P <: Property](
        ps:  PropertyStore,
        epk: EPK[E, P],
        c:   OnUpdateContinuation
) extends QualifiedTask {

    override def apply(): Unit = {
        // Get the most current property when the depender is eventually evaluated.
        ps.handleResult(c(ps(epk).asEPS))
    }

    override def istriggeredByFinalProperty : Boolean = false

}

private[seq] final case class HandleResultTask[E <: Entity, P <: Property](
        ps: PropertyStore,
        r:  PropertyComputationResult
) extends QualifiedTask {

    override def apply(): Unit = {
        // Get the most current property when the depender is eventually evaluated.
        ps.handleResult(r)
    }

    override def triggeredByFinalProperty : Boolean = {
        r match  {
            case NoResult | _: FinalPropertyComputationResult => true
            case _ => false
        }
    }
}
