/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ifds.old

import org.opalj.br.analyses.SomeProject
import org.opalj.ifds.AbstractIFDSFact

/**
 * Defines functions, which can be overwritten to implement subsuming.
 *
 * @author Mario Trageser
 */
trait Subsumable[S, IFDSFact <: AbstractIFDSFact] {

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
        newFacts.exists(newFact => !oldFacts.contains(newFact))

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
        newExitFacts: Map[S, Set[T]],
        oldExitFacts: Map[S, Set[T]],
        project:      SomeProject
    ): Map[S, Set[T]] = {
        var result = newExitFacts
        for ((key, values) <- oldExitFacts) {
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
    protected def notSubsumedBy[T <: IFDSFact](
        facts:      Set[T],
        otherFacts: Set[T],
        project:    SomeProject
    ): Set[T] =
        facts -- otherFacts
}
