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
package tac

import org.opalj.collection.immutable.IntSet
import org.opalj.br.ComputationalType
import org.opalj.br.ComputationalTypeReturnAddress
import org.opalj.ai.ValueOrigin

/**
 * Identifies a variable which has a single static definition/initialization site.
 */
abstract class DUVar[+Value <: org.opalj.ai.ValuesDomain#DomainValue] extends Var[DUVar[Value]] {

    def value: Value

    final def cTpe: ComputationalType = value.computationalType

}

/**
 * Identifies the single index(pc) of the instruction which initialized
 * the variable. I.e., per method there must be at most one D variable which
 * has the given origin. Initially, the pc of the underlying bytecode instruction is used.
 *
 * @param value The value information; the value may be "illegal" if the value is not used
 * at all and an optimization is performed that identifies unused code and marks the
 * corresponding values as such. In that case, the value also has no
 * usages!
 *
 */
class DVar[+Value <: org.opalj.ai.ValuesDomain#DomainValue] private (
        private[tac] var origin:   ValueOrigin,
        val value:                 Value,
        private[tac] var useSites: IntSet
) extends DUVar[Value] {

    def definedBy: ValueOrigin = origin

    def usedBy: IntSet = useSites

    def name: String = {
        if (origin < 0)
            s"param${(-origin - 1).toHexString}/*:$value*/"
        else
            s"lv${origin.toHexString}/*:$value*/"
    }

    final def isSideEffectFree: Boolean = true

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        if (origin > 0 /* <=> it is not a parameter*/ ) origin = pcToIndex(origin)
        useSites = useSites.map(pcToIndex.apply) // use site are always positive...
    }

    override def toString: String = s"DVar(useSites=$useSites,value=$value,origin=$origin)"

}

object DVar {

    def apply(
        d: org.opalj.ai.ValuesDomain
    )(
        origin: ValueOrigin, value: d.DomainValue, useSites: IntSet
    ): DVar[d.DomainValue] = {

        assert(useSites != null, s"no uses (null) for $origin: $value")
        assert(value != null)
        assert(
            value == d.TheIllegalValue || value.computationalType != ComputationalTypeReturnAddress,
            s"value has unexpected computational type: $value"
        )

        new DVar[d.DomainValue](origin, value, useSites)
    }

    def unapply[Value <: org.opalj.ai.ValuesDomain#DomainValue](
        d: DVar[Value]
    ): Some[(Value, IntSet)] = {
        Some((d.value, d.useSites))
    }

}

class UVar[+Value <: org.opalj.ai.ValuesDomain#DomainValue] private (
        val value:                 Value,
        private[tac] var defSites: IntSet
) extends DUVar[Value] {

    def name: String = {
        defSites.iterator.map { defSite ⇒
            if (defSite < 0)
                s"param${(-defSite - 1).toHexString}/*:$value*/"
            else
                "lv"+defSite.toHexString
        }.mkString("{", ", ", s"}/*:$value*/")
    }

    def definedBy: IntSet = defSites

    final def isSideEffectFree: Boolean = true

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        defSites = defSites.map { defSite ⇒
            if (defSite >= 0) pcToIndex(defSite) else defSite /* <= it is referecing a parameter */
        }
    }

    override def toString: String = s"UVar(defSites=$defSites,value=$value)"

}

object UVar {

    def apply(
        d: org.opalj.ai.ValuesDomain
    )(
        value: d.DomainValue, useSites: IntSet
    ): UVar[d.DomainValue] = {
        new UVar[d.DomainValue](value, useSites)
    }

    def unapply[Value <: org.opalj.ai.ValuesDomain#DomainValue](
        u: UVar[Value]
    ): Some[(Value, IntSet)] = {
        Some((u.value, u.defSites))
    }

}
