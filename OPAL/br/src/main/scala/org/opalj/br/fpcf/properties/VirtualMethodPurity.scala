/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait VirtualMethodPurityPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = VirtualMethodPurity

}

/**
 * Describes the aggregated purity for a virtual method.
 *
 * @author Dominik Helm
 */
sealed case class VirtualMethodPurity(
        individualProperty: Purity
) extends AggregatedProperty[Purity, VirtualMethodPurity]
    with VirtualMethodPurityPropertyMetaInformation {

    /**
     * The globally unique key of the [[VirtualMethodPurity]] property.
     */
    final def key: PropertyKey[VirtualMethodPurity] = VirtualMethodPurity.key

    override def toString: String = s"VirtualMethodPurity($individualProperty)"
}

object VirtualMethodPurity extends VirtualMethodPurityPropertyMetaInformation {

    def apply(name: String): Option[VirtualMethodPurity] =
        if (name.charAt(0) == 'V') Purity(name.substring(1)).map(_.aggregatedProperty) else None

    final val VCompileTimePure = CompileTimePure.aggregatedProperty
    final val VPure = Pure.aggregatedProperty
    final val VSideEffectFree = SideEffectFree.aggregatedProperty
    final val VDPure = DPure.aggregatedProperty
    final val VDSideEffectFree = DSideEffectFree.aggregatedProperty
    final val VImpureByAnalysis = ImpureByAnalysis.aggregatedProperty
    final val VImpureByLackOfInformation = ImpureByLackOfInformation.aggregatedProperty

    /**
     * The key associated with every purity property. The name is "VirtualMethodPurity";
     * the fallback is "VImpure".
     */
    final val key = PropertyKey.create[DeclaredMethod, VirtualMethodPurity](
        "VirtualMethodPurity",
        VImpureByLackOfInformation
    )
}
