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
package org.opalj.fp

//import java.util.concurrent.atomic.AtomicInteger

/**
 * Encapsulates the result of the computation of a property.
 */
sealed trait PropertyComputationResult

/**
 * Computing a property for the respective element is not possible or did not
 * result in a new result.
 */
case object NoResult extends PropertyComputationResult

/**
 * Encapsulates the '''final results''' of the computation of the property.
 *
 * A [[MultiResult]] is only to be used if no further refinement is possible or may happen. The
 * framework will then invoke and deregister all observers (garbage collection).
 */
case class MultiResult(
    properties: Traversable[(Entity, Property)])
        extends PropertyComputationResult

/**
 * Encapsulates the '''final result''' of the computation of the property.
 *
 * A [[Result]] is only to be used if no further refinement is possible or may happen. The
 * framework will then invoke and deregister all observers (garbage collection).
 */
case class Result(e: Entity, p: Property) extends PropertyComputationResult

/**
 * Encapsulates an intermediate result of the computation of the property.
 *
 * Intermediate results are to be used if further refinements are possible or may happen.
 * All current observer remain registered and will be informed in the future.
 */
case class IntermediateResult(
    e: Entity, p: Property,
    dependeeEs: Traversable[EP],
    c: Continuation)
        extends PropertyComputationResult

///**
// * Encapsulates the result of the computation of a property which may be refined if
// *
// * If the property of any required element is refined, the property computation
// * function is called again.
// *
// * @param dependees A `Traversable` of all entities on which the computation
// *      depends and – for which a refinement may yield a more precise result.
// *      The specified property (if any) is the value of the property which was used
// *      to perform the computation. I.e., the framework will only call the property
// *      computation function if the property has changed.
// */
//case class RefineableResult(
//    properties: Traversable[(Entity, Property)],
//    dependees: Traversable[(Entity, PropertyKey, Option[Property], Continuation)])
//        extends PropertyComputationResult {
//
//    assert(dependees.size > 0, "intermediate results must have dependencies")
//
//    //private[fp] val dependenciesCount = dependingEntities.size
//    //private[fp] val unrefinedDependencies = new AtomicInteger(dependenciesCount)
//
//}

/**
 * @param dependeeE The entity about which some knowledge is required by this
 *      computation before the computation can be continued.
 * @param dependeePK The property kind of the given entity about which some knowledge
 *      is required.
 */
private[fp] abstract class Suspended(
    val e: Entity,
    val pk: PropertyKey,
    val dependeeE: Entity,
    val dependeePk: PropertyKey)
        extends PropertyComputationResult {

    /**
     * Called by the framework if the property of the element this computation is
     * depending on was computed.
     */
    def continue(
        dependeeE: Entity,
        dependeeP: Property): PropertyComputationResult

}

private[fp] object Suspended {

    def unapply(c: Suspended): Some[(Entity, PropertyKey, Entity, PropertyKey)] =
        Some((c.e, c.pk, c.dependeeE, c.dependeePk))

}
