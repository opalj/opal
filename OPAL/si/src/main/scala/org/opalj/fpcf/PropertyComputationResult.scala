/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

/**
 * Encapsulates the (intermediate) result of the computation of a property.
 *
 * @author Michael Eichberg
 */
sealed abstract class PropertyComputationResult {

    private[fpcf] def id: Int

    private[fpcf] def isIntermediateResult: Boolean = false
    private[fpcf] def asIntermediateResult: IntermediateResult[_ <: Property] = {
        throw new ClassCastException();
    }
}

/**
 * Used if the analysis found no entities for which a property could be computed.
 *
 * @note A `NoResult` can only be used as the result of an initial computation. Hence, an
 *       `OnUpdateContinuation` must never return `NoResult`.
 */
object NoResult extends PropertyComputationResult {
    private[fpcf] final val id = 0
}

/**
 * Encapsulates the final result of the computation of a property. I.e., the analysis
 * determined that the computed property will not be updated in the future.
 *
 * A final result is only to be used if no further refinement is possible or may happen and
 * if the bounds are correct/sound abstractions.
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

    override def toString: String = s"Result($e@${System.identityHashCode(e).toHexString},p=$p)"
}
object Result { private[fpcf] final val id = 1 }

/**
 * Encapsulates the '''final results''' of the computation of a set of properties. Hence, all
 * results have to be w.r.t. different e/pk pairs.
 *
 * The encapsulated results are not atomically set; they are set one after another.
 *
 * @see [[FinalPropertyComputationResult]] for further information.
 */
case class MultiResult(properties: ComputationResults) extends FinalPropertyComputationResult {

    private[fpcf] final def id = MultiResult.id

}
object MultiResult { private[fpcf] final val id = 2 }

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
        c:         OnUpdateContinuation,
        hint:      PropertyComputationHint   = DefaultPropertyComputation
) extends PropertyComputationResult {

    if (PropertyStore.Debug) {
        if (lb == ub) {
            throw new IllegalArgumentException(s"intermediate result with equal bounds: $this")
        }
        if (dependees.isEmpty) {
            throw new IllegalArgumentException(
                s"intermediate result without dependencies: $this"+
                    " (use PartialResult for collaboratively computed results)"
            )
        }
        if (dependees.exists(eOptP ⇒ eOptP.e == e && eOptP.pk == ub.key)) {
            throw new IllegalArgumentException(
                s"intermediate result with an illegal self-dependency: "+this
            )
        }
        if (lb.isOrderedProperty) {
            val ubAsOP = ub.asOrderedProperty
            ubAsOP.checkIsEqualOrBetterThan(e, lb.asInstanceOf[ubAsOP.Self])
        }
        if (ub.key != lb.key) {
            throw new IllegalArgumentException(
                s"property keys for lower ${lb.key} and upper ${ub.key} bound don't match"
            )
        }
    }

    private[fpcf] final def id = IntermediateResult.id

    private[fpcf] final override def isIntermediateResult: Boolean = true
    private[fpcf] final override def asIntermediateResult: IntermediateResult[_ <: Property] = this

    override def hashCode: Int = e.hashCode * 17 + dependees.hashCode

    override def equals(other: Any): Boolean = {
        other match {
            case IntermediateResult(e, lb, ub, otherDependees, _, _) if (
                this.e == e && this.lb == lb && this.ub == ub
            ) ⇒
                val dependees = this.dependees
                dependees.size == otherDependees.size &&
                    dependees.forall(thisDependee ⇒ otherDependees.exists(_ == thisDependee))

            case _ ⇒
                false
        }
    }

    override def toString: String = {
        s"IntermediateResult($e@${System.identityHashCode(e).toHexString},lb=$lb,ub=$ub,"+
            s"dependees=${dependees.mkString("{", ",", "}")},c=$c)"
    }
}
object IntermediateResult {
    private[fpcf] final val id = 3
}

/**
 * Encapsulates some result and also some computations that should be computed next.
 * In this case the property store DOES NOT guarantee that the result is processed
 * before the next computations are triggered. Hence, `nextComputations` can query the e/pk
 * related to the previous result, but should not expect to already see the value of the
 * given result(s).
 *
 * Incremental results are particularly useful to process tree structures such as the class
 * hierarchy.
 *
 * @note All computations must compute different e/pk pairs which are not yet computed/scheduled or
 *       for which lazy computations are scheduled.
 *
 * @note To ensure correctness it is absolutely essential that all entities - for which some
 *       property could eventually be computed - has a property before the
 *       property store reaches quiescence. Hence, it is generally not possible that a lazy
 *       computation returns `IncrementalResult` objects.
 */
case class IncrementalResult[E <: Entity](
        result:                   PropertyComputationResult,
        nextComputations:         Iterator[(PropertyComputation[E], E)],
        propertyComputationsHint: PropertyComputationHint               = DefaultPropertyComputation
) extends PropertyComputationResult {

    private[fpcf] final def id = IncrementalResult.id

}

object IncrementalResult { private[fpcf] final val id = 4 }

/**
 * Just a collection of multiple results. The results have to be disjoint w.r.t. the underlying
 * e/pk pairs for which it contains results.
 */
case class Results(
        results: TraversableOnce[PropertyComputationResult]
) extends PropertyComputationResult {

    private[fpcf] final def id = Results.id

}
object Results {

    private[fpcf] final val id = 5

    def apply(results: PropertyComputationResult*): Results = new Results(results)
}

/**
 * `PartialResult`s are used for properties of entities which are computed collaboratively/in
 * a piecewise fashion.
 *
 * For example, let's assume that we have an entity `Project` which has the property to store
 * the types which are instantiated and which is updated whenever an analysis of a method
 * detects the instantiation of a type. In this case, the analysis of the method could return
 * a [[Results]] object which contains the `(Intermediate)Result` for the analysis of the method as
 * such and a `PartialResult` which will update the information about the overall set of
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
object PartialResult { private[fpcf] final val id = 6 }

/**************************************************************************************************\
 *
 *                              ONLY USED INTERNALLY BY THE FRAMEWORK! 
 *
\**************************************************************************************************/

private[fpcf] case class ExternalResult(
        e: Entity,
        p: Property
) extends FinalPropertyComputationResult {

    private[fpcf] final def id = ExternalResult.id

}
private[fpcf] object ExternalResult { private[fpcf] final val id = 7 }

private[fpcf] case class CSCCsResult(
        csccs: List[Iterable[SomeEPK]]
) extends FinalPropertyComputationResult {

    private[fpcf] final def id = CSCCsResult.id

}
private[fpcf] object CSCCsResult { private[fpcf] final val id = 8 }

private[fpcf] case class IdempotentResult(
        finalEP: SomeFinalEP
) extends FinalPropertyComputationResult {
    private[fpcf] final def id = IdempotentResult.id

}
private[fpcf] object IdempotentResult { private[fpcf] final val id = 9 }
