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

import org.opalj.fpcf.properties.DomainSpecific.DomainSpecificReason
import org.opalj.fpcf.properties.DomainSpecific.RaisesExceptions
import org.opalj.fpcf.properties.DomainSpecific.UsesSystemOutOrErr
import org.opalj.fpcf.properties.DomainSpecific.UsesLogging
import org.opalj.fpcf.properties.LBImpureBase.LBImpureDueToUnknownEntity
import org.opalj.fpcf.properties.LBImpureBase.LBImpureDueToFutureExtension
import org.opalj.fpcf.properties.LBImpureBase.LBImpureDueToUnknownProperty
import org.opalj.fpcf.properties.LBImpureBase.LBImpureDueToHeapModification
import org.opalj.fpcf.properties.LBImpureBase.LBImpureDueToSynchronization
import org.opalj.fpcf.properties.LBImpureBase.LBImpureDueToImpureCall
import org.opalj.fpcf.properties.LBImpureBase.LBImpure
import org.scalatest.Matchers
import org.scalatest.FlatSpec

/**
 * Tests the [[Purity]] property, especially correctness of the combine operator.
 *
 * @author Dominik Helm
 */
class PurityPropertyTest extends FlatSpec with Matchers {
    val allPurities: List[Purity] = List(
        PureWithoutAllocations, LBSideEffectFreeWithoutAllocations, LBPure, LBSideEffectFree,
        LBExternallyPure, LBExternallySideEffectFree, LBDPure(Set.empty),
        LBDSideEffectFree(Set.empty), LBDExternallyPure(Set.empty),
        LBDExternallySideEffectFree(Set.empty), CPureWithoutAllocations,
        CLBSideEffectFreeWithoutAllocations, CLBPure, CLBSideEffectFree, CLBExternallyPure,
        CLBExternallySideEffectFree, CLBDPure(Set.empty), CLBDSideEffectFree(Set.empty),
        CLBDExternallyPure(Set.empty), CLBDExternallySideEffectFree(Set.empty), MaybePure,
        LBImpure, LBImpureDueToSynchronization, LBImpureDueToHeapModification,
        LBImpureDueToFutureExtension, LBImpureDueToImpureCall, LBImpureDueToUnknownEntity,
        LBImpureDueToUnknownProperty
    )

    val doesntModifiyReceiver: Set[Purity] = Set(
        PureWithoutAllocations, LBSideEffectFreeWithoutAllocations, LBPure, LBSideEffectFree,
        LBDPure(Set.empty), LBDSideEffectFree(Set.empty), CPureWithoutAllocations,
        CLBSideEffectFreeWithoutAllocations, CLBPure, CLBSideEffectFree, CLBDPure(Set.empty),
        CLBDSideEffectFree(Set.empty)
    )

    val conditional: Set[Purity] = Set(
        CPureWithoutAllocations, CLBSideEffectFreeWithoutAllocations, CLBPure, CLBSideEffectFree,
        CLBExternallyPure, CLBExternallySideEffectFree, CLBDPure(Set.empty),
        CLBDSideEffectFree(Set.empty), CLBDExternallyPure(Set.empty),
        CLBDExternallySideEffectFree(Set.empty)
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
            PureWithoutAllocations, LBPure, LBExternallyPure, LBDPure(Set.empty),
            LBDExternallyPure(Set.empty), CPureWithoutAllocations, CLBPure, CLBExternallyPure,
            CLBDPure(Set.empty), CLBDExternallyPure(Set.empty)
        )

        for (prop ← allPurities) {
            assert(
                prop.isDeterministic == deterministic.contains(prop),
                s"$prop.isDeterministic was ${prop.isDeterministic}"
            )
        }

        for (prop ← allPurities) {
            assert(
                prop.modifiesReceiver != doesntModifiyReceiver.contains(prop),
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
            if (doesntModifiyReceiver.contains(prop))
                assert(
                    prop.withoutExternal == prop,
                    s"$prop.withoutExternal modified $prop (was ${prop.withoutExternal})"
                )
            else
                assert(
                    prop.withoutExternal.flags == (prop.flags & ~Purity.MODIFIES_RECEIVER),
                    s"$prop.withoutExternal was incorrect (was ${prop.withoutExternal})"
                )
            if (conditional.contains(prop))
                assert(
                    prop.unconditional.flags == (prop.flags & ~Purity.IS_CONDITIONAL),
                    s"$prop.unconditional was incorrect (was ${prop.unconditional})"
                )
            else
                assert(
                    prop.unconditional == prop,
                    s"$prop.unconditional modified $prop (was ${prop.unconditional})"
                )
        }
    }

    "domain specific reasons" should "be unioned correctly" in {
        val domainSpecific = Set(
            LBDPure, LBDSideEffectFree, LBDExternallyPure,
            LBDExternallySideEffectFree, CLBDPure,
            CLBDSideEffectFree, CLBDExternallyPure,
            CLBDExternallySideEffectFree
        )

        val reasons: Set[DomainSpecificReason] =
            Set(RaisesExceptions, UsesSystemOutOrErr, UsesLogging)

        for {
            p1 ← domainSpecific
            p2 ← domainSpecific
            reasons1 ← reasons.subsets
            reasons2 ← reasons.subsets
        } {
            val prop1 = p1(reasons1)
            val prop2 = p2(reasons2)
            val result = prop1 combine prop2
            assert(
                result.reasons == (reasons1 | reasons2),
                s"$prop1 combine $prop2 did not have the union of both reasons (was $result)"
            )
            assert(
                result.getClass == (p1(Set.empty) combine p2(Set.empty)).getClass,
                s"$prop1 combine $prop2 was not the correct purity level (was $result)"
            )
        }
    }

