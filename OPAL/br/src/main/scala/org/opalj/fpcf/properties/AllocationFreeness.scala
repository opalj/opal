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
package org.opalj
package fpcf
package properties

import org.opalj.br.DeclaredMethod
import org.opalj.fpcf.properties.VirtualMethodAllocationFreeness.VAllocationFreeMethod
import org.opalj.fpcf.properties.VirtualMethodAllocationFreeness.VMethodWithAllocations

sealed trait AllocationFreenessPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = AllocationFreeness

}

/**
 * Describes whether a [[DeclaredMethod]] or its (transitive) callees may allocate any
 * objects/arrays.
 *
 * @author Dominik Helm
 */
sealed abstract class AllocationFreeness
    extends OrderedProperty
    with IndividualProperty[AllocationFreeness, VirtualMethodAllocationFreeness]
    with AllocationFreenessPropertyMetaInformation {

    /**
     * The globally unique key of the [[AllocationFreeness]] property.
     */
    final def key: PropertyKey[AllocationFreeness] = AllocationFreeness.key

}

object AllocationFreeness extends AllocationFreenessPropertyMetaInformation {
    /**
     * The key associated with every allocation freeness property. The name is "AllocationFreeness";
     * the fallback is "MethodWithAllocations".
     */
    final val key = PropertyKey.create[DeclaredMethod, AllocationFreeness](
        "AllocationFreeness",
        MethodWithAllocations
    )
}

/**
 * The method does not allocate new objects/arrays and neither does any of its (transitive) callees.
 */
case object AllocationFreeMethod extends AllocationFreeness {

    override def checkIsEqualOrBetterThan(other: AllocationFreeness): Unit = {}

    override val aggregatedProperty: VirtualMethodAllocationFreeness = VAllocationFreeMethod

    override def meet(other: AllocationFreeness): AllocationFreeness = other
}

/**
 * The method or any of its (transitive) callees may allocate new objects/arrays.
 */
case object MethodWithAllocations extends AllocationFreeness {

    override def checkIsEqualOrBetterThan(other: AllocationFreeness): Unit = {
        if (other ne MethodWithAllocations)
            throw new IllegalArgumentException(s"impossible refinement: $other ⇒ $this")
    }

    override val aggregatedProperty: VirtualMethodAllocationFreeness = VMethodWithAllocations

    override def meet(other: AllocationFreeness): AllocationFreeness = this
}