/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

/**
 * Encapsulates the state of a single entity and a property of a specific kind.
 *
 * @note The property may change (monotonically), but the property kind has to be stable.
 *
 * @note All methods are effectively thread safe; i.e., clients will always see a
 *       consistent state.
 */
private[par] sealed abstract class EPKState {

    //
    // ------------------- PROPERTIES OF THIS EPK STATE ------------------------
    //

    /**
     * Returns `true` if this entity/property pair is not yet final.
     *
     * @note Even the InterimEPKState object may eventually reference a final property!
     */
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
     * @note Just a convenience method for `eOptionP.isEPK`
     */
    final def isEPK: Boolean = eOptionP.isEPK

    /**
     * Returns the underlying entity.
     *
     * @note Just a convenience method for `eOptionP.e`.
     */
    final def e: Entity = eOptionP.e

    /**
     * Returns the underlying property key – which must never change.
     *
     * @note Just a convenience method for `eOptionP.pk`.
     */
    final def pk: SomePropertyKey = eOptionP.pk

    /**
     * Returns the underlying property key if – which must never change.
     *
     * @note Just a convenience method for `eOptionP.pk.id`.
     */
    final def pkId: Int = eOptionP.pk.id

    /**
     * Atomically updates the underlying `EOptionP` value; if the update is relevant, the current
     * dependers that should be informed are removed and returned along with the old
     * `eOptionP` value.
     *
     * @note This function is only defined if the current `EOptionP` value is not already a
     *       final value. Hence, the client is required to handle (potentially) idempotent updates
     *       and to take care of appropriate synchronization.
     *
     * @note Update is never called concurrently, however, the changes still have to applied
     *       atomically because some of the other methods rely on a consistent snapshot regarding
     *       the relation of the values.
     *
     * @param suppressInterimUpdates (See the corresponding property store datastructure.)
     */
    def update(
        newEOptionP: SomeInterimEP,
        c:           OnUpdateContinuation,
        dependees:   Traversable[SomeEOptionP],
        // BASICALLY CONSTANTS:
        hasSuppressedDependers: Array[Boolean],
        suppressInterimUpdates: Array[Array[Boolean]],
        debug:                  Boolean
    ): Option[(SomeEOptionP, Traversable[SomeEPK])]
    //  newEOptionP.isUpdatedComparedTo(eOptionP)

    /**
     * Atomically updates the underlying `EOptionP` value by applying the given update function;
     * if the update is relevant, the current dependers that should be informed are removed and
     * returned along with the old `eOptionP` value.
     */
    def update(
        u: SomeUpdateComputation,
        // BASICALLY CONSTANTS:
        hasSuppressedDependers: Array[Boolean],
        suppressInterimUpdates: Array[Array[Boolean]]
    ): Option[(SomeEOptionP, SomeInterimEP, Traversable[SomeEPK])]

    def update(
        expectedEOptionP:       SomeEOptionP,
        updatedEOptionP:        SomeInterimEP,
        u:                      SomeUpdateComputation,
        hasSuppressedDependers: Array[Boolean],
        suppressInterimUpdates: Array[Array[Boolean]]
    ): Option[(SomeEOptionP, SomeInterimEP, Traversable[SomeEPK])]

    /**
     * Atomically updates the underlying `eOptionP` and returns the set of dependers.
     */
    def finalUpdate(newEOptionP: SomeFinalEP): Traversable[SomeEPK]

    /**
     * Adds the given EPK as a depender if the current `(this.)eOptionP`
     * equals the given `eOptionP` – based on a reference comparison. If this `eOptionP`
     * has changed `false` will be returned (adding the depender was not successful); `true`
     * otherwise.
     *
     * @note  This operation is idempotent; that is, adding the same EPK multiple times has no
     *        special effect.
     *
     * @param alwaysExceptIfFinal The depender is always added unless the current eOptionP is final.
     */
    def addDepender(
        expectedEOptionP:    SomeEOptionP,
        someEPK:             SomeEPK,
        alwaysExceptIfFinal: Boolean
    ): Boolean

    /**
     * If a continuation function still exists and the given dependee is among the set
     * of current dependees then the continuation function is cleared and returned.
     *
     * @note The set of dependees will not be updated!
     */
    def prepareInvokeC(updatedDependeeEOptionP: SomeEOptionP): Option[OnUpdateContinuation]

    /**
     * If the current continuation function is reference equal to the given function the the
     * continuation function is cleared and `true` is returned; otherwise `false` is returned.
     *
     * @note The set of dependees will not be updated!
     */
    def prepareInvokeC(expectedC: OnUpdateContinuation): Boolean

    def clearDependees(): Unit

    /**
     * Removes the given EPK from the list of dependers of this EPKState.
     *
     * @note This method is always defined and never throws an exception for convenience purposes.
     */
    def removeDepender(someEPK: SomeEPK): Unit

    /**
     * Returns `true` if the current continuation function `c` is reference equal to the given one.
     */
    def isCurrentC(c: OnUpdateContinuation): Boolean

    /**
     * Returns `true` if and only if this `EPKState` has dependees.
     *
     * @note The set of dependees is only updated when a property computation result is processed.
     *       There exists, w.r.t. an Entity/Property kind pair, always at most one
     *       `PropertyComputationResult`.
     *       (Partially computed properties never have dependees on their own.)
     */
    def hasDependees: Boolean

    /**
     * Returns the current set of depeendes or `null`.
     *
     * @note The set of dependees is only updated when a property computation result is processed.
     *       There exists, w.r.t. an Entity/Property kind pair, always at most one
     *       `PropertyComputationResult`.
     *       (Partially computed properties never have dependees on their own.)
     */
    def dependees: Traversable[SomeEOptionP]

    /**
     * Removes the depender/dependee relations related to the given pksTOFinalize
     *
     * __This method is not thread safe. Concurrent execution is NOT supported.__
     */
    def cleanUp(pksToFinalize: List[PropertyKind]): Unit
}

