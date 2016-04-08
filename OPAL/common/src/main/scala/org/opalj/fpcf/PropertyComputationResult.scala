/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
sealed trait PropertyComputationResult {

    private[fpcf] def id: Int

}

sealed trait FinalPropertyComputationResult extends PropertyComputationResult

/**
 * Encapsulates the '''final result''' of the computation of the property.
 *
 * A [[Result]] is only to be used if no further refinement is possible
 * or may happen. The framework will then invoke and deregister all
 * dependent computations (observers).
 */
case class Result(e: Entity, p: Property) extends FinalPropertyComputationResult {

    private[fpcf] final def id = Result.id

}
private[fpcf] object Result { private[fpcf] final val id = 3 }

/**
 * Encapsulates the '''final result''' of a computation of a property that '''required
 * no intermediate results'''; i.e., if no properties of other entities were queried.
 *
 * An `ImmediateResult` is only to be used if no further refinement is possible
 * or may happen. The framework will then invoke and deregister all
 * dependent computations (observers).
 */
case class ImmediateResult(e: Entity, p: Property) extends FinalPropertyComputationResult {

    private[fpcf] final def id = ImmediateResult.id

}
private[fpcf] object ImmediateResult { private[fpcf] final val id = 4 }

/**
 * Encapsulates the '''final results''' of the computation of a set of properties.
 *
 * A [[MultiResult]] is only to be used if no further refinement is possible
 * or may happen. The framework will then invoke and deregister all
 * dependent computations (observers).
 *
 * The encapsulated results are not atomically set.
 */
case class MultiResult(properties: ComputationResults) extends FinalPropertyComputationResult {

    private[fpcf] final def id = MultiResult.id

}
private[fpcf] object MultiResult { private[fpcf] final val id = 1 }

/**
 * Encapsulates the '''final results''' of the computation of a set of properties that
 * required no intermediate steps.
 *
 * An `ImmediateMultiResult` is only to be used if no further refinement is possible
 * or may happen. The framework will then invoke and deregister all
 * dependent computations (observers).
 */
case class ImmediateMultiResult(properties: ComputationResults) extends FinalPropertyComputationResult {

    private[fpcf] final def id = ImmediateMultiResult.id

}
private[fpcf] object ImmediateMultiResult { private[fpcf] final val id = 2 }

/**
 * Used if the analysis found no entities for which a property could be computed.
 */
object NoEntities extends ImmediateMultiResult(Traversable.empty)

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
 * 		entity/property kind must occur at most once in the list.
 *
 * @param c
 * 		The function which is called if a property of any of the dependees is updated.
 * 		`c` does not have to be thread safe unless the same instance of c is returned multiple
 * 		times for different entities (`e`).
 *
 * @note All elements on which the result declares to be dependent on must have been queried
 * 		before (using one of the `apply` functions of the property store.)
 */
case class IntermediateResult(
        e:         Entity,
        p:         Property,
        dependees: Traversable[SomeEOptionP],
        c:         OnUpdateContinuation
) extends PropertyComputationResult {

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

    override def hashCode: Int = (e.hashCode * 13 + p.hashCode) * 17 + dependees.hashCode

    override def equals(other: Any): Boolean = {
        other match {
            case IntermediateResult(otherE, otherP, otherDependeeEs, _) ⇒
                this.e == otherE && this.p == otherP && {
                    val dependees = this.dependees
                    dependees.forall(thisDependee ⇒ otherDependeeEs.exists(_ == thisDependee)) &&
                        dependees.size == otherDependeeEs.size
                }
            case _ ⇒ false
        }
    }

    override def toString: String = {
        s"IntermediateResult($e,$p,dependees=${dependees.mkString("{", ",", "}")})"
    }
}
private[fpcf] object IntermediateResult { private[fpcf] final val id = 5 }

/**
 * Encapsulates some result and also some computations that should be scheduled after handling the
 * result.
 */
case class IncrementalResult[E <: Entity](
        result:           PropertyComputationResult,
        nextComputations: Traversable[(PropertyComputation[E], E)]
) extends PropertyComputationResult {

    private[fpcf] final def id = IncrementalResult.id

}

private[fpcf] object IncrementalResult { private[fpcf] final val id = 6 }

case class Results(results: List[PropertyComputationResult]) extends PropertyComputationResult {

    private[fpcf] final def id = Results.id

}
private[fpcf] object Results { private[fpcf] final val id = 7 }

//
//
// PACKAGE PRIVATE (INTERNALLY USED) PropertyComputationResult OBJECTS
//
//

/**
 * Represents a suspended computation.
 *
 * @param dependeeE The entity about which some knowledge is required by this
 *      computation before the computation can be continued.
 * @param dependeePK The property kind of the given entity about which some knowledge
 *      is required.
 */
abstract class SuspendedPC[DependeeP <: Property] private[fpcf] (
        val e:          Entity,
        val pk:         SomePropertyKey,
        val dependeeE:  Entity,
        val dependeePK: PropertyKey[DependeeP]
) extends PropertyComputationResult {

    /**
     * Called by the framework when the property of the element `dependeeE` on which
     * this computation is depending on was computed.
     */
    def continue(dependeeP: DependeeP): PropertyComputationResult

    private[fpcf] final def id = SuspendedPC.id

    override def toString: String = {
        s"SuspendedPropertyComputation(DependerEPK($e,$pk),DependeeEPK($dependeeE,$dependeePK))"
    }
}

/**
 * Factory for creating [[Suspended]] computations.
 */
private[fpcf] object SuspendedPC {

    private[fpcf] final val id = 8

    def unapply[DependeeP <: Property](
        c: SuspendedPC[DependeeP]
    ): Some[(Entity, SomePropertyKey, Entity, PropertyKey[DependeeP])] =
        Some((c.e, c.pk, c.dependeeE, c.dependeePK))

}
