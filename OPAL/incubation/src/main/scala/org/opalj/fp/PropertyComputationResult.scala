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
 * Encapsulates the '''final results''' of the computation of a set of properties.
 *
 * A [[MultiResult]] is only to be used if no further refinement is possible
 * or may happen. The framework will then invoke and deregister all
 * dependent computations (observers).
 */
case class MultiResult(
    properties: Traversable[(Entity, Property)]
)
        extends PropertyComputationResult

/**
 * Encapsulates the '''final results''' of the computation of a set of properties that
 * required no intermediate steps.
 *
 * An `ImmediateMultiResult` is only to be used if no further refinement is possible
 * or may happen. The framework will then invoke and deregister all
 * dependent computations (observers).
 */
case class ImmediateMultiResult(
    properties: Traversable[(Entity, Property)]
)
        extends PropertyComputationResult

/**
 * Encapsulates the '''final result''' of the computation of the property.
 *
 * A [[Result]] is only to be used if no further refinement is possible
 * or may happen. The framework will then invoke and deregister all
 * dependent computations (observers).
 */
case class Result(e: Entity, p: Property) extends PropertyComputationResult

private[fp] case class FallbackResult(e: Entity, p: Property) extends PropertyComputationResult

/**
 * Encapsulates the '''final result''' of a computation of a property that '''required
 * no intermediate results'''.
 *
 * An `ImmediateResult` is only to be used if no further refinement is possible
 * or may happen. The framework will then invoke and deregister all
 * dependent computations (observers).
 */
case class ImmediateResult(e: Entity, p: Property) extends PropertyComputationResult

/**
 * Factory for [[Result]] and [[ImmediateResult]] objects.
 */
object Result {

    def apply(e: Entity, p: Property, immediate: Boolean): PropertyComputationResult = {
        if (immediate)
            new ImmediateResult(e, p)
        else
            new Result(e, p)
    }
}

/**
 * Encapsulates an intermediate result of the computation of a property.
 *
 * Intermediate results are to be used if further refinements are possible and may happen.
 *
 * All current computations (incoming dependencies)
 * depending on the given entry's property remain registered and will be invoked in the future
 * if another `IntermediateResult` or `Result` is computed for the specified entity `e`.
 *
 * Furthermore, if a property of any of the dependees changes (outgoing dependencies),
 * the given continuation `c` is invoked.
 * (This requires that the given continuation is thread-safe! In most cases the easiest
 * and correct solution is to just wrap it in a synchronized block.)
 */
case class IntermediateResult(
    e: Entity, p: Property,
    dependeeEs: Traversable[EOptionP],
    c:          Continuation
)
        extends PropertyComputationResult

/**
 * Represents a suspended computation.
 *
 * @param dependeeE The entity about which some knowledge is required by this
 *      computation before the computation can be continued.
 * @param dependeePK The property kind of the given entity about which some knowledge
 *      is required.
 */
private[fp] abstract class Suspended(
    val e:          Entity,
    val pk:         PropertyKey,
    val dependeeE:  Entity,
    val dependeePk: PropertyKey
)
        extends PropertyComputationResult {

    /**
     * Called by the framework if the property of the element this computation is
     * depending on was computed.
     */
    def continue(
        dependeeE: Entity,
        dependeeP: Property
    ): PropertyComputationResult

}

private[fp] object Suspended {

    def unapply(c: Suspended): Some[(Entity, PropertyKey, Entity, PropertyKey)] =
        Some((c.e, c.pk, c.dependeeE, c.dependeePk))

}