    "the combine operator" should "be reflexive and symmetric" in {
        for (prop1 ← allPurities) {
            assert((prop1 combine prop1) == prop1, s"combine was not reflexive for $prop1")
            for (prop2 ← allPurities) {
                val combine12 = prop1 combine prop2
                val combine21 = prop2 combine prop1
                (prop1, prop2) match {
                    case (LBImpureBase(_), LBImpureBase(_)) ⇒
                        assert(
                            combine12 == prop1 || combine12 == prop2,
                            s"$prop1 combine $prop2 was not one of the impure reasons (was $combine12)"
                        )
                    case _ ⇒
                        assert(
                            combine12 == combine21,
                            s"$prop1 combine $prop2 was not symmetric (was $combine12 / $combine21)"
                        )
                }
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

        assert(
            (LBSideEffectFreeWithoutAllocations combine LBPure) == LBSideEffectFree,
            "LBSideEffectFreeWithoutAllocations combine LBPure was not LBSideEffectFree"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine LBPure})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine LBSideEffectFree) == LBSideEffectFree,
            "LBSideEffectFreeWithoutAllocations combine LBSideEffectFree was not LBSideEffectFree"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine LBSideEffectFree})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine LBExternallyPure) == LBExternallySideEffectFree,
            "LBSideEffectFreeWithoutAllocations combine LBExternallyPure was not LBExternallySideEffectFree"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine LBExternallyPure})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine LBExternallySideEffectFree) == LBExternallySideEffectFree,
            "LBSideEffectFreeWithoutAllocations combine LBExternallySideEffectFree was not LBExternallySideEffectFree"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine LBExternallySideEffectFree})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine LBDPure(Set.empty)) == LBDSideEffectFree(Set.empty),
            "LBSideEffectFreeWithoutAllocations combine LBDPure(Set.empty) was not LBDSideEffectFree(Set.empty)"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine LBDPure(Set.empty)})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine LBDSideEffectFree(Set.empty)) == LBDSideEffectFree(Set.empty),
            "LBSideEffectFreeWithoutAllocations combine LBDSideEffectFree(Set.empty) was not LBDSideEffectFree(Set.empty)"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine LBDSideEffectFree(Set.empty)})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine LBDExternallyPure(Set.empty)) == LBDExternallySideEffectFree(Set.empty),
            "LBSideEffectFreeWithoutAllocations combine LBDExternallyPure(Set.empty) was not LBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine LBDExternallyPure(Set.empty)})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine LBDExternallySideEffectFree(Set.empty)) == LBDExternallySideEffectFree(Set.empty),
            "LBSideEffectFreeWithoutAllocations combine LBDExternallySideEffectFree(Set.empty) was not LBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine LBDExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine CPureWithoutAllocations) == CLBSideEffectFreeWithoutAllocations,
            "LBSideEffectFreeWithoutAllocations combine CPureWithoutAllocations was not CLBSideEffectFreeWithoutAllocations"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine CPureWithoutAllocations})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine CLBSideEffectFreeWithoutAllocations) == CLBSideEffectFreeWithoutAllocations,
            "LBSideEffectFreeWithoutAllocations combine CLBSideEffectFreeWithoutAllocations was not CLBSideEffectFreeWithoutAllocations"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine CLBSideEffectFreeWithoutAllocations})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine CLBPure) == CLBSideEffectFree,
            "LBSideEffectFreeWithoutAllocations combine CLBPure was not CLBSideEffectFree"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine CLBPure})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine CLBSideEffectFree) == CLBSideEffectFree,
            "LBSideEffectFreeWithoutAllocations combine CLBSideEffectFree was not CLBSideEffectFree"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine CLBSideEffectFree})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine CLBExternallyPure) == CLBExternallySideEffectFree,
            "LBSideEffectFreeWithoutAllocations combine CLBExternallyPure was not CLBExternallySideEffectFree"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine CLBExternallyPure})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine CLBExternallySideEffectFree) == CLBExternallySideEffectFree,
            "LBSideEffectFreeWithoutAllocations combine CLBExternallySideEffectFree was not CLBExternallySideEffectFree"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine CLBExternallySideEffectFree})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine CLBDPure(Set.empty)) == CLBDSideEffectFree(Set.empty),
            "LBSideEffectFreeWithoutAllocations combine CLBDPure(Set.empty) was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine CLBDPure(Set.empty)})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine CLBDSideEffectFree(Set.empty)) == CLBDSideEffectFree(Set.empty),
            "LBSideEffectFreeWithoutAllocations combine CLBDSideEffectFree(Set.empty) was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine CLBDSideEffectFree(Set.empty)})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine CLBDExternallyPure(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBSideEffectFreeWithoutAllocations combine CLBDExternallyPure(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine CLBDExternallyPure(Set.empty)})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBSideEffectFreeWithoutAllocations combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine CLBDExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine MaybePure) == MaybePure,
            "LBSideEffectFreeWithoutAllocations combine MaybePure was not MaybePure"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine MaybePure})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine LBImpure) == LBImpure,
            "LBSideEffectFreeWithoutAllocations combine LBImpure was not LBImpure"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine LBImpure})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "LBSideEffectFreeWithoutAllocations combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine LBImpureDueToSynchronization})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "LBSideEffectFreeWithoutAllocations combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine LBImpureDueToHeapModification})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "LBSideEffectFreeWithoutAllocations combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine LBImpureDueToFutureExtension})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "LBSideEffectFreeWithoutAllocations combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (LBSideEffectFreeWithoutAllocations combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "LBSideEffectFreeWithoutAllocations combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${LBSideEffectFreeWithoutAllocations combine LBImpureDueToUnknownProperty})"
        )

        assert(
            (LBPure combine LBSideEffectFree) == LBSideEffectFree,
            "LBPure combine LBSideEffectFree was not LBSideEffectFree"+
                s" (was ${LBPure combine LBSideEffectFree})"
        )
        assert(
            (LBPure combine LBExternallyPure) == LBExternallyPure,
            "LBPure combine LBExternallyPure was not LBExternallyPure"+
                s" (was ${LBPure combine LBExternallyPure})"
        )
        assert(
            (LBPure combine LBExternallySideEffectFree) == LBExternallySideEffectFree,
            "LBPure combine LBExternallySideEffectFree was not LBExternallySideEffectFree"+
                s" (was ${LBPure combine LBExternallySideEffectFree})"
        )
        assert(
            (LBPure combine LBDPure(Set.empty)) == LBDPure(Set.empty),
            "LBPure combine LBDPure(Set.empty) was not LBDPure(Set.empty)"+
                s" (was ${LBPure combine LBDPure(Set.empty)})"
        )
        assert(
            (LBPure combine LBDSideEffectFree(Set.empty)) == LBDSideEffectFree(Set.empty),
            "LBPure combine LBDSideEffectFree(Set.empty) was not LBDSideEffectFree(Set.empty)"+
                s" (was ${LBPure combine LBDSideEffectFree(Set.empty)})"
        )
        assert(
            (LBPure combine LBDExternallyPure(Set.empty)) == LBDExternallyPure(Set.empty),
            "LBPure combine LBDExternallyPure(Set.empty) was not LBDExternallyPure(Set.empty)"+
                s" (was ${LBPure combine LBDExternallyPure(Set.empty)})"
        )
        assert(
            (LBPure combine LBDExternallySideEffectFree(Set.empty)) == LBDExternallySideEffectFree(Set.empty),
            "LBPure combine LBDExternallySideEffectFree(Set.empty) was not LBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBPure combine LBDExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (LBPure combine CPureWithoutAllocations) == CLBPure,
            "LBPure combine CPureWithoutAllocations was not CLBPure"+
                s" (was ${LBPure combine CPureWithoutAllocations})"
        )
        assert(
            (LBPure combine CLBSideEffectFreeWithoutAllocations) == CLBSideEffectFree,
            "LBPure combine CLBSideEffectFreeWithoutAllocations was not CLBSideEffectFree"+
                s" (was ${LBPure combine CLBSideEffectFreeWithoutAllocations})"
        )
        assert(
            (LBPure combine CLBPure) == CLBPure,
            "LBPure combine CLBPure was not CLBPure"+
                s" (was ${LBPure combine CLBPure})"
        )
        assert(
            (LBPure combine CLBSideEffectFree) == CLBSideEffectFree,
            "LBPure combine CLBSideEffectFree was not CLBSideEffectFree"+
                s" (was ${LBPure combine CLBSideEffectFree})"
        )
        assert(
            (LBPure combine CLBExternallyPure) == CLBExternallyPure,
            "LBPure combine CLBExternallyPure was not CLBExternallyPure"+
                s" (was ${LBPure combine CLBExternallyPure})"
        )
        assert(
            (LBPure combine CLBExternallySideEffectFree) == CLBExternallySideEffectFree,
            "LBPure combine CLBExternallySideEffectFree was not CLBExternallySideEffectFree"+
                s" (was ${LBPure combine CLBExternallySideEffectFree})"
        )
        assert(
            (LBPure combine CLBDPure(Set.empty)) == CLBDPure(Set.empty),
            "LBPure combine CLBDPure(Set.empty) was not CLBDPure(Set.empty)"+
                s" (was ${LBPure combine CLBDPure(Set.empty)})"
        )
        assert(
            (LBPure combine CLBDSideEffectFree(Set.empty)) == CLBDSideEffectFree(Set.empty),
            "LBPure combine CLBDSideEffectFree(Set.empty) was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${LBPure combine CLBDSideEffectFree(Set.empty)})"
        )
        assert(
            (LBPure combine CLBDExternallyPure(Set.empty)) == CLBDExternallyPure(Set.empty),
            "LBPure combine CLBDExternallyPure(Set.empty) was not CLBDExternallyPure(Set.empty)"+
                s" (was ${LBPure combine CLBDExternallyPure(Set.empty)})"
        )
        assert(
            (LBPure combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBPure combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBPure combine CLBDExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (LBPure combine MaybePure) == MaybePure,
            "LBPure combine MaybePure was not MaybePure"+
                s" (was ${LBPure combine MaybePure})"
        )
        assert(
            (LBPure combine LBImpure) == LBImpure,
            "LBPure combine LBImpure was not LBImpure"+
                s" (was ${LBPure combine LBImpure})"
        )
        assert(
            (LBPure combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "LBPure combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${LBPure combine LBImpureDueToSynchronization})"
        )
        assert(
            (LBPure combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "LBPure combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${LBPure combine LBImpureDueToHeapModification})"
        )
        assert(
            (LBPure combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "LBPure combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${LBPure combine LBImpureDueToFutureExtension})"
        )
        assert(
            (LBPure combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "LBPure combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${LBPure combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (LBPure combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "LBPure combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${LBPure combine LBImpureDueToUnknownProperty})"
        )

        assert(
            (LBSideEffectFree combine LBExternallyPure) == LBExternallySideEffectFree,
            "LBSideEffectFree combine LBExternallyPure was not LBExternallySideEffectFree"+
                s" (was ${LBSideEffectFree combine LBExternallyPure})"
        )
        assert(
            (LBSideEffectFree combine LBExternallySideEffectFree) == LBExternallySideEffectFree,
            "LBSideEffectFree combine LBExternallySideEffectFree was not LBExternallySideEffectFree"+
                s" (was ${LBSideEffectFree combine LBExternallySideEffectFree})"
        )
        assert(
            (LBSideEffectFree combine LBDPure(Set.empty)) == LBDSideEffectFree(Set.empty),
            "LBSideEffectFree combine LBDPure(Set.empty) was not LBDSideEffectFree(Set.empty)"+
                s" (was ${LBSideEffectFree combine LBDPure(Set.empty)})"
        )
        assert(
            (LBSideEffectFree combine LBDSideEffectFree(Set.empty)) == LBDSideEffectFree(Set.empty),
            "LBSideEffectFree combine LBDSideEffectFree(Set.empty) was not LBDSideEffectFree(Set.empty)"+
                s" (was ${LBSideEffectFree combine LBDSideEffectFree(Set.empty)})"
        )
        assert(
            (LBSideEffectFree combine LBDExternallyPure(Set.empty)) == LBDExternallySideEffectFree(Set.empty),
            "LBSideEffectFree combine LBDExternallyPure(Set.empty) was not LBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBSideEffectFree combine LBDExternallyPure(Set.empty)})"
        )
        assert(
            (LBSideEffectFree combine LBDExternallySideEffectFree(Set.empty)) == LBDExternallySideEffectFree(Set.empty),
            "LBSideEffectFree combine LBDExternallySideEffectFree(Set.empty) was not LBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBSideEffectFree combine LBDExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (LBSideEffectFree combine CPureWithoutAllocations) == CLBSideEffectFree,
            "LBSideEffectFree combine CPureWithoutAllocations was not CLBSideEffectFree"+
                s" (was ${LBSideEffectFree combine CPureWithoutAllocations})"
        )
        assert(
            (LBSideEffectFree combine CLBSideEffectFreeWithoutAllocations) == CLBSideEffectFree,
            "LBSideEffectFree combine CLBSideEffectFreeWithoutAllocations was not CLBSideEffectFree"+
                s" (was ${LBSideEffectFree combine CLBSideEffectFreeWithoutAllocations})"
        )
        assert(
            (LBSideEffectFree combine CLBPure) == CLBSideEffectFree,
            "LBSideEffectFree combine CLBPure was not CLBSideEffectFree"+
                s" (was ${LBSideEffectFree combine CLBPure})"
        )
        assert(
            (LBSideEffectFree combine CLBSideEffectFree) == CLBSideEffectFree,
            "LBSideEffectFree combine CLBSideEffectFree was not CLBSideEffectFree"+
                s" (was ${LBSideEffectFree combine CLBSideEffectFree})"
        )
        assert(
            (LBSideEffectFree combine CLBExternallyPure) == CLBExternallySideEffectFree,
            "LBSideEffectFree combine CLBExternallyPure was not CLBExternallySideEffectFree"+
                s" (was ${LBSideEffectFree combine CLBExternallyPure})"
        )
        assert(
            (LBSideEffectFree combine CLBExternallySideEffectFree) == CLBExternallySideEffectFree,
            "LBSideEffectFree combine CLBExternallySideEffectFree was not CLBExternallySideEffectFree"+
                s" (was ${LBSideEffectFree combine CLBExternallySideEffectFree})"
        )
        assert(
            (LBSideEffectFree combine CLBDPure(Set.empty)) == CLBDSideEffectFree(Set.empty),
            "LBSideEffectFree combine CLBDPure(Set.empty) was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${LBSideEffectFree combine CLBDPure(Set.empty)})"
        )
        assert(
            (LBSideEffectFree combine CLBDSideEffectFree(Set.empty)) == CLBDSideEffectFree(Set.empty),
            "LBSideEffectFree combine CLBDSideEffectFree(Set.empty) was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${LBSideEffectFree combine CLBDSideEffectFree(Set.empty)})"
        )
        assert(
            (LBSideEffectFree combine CLBDExternallyPure(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBSideEffectFree combine CLBDExternallyPure(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBSideEffectFree combine CLBDExternallyPure(Set.empty)})"
        )
        assert(
            (LBSideEffectFree combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBSideEffectFree combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBSideEffectFree combine CLBDExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (LBSideEffectFree combine MaybePure) == MaybePure,
            "LBSideEffectFree combine MaybePure was not MaybePure"+
                s" (was ${LBSideEffectFree combine MaybePure})"
        )
        assert(
            (LBSideEffectFree combine LBImpure) == LBImpure,
            "LBSideEffectFree combine LBImpure was not LBImpure"+
                s" (was ${LBSideEffectFree combine LBImpure})"
        )
        assert(
            (LBSideEffectFree combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "LBSideEffectFree combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${LBSideEffectFree combine LBImpureDueToSynchronization})"
        )
        assert(
            (LBSideEffectFree combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "LBSideEffectFree combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${LBSideEffectFree combine LBImpureDueToHeapModification})"
        )
        assert(
            (LBSideEffectFree combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "LBSideEffectFree combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${LBSideEffectFree combine LBImpureDueToFutureExtension})"
        )
        assert(
            (LBSideEffectFree combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "LBSideEffectFree combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${LBSideEffectFree combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (LBSideEffectFree combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "LBSideEffectFree combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${LBSideEffectFree combine LBImpureDueToUnknownProperty})"
        )

        assert(
            (LBExternallyPure combine LBExternallySideEffectFree) == LBExternallySideEffectFree,
            "LBExternallyPure combine LBExternallySideEffectFree was not LBExternallySideEffectFree"+
                s" (was ${LBExternallyPure combine LBExternallySideEffectFree})"
        )
        assert(
            (LBExternallyPure combine LBDPure(Set.empty)) == LBDExternallyPure(Set.empty),
            "LBExternallyPure combine LBDPure(Set.empty) was not LBDExternallyPure(Set.empty)"+
                s" (was ${LBExternallyPure combine LBDPure(Set.empty)})"
        )
        assert(
            (LBExternallyPure combine LBDSideEffectFree(Set.empty)) == LBDExternallySideEffectFree(Set.empty),
            "LBExternallyPure combine LBDSideEffectFree(Set.empty) was not LBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBExternallyPure combine LBDSideEffectFree(Set.empty)})"
        )
        assert(
            (LBExternallyPure combine LBDExternallyPure(Set.empty)) == LBDExternallyPure(Set.empty),
            "LBExternallyPure combine LBDExternallyPure(Set.empty) was not LBDExternallyPure(Set.empty)"+
                s" (was ${LBExternallyPure combine LBDExternallyPure(Set.empty)})"
        )
        assert(
            (LBExternallyPure combine LBDExternallySideEffectFree(Set.empty)) == LBDExternallySideEffectFree(Set.empty),
            "LBExternallyPure combine LBDExternallySideEffectFree(Set.empty) was not LBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBExternallyPure combine LBDExternallySideEffectFree(Set.empty)})"
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
            (LBExternallyPure combine CLBDPure(Set.empty)) == CLBDExternallyPure(Set.empty),
            "LBExternallyPure combine CLBDPure(Set.empty) was not CLBDExternallyPure(Set.empty)"+
                s" (was ${LBExternallyPure combine CLBDPure(Set.empty)})"
        )
        assert(
            (LBExternallyPure combine CLBDSideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBExternallyPure combine CLBDSideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBExternallyPure combine CLBDSideEffectFree(Set.empty)})"
        )
        assert(
            (LBExternallyPure combine CLBDExternallyPure(Set.empty)) == CLBDExternallyPure(Set.empty),
            "LBExternallyPure combine CLBDExternallyPure(Set.empty) was not CLBDExternallyPure(Set.empty)"+
                s" (was ${LBExternallyPure combine CLBDExternallyPure(Set.empty)})"
        )
        assert(
            (LBExternallyPure combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBExternallyPure combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBExternallyPure combine CLBDExternallySideEffectFree(Set.empty)})"
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
            (LBExternallyPure combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "LBExternallyPure combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${LBExternallyPure combine LBImpureDueToSynchronization})"
        )
        assert(
            (LBExternallyPure combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "LBExternallyPure combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${LBExternallyPure combine LBImpureDueToHeapModification})"
        )
        assert(
            (LBExternallyPure combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "LBExternallyPure combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${LBExternallyPure combine LBImpureDueToFutureExtension})"
        )
        assert(
            (LBExternallyPure combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "LBExternallyPure combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${LBExternallyPure combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (LBExternallyPure combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "LBExternallyPure combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${LBExternallyPure combine LBImpureDueToUnknownProperty})"
        )

        assert(
            (LBExternallySideEffectFree combine LBDPure(Set.empty)) == LBDExternallySideEffectFree(Set.empty),
            "LBExternallySideEffectFree combine LBDPure(Set.empty) was not LBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBExternallySideEffectFree combine LBDPure(Set.empty)})"
        )
        assert(
            (LBExternallySideEffectFree combine LBDSideEffectFree(Set.empty)) == LBDExternallySideEffectFree(Set.empty),
            "LBExternallySideEffectFree combine LBDSideEffectFree(Set.empty) was not LBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBExternallySideEffectFree combine LBDSideEffectFree(Set.empty)})"
        )
        assert(
            (LBExternallySideEffectFree combine LBDExternallyPure(Set.empty)) == LBDExternallySideEffectFree(Set.empty),
            "LBExternallySideEffectFree combine LBDExternallyPure(Set.empty) was not LBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBExternallySideEffectFree combine LBDExternallyPure(Set.empty)})"
        )
        assert(
            (LBExternallySideEffectFree combine LBDExternallySideEffectFree(Set.empty)) == LBDExternallySideEffectFree(Set.empty),
            "LBExternallySideEffectFree combine LBDExternallySideEffectFree(Set.empty) was not LBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBExternallySideEffectFree combine LBDExternallySideEffectFree(Set.empty)})"
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
            (LBExternallySideEffectFree combine CLBDPure(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBExternallySideEffectFree combine CLBDPure(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBExternallySideEffectFree combine CLBDPure(Set.empty)})"
        )
        assert(
            (LBExternallySideEffectFree combine CLBDSideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBExternallySideEffectFree combine CLBDSideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBExternallySideEffectFree combine CLBDSideEffectFree(Set.empty)})"
        )
        assert(
            (LBExternallySideEffectFree combine CLBDExternallyPure(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBExternallySideEffectFree combine CLBDExternallyPure(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBExternallySideEffectFree combine CLBDExternallyPure(Set.empty)})"
        )
        assert(
            (LBExternallySideEffectFree combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBExternallySideEffectFree combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBExternallySideEffectFree combine CLBDExternallySideEffectFree(Set.empty)})"
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
            (LBExternallySideEffectFree combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "LBExternallySideEffectFree combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${LBExternallySideEffectFree combine LBImpureDueToSynchronization})"
        )
        assert(
            (LBExternallySideEffectFree combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "LBExternallySideEffectFree combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${LBExternallySideEffectFree combine LBImpureDueToHeapModification})"
        )
        assert(
            (LBExternallySideEffectFree combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "LBExternallySideEffectFree combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${LBExternallySideEffectFree combine LBImpureDueToFutureExtension})"
        )
        assert(
            (LBExternallySideEffectFree combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "LBExternallySideEffectFree combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${LBExternallySideEffectFree combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (LBExternallySideEffectFree combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "LBExternallySideEffectFree combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${LBExternallySideEffectFree combine LBImpureDueToUnknownProperty})"
        )

        assert(
            (LBDPure(Set.empty) combine LBDSideEffectFree(Set.empty)) == LBDSideEffectFree(Set.empty),
            "LBDPure(Set.empty) combine LBDSideEffectFree(Set.empty) was not LBDSideEffectFree(Set.empty)"+
                s" (was ${LBDPure(Set.empty) combine LBDSideEffectFree(Set.empty)})"
        )
        assert(
            (LBDPure(Set.empty) combine LBDExternallyPure(Set.empty)) == LBDExternallyPure(Set.empty),
            "LBDPure(Set.empty) combine LBDExternallyPure(Set.empty) was not LBDExternallyPure(Set.empty)"+
                s" (was ${LBDPure(Set.empty) combine LBDExternallyPure(Set.empty)})"
        )
        assert(
            (LBDPure(Set.empty) combine LBDExternallySideEffectFree(Set.empty)) == LBDExternallySideEffectFree(Set.empty),
            "LBDPure(Set.empty) combine LBDExternallySideEffectFree(Set.empty) was not LBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDPure(Set.empty) combine LBDExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (LBDPure(Set.empty) combine CPureWithoutAllocations) == CLBDPure(Set.empty),
            "LBDPure(Set.empty) combine CPureWithoutAllocations was not CLBDPure(Set.empty)"+
                s" (was ${LBDPure(Set.empty) combine CPureWithoutAllocations})"
        )
        assert(
            (LBDPure(Set.empty) combine CLBSideEffectFreeWithoutAllocations) == CLBDSideEffectFree(Set.empty),
            "LBDPure(Set.empty) combine CLBSideEffectFreeWithoutAllocations was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${LBDPure(Set.empty) combine CLBSideEffectFreeWithoutAllocations})"
        )
        assert(
            (LBDPure(Set.empty) combine CLBPure) == CLBDPure(Set.empty),
            "LBDPure(Set.empty) combine CLBPure was not CLBDPure(Set.empty)"+
                s" (was ${LBDPure(Set.empty) combine CLBPure})"
        )
        assert(
            (LBDPure(Set.empty) combine CLBSideEffectFree) == CLBDSideEffectFree(Set.empty),
            "LBDPure(Set.empty) combine CLBSideEffectFree was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${LBDPure(Set.empty) combine CLBSideEffectFree})"
        )
        assert(
            (LBDPure(Set.empty) combine CLBExternallyPure) == CLBDExternallyPure(Set.empty),
            "LBDPure(Set.empty) combine CLBExternallyPure was not CLBDExternallyPure(Set.empty)"+
                s" (was ${LBDPure(Set.empty) combine CLBExternallyPure})"
        )
        assert(
            (LBDPure(Set.empty) combine CLBExternallySideEffectFree) == CLBDExternallySideEffectFree(Set.empty),
            "LBDPure(Set.empty) combine CLBExternallySideEffectFree was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDPure(Set.empty) combine CLBExternallySideEffectFree})"
        )
        assert(
            (LBDPure(Set.empty) combine CLBDPure(Set.empty)) == CLBDPure(Set.empty),
            "LBDPure(Set.empty) combine CLBDPure(Set.empty) was not CLBDPure(Set.empty)"+
                s" (was ${LBDPure(Set.empty) combine CLBDPure(Set.empty)})"
        )
        assert(
            (LBDPure(Set.empty) combine CLBDSideEffectFree(Set.empty)) == CLBDSideEffectFree(Set.empty),
            "LBDPure(Set.empty) combine CLBDSideEffectFree(Set.empty) was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${LBDPure(Set.empty) combine CLBDSideEffectFree(Set.empty)})"
        )
        assert(
            (LBDPure(Set.empty) combine CLBDExternallyPure(Set.empty)) == CLBDExternallyPure(Set.empty),
            "LBDPure(Set.empty) combine CLBDExternallyPure(Set.empty) was not CLBDExternallyPure(Set.empty)"+
                s" (was ${LBDPure(Set.empty) combine CLBDExternallyPure(Set.empty)})"
        )
        assert(
            (LBDPure(Set.empty) combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBDPure(Set.empty) combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDPure(Set.empty) combine CLBDExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (LBDPure(Set.empty) combine MaybePure) == MaybePure,
            "LBDPure(Set.empty) combine MaybePure was not MaybePure"+
                s" (was ${LBDPure(Set.empty) combine MaybePure})"
        )
        assert(
            (LBDPure(Set.empty) combine LBImpure) == LBImpure,
            "LBDPure(Set.empty) combine LBImpure was not LBImpure"+
                s" (was ${LBDPure(Set.empty) combine LBImpure})"
        )
        assert(
            (LBDPure(Set.empty) combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "LBDPure(Set.empty) combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${LBDPure(Set.empty) combine LBImpureDueToSynchronization})"
        )
        assert(
            (LBDPure(Set.empty) combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "LBDPure(Set.empty) combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${LBDPure(Set.empty) combine LBImpureDueToHeapModification})"
        )
        assert(
            (LBDPure(Set.empty) combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "LBDPure(Set.empty) combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${LBDPure(Set.empty) combine LBImpureDueToFutureExtension})"
        )
        assert(
            (LBDPure(Set.empty) combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "LBDPure(Set.empty) combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${LBDPure(Set.empty) combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (LBDPure(Set.empty) combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "LBDPure(Set.empty) combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${LBDPure(Set.empty) combine LBImpureDueToUnknownProperty})"
        )

        assert(
            (LBDSideEffectFree(Set.empty) combine LBDExternallyPure(Set.empty)) == LBDExternallySideEffectFree(Set.empty),
            "LBDSideEffectFree(Set.empty) combine LBDExternallyPure(Set.empty) was not LBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDSideEffectFree(Set.empty) combine LBDExternallyPure(Set.empty)})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine LBDExternallySideEffectFree(Set.empty)) == LBDExternallySideEffectFree(Set.empty),
            "LBDSideEffectFree(Set.empty) combine LBDExternallySideEffectFree(Set.empty) was not LBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDSideEffectFree(Set.empty) combine LBDExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine CPureWithoutAllocations) == CLBDSideEffectFree(Set.empty),
            "LBDSideEffectFree(Set.empty) combine CPureWithoutAllocations was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${LBDSideEffectFree(Set.empty) combine CPureWithoutAllocations})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine CLBSideEffectFreeWithoutAllocations) == CLBDSideEffectFree(Set.empty),
            "LBDSideEffectFree(Set.empty) combine CLBSideEffectFreeWithoutAllocations was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${LBDSideEffectFree(Set.empty) combine CLBSideEffectFreeWithoutAllocations})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine CLBPure) == CLBDSideEffectFree(Set.empty),
            "LBDSideEffectFree(Set.empty) combine CLBPure was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${LBDSideEffectFree(Set.empty) combine CLBPure})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine CLBSideEffectFree) == CLBDSideEffectFree(Set.empty),
            "LBDSideEffectFree(Set.empty) combine CLBSideEffectFree was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${LBDSideEffectFree(Set.empty) combine CLBSideEffectFree})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine CLBExternallyPure) == CLBDExternallySideEffectFree(Set.empty),
            "LBDSideEffectFree(Set.empty) combine CLBExternallyPure was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDSideEffectFree(Set.empty) combine CLBExternallyPure})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine CLBExternallySideEffectFree) == CLBDExternallySideEffectFree(Set.empty),
            "LBDSideEffectFree(Set.empty) combine CLBExternallySideEffectFree was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDSideEffectFree(Set.empty) combine CLBExternallySideEffectFree})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine CLBDPure(Set.empty)) == CLBDSideEffectFree(Set.empty),
            "LBDSideEffectFree(Set.empty) combine CLBDPure(Set.empty) was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${LBDSideEffectFree(Set.empty) combine CLBDPure(Set.empty)})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine CLBDSideEffectFree(Set.empty)) == CLBDSideEffectFree(Set.empty),
            "LBDSideEffectFree(Set.empty) combine CLBDSideEffectFree(Set.empty) was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${LBDSideEffectFree(Set.empty) combine CLBDSideEffectFree(Set.empty)})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine CLBDExternallyPure(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBDSideEffectFree(Set.empty) combine CLBDExternallyPure(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDSideEffectFree(Set.empty) combine CLBDExternallyPure(Set.empty)})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBDSideEffectFree(Set.empty) combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDSideEffectFree(Set.empty) combine CLBDExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine MaybePure) == MaybePure,
            "LBDSideEffectFree(Set.empty) combine MaybePure was not MaybePure"+
                s" (was ${LBDSideEffectFree(Set.empty) combine MaybePure})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine LBImpure) == LBImpure,
            "LBDSideEffectFree(Set.empty) combine LBImpure was not LBImpure"+
                s" (was ${LBDSideEffectFree(Set.empty) combine LBImpure})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "LBDSideEffectFree(Set.empty) combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${LBDSideEffectFree(Set.empty) combine LBImpureDueToSynchronization})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "LBDSideEffectFree(Set.empty) combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${LBDSideEffectFree(Set.empty) combine LBImpureDueToHeapModification})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "LBDSideEffectFree(Set.empty) combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${LBDSideEffectFree(Set.empty) combine LBImpureDueToFutureExtension})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "LBDSideEffectFree(Set.empty) combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${LBDSideEffectFree(Set.empty) combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (LBDSideEffectFree(Set.empty) combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "LBDSideEffectFree(Set.empty) combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${LBDSideEffectFree(Set.empty) combine LBImpureDueToUnknownProperty})"
        )

        assert(
            (LBDExternallyPure(Set.empty) combine LBDExternallySideEffectFree(Set.empty)) == LBDExternallySideEffectFree(Set.empty),
            "LBDExternallyPure(Set.empty) combine LBDExternallySideEffectFree(Set.empty) was not LBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDExternallyPure(Set.empty) combine LBDExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (LBDExternallyPure(Set.empty) combine CPureWithoutAllocations) == CLBDExternallyPure(Set.empty),
            "LBDExternallyPure(Set.empty) combine CPureWithoutAllocations was not CLBDExternallyPure(Set.empty)"+
                s" (was ${LBDExternallyPure(Set.empty) combine CPureWithoutAllocations})"
        )
        assert(
            (LBDExternallyPure(Set.empty) combine CLBSideEffectFreeWithoutAllocations) == CLBDExternallySideEffectFree(Set.empty),
            "LBDExternallyPure(Set.empty) combine CLBSideEffectFreeWithoutAllocations was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDExternallyPure(Set.empty) combine CLBSideEffectFreeWithoutAllocations})"
        )
        assert(
            (LBDExternallyPure(Set.empty) combine CLBPure) == CLBDExternallyPure(Set.empty),
            "LBDExternallyPure(Set.empty) combine CLBPure was not CLBDExternallyPure(Set.empty)"+
                s" (was ${LBDExternallyPure(Set.empty) combine CLBPure})"
        )
        assert(
            (LBDExternallyPure(Set.empty) combine CLBSideEffectFree) == CLBDExternallySideEffectFree(Set.empty),
            "LBDExternallyPure(Set.empty) combine CLBSideEffectFree was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDExternallyPure(Set.empty) combine CLBSideEffectFree})"
        )
        assert(
            (LBDExternallyPure(Set.empty) combine CLBExternallyPure) == CLBDExternallyPure(Set.empty),
            "LBDExternallyPure(Set.empty) combine CLBExternallyPure was not CLBDExternallyPure(Set.empty)"+
                s" (was ${LBDExternallyPure(Set.empty) combine CLBExternallyPure})"
        )
        assert(
            (LBDExternallyPure(Set.empty) combine CLBExternallySideEffectFree) == CLBDExternallySideEffectFree(Set.empty),
            "LBDExternallyPure(Set.empty) combine CLBExternallySideEffectFree was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDExternallyPure(Set.empty) combine CLBExternallySideEffectFree})"
        )
        assert(
            (LBDExternallyPure(Set.empty) combine CLBDPure(Set.empty)) == CLBDExternallyPure(Set.empty),
            "LBDExternallyPure(Set.empty) combine CLBDPure(Set.empty) was not CLBDExternallyPure(Set.empty)"+
                s" (was ${LBDExternallyPure(Set.empty) combine CLBDPure(Set.empty)})"
        )
        assert(
            (LBDExternallyPure(Set.empty) combine CLBDSideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBDExternallyPure(Set.empty) combine CLBDSideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDExternallyPure(Set.empty) combine CLBDSideEffectFree(Set.empty)})"
        )
        assert(
            (LBDExternallyPure(Set.empty) combine CLBDExternallyPure(Set.empty)) == CLBDExternallyPure(Set.empty),
            "LBDExternallyPure(Set.empty) combine CLBDExternallyPure(Set.empty) was not CLBDExternallyPure(Set.empty)"+
                s" (was ${LBDExternallyPure(Set.empty) combine CLBDExternallyPure(Set.empty)})"
        )
        assert(
            (LBDExternallyPure(Set.empty) combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBDExternallyPure(Set.empty) combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDExternallyPure(Set.empty) combine CLBDExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (LBDExternallyPure(Set.empty) combine MaybePure) == MaybePure,
            "LBDExternallyPure(Set.empty) combine MaybePure was not MaybePure"+
                s" (was ${LBDExternallyPure(Set.empty) combine MaybePure})"
        )
        assert(
            (LBDExternallyPure(Set.empty) combine LBImpure) == LBImpure,
            "LBDExternallyPure(Set.empty) combine LBImpure was not LBImpure"+
                s" (was ${LBDExternallyPure(Set.empty) combine LBImpure})"
        )
        assert(
            (LBDExternallyPure(Set.empty) combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "LBDExternallyPure(Set.empty) combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${LBDExternallyPure(Set.empty) combine LBImpureDueToSynchronization})"
        )
        assert(
            (LBDExternallyPure(Set.empty) combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "LBDExternallyPure(Set.empty) combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${LBDExternallyPure(Set.empty) combine LBImpureDueToHeapModification})"
        )
        assert(
            (LBDExternallyPure(Set.empty) combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "LBDExternallyPure(Set.empty) combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${LBDExternallyPure(Set.empty) combine LBImpureDueToFutureExtension})"
        )
        assert(
            (LBDExternallyPure(Set.empty) combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "LBDExternallyPure(Set.empty) combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${LBDExternallyPure(Set.empty) combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (LBDExternallyPure(Set.empty) combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "LBDExternallyPure(Set.empty) combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${LBDExternallyPure(Set.empty) combine LBImpureDueToUnknownProperty})"
        )

        assert(
            (LBDExternallySideEffectFree(Set.empty) combine CPureWithoutAllocations) == CLBDExternallySideEffectFree(Set.empty),
            "LBDExternallySideEffectFree(Set.empty) combine CPureWithoutAllocations was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDExternallySideEffectFree(Set.empty) combine CPureWithoutAllocations})"
        )
        assert(
            (LBDExternallySideEffectFree(Set.empty) combine CLBSideEffectFreeWithoutAllocations) == CLBDExternallySideEffectFree(Set.empty),
            "LBDExternallySideEffectFree(Set.empty) combine CLBSideEffectFreeWithoutAllocations was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDExternallySideEffectFree(Set.empty) combine CLBSideEffectFreeWithoutAllocations})"
        )
        assert(
            (LBDExternallySideEffectFree(Set.empty) combine CLBPure) == CLBDExternallySideEffectFree(Set.empty),
            "LBDExternallySideEffectFree(Set.empty) combine CLBPure was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDExternallySideEffectFree(Set.empty) combine CLBPure})"
        )
        assert(
            (LBDExternallySideEffectFree(Set.empty) combine CLBSideEffectFree) == CLBDExternallySideEffectFree(Set.empty),
            "LBDExternallySideEffectFree(Set.empty) combine CLBSideEffectFree was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDExternallySideEffectFree(Set.empty) combine CLBSideEffectFree})"
        )
        assert(
            (LBDExternallySideEffectFree(Set.empty) combine CLBExternallyPure) == CLBDExternallySideEffectFree(Set.empty),
            "LBDExternallySideEffectFree(Set.empty) combine CLBExternallyPure was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDExternallySideEffectFree(Set.empty) combine CLBExternallyPure})"
        )
        assert(
            (LBDExternallySideEffectFree(Set.empty) combine CLBExternallySideEffectFree) == CLBDExternallySideEffectFree(Set.empty),
            "LBDExternallySideEffectFree(Set.empty) combine CLBExternallySideEffectFree was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDExternallySideEffectFree(Set.empty) combine CLBExternallySideEffectFree})"
        )
        assert(
            (LBDExternallySideEffectFree(Set.empty) combine CLBDPure(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBDExternallySideEffectFree(Set.empty) combine CLBDPure(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDExternallySideEffectFree(Set.empty) combine CLBDPure(Set.empty)})"
        )
        assert(
            (LBDExternallySideEffectFree(Set.empty) combine CLBDSideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBDExternallySideEffectFree(Set.empty) combine CLBDSideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDExternallySideEffectFree(Set.empty) combine CLBDSideEffectFree(Set.empty)})"
        )
        assert(
            (LBDExternallySideEffectFree(Set.empty) combine CLBDExternallyPure(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBDExternallySideEffectFree(Set.empty) combine CLBDExternallyPure(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDExternallySideEffectFree(Set.empty) combine CLBDExternallyPure(Set.empty)})"
        )
        assert(
            (LBDExternallySideEffectFree(Set.empty) combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "LBDExternallySideEffectFree(Set.empty) combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${LBDExternallySideEffectFree(Set.empty) combine CLBDExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (LBDExternallySideEffectFree(Set.empty) combine MaybePure) == MaybePure,
            "LBDExternallySideEffectFree(Set.empty) combine MaybePure was not MaybePure"+
                s" (was ${LBDExternallySideEffectFree(Set.empty) combine MaybePure})"
        )
        assert(
            (LBDExternallySideEffectFree(Set.empty) combine LBImpure) == LBImpure,
            "LBDExternallySideEffectFree(Set.empty) combine LBImpure was not LBImpure"+
                s" (was ${LBDExternallySideEffectFree(Set.empty) combine LBImpure})"
        )
        assert(
            (LBDExternallySideEffectFree(Set.empty) combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "LBDExternallySideEffectFree(Set.empty) combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${LBDExternallySideEffectFree(Set.empty) combine LBImpureDueToSynchronization})"
        )
        assert(
            (LBDExternallySideEffectFree(Set.empty) combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "LBDExternallySideEffectFree(Set.empty) combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${LBDExternallySideEffectFree(Set.empty) combine LBImpureDueToHeapModification})"
        )
        assert(
            (LBDExternallySideEffectFree(Set.empty) combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "LBDExternallySideEffectFree(Set.empty) combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${LBDExternallySideEffectFree(Set.empty) combine LBImpureDueToFutureExtension})"
        )
        assert(
            (LBDExternallySideEffectFree(Set.empty) combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "LBDExternallySideEffectFree(Set.empty) combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${LBDExternallySideEffectFree(Set.empty) combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (LBDExternallySideEffectFree(Set.empty) combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "LBDExternallySideEffectFree(Set.empty) combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${LBDExternallySideEffectFree(Set.empty) combine LBImpureDueToUnknownProperty})"
        )
    }

    it should "return the correct purity levels for conditional levels" in {
        assert(
            (CPureWithoutAllocations combine CLBSideEffectFreeWithoutAllocations) == CLBSideEffectFreeWithoutAllocations,
            "CPureWithoutAllocations combine CLBSideEffectFreeWithoutAllocations was not CLBSideEffectFreeWithoutAllocations"+
                s" (was ${CPureWithoutAllocations combine CLBSideEffectFreeWithoutAllocations})"
        )
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
            (CPureWithoutAllocations combine CLBDPure(Set.empty)) == CLBDPure(Set.empty),
            "CPureWithoutAllocations combine CLBDPure(Set.empty) was not CLBDPure(Set.empty)"+
                s" (was ${CPureWithoutAllocations combine CLBDPure(Set.empty)})"
        )
        assert(
            (CPureWithoutAllocations combine CLBDSideEffectFree(Set.empty)) == CLBDSideEffectFree(Set.empty),
            "CPureWithoutAllocations combine CLBDSideEffectFree(Set.empty) was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${CPureWithoutAllocations combine CLBDSideEffectFree(Set.empty)})"
        )
        assert(
            (CPureWithoutAllocations combine CLBDExternallyPure(Set.empty)) == CLBDExternallyPure(Set.empty),
            "CPureWithoutAllocations combine CLBDExternallyPure(Set.empty) was not CLBDExternallyPure(Set.empty)"+
                s" (was ${CPureWithoutAllocations combine CLBDExternallyPure(Set.empty)})"
        )
        assert(
            (CPureWithoutAllocations combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "CPureWithoutAllocations combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${CPureWithoutAllocations combine CLBDExternallySideEffectFree(Set.empty)})"
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
            (CPureWithoutAllocations combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "CPureWithoutAllocations combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${CPureWithoutAllocations combine LBImpureDueToSynchronization})"
        )
        assert(
            (CPureWithoutAllocations combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "CPureWithoutAllocations combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${CPureWithoutAllocations combine LBImpureDueToHeapModification})"
        )
        assert(
            (CPureWithoutAllocations combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "CPureWithoutAllocations combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${CPureWithoutAllocations combine LBImpureDueToFutureExtension})"
        )
        assert(
            (CPureWithoutAllocations combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "CPureWithoutAllocations combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${CPureWithoutAllocations combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (CPureWithoutAllocations combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "CPureWithoutAllocations combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${CPureWithoutAllocations combine LBImpureDueToUnknownProperty})"
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
            (CLBSideEffectFreeWithoutAllocations combine CLBDPure(Set.empty)) == CLBDSideEffectFree(Set.empty),
            "CLBSideEffectFreeWithoutAllocations combine CLBDPure(Set.empty) was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine CLBDPure(Set.empty)})"
        )
        assert(
            (CLBSideEffectFreeWithoutAllocations combine CLBDSideEffectFree(Set.empty)) == CLBDSideEffectFree(Set.empty),
            "CLBSideEffectFreeWithoutAllocations combine CLBDSideEffectFree(Set.empty) was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine CLBDSideEffectFree(Set.empty)})"
        )
        assert(
            (CLBSideEffectFreeWithoutAllocations combine CLBDExternallyPure(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "CLBSideEffectFreeWithoutAllocations combine CLBDExternallyPure(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine CLBDExternallyPure(Set.empty)})"
        )
        assert(
            (CLBSideEffectFreeWithoutAllocations combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "CLBSideEffectFreeWithoutAllocations combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine CLBDExternallySideEffectFree(Set.empty)})"
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
            (CLBSideEffectFreeWithoutAllocations combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "CLBSideEffectFreeWithoutAllocations combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine LBImpureDueToSynchronization})"
        )
        assert(
            (CLBSideEffectFreeWithoutAllocations combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "CLBSideEffectFreeWithoutAllocations combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine LBImpureDueToHeapModification})"
        )
        assert(
            (CLBSideEffectFreeWithoutAllocations combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "CLBSideEffectFreeWithoutAllocations combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine LBImpureDueToFutureExtension})"
        )
        assert(
            (CLBSideEffectFreeWithoutAllocations combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "CLBSideEffectFreeWithoutAllocations combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (CLBSideEffectFreeWithoutAllocations combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "CLBSideEffectFreeWithoutAllocations combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${CLBSideEffectFreeWithoutAllocations combine LBImpureDueToUnknownProperty})"
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
            (CLBPure combine CLBDPure(Set.empty)) == CLBDPure(Set.empty),
            "CLBPure combine CLBDPure(Set.empty) was not CLBDPure(Set.empty)"+
                s" (was ${CLBPure combine CLBDPure(Set.empty)})"
        )
        assert(
            (CLBPure combine CLBDSideEffectFree(Set.empty)) == CLBDSideEffectFree(Set.empty),
            "CLBPure combine CLBDSideEffectFree(Set.empty) was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${CLBPure combine CLBDSideEffectFree(Set.empty)})"
        )
        assert(
            (CLBPure combine CLBDExternallyPure(Set.empty)) == CLBDExternallyPure(Set.empty),
            "CLBPure combine CLBDExternallyPure(Set.empty) was not CLBDExternallyPure(Set.empty)"+
                s" (was ${CLBPure combine CLBDExternallyPure(Set.empty)})"
        )
        assert(
            (CLBPure combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "CLBPure combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${CLBPure combine CLBDExternallySideEffectFree(Set.empty)})"
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
            (CLBPure combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "CLBPure combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${CLBPure combine LBImpureDueToSynchronization})"
        )
        assert(
            (CLBPure combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "CLBPure combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${CLBPure combine LBImpureDueToHeapModification})"
        )
        assert(
            (CLBPure combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "CLBPure combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${CLBPure combine LBImpureDueToFutureExtension})"
        )
        assert(
            (CLBPure combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "CLBPure combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${CLBPure combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (CLBPure combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "CLBPure combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${CLBPure combine LBImpureDueToUnknownProperty})"
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
            (CLBSideEffectFree combine CLBDPure(Set.empty)) == CLBDSideEffectFree(Set.empty),
            "CLBSideEffectFree combine CLBDPure(Set.empty) was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${CLBSideEffectFree combine CLBDPure(Set.empty)})"
        )
        assert(
            (CLBSideEffectFree combine CLBDSideEffectFree(Set.empty)) == CLBDSideEffectFree(Set.empty),
            "CLBSideEffectFree combine CLBDSideEffectFree(Set.empty) was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${CLBSideEffectFree combine CLBDSideEffectFree(Set.empty)})"
        )
        assert(
            (CLBSideEffectFree combine CLBDExternallyPure(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "CLBSideEffectFree combine CLBDExternallyPure(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${CLBSideEffectFree combine CLBDExternallyPure(Set.empty)})"
        )
        assert(
            (CLBSideEffectFree combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "CLBSideEffectFree combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${CLBSideEffectFree combine CLBDExternallySideEffectFree(Set.empty)})"
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
            (CLBSideEffectFree combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "CLBSideEffectFree combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${CLBSideEffectFree combine LBImpureDueToSynchronization})"
        )
        assert(
            (CLBSideEffectFree combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "CLBSideEffectFree combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${CLBSideEffectFree combine LBImpureDueToHeapModification})"
        )
        assert(
            (CLBSideEffectFree combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "CLBSideEffectFree combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${CLBSideEffectFree combine LBImpureDueToFutureExtension})"
        )
        assert(
            (CLBSideEffectFree combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "CLBSideEffectFree combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${CLBSideEffectFree combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (CLBSideEffectFree combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "CLBSideEffectFree combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${CLBSideEffectFree combine LBImpureDueToUnknownProperty})"
        )

        assert(
            (CLBExternallyPure combine CLBExternallySideEffectFree) == CLBExternallySideEffectFree,
            "CLBExternallyPure combine CLBExternallySideEffectFree was not CLBExternallySideEffectFree"+
                s" (was ${CLBExternallyPure combine CLBExternallySideEffectFree})"
        )
        assert(
            (CLBExternallyPure combine CLBDPure(Set.empty)) == CLBDExternallyPure(Set.empty),
            "CLBExternallyPure combine CLBDPure(Set.empty) was not CLBDExternallyPure(Set.empty)"+
                s" (was ${CLBExternallyPure combine CLBDPure(Set.empty)})"
        )
        assert(
            (CLBExternallyPure combine CLBDSideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "CLBExternallyPure combine CLBDSideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${CLBExternallyPure combine CLBDSideEffectFree(Set.empty)})"
        )
        assert(
            (CLBExternallyPure combine CLBDExternallyPure(Set.empty)) == CLBDExternallyPure(Set.empty),
            "CLBExternallyPure combine CLBDExternallyPure(Set.empty) was not CLBDExternallyPure(Set.empty)"+
                s" (was ${CLBExternallyPure combine CLBDExternallyPure(Set.empty)})"
        )
        assert(
            (CLBExternallyPure combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "CLBExternallyPure combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${CLBExternallyPure combine CLBDExternallySideEffectFree(Set.empty)})"
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
            (CLBExternallyPure combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "CLBExternallyPure combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${CLBExternallyPure combine LBImpureDueToSynchronization})"
        )
        assert(
            (CLBExternallyPure combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "CLBExternallyPure combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${CLBExternallyPure combine LBImpureDueToHeapModification})"
        )
        assert(
            (CLBExternallyPure combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "CLBExternallyPure combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${CLBExternallyPure combine LBImpureDueToFutureExtension})"
        )
        assert(
            (CLBExternallyPure combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "CLBExternallyPure combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${CLBExternallyPure combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (CLBExternallyPure combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "CLBExternallyPure combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${CLBExternallyPure combine LBImpureDueToUnknownProperty})"
        )

        assert(
            (CLBExternallySideEffectFree combine CLBDPure(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "CLBExternallySideEffectFree combine CLBDPure(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${CLBExternallySideEffectFree combine CLBDPure(Set.empty)})"
        )
        assert(
            (CLBExternallySideEffectFree combine CLBDSideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "CLBExternallySideEffectFree combine CLBDSideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${CLBExternallySideEffectFree combine CLBDSideEffectFree(Set.empty)})"
        )
        assert(
            (CLBExternallySideEffectFree combine CLBDExternallyPure(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "CLBExternallySideEffectFree combine CLBDExternallyPure(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${CLBExternallySideEffectFree combine CLBDExternallyPure(Set.empty)})"
        )
        assert(
            (CLBExternallySideEffectFree combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "CLBExternallySideEffectFree combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${CLBExternallySideEffectFree combine CLBDExternallySideEffectFree(Set.empty)})"
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
            (CLBExternallySideEffectFree combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "CLBExternallySideEffectFree combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${CLBExternallySideEffectFree combine LBImpureDueToSynchronization})"
        )
        assert(
            (CLBExternallySideEffectFree combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "CLBExternallySideEffectFree combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${CLBExternallySideEffectFree combine LBImpureDueToHeapModification})"
        )
        assert(
            (CLBExternallySideEffectFree combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "CLBExternallySideEffectFree combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${CLBExternallySideEffectFree combine LBImpureDueToFutureExtension})"
        )
        assert(
            (CLBExternallySideEffectFree combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "CLBExternallySideEffectFree combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${CLBExternallySideEffectFree combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (CLBExternallySideEffectFree combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "CLBExternallySideEffectFree combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${CLBExternallySideEffectFree combine LBImpureDueToUnknownProperty})"
        )

        assert(
            (CLBDPure(Set.empty) combine CLBDSideEffectFree(Set.empty)) == CLBDSideEffectFree(Set.empty),
            "CLBDPure(Set.empty) combine CLBDSideEffectFree(Set.empty) was not CLBDSideEffectFree(Set.empty)"+
                s" (was ${CLBDPure(Set.empty) combine CLBDSideEffectFree(Set.empty)})"
        )
        assert(
            (CLBDPure(Set.empty) combine CLBDExternallyPure(Set.empty)) == CLBDExternallyPure(Set.empty),
            "CLBDPure(Set.empty) combine CLBDExternallyPure(Set.empty) was not CLBDExternallyPure(Set.empty)"+
                s" (was ${CLBDPure(Set.empty) combine CLBDExternallyPure(Set.empty)})"
        )
        assert(
            (CLBDPure(Set.empty) combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "CLBDPure(Set.empty) combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${CLBDPure(Set.empty) combine CLBDExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (CLBDPure(Set.empty) combine MaybePure) == MaybePure,
            "CLBDPure(Set.empty) combine MaybePure was not MaybePure"+
                s" (was ${CLBDPure(Set.empty) combine MaybePure})"
        )
        assert(
            (CLBDPure(Set.empty) combine LBImpure) == LBImpure,
            "CLBDPure(Set.empty) combine LBImpure was not LBImpure"+
                s" (was ${CLBDPure(Set.empty) combine LBImpure})"
        )
        assert(
            (CLBDPure(Set.empty) combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "CLBDPure(Set.empty) combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${CLBDPure(Set.empty) combine LBImpureDueToSynchronization})"
        )
        assert(
            (CLBDPure(Set.empty) combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "CLBDPure(Set.empty) combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${CLBDPure(Set.empty) combine LBImpureDueToHeapModification})"
        )
        assert(
            (CLBDPure(Set.empty) combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "CLBDPure(Set.empty) combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${CLBDPure(Set.empty) combine LBImpureDueToFutureExtension})"
        )
        assert(
            (CLBDPure(Set.empty) combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "CLBDPure(Set.empty) combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${CLBDPure(Set.empty) combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (CLBDPure(Set.empty) combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "CLBDPure(Set.empty) combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${CLBDPure(Set.empty) combine LBImpureDueToUnknownProperty})"
        )

        assert(
            (CLBDSideEffectFree(Set.empty) combine CLBDExternallyPure(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "CLBDSideEffectFree(Set.empty) combine CLBDExternallyPure(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${CLBDSideEffectFree(Set.empty) combine CLBDExternallyPure(Set.empty)})"
        )
        assert(
            (CLBDSideEffectFree(Set.empty) combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "CLBDSideEffectFree(Set.empty) combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${CLBDSideEffectFree(Set.empty) combine CLBDExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (CLBDSideEffectFree(Set.empty) combine MaybePure) == MaybePure,
            "CLBDSideEffectFree(Set.empty) combine MaybePure was not MaybePure"+
                s" (was ${CLBDSideEffectFree(Set.empty) combine MaybePure})"
        )
        assert(
            (CLBDSideEffectFree(Set.empty) combine LBImpure) == LBImpure,
            "CLBDSideEffectFree(Set.empty) combine LBImpure was not LBImpure"+
                s" (was ${CLBDSideEffectFree(Set.empty) combine LBImpure})"
        )
        assert(
            (CLBDSideEffectFree(Set.empty) combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "CLBDSideEffectFree(Set.empty) combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${CLBDSideEffectFree(Set.empty) combine LBImpureDueToSynchronization})"
        )
        assert(
            (CLBDSideEffectFree(Set.empty) combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "CLBDSideEffectFree(Set.empty) combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${CLBDSideEffectFree(Set.empty) combine LBImpureDueToHeapModification})"
        )
        assert(
            (CLBDSideEffectFree(Set.empty) combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "CLBDSideEffectFree(Set.empty) combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${CLBDSideEffectFree(Set.empty) combine LBImpureDueToFutureExtension})"
        )
        assert(
            (CLBDSideEffectFree(Set.empty) combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "CLBDSideEffectFree(Set.empty) combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${CLBDSideEffectFree(Set.empty) combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (CLBDSideEffectFree(Set.empty) combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "CLBDSideEffectFree(Set.empty) combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${CLBDSideEffectFree(Set.empty) combine LBImpureDueToUnknownProperty})"
        )

        assert(
            (CLBDExternallyPure(Set.empty) combine CLBDExternallySideEffectFree(Set.empty)) == CLBDExternallySideEffectFree(Set.empty),
            "CLBDExternallyPure(Set.empty) combine CLBDExternallySideEffectFree(Set.empty) was not CLBDExternallySideEffectFree(Set.empty)"+
                s" (was ${CLBDExternallyPure(Set.empty) combine CLBDExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (CLBDExternallyPure(Set.empty) combine MaybePure) == MaybePure,
            "CLBDExternallyPure(Set.empty) combine MaybePure was not MaybePure"+
                s" (was ${CLBDExternallyPure(Set.empty) combine MaybePure})"
        )
        assert(
            (CLBDExternallyPure(Set.empty) combine LBImpure) == LBImpure,
            "CLBDExternallyPure(Set.empty) combine LBImpure was not LBImpure"+
                s" (was ${CLBDExternallyPure(Set.empty) combine LBImpure})"
        )
        assert(
            (CLBDExternallyPure(Set.empty) combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "CLBDExternallyPure(Set.empty) combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${CLBDExternallyPure(Set.empty) combine LBImpureDueToSynchronization})"
        )
        assert(
            (CLBDExternallyPure(Set.empty) combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "CLBDExternallyPure(Set.empty) combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${CLBDExternallyPure(Set.empty) combine LBImpureDueToHeapModification})"
        )
        assert(
            (CLBDExternallyPure(Set.empty) combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "CLBDExternallyPure(Set.empty) combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${CLBDExternallyPure(Set.empty) combine LBImpureDueToFutureExtension})"
        )
        assert(
            (CLBDExternallyPure(Set.empty) combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "CLBDExternallyPure(Set.empty) combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${CLBDExternallyPure(Set.empty) combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (CLBDExternallyPure(Set.empty) combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "CLBDExternallyPure(Set.empty) combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${CLBDExternallyPure(Set.empty) combine LBImpureDueToUnknownProperty})"
        )

        assert(
            (CLBDExternallySideEffectFree(Set.empty) combine MaybePure) == MaybePure,
            "CLBDExternallySideEffectFree(Set.empty) combine MaybePure was not MaybePure"+
                s" (was ${CLBDExternallySideEffectFree(Set.empty) combine MaybePure})"
        )
        assert(
            (CLBDExternallySideEffectFree(Set.empty) combine LBImpure) == LBImpure,
            "CLBDExternallySideEffectFree(Set.empty) combine LBImpure was not LBImpure"+
                s" (was ${CLBDExternallySideEffectFree(Set.empty) combine LBImpure})"
        )
        assert(
            (CLBDExternallySideEffectFree(Set.empty) combine LBImpureDueToSynchronization) == LBImpureDueToSynchronization,
            "CLBDExternallySideEffectFree(Set.empty) combine LBImpureDueToSynchronization was not LBImpureDueToSynchronization"+
                s" (was ${CLBDExternallySideEffectFree(Set.empty) combine LBImpureDueToSynchronization})"
        )
        assert(
            (CLBDExternallySideEffectFree(Set.empty) combine LBImpureDueToHeapModification) == LBImpureDueToHeapModification,
            "CLBDExternallySideEffectFree(Set.empty) combine LBImpureDueToHeapModification was not LBImpureDueToHeapModification"+
                s" (was ${CLBDExternallySideEffectFree(Set.empty) combine LBImpureDueToHeapModification})"
        )
        assert(
            (CLBDExternallySideEffectFree(Set.empty) combine LBImpureDueToFutureExtension) == LBImpureDueToFutureExtension,
            "CLBDExternallySideEffectFree(Set.empty) combine LBImpureDueToFutureExtension was not LBImpureDueToFutureExtension"+
                s" (was ${CLBDExternallySideEffectFree(Set.empty) combine LBImpureDueToFutureExtension})"
        )
        assert(
            (CLBDExternallySideEffectFree(Set.empty) combine LBImpureDueToUnknownEntity) == LBImpureDueToUnknownEntity,
            "CLBDExternallySideEffectFree(Set.empty) combine LBImpureDueToUnknownEntity was not LBImpureDueToUnknownEntity"+
                s" (was ${CLBDExternallySideEffectFree(Set.empty) combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (CLBDExternallySideEffectFree(Set.empty) combine LBImpureDueToUnknownProperty) == LBImpureDueToUnknownProperty,
            "CLBDExternallySideEffectFree(Set.empty) combine LBImpureDueToUnknownProperty was not LBImpureDueToUnknownProperty"+
                s" (was ${CLBDExternallySideEffectFree(Set.empty) combine LBImpureDueToUnknownProperty})"
        )

        assert(
            (MaybePure combine LBImpure) == MaybePure,
            "MaybePure combine LBImpure was not MaybePure"+
                s" (was ${MaybePure combine LBImpure})"
        )
        assert(
            (MaybePure combine LBImpureDueToSynchronization) == MaybePure,
            "MaybePure combine LBImpureDueToSynchronization was not MaybePure"+
                s" (was ${MaybePure combine LBImpureDueToSynchronization})"
        )
        assert(
            (MaybePure combine LBImpureDueToHeapModification) == MaybePure,
            "MaybePure combine LBImpureDueToHeapModification was not MaybePure"+
                s" (was ${MaybePure combine LBImpureDueToHeapModification})"
        )
        assert(
            (MaybePure combine LBImpureDueToFutureExtension) == MaybePure,
            "MaybePure combine LBImpureDueToFutureExtension was not MaybePure"+
                s" (was ${MaybePure combine LBImpureDueToFutureExtension})"
        )
        assert(
            (MaybePure combine LBImpureDueToUnknownEntity) == MaybePure,
            "MaybePure combine LBImpureDueToUnknownEntity was not MaybePure"+
                s" (was ${MaybePure combine LBImpureDueToUnknownEntity})"
        )
        assert(
            (MaybePure combine LBImpureDueToUnknownProperty) == MaybePure,
            "MaybePure combine LBImpureDueToUnknownProperty was not MaybePure"+
                s" (was ${MaybePure combine LBImpureDueToUnknownProperty})"
        )
    }
}
