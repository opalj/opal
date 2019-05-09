/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait VirtualMethodAllocationFreenessPropertyMetaInformation
    extends PropertyMetaInformation {

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
