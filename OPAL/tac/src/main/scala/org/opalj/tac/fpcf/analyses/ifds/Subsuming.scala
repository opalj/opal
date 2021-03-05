/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds

import org.opalj.br.analyses.SomeProject

/**
 * An IFDS analysis, which implements subsuming.
 *
 * @author Mario Trageser
 */
trait Subsuming[IFDSFact <: SubsumableFact] extends Subsumable[IFDSFact] {

    val numberOfSubsumptions = new NumberOfSubsumptions

    /**
     * Considers subsuming.
     */
    override protected def subsume[T <: IFDSFact](facts: Set[T], project: SomeProject): Set[T] = {
        val result = facts.foldLeft(facts) {
            (result, fact) ⇒
                if (facts.exists(other ⇒ other != fact && other.subsumes(fact, project))) result - fact
                else result
        }
        numberOfSubsumptions.triesToSubsume += 1
        if (result.size != facts.size) numberOfSubsumptions.successfulSubsumes += 1
        result
    }

    /**
     * Considers subsuming.
     */
    override protected def containsNewInformation[T <: IFDSFact](newFacts: Set[T], oldFacts: Set[T],
                                                                 project: SomeProject): Boolean =
        newFacts.exists {
            /*
            * In most cases, the fact will be contained in the old facts.
            * This is why we first do the contains check before linearly iterating over the old facts.
            */
            fact ⇒ !(oldFacts.contains(fact) || oldFacts.exists(_.subsumes(fact, project)))
        }

    /**
     * Considers subsuming.
     */
    override protected def filterNewInformation[T <: IFDSFact](
        newExitFacts: Map[Statement, Set[T]],
        oldExitFacts: Map[Statement, Set[T]], project: SomeProject
    ): Map[Statement, Set[T]] =
        newExitFacts.keys.map {
            statement ⇒
                val old = oldExitFacts.get(statement)
                val newFacts = newExitFacts(statement)
                val newInformation =
                    if (old.isDefined && old.get.nonEmpty) notSubsumedBy(newFacts, old.get, project)
                    else newFacts
                statement → newInformation
        }.toMap

    /**
     * Considers subsuming.
     */
    override protected def notSubsumedBy[T <: IFDSFact](facts: Set[T], otherFacts: Set[T],
                                                        project: SomeProject): Set[T] = {
        val result = facts.foldLeft(facts) {
            (result, fact) ⇒
                if (otherFacts.contains(fact) || otherFacts.exists(_.subsumes(fact, project)))
                    result - fact
                else result
        }
        numberOfSubsumptions.triesToSubsume += 1
        if (result.size != facts.size) numberOfSubsumptions.successfulSubsumes += 1
        result
    }
}

/**
 * Counts, how often subsume was called and how often it eliminated a fact.
 */
class NumberOfSubsumptions {

    /**
     * The number of subsume and notSubsumedBy calls.
     */
    var triesToSubsume = 0

    /**
     * The number of subsume and notSubsumedBy calls, which eliminated a fact.
     */
    var successfulSubsumes = 0
}