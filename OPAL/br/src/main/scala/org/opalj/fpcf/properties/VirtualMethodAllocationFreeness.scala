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

sealed trait VirtualMethodAllocationFreenessPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = VirtualMethodAllocationFreeness

}

/**
 * Describes the aggregated allocation freeness for a virtual method.
 *
 * @author Dominik Helm
 */
sealed case class VirtualMethodAllocationFreeness(
        individualProperty: AllocationFreeness
) extends AggregatedProperty[AllocationFreeness, VirtualMethodAllocationFreeness]
    with VirtualMethodAllocationFreenessPropertyMetaInformation {

    /**
     * The globally unique key of the [[VirtualMethodAllocationFreeness]] property.
     */
    final def key: PropertyKey[VirtualMethodAllocationFreeness] =
        VirtualMethodAllocationFreeness.key

    override def toString: String = s"VirtualMethodAllocationFreeness($individualProperty)"
}

object VirtualMethodAllocationFreeness
    extends VirtualMethodAllocationFreenessPropertyMetaInformation {

    final val VAllocationFreeMethod = AllocationFreeMethod.aggregatedProperty
    final val VMethodWithAllocations = MethodWithAllocations.aggregatedProperty

    /**
     * The key associated with every virtual method allocation freeness property. The name is
     * "VirtualMethodAllocationFreeness"; the fallback is "VMethodWithAllocations".
     */
    final val key = PropertyKey.create[DeclaredMethod, VirtualMethodAllocationFreeness](
        "VirtualMethodAllocationFreeness",
        VMethodWithAllocations
    )
}
