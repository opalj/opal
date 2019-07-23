/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

import java.util.concurrent.atomic.AtomicReference

/**
 * Encapsulates the state of a single entity and its property of a specific kind.
 *
 * @note Read the documentation of the methods to understand the behavior  
 *       in case of concurrent access.
 */
sealed abstract class EPKState {

    //
    // ------------------- PROPERTIES OF THIS EPK STATE ------------------------
    //

    /** Returns `true` if this entity/property pair is not yet final. */
    def isRefinable: Boolean

    def isFinal: Boolean

    //
    // ---------------- PROPERTIES OF THE UNDERLYING EP PAIR -------------------
    //

    /** 
     * Returns the current property extension. 
     */
    def eOptionP: SomeEOptionP

    /** 
     * Returns `true` if no property has been computed yet; `false` otherwise. 
     * 
     * @note Just a convenience method delegating to eOptionP.
     */
    final def isEPK: Boolean = eOptionP.isEPK

    /** 
     * Returns the underlying entity. 
     * 
     * @note Just a convenience method delegating to eOptionP.
     */
    final def e: Entity = eOptionP.e

    /**
     * Updates the underlying `EOptionP` value; if the update is relevant, the current set
     * of dependers is cleared and returned along with the old `eOptionP` value.
     *
     * @note This function is only defined if the current `EOptionP` value is not already a
     *       final value. Hence, the client is required to handle (potentially) idempotent updates
     *       and to take care of appropriate synchronization.
     */
    def update(
        newEOptionP: SomeInterimEP,
        c:           OnUpdateContinuation,
        dependees:   Traversable[SomeEOptionP],
        debug:       Boolean
    ): Option[(SomeEOptionP,Set[SomeEPK])]
    //  newEOptionP.isUpdatedComparedTo(eOptionP)

    /**
     * Updates the underlying `eOptionP` and returns the set of dependers.
     */
    def finalUpdate(eOptionP : SomeFinalEP):  Set[SomeEPK] 

    /**
     * Adds the given EPK as a depender on this EPK instance if the current `(this.)eOptionP`
     * equals the given `eOptionP` – based on a reference comparison. If this `eOption`
     * has changed `false` will be returned (adding a depender was not successful); `true` otherwise.
     *
     * @note  This operation is idempotent; that is, adding the same EPK multiple times has no
     *        special effect.
     */
    def addDepender(expectedEOptionP : SomeEOptionP, someEPK: SomeEPK): Boolean

    /**
     * ATOMICALLY: If a continuation function still exists and the given dependee is among the set
     * of current dependees then the continuation function is cleared and returned.
     *
     * (The set of dependees will not be updated!)
     */
    def prepareInvokeC(updatedDependeeEOptionP : SomeEOptionP): Option[OnUpdateContinuation]

    /**
     * ATOMICALLY: If the current continuation function is equal to the given function, the  
     * continuation function is cleared and returned (Some(c)); otherwise None is returned.
     *
     * (The set of dependees will not be updated!)
     */
    def prepareInvokeC(expectedC : OnUpdateContinuation): Option[OnUpdateContinuation]

    def clearDependees() : Unit
    
    /**
     * Removes the given E/PK from the list of dependers of this EPKState.
     *
     * @note This method is always defined and never throws an exception for convenience purposes.
     */
    def removeDepender(someEPK: SomeEPK): Unit

    /**
     * Returns `true` if the current continuation function `c` is reference equal to the
     * given one.
     */
    def isCurrentC(c : OnUpdateContinuation) : Boolean

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

    /*

    def resetDependers(): Set[SomeEPK]

    def lastDependers(): Set[SomeEPK]

    /**
     * Returns the current `OnUpdateComputation` or `null`, if the `OnUpdateComputation` was
     * already triggered. This is an atomic operation. Additionally – in a second step –
     * removes the EPK underlying the EPKState from the the dependees and clears the dependees.
     *
     * @note This method is always defined and never throws an exception.
     */
    def clearOnUpdateComputationAndDependees(): OnUpdateContinuation

    /**
     * Returns `true` if the current `EPKState` has an `OnUpdateComputation` that was not yet
     * triggered.
     *
     * @note The returned value may have changed in the meantime; hence, this method
     *       can/should only be used as a hint.
     */
    def hasPendingOnUpdateComputation: Boolean

 

