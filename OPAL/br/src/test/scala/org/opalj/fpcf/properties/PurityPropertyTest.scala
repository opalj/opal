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

import org.scalatest.Matchers
import org.scalatest.FlatSpec

/**
 * Tests the [[Purity]] property, especially correctness of the combine operator.
 *
 * @author Dominik Helm
 */
class PurityPropertyTest extends FlatSpec with Matchers {

    val allPurities: List[Purity] = List(
        PureWithoutAllocations, LBSideEffectFreeWithoutAllocations,
        LBPure, LBSideEffectFree,
        LBExternallyPure, LBExternallySideEffectFree, LBDPure,
        LBDSideEffectFree, LBDExternallyPure,
        LBDExternallySideEffectFree,
        CPureWithoutAllocations,
        CLBSideEffectFreeWithoutAllocations, CLBPure, CLBSideEffectFree, CLBExternallyPure,
        CLBExternallySideEffectFree, CLBDPure, CLBDSideEffectFree,
        CLBDExternallyPure, CLBDExternallySideEffectFree,
        MaybePure, LBImpure, Impure
    )

    val doesntModifyReceiver: Set[Purity] = Set(
        PureWithoutAllocations, LBSideEffectFreeWithoutAllocations, LBPure, LBSideEffectFree,
        LBDPure, LBDSideEffectFree, CPureWithoutAllocations,
        CLBSideEffectFreeWithoutAllocations, CLBPure, CLBSideEffectFree, CLBDPure,
        CLBDSideEffectFree
    )

    val conditional: Set[Purity] = Set(
        CPureWithoutAllocations, CLBSideEffectFreeWithoutAllocations, CLBPure, CLBSideEffectFree,
        CLBExternallyPure, CLBExternallySideEffectFree, CLBDPure,
        CLBDSideEffectFree, CLBDExternallyPure,
        CLBDExternallySideEffectFree,
        MaybePure
    )

    "purity levels" should "have the right properties" in {
        val withoutAllocations: Set[Purity] = Set(
            PureWithoutAllocations, LBSideEffectFreeWithoutAllocations, CPureWithoutAllocations,
            CLBSideEffectFreeWithoutAllocations
        )

        for (prop ← allPurities) {
            assert(
                prop.hasAllocations != withoutAllocations.contains(prop),
                s"$prop.hasAllocations was ${prop.hasAllocations}"
            )
        }

        val deterministic: Set[Purity] = Set(
            PureWithoutAllocations, LBPure, LBExternallyPure, LBDPure,
            LBDExternallyPure, CPureWithoutAllocations, CLBPure, CLBExternallyPure,
            CLBDPure, CLBDExternallyPure
        )

        for (prop ← allPurities) {
            assert(
                prop.isDeterministic == deterministic.contains(prop),
                s"$prop.isDeterministic was ${prop.isDeterministic}"
            )
        }

        for (prop ← allPurities) {
            assert(
                prop.modifiesReceiver != doesntModifyReceiver.contains(prop),
                s"$prop.modifiesReceiver was ${prop.modifiesReceiver}"
            )
        }

        val doesntUseDomainSpecificActions: Set[Purity] = Set(
            PureWithoutAllocations, LBSideEffectFreeWithoutAllocations, LBPure, LBSideEffectFree,
            LBExternallyPure, LBExternallySideEffectFree, CPureWithoutAllocations,
            CLBSideEffectFreeWithoutAllocations, CLBPure, CLBSideEffectFree, CLBExternallyPure,
            CLBExternallySideEffectFree
        )

        for (prop ← allPurities) {
            assert(
                prop.usesDomainSpecificActions != doesntUseDomainSpecificActions.contains(prop),
                s"$prop.usesDomainSpecicifActions was ${prop.usesDomainSpecificActions}"
            )
        }

        for (prop ← allPurities) {
            assert(
                prop.isConditional == conditional.contains(prop),
                s"$prop.isConditional was ${prop.isConditional}"
            )
        }
    }

    they should "be converted correctly" in {
        for { prop ← allPurities } {
            if (doesntModifyReceiver.contains(prop))
                assert(
                    prop.withoutExternal == prop,
                    s"$prop.withoutExternal modified $prop (was ${prop.withoutExternal})"
                )
            else
                assert(
                    prop.withoutExternal.flags == (prop.flags & ~Purity.ModifiesReceiver),
                    s"$prop.withoutExternal was incorrect (was ${prop.withoutExternal})"
                )
            if (conditional.contains(prop))
                assert(
                    prop.unconditional.flags == (prop.flags & ~Purity.IsConditional),
                    s"$prop.unconditional was incorrect (was ${prop.unconditional})"
                )
            else
                assert(
                    prop.unconditional == prop,
                    s"$prop.unconditional modified $prop (was ${prop.unconditional})"
                )
        }
    }

    "the combine operator" should "be reflexive and symmetric" in {
        for (prop1 ← allPurities) {
            assert((prop1 combine prop1) == prop1, s"combine was not reflexive for $prop1")
            for (prop2 ← allPurities) {
                val combine12 = prop1 combine prop2
                val combine21 = prop2 combine prop1
                assert(
                    combine12 == combine21,
                    s"$prop1 combine $prop2 was not symmetric (was $combine12 / $combine21)"
                )
            }
        }
    }

