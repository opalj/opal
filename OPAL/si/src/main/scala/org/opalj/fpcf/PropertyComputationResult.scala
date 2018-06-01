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
 *
 * @author Michael Eichberg
 */
sealed abstract class PropertyComputationResult {

    private[fpcf] def id: Int

}

/**
 * Used if the analysis found no entities for which a property could be computed.
 *
 * In some cases it makes sense to do, e.g., an analysis per class to compute the properties
 * for some fields.
 */
object NoResult extends PropertyComputationResult {
    private[fpcf] final val id = 0
}

/**
 * Encapsulates the final result of the computation of a property. I.e., the analysis
 * determined that the computed property will not be updated because there is no further
 * chance to do so.
 *
 * A final result is only to be used if no further refinement is possible or may happen.
 *
 * @note   The framework will invoke and deregister all dependent computations (observers).
 *         If – after having a result - another result w.r.t. the given entity and property is given
 *         to the property store the behavior is undefined and may/will result in immediate and/or
 *         deferred failures!
 */
sealed abstract class FinalPropertyComputationResult extends PropertyComputationResult

/**
 * Encapsulates the '''final result''' of the computation of the property `p` for the given
 * entity `e`. See [[EOptionP#ub]] for a discussion related to properties.
 *
 * @see [[FinalPropertyComputationResult]] for further information.
 */
case class Result(e: Entity, p: Property) extends FinalPropertyComputationResult {

    private[fpcf] final def id = Result.id

}
private[fpcf] object Result { private[fpcf] final val id = 1 }

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
private[fpcf] object MultiResult { private[fpcf] final val id = 2 }

/**
 * Encapsulates an intermediate result of the computation of a property.
 *
 * Intermediate results are to be used if further refinements are possible.
 * Hence, if a property of any of the dependees changes (outgoing dependencies),
 * the given continuation `c` is invoked.
 *
 * All current computations that depend on the property of the entity will be invoked.
 *
 * @param dependees The entity/property (kind) pairs the analysis depends on. Each
 *      `entity`/`property kind` pair must occur at most once in the list, the current
 *      entity/property kind (`ep`) must not occur; i.e., self-reference are forbidden!
 *      A dependee must have been queried using `PropertyStore.apply(...)`; directly
 *      returning a dependee without a prior querying of the property store can lead to
 *      unexpected results. A dependee must NEVER be less precise than the value returned by
 *      the query.
 *
 *      In general, the set of dependees is expected to shrink over time and the result should
 *      capture the effect of all properties. However, it is possible to first wait on specific
 *      properties of specific entities, if these properties ultimately determine the overall
 *      result. Hence, it is possible to partition the set of entity / properties and to query
 *      each group one after another.
 *
 *      An `IntermediateResult` returned by an `OnUpdateContinuation` must contain the EPS given
 *      to the continuation function or a newer EPS (i.e., an onUpdateContinuation is allowed
 *      to query the store again).
 *
 *      ''The given set of dependees must not be mutated; it is used internally and if the
 *      set is mutated, propagation of changes no longer works reliably.''
 *
 * @param c
 *      The function which is called if a property of any of the dependees is updated.
 *      `c` does not have to be thread safe unless the same instance of `c` is returned multiple
 *      times for different entities (`e`) which should be avoided and is generally not necessary.
 *      I.e., it is recommended to think about `c` as the function that completes the
 *      computation of the property `p` for the entity `e` identified by `ep`.
 *      In general, `c` can have (mutual) state encapsulates
 *      (temporary) information required to compute the final property.
 *
 * @note All elements on which the result declares to be dependent on must have been queried
 *      before (using one of the `apply` functions of the property store.)
 */
case class IntermediateResult[P <: Property](
        e:         Entity,
        lb:        P,
        ub:        P,
        dependees: Traversable[SomeEOptionP],
        c:         OnUpdateContinuation
) extends PropertyComputationResult {

    assert(e ne null)
    assert(lb ne null)
    assert(ub ne null)
    assert(c ne null, "onUpdateContinuation is null")
    assert(dependees.nonEmpty, s"intermediate result $this without open dependencies")
    assert(lb ne ub, s"intermediate result $this with same lower and upper bound")

    private[fpcf] final def id = IntermediateResult.id

    override def hashCode: Int = e.hashCode * 17 + dependees.hashCode

    override def equals(other: Any): Boolean = {
        other match {
            case IntermediateResult(e, lb, ub, otherDependees, _) if (
                (this.e eq e) && this.lb == lb && this.ub == ub
            ) ⇒
                val dependees = this.dependees
                dependees.size == otherDependees.size &&
                    dependees.forall(thisDependee ⇒ otherDependees.exists(_ == thisDependee))

            case _ ⇒
                false
        }
    }

    override def toString: String = {
        s"IntermediateResult($e,lb=$lb,ub=$ub,dependees=${dependees.mkString("{", ",", "}")},c=$c)"
    }
}
private[fpcf] object IntermediateResult {
    private[fpcf] final val id = 3
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

private[fpcf] object IncrementalResult { private[fpcf] final val id = 4 }

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
    private[fpcf] final val id = 5

    def apply(results: PropertyComputationResult*): Results = {
        new Results(results)
    }

}

/**
 * PartialResults are used for properties of entities which are computed collaboratively/in
 * a piecewise fashion. PartialResults cannot be used for properties which are (also) computed
 * by lazy property computations.
 *
 * For example, let's assume that we have an entity `CFG` which has the property to store
 * the types which are instantiated and which is updated whenever an analysis of a method
 * detects the instantiation of a type. In this case, the analysis of the method could return
 * a MultiResult which contains the (Intermediate)Result for the analysis of the method as
 * such and a PartialResult which will update the information about the overall set of
 * instantiated types.
 *
 * @param e The entity for which we have a partial result.
 * @param pk The kind of the property for which we have a partial result.
 * @param u The function which is given the current property (if any) and which computes the
 *          new property. `u` has to return `None` if the update does not change the property
 *          and `Some(NewProperty)` otherwise.
 * @tparam P The type of the property.
 */
case class PartialResult[E >: Null <: Entity, P >: Null <: Property](
        e:  E,
        pk: PropertyKey[P],
        u:  EOptionP[E, P] ⇒ Option[EPS[E, P]]
) extends PropertyComputationResult {

    private[fpcf] final def id = PartialResult.id

}
private[fpcf] object PartialResult { private[fpcf] final val id = 6 }

