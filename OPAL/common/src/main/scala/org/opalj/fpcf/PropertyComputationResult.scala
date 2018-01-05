/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj.fpcf

/**
 * Encapsulates the result of the computation of a property.
 */
sealed abstract class PropertyComputationResult {

    private[fpcf] def id: Int

}

/**
 * Encapsulates the final result of the computation of a property. I.e., the analysis
 * determined that the computed property will not be updated because there is no further chance
 * to do so.
 *
 * A final result is only to be used if no further refinement is possible or may happen.
 *
 * @note The framework will invoke and deregister all dependent computations (observers). If –
 *      after having a result another result w.r.t. the given entity and property is given to
 *      the property store – the behavior is undefined and may/will result in immediate but
 *      also deferred arbitrary failures!
 */
sealed abstract class FinalPropertyComputationResult extends PropertyComputationResult

/**
 * Encapsulates the '''final result''' of the computation of the property `p` for the given
 * entity `e`.
 *
 * @see [[FinalPropertyComputationResult]] for further information.
 */
case class Result(e: Entity, p: Property) extends FinalPropertyComputationResult {

    private[fpcf] final def id = Result.id

}
private[fpcf] object Result { private[fpcf] final val id = 3 }

/**
 * Encapsulates the '''final results''' of the computation of a set of properties.
 *
 * The encapsulated results are not atomically set; they are set one after another.
 *
 * @see [[FinalPropertyComputationResult]] for further information.
 */
case class MultiResult(properties: ComputationResults) extends FinalPropertyComputationResult {

    private[fpcf] final def id = MultiResult.id

}
private[fpcf] object MultiResult { private[fpcf] final val id = 1 }

/**
 * Used if the analysis found no entities for which a property could be computed.
 */
object NoResult extends PropertyComputationResult {
    private[fpcf] final val id = 5
}

/**
 * Encapsulates an intermediate result of the computation of a property.
 *
 * Intermediate results are to be used if further refinements are possible and may happen.
 * Hence, if a property of any of the dependees changes (outgoing dependencies),
 * the given continuation `c` is invoked.
 *
 * All current computations that depend on the property of the entity will be invoked.
 *
 * @param dependees A traversable of entity/property (kind) pairs the analysis depends on. Each
 *      entity/property kind must occur at most once in the list, the current entity/propertykind
 *      must not occur; i.e., self-reference are forbidden!
 *
 * @param c
 *      The function which is called if a property of any of the dependees is updated.
 *      `c` does not have to be thread safe unless the same instance of `c` is returned multiple
 *      times for different entities (`e`) which should be avoided.
 *
 * @note All elements on which the result declares to be dependent on must have been queried
 *      before (using one of the `apply` functions of the property store.)
 */
case class IntermediateResult(
        ep:        SomeEP,
        dependees: Traversable[SomeEOptionP],
        c:         OnUpdateContinuation
) extends PropertyComputationResult {

    assert(ep.p.isRefinable, s"intermediate result $this used to store final property")
    assert(dependees.nonEmpty, s"intermediate result $this without open dependencies")
    assert(c ne null, "onUpdateContinuation is null")

    // TODO Make it possible to activate the assertions!
    //    // End-User oriented assertion:
    //    assert(
    //        dependees.map(eOptP ⇒ eOptP.toEPK).toSet.size == dependees.size,
    //        s"the intermediate result's dependees list ${dependees.mkString("(", ",", ")")} "+
    //            "contains duplicate entries (E - PK pairs)!"
    //    )
    //
    //    // End-User oriented assertion:
    //    assert(
    //        { val dependerEPK = EPK(e, p.key); !dependees.exists(_ == dependerEPK) },
    //        s"the computation of ${EPK(e, p.key)} depends on its own: ${dependees.find(_ == EPK(e, p.key))}"
    //    )

    private[fpcf] final def id = IntermediateResult.id

    override def hashCode: Int = ep.hashCode * 17 + dependees.hashCode

    override def equals(other: Any): Boolean = {
        other match {
            case IntermediateResult(`ep`, otherDependeeEs, _) ⇒
                val dependees = this.dependees
                dependees.size == otherDependeeEs.size &&
                    dependees.forall(thisDependee ⇒ otherDependeeEs.exists(_ == thisDependee))

            case _ ⇒
                false
        }
    }

    override def toString: String = {
        s"IntermediateResult($ep,dependees=${dependees.mkString("{", ",", "}")})"
    }
}
private[fpcf] object IntermediateResult {
    private[fpcf] final val id = 6

    def apply(
        e:         Entity,
        p:         Property,
        dependees: Traversable[SomeEOptionP],
        c:         OnUpdateContinuation
    ): IntermediateResult = {
        new IntermediateResult(EP(e, p), dependees, c)
    }

}

/**
 * Encapsulates some result and also some computations that should be scheduled after the results
 * were stored. I.e., in this case the property store guarantees that all values stored previously
 * can be queried by `nextComputations` if necessary.
 *
 * To ensure correctness it is absolutely essential that all entities - for which some result
 * could eventually be computed - are actually associated with some result before the
 * property store reaches quiescence. Hence, it is generally not possible that a lazy
 * computation returns `IncrementalResult` objects.
 *
 * Incremental results are particularly useful to process tree structures such as the class
 * hierarchy.
 */
case class IncrementalResult[E <: Entity](
        result:           PropertyComputationResult,
        nextComputations: Traversable[(PropertyComputation[E], E)]
) extends PropertyComputationResult {

    private[fpcf] final def id = IncrementalResult.id

}

private[fpcf] object IncrementalResult { private[fpcf] final val id = 7 }

/**
 * If an analysis is finished and will not return more precise results, but a subsequent
 * analysis (scheduled in a subsequent phase) may refine the results, a ```PhaseResult```
 * has to be used.
 *
 * @note If the given property is not refinable a (Immediate)Result has to be used.
 * @note `PhaseResults` will never lead to cycles as we have no more outgoing dependencies.
 *      TODO The property store can be configured to know which properties are collaboratively
 *      computed in multiple phases; if no subsequent analysis is scheduled the `PropertyStore`
 *      will commit a `PhaseResult` directly as a final result.
 *
 */
// TODO Rename "PhaseResult"
case class RefinableResult(e: Entity, p: Property) extends PropertyComputationResult {

    assert(p.isRefinable)

    private[fpcf] final def id = RefinableResult.id

}
private[fpcf] object RefinableResult { private[fpcf] final val id = 10 }

/**
 * Just a collection of multiple results.
 *
 * @param results
 */
case class Results(
        results: TraversableOnce[PropertyComputationResult]
) extends PropertyComputationResult {

    private[fpcf] final def id = Results.id

}
private[fpcf] object Results {
    private[fpcf] final val id = 8

    def apply(results: PropertyComputationResult*): Results = {
        new Results(results)
    }

}