    */

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
        var eOptionP:            SomeEOptionP,
        val cAR:                 AtomicReference[OnUpdateContinuation],
        @volatile var dependees: Set[SomeEOptionP],
        var dependersAR:         AtomicReference[Set[SomeEPK]]
) extends EPKState {

    assert(eOptionP.isRefinable)

    override def isRefinable: Boolean = true
    override def isFinal: Boolean = false

    override def addDepender(someEPK: SomeEPK): Unit = {
        val dependersAR = this.dependersAR
        if (dependersAR == null)
            return ;

        var prev, next: Set[SomeEPK] = null
        do {
            prev = dependersAR.get()
            next = prev + someEPK
        } while (!dependersAR.compareAndSet(prev, next))
    }

    override def removeDepender(someEPK: SomeEPK): Unit = {
        val dependersAR = this.dependersAR
        if (dependersAR == null)
            return ;

        var prev, next: Set[SomeEPK] = null
        do {
            prev = dependersAR.get()
            next = prev - someEPK
        } while (!dependersAR.compareAndSet(prev, next))
    }

    override def lastDependers(): Set[SomeEPK] = {
        val dependers = dependersAR.get()
        dependersAR = null
        dependers
    }

    override def clearOnUpdateComputationAndDependees(): OnUpdateContinuation = {
        val c = cAR.getAndSet(null)
        dependees = Set.empty
        c
    }

    override def hasPendingOnUpdateComputation: Boolean = cAR.get() != null

    override def update(
        eOptionP:  SomeInterimEP,
        c:         OnUpdateContinuation,
        dependees: Traversable[SomeEOptionP],
        debug:     Boolean
    ): SomeEOptionP = {
        val oldEOptionP = this.eOptionP
        if (debug) oldEOptionP.checkIsValidPropertiesUpdate(eOptionP, dependees)

        this.eOptionP = eOptionP

        val oldOnUpdateContinuation = cAR.getAndSet(c)
        assert(oldOnUpdateContinuation == null)

        assert(this.dependees.isEmpty)
        this.dependees = dependees

        oldEOptionP
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
    override def isFinal: Boolean = true

    override def update(newEOptionP: SomeInterimEP, debug: Boolean): SomeEOptionP = {
        throw new UnknownError(s"the final property $eOptionP can't be updated to $newEOptionP")
    }

    override def addDepender(epk: SomeEPK): Unit = {
        throw new UnknownError(s"final properties can't have dependers")
    }
    
    override def lastDependers(): Set[SomeEPK] = {
        throw new UnknownError(s"the final property $eOptionP can't have dependers")
    }
    
    override def resetDependers(): Set[SomeEPK] = {
                    throw new UnknownError(s"the final property $eOptionP can't have dependers")
                }

    override def removeDepender(someEPK: SomeEPK): Unit = { /* There is nothing to do! */ }

    override def clearOnUpdateComputationAndDependees(): OnUpdateContinuation = {
        null
    }

    override def dependees: Traversable[SomeEOptionP] = {
        throw new UnknownError("final properties don't have dependees")
    }

    override def hasDependees: Boolean = false

    override def setOnUpdateComputationAndDependees(
        c:         OnUpdateContinuation,
        dependees: Traversable[SomeEOptionP]
    ): Unit = {
        throw new UnknownError("final properties can't have \"OnUpdateContinuations\"")
    }

    override def hasPendingOnUpdateComputation: Boolean = false

    override def toString: String = s"FinalEPKState(finalEP=$eOptionP)"
}

object EPKState {

    def apply(finalEP: SomeFinalEP): EPKState = new FinalEPKState(finalEP)

    def apply(eOptionP: SomeEOptionP): InterimEPKState = {
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
    ): InterimEPKState = {
        new InterimEPKState(
            new AtomicReference[SomeEOptionP](eOptionP),
            new AtomicReference[OnUpdateContinuation](c),
            dependees,
            new AtomicReference[Set[SomeEPK]](Set.empty)
        )
    }

    def unapply(epkState: EPKState): Some[SomeEOptionP] = Some(epkState.eOptionP)

}