/**
 * Represents the intermediate property of a specific kind related to a specific entity.
 *
 * @note Though an `InterimEPKState` object primarily stores `InterimEP` objects, it may
 *       eventually reference a `FinalEP` object to ensure that clients, which didn't get
 *       the update `FinalEPKState` object are still able to determine that the analysis
 *       of the respective property has finished.
 *
 * @note Basically every `InterimEPKState` object is eventually lifted to a `FinalEPKState`
 *       object which requires no more synchronization and also less memory.
 *
 * @param eOptionP The current property extension; never null.
 * @param c The on update continuation function; null if triggered.
 * @param dependees The dependees.
 */
private[par] final class InterimEPKState(
        @volatile var eOptionP:                SomeEOptionP,
        @volatile var c:                       OnUpdateContinuation,
        @volatile var dependees:               Traversable[SomeEOptionP],
        @volatile private[this] var dependers: Set[SomeEPK]
) extends EPKState /*with Locking*/ {

    assert(eOptionP.isRefinable) // an update which makes it final is possible...

    private[this] final val thisPKId = eOptionP.pk.id

    // NOT THREAD SAFE!
    override def cleanUp(pksToFinalize: List[PropertyKind]): Unit = {
        pksToFinalize.foreach { pkToFinalize ⇒
            if (eOptionP.pk == pkToFinalize) {
                clearDependees()
            }
            dependers = dependers.filter(dependerEPK ⇒ !pksToFinalize.contains(dependerEPK.pk))
        }
    }

    override def isRefinable: Boolean = eOptionP.isRefinable
    override def isFinal: Boolean = eOptionP.isFinal

    override def update(
        eOptionP:               SomeInterimEP,
        c:                      OnUpdateContinuation,
        dependees:              Traversable[SomeEOptionP],
        hasSuppressedDependers: Array[Boolean],
        suppressInterimUpdates: Array[Array[Boolean]],
        debug:                  Boolean
    ): Option[(SomeEOptionP, Traversable[SomeEPK])] = {
        assert(this.c == null)
        // The following assert is not possible, because we only strive for
        // eventual consistency w.r.t. the depender/dependee relation:
        // assert(this.dependees.isEmpty)

        val dependeePKId = this.eOptionP.pk.id
        this.synchronized {
            val oldEOptionP = this.eOptionP
            if (debug) oldEOptionP.checkIsValidPropertiesUpdate(eOptionP, dependees)

            this.c = c
            this.dependees = dependees

            val isRelevantUpdate = eOptionP.isUpdatedComparedTo(oldEOptionP)
            if (isRelevantUpdate) {
                this.eOptionP = eOptionP
                val oldDependers = this.dependers
                if (oldDependers.nonEmpty) {
                    // IMPROVE Given that suppression is rarely required/used(?) it may be more efficient to filter those dependers that should not be informed and then substract that set from the original set.
                    val (suppressedDependers, dependersToBeNotified) =
                        oldDependers.partition { dependerEPK ⇒
                            suppressInterimUpdates(dependerEPK.pk.id)(dependeePKId)
                        }
                    this.dependers = suppressedDependers
                    Some((oldEOptionP, dependersToBeNotified))
                } else {
                    Some((oldEOptionP, oldDependers /* <= basically "NIL" */ ))
                }
            } else {
                None
            }
        }
    }

    override def update(
        u:                      SomeUpdateComputation,
        hasSuppressedDependers: Array[Boolean],
        suppressInterimUpdates: Array[Array[Boolean]]
    ): Option[(SomeEOptionP, SomeInterimEP, Traversable[SomeEPK])] = {
        this.synchronized {
            val oldEOptionP = this.eOptionP
            val newInterimEPOption = u.asInstanceOf[SomeEOptionP ⇒ Option[SomeInterimEP]](oldEOptionP)
            if (newInterimEPOption.isEmpty) {
                return None;
            }

            val newInterimEP = newInterimEPOption.get
            // The test whether we have a relevant update or not should have been done
            // by the update function - it must return "None" if the update is not
            // relevant; i.e., there is no change.
            if (PropertyStore.Debug && !newInterimEP.isUpdatedComparedTo(oldEOptionP)) {
                throw new IllegalArgumentException(
                    s"the update ($u) computed an irrelevant update: $oldEOptionP => $newInterimEP"
                )
            }

            performUpdate(oldEOptionP, newInterimEP, hasSuppressedDependers, suppressInterimUpdates)
        }
    }

    override def update(
        expectedEOptionP:       SomeEOptionP,
        updatedEOptionP:        SomeInterimEP,
        u:                      SomeUpdateComputation,
        hasSuppressedDependers: Array[Boolean],
        suppressInterimUpdates: Array[Array[Boolean]]
    ): Option[(SomeEOptionP, SomeInterimEP, Traversable[SomeEPK])] = {
        this.synchronized {
            if (this.eOptionP eq expectedEOptionP) {
                performUpdate(
                    expectedEOptionP, updatedEOptionP,
                    hasSuppressedDependers, suppressInterimUpdates
                )
            } else {
                update(u, hasSuppressedDependers, suppressInterimUpdates)
            }
        }
    }

    private[this] def performUpdate(
        oldEOptionP:            SomeEOptionP,
        newInterimEP:           SomeInterimEP,
        hasSuppressedDependers: Array[Boolean],
        suppressInterimUpdates: Array[Array[Boolean]]
    ): Option[(SomeEOptionP, SomeInterimEP, Traversable[SomeEPK])] = {
        this.eOptionP = newInterimEP

        val oldDependers = this.dependers
        if (oldDependers.nonEmpty) {
            if (hasSuppressedDependers(thisPKId)) {
                // IMPROVE Given that suppression is rarely required/used(?) it may be more efficient to filter those dependers that should not be informed and then substract that set from the original set.
                val (suppressedDependers, dependersToBeNotified) =
                    oldDependers.partition { dependerEPK ⇒
                        suppressInterimUpdates(dependerEPK.pk.id)(thisPKId)
                    }
                this.dependers = suppressedDependers
                Some((oldEOptionP, newInterimEP, dependersToBeNotified))
            } else {
                this.dependers = Set.empty
                Some((oldEOptionP, newInterimEP, oldDependers))
            }
        } else {
            Some((oldEOptionP, newInterimEP, oldDependers /*<= there are none!*/ ))
        }
    }

    override def finalUpdate(eOptionP: SomeFinalEP): Traversable[SomeEPK] = {
        this.synchronized {
            assert(this.eOptionP.isRefinable)

            this.eOptionP = eOptionP
            val oldDependers = this.dependers
            this.dependers = null
            oldDependers
        }
    }

    override def addDepender(
        expectedEOptionP:    SomeEOptionP,
        someEPK:             SomeEPK,
        alwaysExceptIfFinal: Boolean
    ): Boolean = {
        assert(expectedEOptionP.isRefinable)

        this.synchronized {
            val thisEOptionP = this.eOptionP
            if ((thisEOptionP eq expectedEOptionP) ||
                (alwaysExceptIfFinal && thisEOptionP.isRefinable)) {
                this.dependers += someEPK
                true
            } else {
                false
            }
        }
    }

    override def prepareInvokeC(
        updatedDependeeEOptionP: SomeEOptionP
    ): Option[OnUpdateContinuation] = {
        if (this.c eq null)
            return None;

        this.synchronized {
            val c = this.c
            if (c != null) {
                // IMPROVE ? Use a set based contains check.
                val isDependee = this.dependees.exists(dependee ⇒
                    dependee.e == updatedDependeeEOptionP.e &&
                        dependee.pk == updatedDependeeEOptionP.pk)
                if (isDependee) {
                    this.c = null
                    Some(c)
                } else {
                    None
                }
            } else {
                None
            }
        }
    }

    override def prepareInvokeC(expectedC: OnUpdateContinuation): Boolean = {
        if (this.c ne expectedC)
            return false;

        this.synchronized {
            if (this.c eq expectedC) {
                this.c = null
                true
            } else {
                false
            }
        }
    }

    override def clearDependees(): Unit = this.dependees = null

    override def removeDepender(someEPK: SomeEPK): Unit = {
        this.synchronized {
            // The write lock is required to avoid lost updates; e.g., if
            // two dependers are removed "concurrently".
            this.dependers -= someEPK
        }
    }

    override def isCurrentC(c: OnUpdateContinuation): Boolean = c eq this.c

    override def hasDependees: Boolean = dependees != null && dependees.nonEmpty

    override def toString: String = {
        "InterimEPKState("+
            s"eOptionP=${eOptionP},"+
            s"<hasOnUpdateComputation=${c != null}>,"+
            s"dependees=${Option(dependees)},"+
            s"dependers=$dependers)"
    }
}

