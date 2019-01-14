/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait VirtualMethodStaticDataUsagePropertyMetaInformation extends PropertyMetaInformation {

    final type Self = VirtualMethodStaticDataUsage

}

/**
 * Describes the aggregated allocation freeness for a virtual method.
 *
 * @author Dominik Helm
 */
sealed case class VirtualMethodStaticDataUsage(
        individualProperty: StaticDataUsage
) extends AggregatedProperty[StaticDataUsage, VirtualMethodStaticDataUsage]
    with VirtualMethodStaticDataUsagePropertyMetaInformation {

    /**
     * The globally unique key of the [[VirtualMethodStaticDataUsage]] property.
     */
    final def key: PropertyKey[VirtualMethodStaticDataUsage] =
        VirtualMethodStaticDataUsage.key

    override def toString: String = s"VirtualMethodStaticDataUsage($individualProperty)"
}

object VirtualMethodStaticDataUsage
    extends VirtualMethodStaticDataUsagePropertyMetaInformation {

    final val VUsesNoStaticData = UsesNoStaticData.aggregatedProperty
    final val VUsesConstantDataOnly = UsesConstantDataOnly.aggregatedProperty
    final val VUsesVaryingData = UsesVaryingData.aggregatedProperty

    /**
     * The key associated with every virtual method allocation freeness property. The name is
     * "VirtualMethodStaticDataUsage"; the fallback is "VUsesVaryingData".
     */
    final val key = PropertyKey.create[DeclaredMethod, VirtualMethodStaticDataUsage](
        "VirtualMethodStaticDataUsage",
        VUsesVaryingData
    )
}
