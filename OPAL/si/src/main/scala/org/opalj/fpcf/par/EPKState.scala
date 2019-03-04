/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

import java.util.concurrent.atomic.AtomicReference

/**
 * Encapsulates the state of a single entity and its property of a specific kind.
 *
 * @note All operations are effectively atomic operations.
 */
sealed trait EPKState {

    /** Returns the current property extension. */
    def eOptionP: SomeEOptionP

    /** Returns `true` if no property has been computed yet; `false` otherwise. */
    final def isEPK: Boolean = eOptionP.isEPK

    /** Returns `true` if this entity/property pair is not yet final. */
    def isRefinable: Boolean

    /** Returns the underlying entity. */
    final def e: Entity = eOptionP.e

    /**
     * Updates the underlying `EOptionP` value.
     *
     * @note This function is only defined if the current `EOptionP` value is not already a
     *       final value. Hence, the client is required to handle (potentially) idempotent updates
     *       and to take care of appropriate synchronization.
     */
    def updateEOptionP(newEOptionP: SomeInterimEP, debug: Boolean): SomeEOptionP

    /**
     * Adds the given E/PK as a depender on this E/PK instance.
     *
     * @note  This operation is idempotent; that is, adding the same EPK multiple times has no
     *        special effect.
     * @note  This method must not throw an exception, because it may happen
     *        that a client reads an intermediate property and based on that it decided
     *        to add a dependency, but this EPKState was updated in the meantime and may
     *        encapsulte a final value for which it does not make sense to store dependers.
     *        This will – however – not lead to a lost update since a client is required
     *        to check – after registering itself as a depender – that the value is still as
     *        expected!
     */
    def addDepender(someEPK: SomeEPK): Unit

    /**
     * Removes the given E/PK from the list of dependers of this EPKState.
     *
     * @note This method is always defined and never throws an exception.
     */
    def removeDepender(someEPK: SomeEPK): Unit

    def getAndClearDependers(): Set[SomeEPK]

    /**
     * Returns the current `OnUpdateComputation` or `null`, if the `OnUpdateComputation` was
     * already triggered. This is an atomic operation.
     *
     * @note This method is always defined and never throws an exception.
     */
    def getAndClearOnUpdateComputation(): OnUpdateContinuation

    /**
     * Sets the `OnUpdateComputation` to the given one.
     *
     * @note This method is only defined if this `EPKState` is refinable.
     */
    def setOnUpdateComputation(c: OnUpdateContinuation): Unit

    /**
     * Returns `true` if the current `EPKState` has an `OnUpdateComputation` that was not yet
     * triggered.
     *
     * @note The returned value may have changed in the meantime; hence, this method
     *       can/should only be used as a hint.
     */
    def hasPendingOnUpdateComputation: Boolean

    /**
     * Returns `true` if and only if this EPKState has dependees.
     *
     * @note The set of dependees is only update when a property computation result is processed
     *       and there exists, w.r.t. an Entity/Property Kind pair, always at most one
     *       `PropertyComputationResult`.
     */
    def hasDependees: Boolean

    /**
     * Returns the current set of depeendes. Defined if and only if this `EPKState` is refinable.
     *
     * @note The set of dependees is only update when a property computation result is processed
     *       and there exists, w.r.t. an Entity/Property Kind pair, always at most one
     *       `PropertyComputationResult`.
     */
    def dependees: Traversable[SomeEOptionP]

    def setDependees(dependees: Traversable[SomeEOptionP]): Unit
}

/**
 *
 * @param eOptionPAR An atomic reference holding the current property extension; we need to
 *         use an atomic reference to enable concurrent update operations as required
 *         by properties computed using partial results.
 *         The referenced `EOptionP` is never null.
 * @param cAR The on update continuation function; null if triggered.
 * @param dependees The dependees; never updated concurrently.
 */
