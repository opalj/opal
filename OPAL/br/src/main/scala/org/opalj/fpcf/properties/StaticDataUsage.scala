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
import org.opalj.fpcf.properties.VirtualMethodStaticDataUsage.VUsesNoStaticData
import org.opalj.fpcf.properties.VirtualMethodStaticDataUsage.VUsesVaryingData
import org.opalj.fpcf.properties.VirtualMethodStaticDataUsage.VUsesConstantDataOnly

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

}

object StaticDataUsage extends StaticDataUsagePropertyMetaInformation {
    /**
     * The key associated with every static constant usage property. The name is
     * "StaticConstantUsage"; the fallback is "UsesVaryingData".
     */
    final val key = PropertyKey.create[DeclaredMethod, StaticDataUsage](
        "StaticConstantUsage",
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

    override lazy val aggregatedProperty: VirtualMethodStaticDataUsage = VUsesNoStaticData

    override def meet(other: StaticDataUsage): StaticDataUsage = other
}

/**
 * The method uses static data that is compile-time constant only.
 */
case object UsesConstantDataOnly extends NoVaryingDataUse {

    override def checkIsEqualOrBetterThan(e: Entity, other: StaticDataUsage): Unit = {
        if (other eq UsesNoStaticData)
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this")
    }

    override lazy val aggregatedProperty: VirtualMethodStaticDataUsage = VUsesConstantDataOnly

    override def meet(other: StaticDataUsage): StaticDataUsage = other match {
        case UsesVaryingData ⇒ other
        case _               ⇒ this
    }
}

/**
 * The method uses static data that may change during one or between several program executions.
 */
case object UsesVaryingData extends StaticDataUsage {

    override def checkIsEqualOrBetterThan(e: Entity, other: StaticDataUsage): Unit = {
        if (other ne UsesVaryingData)
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this")
    }

    override lazy val aggregatedProperty: VirtualMethodStaticDataUsage = VUsesVaryingData

    override def meet(other: StaticDataUsage): StaticDataUsage = this
}
