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

import org.opalj.fpcf.PropertyKey.SomeEPKs

sealed trait VirtualMethodPurityPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = VirtualMethodPurity

}

/**
 * Describes the aggregated purity for a virtual method.
 *
 * @author Dominik Helm
 */
sealed case class VirtualMethodPurity(
        purity: Purity
) extends Property with VirtualMethodPurityPropertyMetaInformation {

    /**
     * The globally unique key of the [[VirtualMethodPurity]] property.
     */
    final def key: PropertyKey[VirtualMethodPurity] = VirtualMethodPurity.key

    final val isRefinable = purity.isRefinable

    def meet(other: VirtualMethodPurity): VirtualMethodPurity =
        VirtualMethodPurity(purity combine other.purity)

    override def toString: String = s"VirtualMethodPurity($purity)"
}

object VirtualMethodPurity extends VirtualMethodPurityPropertyMetaInformation {

    def baseCycleResolutionStrategy(
        propertyStore: PropertyStore,
        epks:          SomeEPKs
    ): Iterable[Result] = {
        // When we have a cycle we can leverage the "purity" - conceptually (unless we
        // we have a programming bug) all properties (also those belonging to other
        // lattice) model conditional properties under the assumption that we have
        // at least the current properties.
        val e = epks.head.e
        val p = propertyStore(e, key).p
        assert(p.purity.isConditional) // a cycle must not contain a non-conditional property
        // NOTE
        // We DO NOT increase the purity of all methods as this will happen automatically as a
        // sideeffect of setting the purity of one method!
        Iterable(Result(e, VirtualMethodPurity(propertyStore(e, key).p.purity.unconditional)))
    }

    def apply(purity: Purity): VirtualMethodPurity = purity match {
        case PureWithoutAllocations              ⇒ VPureWithoutAllocations
        case CPureWithoutAllocations             ⇒ VCPureWithoutAllocations
        case LBSideEffectFreeWithoutAllocations  ⇒ VLBSideEffectFreeWithoutAllocations
        case CLBSideEffectFreeWithoutAllocations ⇒ VCLBSideEffectFreeWithoutAllocations
        case LBPure                              ⇒ VLBPure
        case CLBPure                             ⇒ VCLBPure
        case LBSideEffectFree                    ⇒ VLBSideEffectFree
        case CLBSideEffectFree                   ⇒ VCLBSideEffectFree
        case LBExternallyPure                    ⇒ VLBExternallyPure
        case CLBExternallyPure                   ⇒ VCLBExternallyPure
        case LBExternallySideEffectFree          ⇒ VLBExternallySideEffectFree
        case CLBExternallySideEffectFree         ⇒ VCLBExternallySideEffectFree
        case LBDPure                             ⇒ VLBDPure
        case CLBDPure                            ⇒ VCLBDPure
        case LBDSideEffectFree                   ⇒ VLBDSideEffectFree
        case CLBDSideEffectFree                  ⇒ VCLBDSideEffectFree
        case LBDExternallyPure                   ⇒ VLBDExternallyPure
        case CLBDExternallyPure                  ⇒ VCLBDExternallyPure
        case LBDExternallySideEffectFree         ⇒ VLBDExternallySideEffectFree
        case CLBDExternallySideEffectFree        ⇒ VCLBDExternallySideEffectFree
        case MaybePure                           ⇒ VMaybePure
        case LBImpure                            ⇒ VLBImpure
        case Impure                              ⇒ VImpure
    }

    final val VPureWithoutAllocations = new VirtualMethodPurity(PureWithoutAllocations)
    final val VCPureWithoutAllocations = new VirtualMethodPurity(CPureWithoutAllocations)
    final val VLBSideEffectFreeWithoutAllocations =
        new VirtualMethodPurity(LBSideEffectFreeWithoutAllocations)
    final val VCLBSideEffectFreeWithoutAllocations =
        new VirtualMethodPurity(CLBSideEffectFreeWithoutAllocations)
    final val VLBPure = new VirtualMethodPurity(LBPure)
    final val VCLBPure = new VirtualMethodPurity(CLBPure)
    final val VLBSideEffectFree = new VirtualMethodPurity(LBSideEffectFree)
    final val VCLBSideEffectFree = new VirtualMethodPurity(CLBSideEffectFree)
    final val VLBExternallyPure = new VirtualMethodPurity(LBExternallyPure)
    final val VCLBExternallyPure = new VirtualMethodPurity(CLBExternallyPure)
    final val VLBExternallySideEffectFree = new VirtualMethodPurity(LBExternallySideEffectFree)
    final val VCLBExternallySideEffectFree = new VirtualMethodPurity(CLBExternallySideEffectFree)
    final val VLBDPure = new VirtualMethodPurity(LBDPure)
    final val VCLBDPure = new VirtualMethodPurity(CLBDPure)
    final val VLBDSideEffectFree = new VirtualMethodPurity(LBDSideEffectFree)
    final val VCLBDSideEffectFree = new VirtualMethodPurity(CLBDSideEffectFree)
    final val VLBDExternallyPure = new VirtualMethodPurity(LBDExternallyPure)
    final val VCLBDExternallyPure = new VirtualMethodPurity(CLBDExternallyPure)
    final val VLBDExternallySideEffectFree = new VirtualMethodPurity(LBDExternallySideEffectFree)
    final val VCLBDExternallySideEffectFree = new VirtualMethodPurity(CLBDExternallySideEffectFree)
    final val VMaybePure = new VirtualMethodPurity(MaybePure)
    final val VLBImpure = new VirtualMethodPurity(LBImpure)
    final val VImpure = new VirtualMethodPurity(Impure)

    /**
     * The key associated with every purity property. The name is "VirtualMethodPurity";
     * the fallback is "VImpure".
     */
    final val key = PropertyKey.create[VirtualMethodPurity](
        "VirtualMethodPurity",
        VImpure,
        baseCycleResolutionStrategy _
    )
}
