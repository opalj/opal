/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

import org.opalj.fpcf.PropertyStore.{Debug ⇒ debug}

/**
 * We generally distinguish between tasks that compute properties which are explicitly required
 * and those tasks which are not yet/no longer required, because no strictly depending analyses
 * requires them (anymore.)
 *
 * @author Michael Eichberg
 */
private[par] sealed trait QualifiedTask[E <: Entity] extends (() ⇒ Unit) {

    def isInitialTask: Boolean
    def asInitialTask: InitialPropertyComputationTask[E] = throw new ClassCastException()
}

private[par] sealed trait FirstPropertyComputationTask[E <: Entity] extends QualifiedTask[E] {
    def e: Entity
}

private[par] final case class InitialPropertyComputationTask[E <: Entity](
        ps:              PKEParallelTasksPropertyStore,
        e:               E,
        pc:              PropertyComputation[E],
        forceEvaluation: Boolean
) extends FirstPropertyComputationTask[E] {

    override def apply(): Unit = {
        val r = pc(e)
        ps.handleResult(r, forceEvaluation)
    }

    override def isInitialTask: Boolean = true
    override def asInitialTask: InitialPropertyComputationTask[E] = this
}

private[par] sealed abstract class TriggeredTask[E <: Entity] extends QualifiedTask[E] {
    final override def isInitialTask: Boolean = false
}

private[par] final case class PropertyComputationTask[E <: Entity](
        ps:   PKEParallelTasksPropertyStore,
        e:    E,
        pkId: Int,
        pc:   PropertyComputation[E]
) extends TriggeredTask[E] with FirstPropertyComputationTask[E] {

    override def apply(): Unit = ps.handleResult(pc(e), forceEvaluation = false)
}

private[par] sealed abstract class ContinuationTask[E <: Entity] extends TriggeredTask[E] {
    def dependeeE: Entity
    def dependeePKId: Int
}

private[par] final case class OnFinalUpdateComputationTask[E <: Entity, P <: Property](
        ps:              PKEParallelTasksPropertyStore,
        dependeeFinalEP: FinalEP[E, P],
        c:               OnUpdateContinuation
) extends ContinuationTask[E] {

    override def dependeeE: E = dependeeFinalEP.e

    override def dependeePKId: Int = dependeeFinalEP.p.id

    override def apply(): Unit = ps.handleResult(c(dependeeFinalEP), forceEvaluation = false)
}

private[par] final case class OnUpdateComputationTask[E <: Entity, P <: Property](
        ps:          PKEParallelTasksPropertyStore,
        dependeeEPK: EPK[E, P],
        c:           OnUpdateContinuation
) extends ContinuationTask[E] {

    override def dependeeE: E = dependeeEPK.e

    override def dependeePKId: Int = dependeeEPK.pk.id

    override def apply(): Unit = {
        // get the most current property when the depender is eventually evaluated;
        // the effectiveness of this check depends on the scheduling strategy(!)
        val eOptionP = ps(dependeeEPK)
        val eps = EPS(dependeeEPK.e, eOptionP.lb, eOptionP.ub)
        ps.handleResult(c(eps), forceEvaluation = false)
    }
}

private[par] final case class ImmediateOnUpdateComputationTask[E <: Entity, P <: Property](
        ps:                          PKEParallelTasksPropertyStore,
        dependeeEPK:                 EPK[E, P],
        previousResult:              PropertyComputationResult,
        forceDependersNotifications: Set[SomeEPK],
        c:                           OnUpdateContinuation
) extends ContinuationTask[E] {

    override def dependeeE: E = dependeeEPK.e

    override def dependeePKId: Int = dependeeEPK.pk.id

    override def apply(): Unit = {
        // Get the most current property when the depender is eventually evaluated;
        // the effectiveness of this check depends on the scheduling strategy(!).
        val newResult = c(ps(dependeeEPK).asEPS)
        if (debug && newResult == previousResult) {
            throw new IllegalStateException(
                s"an on-update continuation resulted in the same result as before: $newResult"
            )
        }
        ps.handleResult(newResult, forceEvaluation = false, forceDependersNotifications)
    }
}

private[par] final case class ImmediateOnFinalUpdateComputationTask[E <: Entity, P <: Property](
        ps:                          PKEParallelTasksPropertyStore,
        dependeeFinalEP:             FinalEP[E, P],
        previousResult:              PropertyComputationResult,
        forceDependersNotifications: Set[SomeEPK],
        c:                           OnUpdateContinuation
) extends ContinuationTask[E] {

    override def dependeeE: E = dependeeFinalEP.e

    override def dependeePKId: Int = dependeeFinalEP.pk.id

    override def apply(): Unit = {
        // get the most current property when the depender is eventually evaluated;
        // the effectiveness of this check depends on the scheduling strategy(!)
        val newResult = c(dependeeFinalEP)
        if (debug && newResult == previousResult) {
            throw new IllegalStateException(
                s"an on-update continuation resulted in the same result as before: $newResult"
            )
        }
        ps.handleResult(newResult, forceEvaluation = false, forceDependersNotifications)
    }
}

