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

import java.util.concurrent.atomic.AtomicInteger

sealed trait PropertyComputationResult

/**
 * Computing a  property for the respective element is not possible.
 */
case object NoResult extends PropertyComputationResult

/**
 * Encapsulates the final result of the computation of the property.
 *
 * Result is only to be used if no further refinement is possible or may happen.
 */
case class Result(
    properties: Traversable[(Entity, Property)])
        extends PropertyComputationResult

object Result {
    def apply(e: Entity, p: Property): Result = new Result(Traversable((e, p)))
}

/**
 * Encapsulates the result of the computation of a property.
 *
 * If the property of any required element is refined, the property computation
 * function is called again.
 *
 * @param requiredEntities A `Traversable` of all entities on which the computation
 *      depended and – for which a refinement may yield a more precise result.
 *      The specified property (if any) is the value of the property which was used
 *      to perform the computation. I.e., the framework will only call the property
 *      computation function if the property has changed.
 */
case class IntermediateResult(
    properties: Traversable[(Entity, Property)],
    requiredEntities: Traversable[(Entity, PropertyKey, Option[Property], Continuation)])
        extends PropertyComputationResult {

    assert(requiredEntities.size > 0, "intermediate results must have dependencies")

    //private[fp] val dependenciesCount = dependingEntities.size
    //private[fp] val unrefinedDependencies = new AtomicInteger(dependenciesCount)

}

/**
 * @param requiredEntity The entity about which some knowledge is required by this
 *      computation before the computation can be continued.
 */
abstract class Suspended(
    val e: Entity,
    val pk: PropertyKey,
    val requiredEntity: Entity,
    val requiredProperty: PropertyKey)
        extends PropertyComputationResult {

    /**
     * Called by the framework if the property for the respective element was computed
     */
    def continue(
        requiredEntity: Entity,
        requiredProperty: Property): PropertyComputationResult

    /**
     * Terminates this computation.
     *
     * This method is called by the framework if this computation is waiting on the results
     * of computations of properties for elements for which no further computations
     * are running.
     */
    def terminate(): Unit

    /**
     * The fallback [[Property]] associated with the computation. This method is
     * called by the framework if it identifies a cycle and tries to continue the computation
     * by using default properties for one or more elements of the cycle.
     */
    def fallback: Property
}

object Suspended {

    def unapply(c: Suspended): Some[(Entity, PropertyKey, Entity, PropertyKey)] =
        Some((c.e, c.pk, c.requiredEntity, c.requiredProperty))

}
