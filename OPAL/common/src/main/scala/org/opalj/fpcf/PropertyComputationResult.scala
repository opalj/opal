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

/**
 * Computing a property for the respective element is not possible or did not
 * result in a new result.
 */
case object NoResult extends PropertyComputationResult {
    private[fpcf] final val id = 0
}

/**
 * Encapsulates the '''final results''' of the computation of a set of properties.
 *
 * A [[MultiResult]] is only to be used if no further refinement is possible
 * or may happen. The framework will then invoke and deregister all
 * dependent computations (observers).
 */
case class MultiResult(properties: ComputationResults) extends PropertyComputationResult {
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
case class ImmediateMultiResult(properties: ComputationResults) extends PropertyComputationResult {
    private[fpcf] final val id = ImmediateMultiResult.id
}
private[fpcf] object ImmediateMultiResult { private[fpcf] final val id = 2 }

/**
 * Encapsulates the '''final result''' of the computation of the property.
 *
 * A [[Result]] is only to be used if no further refinement is possible
 * or may happen. The framework will then invoke and deregister all
 * dependent computations (observers).
 */
case class Result(e: Entity, p: Property) extends PropertyComputationResult {
    private[fpcf] final def id = Result.id
}

/**
 * Encapsulates the '''final result''' of a computation of a property that '''required
 * no intermediate results'''; i.e., if no properties of other entities were queried.
 *
 * An `ImmediateResult` is only to be used if no further refinement is possible
 * or may happen. The framework will then invoke and deregister all
 * dependent computations (observers).
 */
case class ImmediateResult(e: Entity, p: Property) extends PropertyComputationResult {
    private[fpcf] final val id = ImmediateResult.id
}
private[fpcf] object ImmediateResult { private[fpcf] final val id = 4 }

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

    private[fpcf] final val id = 3
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
        e:          Entity,
        p:          Property,
        dependeeEs: Traversable[EOptionP],
        c:          Continuation
) extends PropertyComputationResult {
    private[fpcf] final def id = IntermediateResult.id
}
private[fpcf] object IntermediateResult { private[fpcf] final val id = 5 }

//
//
// PACKAGE PRIVATE (INTERNALLY USED) PropertyComputationResult OBJECTS
//
//

private[fpcf] case class FallbackResult(
        e: Entity,
        p: Property
) extends PropertyComputationResult {

    private[fpcf] final def id = FallbackResult.id

}
private[fpcf] object FallbackResult { private[fpcf] final val id = 6 }

/**
 * Represents a suspended computation.
 *
 * @param dependeeE The entity about which some knowledge is required by this
 *      computation before the computation can be continued.
 * @param dependeePK The property kind of the given entity about which some knowledge
 *      is required.
 */
private[fpcf] abstract class SuspendedPC(
        val e:          Entity,
        val pk:         PropertyKey,
        val dependeeE:  Entity,
        val dependeePK: PropertyKey
) extends PropertyComputationResult {

    /**
     * Called by the framework when the property of the element `dependeeE` on which
     * this computation is depending on was computed.
     */
    def continue(dependeeP: Property): PropertyComputationResult

    private[fpcf] final def id = SuspendedPC.id
}

/**
 * Factory for creating [[Suspended]] computations.
 */
private[fpcf] object SuspendedPC {

    private[fpcf] final val id = 7

    def unapply(c: SuspendedPC): Some[(Entity, PropertyKey, Entity, PropertyKey)] =
        Some((c.e, c.pk, c.dependeeE, c.dependeePK))

}

/**
 * Represents a suspended '''incremental''' computation.
 *
 * @param dependeeE The entity about which some knowledge is required by this
 *      computation before the computation can be continued.
 * @param dependeePK The property kind of the given entity about which some knowledge
 *      is required.
 */
private[fpcf] abstract class SuspendedIPC(
        val e:          Entity,
        val pk:         PropertyKey,
        val dependeeE:  Entity,
        val dependeePK: PropertyKey
) extends PropertyComputationResult {

    /**
     * Called by the framework when the property of the element `dependeeE` on which
     * this computation is depending on was computed.
     */
    def continue(dependeeP: Property): IncrementalPropertyComputationResult

    private[fpcf] final def id = SuspendedIPC.id
}

/**
 * Factory for creating [[Suspended]] computations.
 */
private[fpcf] object SuspendedIPC {

    private[fpcf] final val id = 8

    def unapply(c: SuspendedIPC): Some[(Entity, PropertyKey, Entity, PropertyKey)] =
        Some((c.e, c.pk, c.dependeeE, c.dependeePK))

}

