/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.Entity
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait StaticDataUsagePropertyMetaInformation extends PropertyMetaInformation {

    final type Self = StaticDataUsage

}

/**
 * Describes whether a [[org.opalj.br.DeclaredMethod]] uses static state and whether the used static
 * state is compile-time constant.
 *
 * @author Dominik Helm
 */
sealed abstract class StaticDataUsage
    extends OrderedProperty
    with IndividualProperty[StaticDataUsage, VirtualMethodStaticDataUsage]
    with StaticDataUsagePropertyMetaInformation {

    /**
     * The globally unique key of the [[StaticDataUsage]] property.
     */
    final def key: PropertyKey[StaticDataUsage] = StaticDataUsage.key

    final val aggregatedProperty = new VirtualMethodStaticDataUsage(this)
}

object StaticDataUsage extends StaticDataUsagePropertyMetaInformation {
    /**
     * The key associated with every static data usage property. The name is
     * "StaticDataUsage"; the fallback is "UsesVaryingData".
     */
    final val key = PropertyKey.create[DeclaredMethod, StaticDataUsage](
        "StaticDataUsage",
        UsesVaryingData
    )
}

/**
 * The method does not use static data that may change during one or between several program runs.
 */
trait NoVaryingDataUse extends StaticDataUsage

/**
 * The method does not use any static data.
 */
case object UsesNoStaticData extends NoVaryingDataUse {

    override def checkIsEqualOrBetterThan(e: Entity, other: StaticDataUsage): Unit = {}

    override def meet(other: StaticDataUsage): StaticDataUsage = other
}

/**
 * The method uses static data that is compile-time constant only.
 */
case object UsesConstantDataOnly extends NoVaryingDataUse {

    override def checkIsEqualOrBetterThan(e: Entity, other: StaticDataUsage): Unit = {
        if (other eq UsesNoStaticData)
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
    }

    override def meet(other: StaticDataUsage): StaticDataUsage = other match {
        case UsesVaryingData => other
        case _               => this
    }
}

/**
 * The method uses static data that may change during one or between several program executions.
 */
case object UsesVaryingData extends StaticDataUsage {

    override def checkIsEqualOrBetterThan(e: Entity, other: StaticDataUsage): Unit = {
        if (other ne UsesVaryingData)
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
    }

    override def meet(other: StaticDataUsage): StaticDataUsage = this
}