private[par] final class FinalEPKState(override val eOptionP: SomeEOptionP) extends EPKState {

    override def isRefinable: Boolean = false
    override def isFinal: Boolean = true

    override def update(
        newEOptionP:            SomeInterimEP,
        c:                      OnUpdateContinuation,
        dependees:              Traversable[SomeEOptionP],
        hasSuppressedDependers: Array[Boolean],
        suppressInterimUpdates: Array[Array[Boolean]],
        debug:                  Boolean
    ): Option[(SomeEOptionP, Set[SomeEPK])] = {
        throw new UnknownError(s"the final property $eOptionP can't be updated to $newEOptionP")
    }

    override def update(
        u:                      SomeUpdateComputation,
        hasSuppressedDependers: Array[Boolean],
        suppressInterimUpdates: Array[Array[Boolean]]
    ): Option[(SomeEOptionP, SomeInterimEP, Set[SomeEPK])] = {
        throw new UnknownError(s"the final property $eOptionP can't be updated using $u")
    }

    override def update(
        expectedEOptionP:       SomeEOptionP,
        updatedEOptionP:        SomeInterimEP,
        u:                      SomeUpdateComputation,
        hasSuppressedDependers: Array[Boolean],
        suppressInterimUpdates: Array[Array[Boolean]]
    ): Option[(SomeEOptionP, SomeInterimEP, Traversable[SomeEPK])] = {
        throw new UnknownError(s"the final property $eOptionP can't be updated using $updatedEOptionP")
    }

    override def finalUpdate(newEOptionP: SomeFinalEP): Set[SomeEPK] = {
        throw new UnknownError(s"the final property $eOptionP can't be updated to $newEOptionP")
    }

    override def addDepender(
        expectedEOptionP:    SomeEOptionP,
        someEPK:             SomeEPK,
        alwaysExceptIfFinal: Boolean
    ): Boolean = false

    override def prepareInvokeC(
        updatedDependeeEOptionP: SomeEOptionP
    ): Option[OnUpdateContinuation] = {
        None
    }

    override def prepareInvokeC(expectedC: OnUpdateContinuation): Boolean = false
    override def clearDependees(): Unit = { /* Nothing to do! */ }
    override def removeDepender(someEPK: SomeEPK): Unit = { /* Nothing to do! */ }
    override def isCurrentC(c: OnUpdateContinuation): Boolean = false
    override def hasDependees: Boolean = false
    override def dependees: Traversable[SomeEOptionP] = null

    override def cleanUp(pksToFinalize: List[PropertyKind]): Unit = { /* Nothing to do! */ }

    override def toString: String = s"FinalEPKState(finalEP=$eOptionP)"
}

object EPKState {

    def apply(finalEP: SomeFinalEP): EPKState = new FinalEPKState(finalEP)

    def apply(eOptionP: SomeEOptionP): InterimEPKState = {
        new InterimEPKState(eOptionP, null, Nil, Set.empty)
    }

    def apply(
        eOptionP:  SomeEOptionP,
        c:         OnUpdateContinuation,
        dependees: Traversable[SomeEOptionP]
    ): InterimEPKState = {
        new InterimEPKState(eOptionP, c, dependees, Set.empty)
    }

    def unapply(epkState: EPKState): Some[SomeEOptionP] = Some(epkState.eOptionP)

}
