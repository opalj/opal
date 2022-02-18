/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds

import org.opalj.br.analyses.SomeProject

/**
 * The supertype of all IFDS facts, which can subsume another fact.
 */
trait SubsumableFact extends AbstractIFDSFact {

    /**
     * Checks, if this fact subsumes an `other` fact.
     *
     * @param other The other fact.
     * @param project The analyzed project.
     * @return True, if this fact subsumes the `other`fact
     */
    def subsumes(other: AbstractIFDSFact, project: SomeProject): Boolean
}

/**
 * The null fact for subsumable facts.
 */
trait SubsumableNullFact extends SubsumableFact with AbstractIFDSNullFact {

    /**
     * The null fact cannot subsume another fact.
     */
    override def subsumes(other: AbstractIFDSFact, project: SomeProject): Boolean = false
}

/**
 * Defines functions, which can be overwritten to implement subsuming.
 *
 * @author Mario Trageser
 */
trait Subsumable[IFDSFact <: AbstractIFDSFact] {

    /**
     * A subclass can override this method to filter the `facts` in some set, which are not subsumed
     * by another fact in the set.
     *
     *
     * @param facts The set of facts.
     * @param project The project, which is analyzed.
     * @return The facts, which are not subsumed by any other fact in `facts`.
     *         By default, `facts` is returned without removing anything.
     */
    protected def subsume[T <: IFDSFact](facts: Set[T], project: SomeProject): Set[T] = facts

    /**
     * Checks, if any fact from some set is not equal to or subsumed by any fact in another set.
     * A subclass implementing subsuming must overwrite this method to consider subsuming.
     *
     * @param newFacts The facts, which were found. Not empty.
     * @param oldFacts The facts, which are already known. Not empty.
     * @param project The project, which is analyzed.
     * @return True, if any fact in `newFacts`, is not equal to or subsumed by any fact in
     *         `oldFacts`.
     */
    protected def containsNewInformation[T <: IFDSFact](
        newFacts: Set[T],
        oldFacts: Set[T],
        project:  SomeProject
    ): Boolean =
        newFacts.exists(newFact ⇒ !oldFacts.contains(newFact))

    /**
     * Filters the new information from a new set of exit facts given the already known exit facts.
     * A subclass implementing subsuming must overwrite this method to consider subsuming.
     *
     * @param newExitFacts The new exit facts.
     * @param oldExitFacts The old exit facts.
     * @param project The project, which is analyzed.
     * @return A map, containing the keys of `newExitFacts`.
     *         Facts, which are equal to or subsumed by any fact for the same statement in
     *         `oldExitFacts` are not present.
     */
    protected def filterNewInformation[T <: IFDSFact](
        newExitFacts: Map[Statement, Set[T]],
        oldExitFacts: Map[Statement, Set[T]],
        project:      SomeProject
    ): Map[Statement, Set[T]] = {
        var result = newExitFacts
        for ((key, values) ← oldExitFacts) {
            result = result.updated(key, result(key) -- values)
        }
        result
    }

    /**
     * Filters the `facts` from some set, which are not equal to or subsumed by any fact in an
     * `other` set.
     * A subclass implementing subsuming must overwrite this method to consider subsuming.
     *
     * @param facts The set, from which facts are filtered.
     * @param otherFacts The set, which may subsume facts from `facts`.
     * @param project The project, which is analyzed.
     * @return The facts from `facts`, which are not equal to or subsumed by any fact in an
     *         `otherFacts`.
     */
    protected def notSubsumedBy[T <: IFDSFact](facts: Set[T], otherFacts: Set[T],
                                               project: SomeProject): Set[T] =
        facts -- otherFacts
}
