/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package seq

/**
 * Encapsulates different tasks.
 *
 * @author Michael Eichberg
 */
private[seq] sealed abstract class QualifiedTask extends (() ⇒ Unit)

private[seq] final case class PropertyComputationTask[E <: Entity](
        ps: PropertyStore,
        e:  E,
        pc: PropertyComputation[E]
) extends QualifiedTask {

    override def apply(): Unit = ps.handleResult(pc(e), true)
}

private[seq] final case class OnFinalUpdateComputationTask[E <: Entity, P <: Property](
        ps: PropertyStore,
        r:  FinalEP[E, P],
        c:  OnUpdateContinuation
) extends QualifiedTask {

    override def apply(): Unit = ps.handleResult(c(r), true)
}

private[seq] final case class OnUpdateComputationTask[E <: Entity, P <: Property](
        ps:  PropertyStore,
        epk: EPK[E, P],
        c:   OnUpdateContinuation
) extends QualifiedTask {

    override def apply(): Unit = {
        // get the most current pValue when the depender
        // is eventually evaluated; the effectiveness
        // of this check depends on the scheduling strategy(!)
        val pValue = ps(epk)
        val eps = EPS(epk.e, pValue.lb, pValue.ub)
        ps.handleResult(c(eps), true)
    }
}