    it should "return the correct purity levels for unconditional levels" in {
        for (prop ← allPurities) {
            val result = PureWithoutAllocations combine prop
            assert(
                result == prop,
                s"PureWithoutAllocations combine $prop was not $prop (was $result)"
            )
        }

        assert((LBSideEffectFreeWithoutAllocations combine LBPure) == LBSideEffectFree)
        assert((LBSideEffectFreeWithoutAllocations combine LBSideEffectFree) == LBSideEffectFree)
        assert((LBSideEffectFreeWithoutAllocations combine LBExternallyPure) == LBExternallySideEffectFree)
        assert((LBSideEffectFreeWithoutAllocations combine LBExternallySideEffectFree) == LBExternallySideEffectFree)
        assert((LBSideEffectFreeWithoutAllocations combine LBDPure) == LBDSideEffectFree)
        assert((LBSideEffectFreeWithoutAllocations combine LBDSideEffectFree) == LBDSideEffectFree)
        assert((LBSideEffectFreeWithoutAllocations combine LBDExternallyPure) == LBDExternallySideEffectFree)
        assert((LBSideEffectFreeWithoutAllocations combine LBDExternallySideEffectFree) == LBDExternallySideEffectFree)
        assert((LBSideEffectFreeWithoutAllocations combine CPureWithoutAllocations) == CLBSideEffectFreeWithoutAllocations)
        assert((LBSideEffectFreeWithoutAllocations combine CLBSideEffectFreeWithoutAllocations) == CLBSideEffectFreeWithoutAllocations)
        assert((LBSideEffectFreeWithoutAllocations combine CLBPure) == CLBSideEffectFree)
        assert((LBSideEffectFreeWithoutAllocations combine CLBSideEffectFree) == CLBSideEffectFree)
        assert((LBSideEffectFreeWithoutAllocations combine CLBExternallyPure) == CLBExternallySideEffectFree)
        assert((LBSideEffectFreeWithoutAllocations combine CLBExternallySideEffectFree) == CLBExternallySideEffectFree)
        assert((LBSideEffectFreeWithoutAllocations combine CLBDPure) == CLBDSideEffectFree)
        assert((LBSideEffectFreeWithoutAllocations combine CLBDSideEffectFree) == CLBDSideEffectFree)
        assert((LBSideEffectFreeWithoutAllocations combine CLBDExternallyPure) == CLBDExternallySideEffectFree)
        assert((LBSideEffectFreeWithoutAllocations combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree)
        assert((LBSideEffectFreeWithoutAllocations combine MaybePure) == MaybePure)
        assert((LBSideEffectFreeWithoutAllocations combine LBImpure) == LBImpure)

        assert((LBPure combine LBSideEffectFree) == LBSideEffectFree)
        assert((LBPure combine LBExternallyPure) == LBExternallyPure)
        assert((LBPure combine LBExternallySideEffectFree) == LBExternallySideEffectFree)
        assert((LBPure combine LBDPure) == LBDPure)
        assert((LBPure combine LBDSideEffectFree) == LBDSideEffectFree)
        assert((LBPure combine LBDExternallyPure) == LBDExternallyPure)
        assert((LBPure combine LBDExternallySideEffectFree) == LBDExternallySideEffectFree)
        assert((LBPure combine CPureWithoutAllocations) == CLBPure)
        assert((LBPure combine CLBSideEffectFreeWithoutAllocations) == CLBSideEffectFree)
        assert((LBPure combine CLBPure) == CLBPure)
        assert((LBPure combine CLBSideEffectFree) == CLBSideEffectFree)
        assert((LBPure combine CLBExternallyPure) == CLBExternallyPure)
        assert((LBPure combine CLBExternallySideEffectFree) == CLBExternallySideEffectFree)
        assert((LBPure combine CLBDPure) == CLBDPure)
        assert((LBPure combine CLBDSideEffectFree) == CLBDSideEffectFree)
        assert((LBPure combine CLBDExternallyPure) == CLBDExternallyPure)
        assert((LBPure combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree)
        assert((LBPure combine MaybePure) == MaybePure)
        assert((LBPure combine LBImpure) == LBImpure)

        assert((LBSideEffectFree combine LBExternallyPure) == LBExternallySideEffectFree)
        assert((LBSideEffectFree combine LBExternallySideEffectFree) == LBExternallySideEffectFree)
        assert((LBSideEffectFree combine LBDPure) == LBDSideEffectFree)
        assert((LBSideEffectFree combine LBDSideEffectFree) == LBDSideEffectFree)
        assert((LBSideEffectFree combine LBDExternallyPure) == LBDExternallySideEffectFree)
        assert((LBSideEffectFree combine LBDExternallySideEffectFree) == LBDExternallySideEffectFree)
        assert((LBSideEffectFree combine CPureWithoutAllocations) == CLBSideEffectFree)
        assert((LBSideEffectFree combine CLBSideEffectFreeWithoutAllocations) == CLBSideEffectFree)
        assert((LBSideEffectFree combine CLBPure) == CLBSideEffectFree)
        assert((LBSideEffectFree combine CLBSideEffectFree) == CLBSideEffectFree)
        assert((LBSideEffectFree combine CLBExternallyPure) == CLBExternallySideEffectFree)
        assert((LBSideEffectFree combine CLBExternallySideEffectFree) == CLBExternallySideEffectFree)
        assert((LBSideEffectFree combine CLBDPure) == CLBDSideEffectFree)
        assert((LBSideEffectFree combine CLBDSideEffectFree) == CLBDSideEffectFree)
        assert((LBSideEffectFree combine CLBDExternallyPure) == CLBDExternallySideEffectFree)
        assert((LBSideEffectFree combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree)
        assert((LBSideEffectFree combine MaybePure) == MaybePure)
        assert((LBSideEffectFree combine LBImpure) == LBImpure)

        assert(
            (LBExternallyPure combine LBExternallySideEffectFree) == LBExternallySideEffectFree,
            "LBExternallyPure combine LBExternallySideEffectFree was not LBExternallySideEffectFree"+
                s" (was ${LBExternallyPure combine LBExternallySideEffectFree})"
        )
        assert(
            (LBExternallyPure combine LBDPure) == LBDExternallyPure,
            "LBExternallyPure combine LBDPure was not LBDExternallyPure"+
                s" (was ${LBExternallyPure combine LBDPure})"
        )
        assert(
            (LBExternallyPure combine LBDSideEffectFree) == LBDExternallySideEffectFree,
            "LBExternallyPure combine LBDSideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${LBExternallyPure combine LBDSideEffectFree})"
        )
        assert(
            (LBExternallyPure combine LBDExternallyPure) == LBDExternallyPure,
            "LBExternallyPure combine LBDExternallyPure was not LBDExternallyPure"+
                s" (was ${LBExternallyPure combine LBDExternallyPure})"
        )
        assert(
            (LBExternallyPure combine LBDExternallySideEffectFree) == LBDExternallySideEffectFree,
            "LBExternallyPure combine LBDExternallySideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${LBExternallyPure combine LBDExternallySideEffectFree})"
        )
        assert(
            (LBExternallyPure combine CPureWithoutAllocations) == CLBExternallyPure,
            "LBExternallyPure combine CPureWithoutAllocations was not CLBExternallyPure"+
                s" (was ${LBExternallyPure combine CPureWithoutAllocations})"
        )
        assert(
            (LBExternallyPure combine CLBSideEffectFreeWithoutAllocations) == CLBExternallySideEffectFree,
            "LBExternallyPure combine CLBSideEffectFreeWithoutAllocations was not CLBExternallySideEffectFree"+
                s" (was ${LBExternallyPure combine CLBSideEffectFreeWithoutAllocations})"
        )
        assert(
            (LBExternallyPure combine CLBPure) == CLBExternallyPure,
            "LBExternallyPure combine CLBPure was not CLBExternallyPure"+
                s" (was ${LBExternallyPure combine CLBPure})"
        )
        assert(
            (LBExternallyPure combine CLBSideEffectFree) == CLBExternallySideEffectFree,
            "LBExternallyPure combine CLBSideEffectFree was not CLBExternallySideEffectFree"+
                s" (was ${LBExternallyPure combine CLBSideEffectFree})"
        )
        assert(
            (LBExternallyPure combine CLBExternallyPure) == CLBExternallyPure,
            "LBExternallyPure combine CLBExternallyPure was not CLBExternallyPure"+
                s" (was ${LBExternallyPure combine CLBExternallyPure})"
        )
        assert(
            (LBExternallyPure combine CLBExternallySideEffectFree) == CLBExternallySideEffectFree,
            "LBExternallyPure combine CLBExternallySideEffectFree was not CLBExternallySideEffectFree"+
                s" (was ${LBExternallyPure combine CLBExternallySideEffectFree})"
        )
        assert(
            (LBExternallyPure combine CLBDPure) == CLBDExternallyPure,
            "LBExternallyPure combine CLBDPure was not CLBDExternallyPure"+
                s" (was ${LBExternallyPure combine CLBDPure})"
        )
        assert(
            (LBExternallyPure combine CLBDSideEffectFree) == CLBDExternallySideEffectFree,
            "LBExternallyPure combine CLBDSideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${LBExternallyPure combine CLBDSideEffectFree})"
        )
        assert(
            (LBExternallyPure combine CLBDExternallyPure) == CLBDExternallyPure,
            "LBExternallyPure combine CLBDExternallyPure was not CLBDExternallyPure"+
                s" (was ${LBExternallyPure combine CLBDExternallyPure})"
        )
        assert(
            (LBExternallyPure combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "LBExternallyPure combine CLBDExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${LBExternallyPure combine CLBDExternallySideEffectFree})"
        )
        assert(
            (LBExternallyPure combine MaybePure) == MaybePure,
            "LBExternallyPure combine MaybePure was not MaybePure"+
                s" (was ${LBExternallyPure combine MaybePure})"
        )
        assert(
            (LBExternallyPure combine LBImpure) == LBImpure,
            "LBExternallyPure combine LBImpure was not LBImpure"+
                s" (was ${LBExternallyPure combine LBImpure})"
        )

        assert(
            (LBExternallySideEffectFree combine LBDPure) == LBDExternallySideEffectFree,
            "LBExternallySideEffectFree combine LBDPure was not LBDExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree combine LBDPure})"
        )
        assert(
            (LBExternallySideEffectFree combine LBDSideEffectFree) == LBDExternallySideEffectFree,
            "LBExternallySideEffectFree combine LBDSideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree combine LBDSideEffectFree})"
        )
        assert(
            (LBExternallySideEffectFree combine LBDExternallyPure) == LBDExternallySideEffectFree,
            "LBExternallySideEffectFree combine LBDExternallyPure was not LBDExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree combine LBDExternallyPure})"
        )
        assert(
            (LBExternallySideEffectFree combine LBDExternallySideEffectFree) == LBDExternallySideEffectFree,
            "LBExternallySideEffectFree combine LBDExternallySideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree combine LBDExternallySideEffectFree})"
        )
        assert(
            (LBExternallySideEffectFree combine CPureWithoutAllocations) == CLBExternallySideEffectFree,
            "LBExternallySideEffectFree combine CPureWithoutAllocations was not CLBExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree combine CPureWithoutAllocations})"
        )
        assert(
            (LBExternallySideEffectFree combine CLBSideEffectFreeWithoutAllocations) == CLBExternallySideEffectFree,
            "LBExternallySideEffectFree combine CLBSideEffectFreeWithoutAllocations was not CLBExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree combine CLBSideEffectFreeWithoutAllocations})"
        )
        assert(
            (LBExternallySideEffectFree combine CLBPure) == CLBExternallySideEffectFree,
            "LBExternallySideEffectFree combine CLBPure was not CLBExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree combine CLBPure})"
        )
        assert(
            (LBExternallySideEffectFree combine CLBSideEffectFree) == CLBExternallySideEffectFree,
            "LBExternallySideEffectFree combine CLBSideEffectFree was not CLBExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree combine CLBSideEffectFree})"
        )
        assert(
            (LBExternallySideEffectFree combine CLBExternallyPure) == CLBExternallySideEffectFree,
            "LBExternallySideEffectFree combine CLBExternallyPure was not CLBExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree combine CLBExternallyPure})"
        )
        assert(
            (LBExternallySideEffectFree combine CLBExternallySideEffectFree) == CLBExternallySideEffectFree,
            "LBExternallySideEffectFree combine CLBExternallySideEffectFree was not CLBExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree combine CLBExternallySideEffectFree})"
        )
        assert(
            (LBExternallySideEffectFree combine CLBDPure) == CLBDExternallySideEffectFree,
            "LBExternallySideEffectFree combine CLBDPure was not CLBDExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree combine CLBDPure})"
        )
        assert(
            (LBExternallySideEffectFree combine CLBDSideEffectFree) == CLBDExternallySideEffectFree,
            "LBExternallySideEffectFree combine CLBDSideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree combine CLBDSideEffectFree})"
        )
        assert(
            (LBExternallySideEffectFree combine CLBDExternallyPure) == CLBDExternallySideEffectFree,
            "LBExternallySideEffectFree combine CLBDExternallyPure was not CLBDExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree combine CLBDExternallyPure})"
        )
        assert(
            (LBExternallySideEffectFree combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "LBExternallySideEffectFree combine CLBDExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree combine CLBDExternallySideEffectFree})"
        )
        assert(
            (LBExternallySideEffectFree combine MaybePure) == MaybePure,
            "LBExternallySideEffectFree combine MaybePure was not MaybePure"+
                s" (was ${LBExternallySideEffectFree combine MaybePure})"
        )
        assert(
            (LBExternallySideEffectFree combine LBImpure) == LBImpure,
            "LBExternallySideEffectFree combine LBImpure was not LBImpure"+
                s" (was ${LBExternallySideEffectFree combine LBImpure})"
        )

        assert(
            (LBDPure combine LBDSideEffectFree) == LBDSideEffectFree,
            "LBDPure combine LBDSideEffectFree was not LBDSideEffectFree"+
                s" (was ${LBDPure combine LBDSideEffectFree})"
        )
        assert(
            (LBDPure combine LBDExternallyPure) == LBDExternallyPure,
            "LBDPure combine LBDExternallyPure was not LBDExternallyPure"+
                s" (was ${LBDPure combine LBDExternallyPure})"
        )
        assert(
            (LBDPure combine LBDExternallySideEffectFree) == LBDExternallySideEffectFree,
            "LBDPure combine LBDExternallySideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${LBDPure combine LBDExternallySideEffectFree})"
        )
        assert(
            (LBDPure combine CPureWithoutAllocations) == CLBDPure,
            "LBDPure combine CPureWithoutAllocations was not CLBDPure"+
                s" (was ${LBDPure combine CPureWithoutAllocations})"
        )
        assert(
            (LBDPure combine CLBSideEffectFreeWithoutAllocations) == CLBDSideEffectFree,
            "LBDPure combine CLBSideEffectFreeWithoutAllocations was not CLBDSideEffectFree"+
                s" (was ${LBDPure combine CLBSideEffectFreeWithoutAllocations})"
        )
        assert(
            (LBDPure combine CLBPure) == CLBDPure,
            "LBDPure combine CLBPure was not CLBDPure"+
                s" (was ${LBDPure combine CLBPure})"
        )
        assert(
            (LBDPure combine CLBSideEffectFree) == CLBDSideEffectFree,
            "LBDPure combine CLBSideEffectFree was not CLBDSideEffectFree"+
                s" (was ${LBDPure combine CLBSideEffectFree})"
        )
        assert(
            (LBDPure combine CLBExternallyPure) == CLBDExternallyPure,
            "LBDPure combine CLBExternallyPure was not CLBDExternallyPure"+
                s" (was ${LBDPure combine CLBExternallyPure})"
        )
        assert(
            (LBDPure combine CLBExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "LBDPure combine CLBExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${LBDPure combine CLBExternallySideEffectFree})"
        )
        assert(
            (LBDPure combine CLBDPure) == CLBDPure,
            "LBDPure combine CLBDPure was not CLBDPure"+
                s" (was ${LBDPure combine CLBDPure})"
        )
        assert(
            (LBDPure combine CLBDSideEffectFree) == CLBDSideEffectFree,
            "LBDPure combine CLBDSideEffectFree was not CLBDSideEffectFree"+
                s" (was ${LBDPure combine CLBDSideEffectFree})"
        )
        assert(
            (LBDPure combine CLBDExternallyPure) == CLBDExternallyPure,
            "LBDPure combine CLBDExternallyPure was not CLBDExternallyPure"+
                s" (was ${LBDPure combine CLBDExternallyPure})"
        )
        assert(
            (LBDPure combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "LBDPure combine CLBDExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${LBDPure combine CLBDExternallySideEffectFree})"
        )
        assert(
            (LBDPure combine MaybePure) == MaybePure,
            "LBDPure combine MaybePure was not MaybePure"+
                s" (was ${LBDPure combine MaybePure})"
        )
        assert(
            (LBDPure combine LBImpure) == LBImpure,
            "LBDPure combine LBImpure was not LBImpure"+
                s" (was ${LBDPure combine LBImpure})"
        )

        assert(
            (LBDSideEffectFree combine LBDExternallyPure) == LBDExternallySideEffectFree,
            "LBDSideEffectFree combine LBDExternallyPure was not LBDExternallySideEffectFree"+
                s" (was ${LBDSideEffectFree combine LBDExternallyPure})"
        )
        assert(
            (LBDSideEffectFree combine LBDExternallySideEffectFree) == LBDExternallySideEffectFree,
            "LBDSideEffectFree combine LBDExternallySideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${LBDSideEffectFree combine LBDExternallySideEffectFree})"
        )
        assert(
            (LBDSideEffectFree combine CPureWithoutAllocations) == CLBDSideEffectFree,
            "LBDSideEffectFree combine CPureWithoutAllocations was not CLBDSideEffectFree"+
                s" (was ${LBDSideEffectFree combine CPureWithoutAllocations})"
        )
        assert(
            (LBDSideEffectFree combine CLBSideEffectFreeWithoutAllocations) == CLBDSideEffectFree,
            "LBDSideEffectFree combine CLBSideEffectFreeWithoutAllocations was not CLBDSideEffectFree"+
                s" (was ${LBDSideEffectFree combine CLBSideEffectFreeWithoutAllocations})"
        )
        assert(
            (LBDSideEffectFree combine CLBPure) == CLBDSideEffectFree,
            "LBDSideEffectFree combine CLBPure was not CLBDSideEffectFree"+
                s" (was ${LBDSideEffectFree combine CLBPure})"
        )
        assert(
            (LBDSideEffectFree combine CLBSideEffectFree) == CLBDSideEffectFree,
            "LBDSideEffectFree combine CLBSideEffectFree was not CLBDSideEffectFree"+
                s" (was ${LBDSideEffectFree combine CLBSideEffectFree})"
        )
        assert(
            (LBDSideEffectFree combine CLBExternallyPure) == CLBDExternallySideEffectFree,
            "LBDSideEffectFree combine CLBExternallyPure was not CLBDExternallySideEffectFree"+
                s" (was ${LBDSideEffectFree combine CLBExternallyPure})"
        )
        assert(
            (LBDSideEffectFree combine CLBExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "LBDSideEffectFree combine CLBExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${LBDSideEffectFree combine CLBExternallySideEffectFree})"
        )
        assert(
            (LBDSideEffectFree combine CLBDPure) == CLBDSideEffectFree,
            "LBDSideEffectFree combine CLBDPure was not CLBDSideEffectFree"+
                s" (was ${LBDSideEffectFree combine CLBDPure})"
        )
        assert(
            (LBDSideEffectFree combine CLBDSideEffectFree) == CLBDSideEffectFree,
            "LBDSideEffectFree combine CLBDSideEffectFree was not CLBDSideEffectFree"+
                s" (was ${LBDSideEffectFree combine CLBDSideEffectFree})"
        )
        assert(
            (LBDSideEffectFree combine CLBDExternallyPure) == CLBDExternallySideEffectFree,
            "LBDSideEffectFree combine CLBDExternallyPure was not CLBDExternallySideEffectFree"+
                s" (was ${LBDSideEffectFree combine CLBDExternallyPure})"
        )
        assert(
            (LBDSideEffectFree combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "LBDSideEffectFree combine CLBDExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${LBDSideEffectFree combine CLBDExternallySideEffectFree})"
        )
        assert(
            (LBDSideEffectFree combine MaybePure) == MaybePure,
            "LBDSideEffectFree combine MaybePure was not MaybePure"+
                s" (was ${LBDSideEffectFree combine MaybePure})"
        )
        assert(
            (LBDSideEffectFree combine LBImpure) == LBImpure,
            "LBDSideEffectFree combine LBImpure was not LBImpure"+
                s" (was ${LBDSideEffectFree combine LBImpure})"
        )

        assert(
            (LBDExternallyPure combine LBDExternallySideEffectFree) == LBDExternallySideEffectFree,
            "LBDExternallyPure combine LBDExternallySideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${LBDExternallyPure combine LBDExternallySideEffectFree})"
        )
        assert(
            (LBDExternallyPure combine CPureWithoutAllocations) == CLBDExternallyPure,
            "LBDExternallyPure combine CPureWithoutAllocations was not CLBDExternallyPure"+
                s" (was ${LBDExternallyPure combine CPureWithoutAllocations})"
        )
        assert(
            (LBDExternallyPure combine CLBSideEffectFreeWithoutAllocations) == CLBDExternallySideEffectFree,
            "LBDExternallyPure combine CLBSideEffectFreeWithoutAllocations was not CLBDExternallySideEffectFree"+
                s" (was ${LBDExternallyPure combine CLBSideEffectFreeWithoutAllocations})"
        )
        assert(
            (LBDExternallyPure combine CLBPure) == CLBDExternallyPure,
            "LBDExternallyPure combine CLBPure was not CLBDExternallyPure"+
                s" (was ${LBDExternallyPure combine CLBPure})"
        )
        assert(
            (LBDExternallyPure combine CLBSideEffectFree) == CLBDExternallySideEffectFree,
            "LBDExternallyPure combine CLBSideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${LBDExternallyPure combine CLBSideEffectFree})"
        )
        assert(
            (LBDExternallyPure combine CLBExternallyPure) == CLBDExternallyPure,
            "LBDExternallyPure combine CLBExternallyPure was not CLBDExternallyPure"+
                s" (was ${LBDExternallyPure combine CLBExternallyPure})"
        )
        assert(
            (LBDExternallyPure combine CLBExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "LBDExternallyPure combine CLBExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${LBDExternallyPure combine CLBExternallySideEffectFree})"
        )
        assert(
            (LBDExternallyPure combine CLBDPure) == CLBDExternallyPure,
            "LBDExternallyPure combine CLBDPure was not CLBDExternallyPure"+
                s" (was ${LBDExternallyPure combine CLBDPure})"
        )
        assert(
            (LBDExternallyPure combine CLBDSideEffectFree) == CLBDExternallySideEffectFree,
            "LBDExternallyPure combine CLBDSideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${LBDExternallyPure combine CLBDSideEffectFree})"
        )
        assert(
            (LBDExternallyPure combine CLBDExternallyPure) == CLBDExternallyPure,
            "LBDExternallyPure combine CLBDExternallyPure was not CLBDExternallyPure"+
                s" (was ${LBDExternallyPure combine CLBDExternallyPure})"
        )
        assert(
            (LBDExternallyPure combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "LBDExternallyPure combine CLBDExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${LBDExternallyPure combine CLBDExternallySideEffectFree})"
        )
        assert(
            (LBDExternallyPure combine MaybePure) == MaybePure,
            "LBDExternallyPure combine MaybePure was not MaybePure"+
                s" (was ${LBDExternallyPure combine MaybePure})"
        )
        assert(
            (LBDExternallyPure combine LBImpure) == LBImpure,
            "LBDExternallyPure combine LBImpure was not LBImpure"+
                s" (was ${LBDExternallyPure combine LBImpure})"
        )

        assert(
            (LBDExternallySideEffectFree combine CPureWithoutAllocations) == CLBDExternallySideEffectFree,
            "LBDExternallySideEffectFree combine CPureWithoutAllocations was not CLBDExternallySideEffectFree"+
                s" (was ${LBDExternallySideEffectFree combine CPureWithoutAllocations})"
        )
        assert(
            (LBDExternallySideEffectFree combine CLBSideEffectFreeWithoutAllocations) == CLBDExternallySideEffectFree,
            "LBDExternallySideEffectFree combine CLBSideEffectFreeWithoutAllocations was not CLBDExternallySideEffectFree"+
                s" (was ${LBDExternallySideEffectFree combine CLBSideEffectFreeWithoutAllocations})"
        )
        assert(
            (LBDExternallySideEffectFree combine CLBPure) == CLBDExternallySideEffectFree,
            "LBDExternallySideEffectFree combine CLBPure was not CLBDExternallySideEffectFree"+
                s" (was ${LBDExternallySideEffectFree combine CLBPure})"
        )
        assert(
            (LBDExternallySideEffectFree combine CLBSideEffectFree) == CLBDExternallySideEffectFree,
            "LBDExternallySideEffectFree combine CLBSideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${LBDExternallySideEffectFree combine CLBSideEffectFree})"
        )
        assert(
            (LBDExternallySideEffectFree combine CLBExternallyPure) == CLBDExternallySideEffectFree,
            "LBDExternallySideEffectFree combine CLBExternallyPure was not CLBDExternallySideEffectFree"+
                s" (was ${LBDExternallySideEffectFree combine CLBExternallyPure})"
        )
        assert(
            (LBDExternallySideEffectFree combine CLBExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "LBDExternallySideEffectFree combine CLBExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${LBDExternallySideEffectFree combine CLBExternallySideEffectFree})"
        )
        assert(
            (LBDExternallySideEffectFree combine CLBDPure) == CLBDExternallySideEffectFree,
            "LBDExternallySideEffectFree combine CLBDPure was not CLBDExternallySideEffectFree"+
                s" (was ${LBDExternallySideEffectFree combine CLBDPure})"
        )
        assert(
            (LBDExternallySideEffectFree combine CLBDSideEffectFree) == CLBDExternallySideEffectFree,
            "LBDExternallySideEffectFree combine CLBDSideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${LBDExternallySideEffectFree combine CLBDSideEffectFree})"
        )
        assert(
            (LBDExternallySideEffectFree combine CLBDExternallyPure) == CLBDExternallySideEffectFree,
            "LBDExternallySideEffectFree combine CLBDExternallyPure was not CLBDExternallySideEffectFree"+
                s" (was ${LBDExternallySideEffectFree combine CLBDExternallyPure})"
        )
        assert(
            (LBDExternallySideEffectFree combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "LBDExternallySideEffectFree combine CLBDExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${LBDExternallySideEffectFree combine CLBDExternallySideEffectFree})"
        )
        assert(
            (LBDExternallySideEffectFree combine MaybePure) == MaybePure,
            "LBDExternallySideEffectFree combine MaybePure was not MaybePure"+
                s" (was ${LBDExternallySideEffectFree combine MaybePure})"
        )
        assert(
            (LBDExternallySideEffectFree combine LBImpure) == LBImpure,
            "LBDExternallySideEffectFree combine LBImpure was not LBImpure"+
                s" (was ${LBDExternallySideEffectFree combine LBImpure})"
        )
    }

    it should "return the correct purity levels for conditional levels" in {

        assert((MaybePure combine Impure) == Impure)

        assert(
            (CPureWithoutAllocations combine CLBPure) == CLBPure,
            "CPureWithoutAllocations combine CLBPure was not CLBPure"+
                s" (was ${CPureWithoutAllocations combine CLBPure})"
        )
        assert(
            (CPureWithoutAllocations combine CLBSideEffectFree) == CLBSideEffectFree,
            "CPureWithoutAllocations combine CLBSideEffectFree was not CLBSideEffectFree"+
                s" (was ${CPureWithoutAllocations combine CLBSideEffectFree})"
        )
        assert(
            (CPureWithoutAllocations combine CLBExternallyPure) == CLBExternallyPure,
            "CPureWithoutAllocations combine CLBExternallyPure was not CLBExternallyPure"+
                s" (was ${CPureWithoutAllocations combine CLBExternallyPure})"
        )
        assert(
            (CPureWithoutAllocations combine CLBExternallySideEffectFree) == CLBExternallySideEffectFree,
            "CPureWithoutAllocations combine CLBExternallySideEffectFree was not CLBExternallySideEffectFree"+
                s" (was ${CPureWithoutAllocations combine CLBExternallySideEffectFree})"
        )
        assert(
            (CPureWithoutAllocations combine CLBDPure) == CLBDPure,
            "CPureWithoutAllocations combine CLBDPure was not CLBDPure"+
                s" (was ${CPureWithoutAllocations combine CLBDPure})"
        )
        assert(
            (CPureWithoutAllocations combine CLBDSideEffectFree) == CLBDSideEffectFree,
            "CPureWithoutAllocations combine CLBDSideEffectFree was not CLBDSideEffectFree"+
                s" (was ${CPureWithoutAllocations combine CLBDSideEffectFree})"
        )
        assert(
            (CPureWithoutAllocations combine CLBDExternallyPure) == CLBDExternallyPure,
            "CPureWithoutAllocations combine CLBDExternallyPure was not CLBDExternallyPure"+
                s" (was ${CPureWithoutAllocations combine CLBDExternallyPure})"
        )
        assert(
            (CPureWithoutAllocations combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "CPureWithoutAllocations combine CLBDExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${CPureWithoutAllocations combine CLBDExternallySideEffectFree})"
        )
        assert(
            (CPureWithoutAllocations combine MaybePure) == MaybePure,
            "CPureWithoutAllocations combine MaybePure was not MaybePure"+
                s" (was ${CPureWithoutAllocations combine MaybePure})"
        )
        assert(
            (CPureWithoutAllocations combine LBImpure) == LBImpure,
            "CPureWithoutAllocations combine LBImpure was not LBImpure"+
                s" (was ${CPureWithoutAllocations combine LBImpure})"
        )

        assert(
            (CLBSideEffectFreeWithoutAllocations combine CLBPure) == CLBSideEffectFree,
            "CLBSideEffectFreeWithoutAllocations combine CLBPure was not CLBSideEffectFree"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine CLBPure})"
        )
        assert(
            (CLBSideEffectFreeWithoutAllocations combine CLBSideEffectFree) == CLBSideEffectFree,
            "CLBSideEffectFreeWithoutAllocations combine CLBSideEffectFree was not CLBSideEffectFree"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine CLBSideEffectFree})"
        )
        assert(
            (CLBSideEffectFreeWithoutAllocations combine CLBExternallyPure) == CLBExternallySideEffectFree,
            "CLBSideEffectFreeWithoutAllocations combine CLBExternallyPure was not CLBExternallySideEffectFree"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine CLBExternallyPure})"
        )
        assert(
            (CLBSideEffectFreeWithoutAllocations combine CLBExternallySideEffectFree) == CLBExternallySideEffectFree,
            "CLBSideEffectFreeWithoutAllocations combine CLBExternallySideEffectFree was not CLBExternallySideEffectFree"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine CLBExternallySideEffectFree})"
        )
        assert(
            (CLBSideEffectFreeWithoutAllocations combine CLBDPure) == CLBDSideEffectFree,
            "CLBSideEffectFreeWithoutAllocations combine CLBDPure was not CLBDSideEffectFree"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine CLBDPure})"
        )
        assert(
            (CLBSideEffectFreeWithoutAllocations combine CLBDSideEffectFree) == CLBDSideEffectFree,
            "CLBSideEffectFreeWithoutAllocations combine CLBDSideEffectFree was not CLBDSideEffectFree"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine CLBDSideEffectFree})"
        )
        assert(
            (CLBSideEffectFreeWithoutAllocations combine CLBDExternallyPure) == CLBDExternallySideEffectFree,
            "CLBSideEffectFreeWithoutAllocations combine CLBDExternallyPure was not CLBDExternallySideEffectFree"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine CLBDExternallyPure})"
        )
        assert(
            (CLBSideEffectFreeWithoutAllocations combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "CLBSideEffectFreeWithoutAllocations combine CLBDExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine CLBDExternallySideEffectFree})"
        )
        assert(
            (CLBSideEffectFreeWithoutAllocations combine MaybePure) == MaybePure,
            "CLBSideEffectFreeWithoutAllocations combine MaybePure was not MaybePure"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine MaybePure})"
        )
        assert(
            (CLBSideEffectFreeWithoutAllocations combine LBImpure) == LBImpure,
            "CLBSideEffectFreeWithoutAllocations combine LBImpure was not LBImpure"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine LBImpure})"
        )

        assert(
            (CLBPure combine CLBSideEffectFree) == CLBSideEffectFree,
            "CLBPure combine CLBSideEffectFree was not CLBSideEffectFree"+
                s" (was ${CLBPure combine CLBSideEffectFree})"
        )
        assert(
            (CLBPure combine CLBExternallyPure) == CLBExternallyPure,
            "CLBPure combine CLBExternallyPure was not CLBExternallyPure"+
                s" (was ${CLBPure combine CLBExternallyPure})"
        )
        assert(
            (CLBPure combine CLBExternallySideEffectFree) == CLBExternallySideEffectFree,
            "CLBPure combine CLBExternallySideEffectFree was not CLBExternallySideEffectFree"+
                s" (was ${CLBPure combine CLBExternallySideEffectFree})"
        )
        assert(
            (CLBPure combine CLBDPure) == CLBDPure,
            "CLBPure combine CLBDPure was not CLBDPure"+
                s" (was ${CLBPure combine CLBDPure})"
        )
        assert(
            (CLBPure combine CLBDSideEffectFree) == CLBDSideEffectFree,
            "CLBPure combine CLBDSideEffectFree was not CLBDSideEffectFree"+
                s" (was ${CLBPure combine CLBDSideEffectFree})"
        )
        assert(
            (CLBPure combine CLBDExternallyPure) == CLBDExternallyPure,
            "CLBPure combine CLBDExternallyPure was not CLBDExternallyPure"+
                s" (was ${CLBPure combine CLBDExternallyPure})"
        )
        assert(
            (CLBPure combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "CLBPure combine CLBDExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${CLBPure combine CLBDExternallySideEffectFree})"
        )
        assert(
            (CLBPure combine MaybePure) == MaybePure,
            "CLBPure combine MaybePure was not MaybePure"+
                s" (was ${CLBPure combine MaybePure})"
        )
        assert(
            (CLBPure combine LBImpure) == LBImpure,
            "CLBPure combine LBImpure was not LBImpure"+
                s" (was ${CLBPure combine LBImpure})"
        )

        assert(
            (CLBSideEffectFree combine CLBExternallyPure) == CLBExternallySideEffectFree,
            "CLBSideEffectFree combine CLBExternallyPure was not CLBExternallySideEffectFree"+
                s" (was ${CLBSideEffectFree combine CLBExternallyPure})"
        )
        assert(
            (CLBSideEffectFree combine CLBExternallySideEffectFree) == CLBExternallySideEffectFree,
            "CLBSideEffectFree combine CLBExternallySideEffectFree was not CLBExternallySideEffectFree"+
                s" (was ${CLBSideEffectFree combine CLBExternallySideEffectFree})"
        )
        assert(
            (CLBSideEffectFree combine CLBDPure) == CLBDSideEffectFree,
            "CLBSideEffectFree combine CLBDPure was not CLBDSideEffectFree"+
                s" (was ${CLBSideEffectFree combine CLBDPure})"
        )
        assert(
            (CLBSideEffectFree combine CLBDSideEffectFree) == CLBDSideEffectFree,
            "CLBSideEffectFree combine CLBDSideEffectFree was not CLBDSideEffectFree"+
                s" (was ${CLBSideEffectFree combine CLBDSideEffectFree})"
        )
        assert(
            (CLBSideEffectFree combine CLBDExternallyPure) == CLBDExternallySideEffectFree,
            "CLBSideEffectFree combine CLBDExternallyPure was not CLBDExternallySideEffectFree"+
                s" (was ${CLBSideEffectFree combine CLBDExternallyPure})"
        )
        assert(
            (CLBSideEffectFree combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "CLBSideEffectFree combine CLBDExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${CLBSideEffectFree combine CLBDExternallySideEffectFree})"
        )
        assert(
            (CLBSideEffectFree combine MaybePure) == MaybePure,
            "CLBSideEffectFree combine MaybePure was not MaybePure"+
                s" (was ${CLBSideEffectFree combine MaybePure})"
        )
        assert(
            (CLBSideEffectFree combine LBImpure) == LBImpure,
            "CLBSideEffectFree combine LBImpure was not LBImpure"+
                s" (was ${CLBSideEffectFree combine LBImpure})"
        )

        assert(
            (CLBExternallyPure combine CLBExternallySideEffectFree) == CLBExternallySideEffectFree,
            "CLBExternallyPure combine CLBExternallySideEffectFree was not CLBExternallySideEffectFree"+
                s" (was ${CLBExternallyPure combine CLBExternallySideEffectFree})"
        )
        assert(
            (CLBExternallyPure combine CLBDPure) == CLBDExternallyPure,
            "CLBExternallyPure combine CLBDPure was not CLBDExternallyPure"+
                s" (was ${CLBExternallyPure combine CLBDPure})"
        )
        assert(
            (CLBExternallyPure combine CLBDSideEffectFree) == CLBDExternallySideEffectFree,
            "CLBExternallyPure combine CLBDSideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${CLBExternallyPure combine CLBDSideEffectFree})"
        )
        assert(
            (CLBExternallyPure combine CLBDExternallyPure) == CLBDExternallyPure,
            "CLBExternallyPure combine CLBDExternallyPure was not CLBDExternallyPure"+
                s" (was ${CLBExternallyPure combine CLBDExternallyPure})"
        )
        assert(
            (CLBExternallyPure combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "CLBExternallyPure combine CLBDExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${CLBExternallyPure combine CLBDExternallySideEffectFree})"
        )
        assert(
            (CLBExternallyPure combine MaybePure) == MaybePure,
            "CLBExternallyPure combine MaybePure was not MaybePure"+
                s" (was ${CLBExternallyPure combine MaybePure})"
        )
        assert(
            (CLBExternallyPure combine LBImpure) == LBImpure,
            "CLBExternallyPure combine LBImpure was not LBImpure"+
                s" (was ${CLBExternallyPure combine LBImpure})"
        )

        assert(
            (CLBExternallySideEffectFree combine CLBDPure) == CLBDExternallySideEffectFree,
            "CLBExternallySideEffectFree combine CLBDPure was not CLBDExternallySideEffectFree"+
                s" (was ${CLBExternallySideEffectFree combine CLBDPure})"
        )
        assert(
            (CLBExternallySideEffectFree combine CLBDSideEffectFree) == CLBDExternallySideEffectFree,
            "CLBExternallySideEffectFree combine CLBDSideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${CLBExternallySideEffectFree combine CLBDSideEffectFree})"
        )
        assert(
            (CLBExternallySideEffectFree combine CLBDExternallyPure) == CLBDExternallySideEffectFree,
            "CLBExternallySideEffectFree combine CLBDExternallyPure was not CLBDExternallySideEffectFree"+
                s" (was ${CLBExternallySideEffectFree combine CLBDExternallyPure})"
        )
        assert(
            (CLBExternallySideEffectFree combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "CLBExternallySideEffectFree combine CLBDExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${CLBExternallySideEffectFree combine CLBDExternallySideEffectFree})"
        )
        assert(
            (CLBExternallySideEffectFree combine MaybePure) == MaybePure,
            "CLBExternallySideEffectFree combine MaybePure was not MaybePure"+
                s" (was ${CLBExternallySideEffectFree combine MaybePure})"
        )
        assert(
            (CLBExternallySideEffectFree combine LBImpure) == LBImpure,
            "CLBExternallySideEffectFree combine LBImpure was not LBImpure"+
                s" (was ${CLBExternallySideEffectFree combine LBImpure})"
        )

        assert(
            (CLBDPure combine CLBDSideEffectFree) == CLBDSideEffectFree,
            "CLBDPure combine CLBDSideEffectFree was not CLBDSideEffectFree"+
                s" (was ${CLBDPure combine CLBDSideEffectFree})"
        )
        assert(
            (CLBDPure combine CLBDExternallyPure) == CLBDExternallyPure,
            "CLBDPure combine CLBDExternallyPure was not CLBDExternallyPure"+
                s" (was ${CLBDPure combine CLBDExternallyPure})"
        )
        assert(
            (CLBDPure combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "CLBDPure combine CLBDExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${CLBDPure combine CLBDExternallySideEffectFree})"
        )
        assert(
            (CLBDPure combine MaybePure) == MaybePure,
            "CLBDPure combine MaybePure was not MaybePure"+
                s" (was ${CLBDPure combine MaybePure})"
        )
        assert(
            (CLBDPure combine LBImpure) == LBImpure,
            "CLBDPure combine LBImpure was not LBImpure"+
                s" (was ${CLBDPure combine LBImpure})"
        )

        assert(
            (CLBDSideEffectFree combine CLBDExternallyPure) == CLBDExternallySideEffectFree,
            "CLBDSideEffectFree combine CLBDExternallyPure was not CLBDExternallySideEffectFree"+
                s" (was ${CLBDSideEffectFree combine CLBDExternallyPure})"
        )
        assert(
            (CLBDSideEffectFree combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "CLBDSideEffectFree combine CLBDExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${CLBDSideEffectFree combine CLBDExternallySideEffectFree})"
        )
        assert(
            (CLBDSideEffectFree combine MaybePure) == MaybePure,
            "CLBDSideEffectFree combine MaybePure was not MaybePure"+
                s" (was ${CLBDSideEffectFree combine MaybePure})"
        )
        assert(
            (CLBDSideEffectFree combine LBImpure) == LBImpure,
            "CLBDSideEffectFree combine LBImpure was not LBImpure"+
                s" (was ${CLBDSideEffectFree combine LBImpure})"
        )

        assert(
            (CLBDExternallyPure combine CLBDExternallySideEffectFree) == CLBDExternallySideEffectFree,
            "CLBDExternallyPure combine CLBDExternallySideEffectFree was not CLBDExternallySideEffectFree"+
                s" (was ${CLBDExternallyPure combine CLBDExternallySideEffectFree})"
        )
        assert(
            (CLBDExternallyPure combine MaybePure) == MaybePure,
            "CLBDExternallyPure combine MaybePure was not MaybePure"+
                s" (was ${CLBDExternallyPure combine MaybePure})"
        )
        assert(
            (CLBDExternallyPure combine LBImpure) == LBImpure,
            "CLBDExternallyPure combine LBImpure was not LBImpure"+
                s" (was ${CLBDExternallyPure combine LBImpure})"
        )

        assert(
            (CLBDExternallySideEffectFree combine MaybePure) == MaybePure,
            "CLBDExternallySideEffectFree combine MaybePure was not MaybePure"+
                s" (was ${CLBDExternallySideEffectFree combine MaybePure})"
        )
        assert(
            (CLBDExternallySideEffectFree combine LBImpure) == LBImpure,
            "CLBDExternallySideEffectFree combine LBImpure was not LBImpure"+
                s" (was ${CLBDExternallySideEffectFree combine LBImpure})"
        )

        assert((MaybePure combine LBImpure) == MaybePure)
    }
}