final class InterimEPKState(
        val eOptionPAR:          AtomicReference[SomeEOptionP],
        val cAR:                 AtomicReference[OnUpdateContinuation],
        @volatile var dependees: Traversable[SomeEOptionP],
        val dependersAR:         AtomicReference[Set[SomeEPK]]
) extends EPKState {

    assert(eOptionPAR.get.isRefinable)

    override def eOptionP: SomeEOptionP = eOptionPAR.get()

    override def isRefinable: Boolean = true

    override def updateEOptionP(newEOptionP: SomeInterimEP, debug: Boolean): SomeEOptionP = {
        val oldEOptionP = eOptionPAR.getAndSet(newEOptionP)
        if (debug) {
            oldEOptionP.checkIsValidPropertiesUpdate(newEOptionP, dependees)
        }
        oldEOptionP
    }

    override def addDepender(someEPK: SomeEPK): Unit = {
        dependersAR.accumulateAndGet(
            Set(someEPK),
            (currentDependers, newDepender) ⇒ currentDependers + newDepender.head
        )
    }

    override def removeDepender(someEPK: SomeEPK): Unit = {
        dependersAR.accumulateAndGet(
            Set(someEPK),
            (currentDependers, newDepender) ⇒ currentDependers - newDepender.head
        )
    }

    override def getAndClearDependers(): Set[SomeEPK] = {
        val dependers = dependersAR.getAndSet(Set.empty)
        if (dependers == null)
            Set.empty
        else
            dependers
    }

    override def getAndClearOnUpdateComputation(): OnUpdateContinuation = cAR.getAndSet(null)

    override def setOnUpdateComputation(c: OnUpdateContinuation): Unit = {
        val oldOnUpdateContinuation = cAR.getAndSet(c)
        assert(oldOnUpdateContinuation == null)
    }

    override def hasPendingOnUpdateComputation: Boolean = cAR.get() != null

    override def setDependees(dependees: Traversable[SomeEOptionP]): Unit = {
        this.dependees = dependees
    }

    override def hasDependees: Boolean = dependees.nonEmpty

    override def toString: String = {
        "InterimEPKState("+
            s"eOptionP=${eOptionPAR.get},"+
            s"<hasOnUpdateComputation=${cAR.get() != null}>,"+
            s"dependees=$dependees,"+
            s"dependers=${dependersAR.get()})"
    }
}

final class FinalEPKState(override val eOptionP: SomeEOptionP) extends EPKState {

    override def isRefinable: Boolean = false

    override def updateEOptionP(newEOptionP: SomeInterimEP, debug: Boolean): SomeEOptionP = {
        throw new UnknownError(s"the final property $eOptionP can't be updated to $newEOptionP")
    }

    override def getAndClearDependers(): Set[SomeEPK] = Set.empty

    override def addDepender(epk: SomeEPK): Unit = { /* There is nothing to do! */ }

    override def removeDepender(someEPK: SomeEPK): Unit = { /* There is nothing to do! */ }

    override def getAndClearOnUpdateComputation(): OnUpdateContinuation = null

    override def dependees: Traversable[SomeEOptionP] = {
        throw new UnknownError("final properties don't have dependees")
    }

    override def setDependees(dependees: Traversable[SomeEOptionP]): Unit = {
        throw new UnknownError("final properties can't have dependees")
    }

    override def hasDependees: Boolean = false

    override def setOnUpdateComputation(c: OnUpdateContinuation): Unit = {
        throw new UnknownError("final properties can't have \"OnUpdateContinuations\"")
    }

    override def hasPendingOnUpdateComputation: Boolean = false

    override def toString: String = s"FinalEPKState(finalEP=$eOptionP)"
}

object EPKState {

    def apply(finalEP: SomeFinalEP): EPKState = new FinalEPKState(finalEP)

    def apply(eOptionP: SomeEOptionP): EPKState = {
        new InterimEPKState(
            new AtomicReference[SomeEOptionP](eOptionP),
            new AtomicReference[OnUpdateContinuation]( /*null*/ ),
            Nil,
            new AtomicReference[Set[SomeEPK]](Set.empty)
        )
    }

    def apply(
        eOptionP:  SomeEOptionP,
        c:         OnUpdateContinuation,
        dependees: Traversable[SomeEOptionP]
    ): EPKState = {
        new InterimEPKState(
            new AtomicReference[SomeEOptionP](eOptionP),
            new AtomicReference[OnUpdateContinuation](c),
            dependees,
            new AtomicReference[Set[SomeEPK]](Set.empty)
        )
    }

    def unapply(epkState: EPKState): Some[SomeEOptionP] = Some(epkState.eOptionP)

}
