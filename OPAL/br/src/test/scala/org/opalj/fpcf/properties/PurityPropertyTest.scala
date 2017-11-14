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
import org.opalj.fpcf.properties.DomainSpecific.UsesLogging
import org.opalj.fpcf.properties.DomainSpecific.UsesSystemOutOrErr
import org.opalj.fpcf.properties.DomainSpecific.RaisesExceptions
import org.opalj.fpcf.properties.ImpureBase.Impure
import org.opalj.fpcf.properties.ImpureBase.ImpureDueToSynchronization
import org.opalj.fpcf.properties.ImpureBase.ImpureDueToHeapModification
import org.opalj.fpcf.properties.ImpureBase.ImpureDueToFutureExtension
import org.opalj.fpcf.properties.ImpureBase.ImpureDueToUnknownProperty
import org.opalj.fpcf.properties.ImpureBase.ImpureDueToUnknownEntity
import org.opalj.fpcf.properties.ImpureBase.ImpureDueToImpureCall
import org.scalatest.Matchers
import org.scalatest.FlatSpec

/**
 * Tests the [[Purity]] property, especially correctness of the meet operator.
 *
 * @author Dominik Helm
 */
class PurityPropertyTest extends FlatSpec with Matchers {
    val allPurities: List[Purity] = List(
        PureWithoutAllocations, SideEffectFreeWithoutAllocations, Pure, SideEffectFree,
        ExternallyPure, ExternallySideEffectFree, DomainSpecificPure(Set.empty),
        DomainSpecificSideEffectFree(Set.empty), DomainSpecificExternallyPure(Set.empty),
        DomainSpecificExternallySideEffectFree(Set.empty), ConditionallyPureWithoutAllocations,
        ConditionallySideEffectFreeWithoutAllocations, ConditionallyPure,
        ConditionallySideEffectFree, ConditionallyExternallyPure,
        ConditionallyExternallySideEffectFree, ConditionallyDomainSpecificPure(Set.empty),
        ConditionallyDomainSpecificSideEffectFree(Set.empty),
        ConditionallyDomainSpecificExternallyPure(Set.empty),
        ConditionallyDomainSpecificExternallySideEffectFree(Set.empty), MaybePure,
        Impure, ImpureDueToSynchronization, ImpureDueToHeapModification, ImpureDueToFutureExtension,
        ImpureDueToImpureCall, ImpureDueToUnknownEntity, ImpureDueToUnknownProperty
    )

    val doesntModifiyReceiver: Set[Purity] = Set(
        PureWithoutAllocations, SideEffectFreeWithoutAllocations, Pure, SideEffectFree,
        DomainSpecificPure(Set.empty), DomainSpecificSideEffectFree(Set.empty),
        ConditionallyPureWithoutAllocations, ConditionallySideEffectFreeWithoutAllocations,
        ConditionallyPure, ConditionallySideEffectFree, ConditionallyDomainSpecificPure(Set.empty),
        ConditionallyDomainSpecificSideEffectFree(Set.empty)
    )

    val conditional: Set[Purity] = Set(
        ConditionallyPureWithoutAllocations, ConditionallySideEffectFreeWithoutAllocations,
        ConditionallyPure, ConditionallySideEffectFree, ConditionallyExternallyPure,
        ConditionallyExternallySideEffectFree, ConditionallyDomainSpecificPure(Set.empty),
        ConditionallyDomainSpecificSideEffectFree(Set.empty),
        ConditionallyDomainSpecificExternallyPure(Set.empty),
        ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)
    )

    "purity levels" should "have the right properties" in {
        val withoutAllocations: Set[Purity] = Set(
            PureWithoutAllocations, SideEffectFreeWithoutAllocations,
            ConditionallyPureWithoutAllocations, ConditionallySideEffectFreeWithoutAllocations
        )

        for (prop ← allPurities) {
            assert(
                prop.hasAllocations != withoutAllocations.contains(prop),
                s"$prop.hasAllocations was ${prop.hasAllocations}"
            )
        }

        val deterministic: Set[Purity] = Set(
            PureWithoutAllocations, Pure, ExternallyPure, DomainSpecificPure(Set.empty),
            DomainSpecificExternallyPure(Set.empty), ConditionallyPureWithoutAllocations,
            ConditionallyPure, ConditionallyExternallyPure, ConditionallyDomainSpecificPure(Set.empty),
            ConditionallyDomainSpecificExternallyPure(Set.empty)
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
            PureWithoutAllocations, SideEffectFreeWithoutAllocations, Pure, SideEffectFree,
            ExternallyPure, ExternallySideEffectFree, ConditionallyPureWithoutAllocations,
            ConditionallySideEffectFreeWithoutAllocations, ConditionallyPure,
            ConditionallySideEffectFree, ConditionallyExternallyPure,
            ConditionallyExternallySideEffectFree
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
        for {prop ← allPurities} {
            if(doesntModifiyReceiver.contains(prop))
                assert(
                    prop.withoutExternal == prop,
                    s"$prop.withoutExternal modified $prop (was ${prop.withoutExternal})"
                )
            else
                assert(
                    prop.withoutExternal.flags == (prop.flags & ~Purity.MODIFIES_RECEIVER),
                    s"$prop.withoutExternal was incorrect (was ${prop.withoutExternal})"
                )
            if(conditional.contains(prop))
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
            DomainSpecificPure, DomainSpecificSideEffectFree, DomainSpecificExternallyPure,
            DomainSpecificExternallySideEffectFree, ConditionallyDomainSpecificPure,
            ConditionallyDomainSpecificSideEffectFree, ConditionallyDomainSpecificExternallyPure,
            ConditionallyDomainSpecificExternallySideEffectFree
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
            val result = prop1 meet prop2
            assert(
                result.reasons == (reasons1 | reasons2),
                s"$prop1 meet $prop2 did not have the union of both reasons (was $result)"
            )
            assert(
                result.getClass == (p1(Set.empty) meet p2(Set.empty)).getClass,
                s"$prop1 meet $prop2 was not the correct purity level (was $result)"
            )
        }
    }

    "the meet operator" should "be reflexive and symmetric" in {
        for (prop1 ← allPurities) {
            assert((prop1 meet prop1) == prop1, s"meet was not reflexive for $prop1")
            for (prop2 ← allPurities) {
                val meet12 = prop1 meet prop2
                val meet21 = prop2 meet prop1
                (prop1, prop2) match {
                    case (ImpureBase(_), ImpureBase(_)) ⇒
                        assert(
                            meet12 == prop1 || meet12 == prop2,
                            s"$prop1 meet $prop2 was not one of the impure reasons (was $meet12)"
                        )
                    case _ ⇒
                        assert(
                            meet12 == meet21,
                            s"$prop1 meet $prop2 was not symmetric (was $meet12 / $meet21)"
                        )
                }
            }
        }
    }

    it should "return the correct purity levels for unconditional levels" in {
        for (prop ← allPurities) {
            val result = PureWithoutAllocations meet prop
            assert(
                result == prop,
                s"PureWithoutAllocations meet $prop was not $prop (was $result)"
            )
        }

        assert(
            (SideEffectFreeWithoutAllocations meet Pure) == SideEffectFree,
            "SideEffectFreeWithoutAllocations meet Pure was not SideEffectFree" +
                s" (was ${SideEffectFreeWithoutAllocations meet Pure})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet SideEffectFree) == SideEffectFree,
            "SideEffectFreeWithoutAllocations meet SideEffectFree was not SideEffectFree" +
                s" (was ${SideEffectFreeWithoutAllocations meet SideEffectFree})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet ExternallyPure) == ExternallySideEffectFree,
            "SideEffectFreeWithoutAllocations meet ExternallyPure was not ExternallySideEffectFree" +
                s" (was ${SideEffectFreeWithoutAllocations meet ExternallyPure})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet ExternallySideEffectFree) == ExternallySideEffectFree,
            "SideEffectFreeWithoutAllocations meet ExternallySideEffectFree was not ExternallySideEffectFree" +
                s" (was ${SideEffectFreeWithoutAllocations meet ExternallySideEffectFree})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet DomainSpecificPure(Set.empty)) == DomainSpecificSideEffectFree(Set.empty),
            "SideEffectFreeWithoutAllocations meet DomainSpecificPure(Set.empty) was not DomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${SideEffectFreeWithoutAllocations meet DomainSpecificPure(Set.empty)})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet DomainSpecificSideEffectFree(Set.empty)) == DomainSpecificSideEffectFree(Set.empty),
            "SideEffectFreeWithoutAllocations meet DomainSpecificSideEffectFree(Set.empty) was not DomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${SideEffectFreeWithoutAllocations meet DomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet DomainSpecificExternallyPure(Set.empty)) == DomainSpecificExternallySideEffectFree(Set.empty),
            "SideEffectFreeWithoutAllocations meet DomainSpecificExternallyPure(Set.empty) was not DomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${SideEffectFreeWithoutAllocations meet DomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet DomainSpecificExternallySideEffectFree(Set.empty)) == DomainSpecificExternallySideEffectFree(Set.empty),
            "SideEffectFreeWithoutAllocations meet DomainSpecificExternallySideEffectFree(Set.empty) was not DomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${SideEffectFreeWithoutAllocations meet DomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet ConditionallyPureWithoutAllocations) == ConditionallySideEffectFreeWithoutAllocations,
            "SideEffectFreeWithoutAllocations meet ConditionallyPureWithoutAllocations was not ConditionallySideEffectFreeWithoutAllocations" +
                s" (was ${SideEffectFreeWithoutAllocations meet ConditionallyPureWithoutAllocations})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet ConditionallySideEffectFreeWithoutAllocations) == ConditionallySideEffectFreeWithoutAllocations,
            "SideEffectFreeWithoutAllocations meet ConditionallySideEffectFreeWithoutAllocations was not ConditionallySideEffectFreeWithoutAllocations" +
                s" (was ${SideEffectFreeWithoutAllocations meet ConditionallySideEffectFreeWithoutAllocations})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet ConditionallyPure) == ConditionallySideEffectFree,
            "SideEffectFreeWithoutAllocations meet ConditionallyPure was not ConditionallySideEffectFree" +
                s" (was ${SideEffectFreeWithoutAllocations meet ConditionallyPure})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet ConditionallySideEffectFree) == ConditionallySideEffectFree,
            "SideEffectFreeWithoutAllocations meet ConditionallySideEffectFree was not ConditionallySideEffectFree" +
                s" (was ${SideEffectFreeWithoutAllocations meet ConditionallySideEffectFree})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet ConditionallyExternallyPure) == ConditionallyExternallySideEffectFree,
            "SideEffectFreeWithoutAllocations meet ConditionallyExternallyPure was not ConditionallyExternallySideEffectFree" +
                s" (was ${SideEffectFreeWithoutAllocations meet ConditionallyExternallyPure})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet ConditionallyExternallySideEffectFree) == ConditionallyExternallySideEffectFree,
            "SideEffectFreeWithoutAllocations meet ConditionallyExternallySideEffectFree was not ConditionallyExternallySideEffectFree" +
                s" (was ${SideEffectFreeWithoutAllocations meet ConditionallyExternallySideEffectFree})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificPure(Set.empty)) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "SideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificPure(Set.empty) was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${SideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificPure(Set.empty)})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificSideEffectFree(Set.empty)) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "SideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificSideEffectFree(Set.empty) was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${SideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificExternallyPure(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "SideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificExternallyPure(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${SideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "SideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${SideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet MaybePure) == MaybePure,
            "SideEffectFreeWithoutAllocations meet MaybePure was not MaybePure" +
                s" (was ${SideEffectFreeWithoutAllocations meet MaybePure})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet Impure) == Impure,
            "SideEffectFreeWithoutAllocations meet Impure was not Impure" +
                s" (was ${SideEffectFreeWithoutAllocations meet Impure})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "SideEffectFreeWithoutAllocations meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${SideEffectFreeWithoutAllocations meet ImpureDueToSynchronization})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "SideEffectFreeWithoutAllocations meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${SideEffectFreeWithoutAllocations meet ImpureDueToHeapModification})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "SideEffectFreeWithoutAllocations meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${SideEffectFreeWithoutAllocations meet ImpureDueToFutureExtension})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "SideEffectFreeWithoutAllocations meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${SideEffectFreeWithoutAllocations meet ImpureDueToUnknownEntity})"
        )
        assert(
            (SideEffectFreeWithoutAllocations meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "SideEffectFreeWithoutAllocations meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${SideEffectFreeWithoutAllocations meet ImpureDueToUnknownProperty})"
        )

        assert(
            (Pure meet SideEffectFree) == SideEffectFree,
            "Pure meet SideEffectFree was not SideEffectFree" +
                s" (was ${Pure meet SideEffectFree})"
        )
        assert(
            (Pure meet ExternallyPure) == ExternallyPure,
            "Pure meet ExternallyPure was not ExternallyPure" +
                s" (was ${Pure meet ExternallyPure})"
        )
        assert(
            (Pure meet ExternallySideEffectFree) == ExternallySideEffectFree,
            "Pure meet ExternallySideEffectFree was not ExternallySideEffectFree" +
                s" (was ${Pure meet ExternallySideEffectFree})"
        )
        assert(
            (Pure meet DomainSpecificPure(Set.empty)) == DomainSpecificPure(Set.empty),
            "Pure meet DomainSpecificPure(Set.empty) was not DomainSpecificPure(Set.empty)" +
                s" (was ${Pure meet DomainSpecificPure(Set.empty)})"
        )
        assert(
            (Pure meet DomainSpecificSideEffectFree(Set.empty)) == DomainSpecificSideEffectFree(Set.empty),
            "Pure meet DomainSpecificSideEffectFree(Set.empty) was not DomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${Pure meet DomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (Pure meet DomainSpecificExternallyPure(Set.empty)) == DomainSpecificExternallyPure(Set.empty),
            "Pure meet DomainSpecificExternallyPure(Set.empty) was not DomainSpecificExternallyPure(Set.empty)" +
                s" (was ${Pure meet DomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (Pure meet DomainSpecificExternallySideEffectFree(Set.empty)) == DomainSpecificExternallySideEffectFree(Set.empty),
            "Pure meet DomainSpecificExternallySideEffectFree(Set.empty) was not DomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${Pure meet DomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (Pure meet ConditionallyPureWithoutAllocations) == ConditionallyPure,
            "Pure meet ConditionallyPureWithoutAllocations was not ConditionallyPure" +
                s" (was ${Pure meet ConditionallyPureWithoutAllocations})"
        )
        assert(
            (Pure meet ConditionallySideEffectFreeWithoutAllocations) == ConditionallySideEffectFree,
            "Pure meet ConditionallySideEffectFreeWithoutAllocations was not ConditionallySideEffectFree" +
                s" (was ${Pure meet ConditionallySideEffectFreeWithoutAllocations})"
        )
        assert(
            (Pure meet ConditionallyPure) == ConditionallyPure,
            "Pure meet ConditionallyPure was not ConditionallyPure" +
                s" (was ${Pure meet ConditionallyPure})"
        )
        assert(
            (Pure meet ConditionallySideEffectFree) == ConditionallySideEffectFree,
            "Pure meet ConditionallySideEffectFree was not ConditionallySideEffectFree" +
                s" (was ${Pure meet ConditionallySideEffectFree})"
        )
        assert(
            (Pure meet ConditionallyExternallyPure) == ConditionallyExternallyPure,
            "Pure meet ConditionallyExternallyPure was not ConditionallyExternallyPure" +
                s" (was ${Pure meet ConditionallyExternallyPure})"
        )
        assert(
            (Pure meet ConditionallyExternallySideEffectFree) == ConditionallyExternallySideEffectFree,
            "Pure meet ConditionallyExternallySideEffectFree was not ConditionallyExternallySideEffectFree" +
                s" (was ${Pure meet ConditionallyExternallySideEffectFree})"
        )
        assert(
            (Pure meet ConditionallyDomainSpecificPure(Set.empty)) == ConditionallyDomainSpecificPure(Set.empty),
            "Pure meet ConditionallyDomainSpecificPure(Set.empty) was not ConditionallyDomainSpecificPure(Set.empty)" +
                s" (was ${Pure meet ConditionallyDomainSpecificPure(Set.empty)})"
        )
        assert(
            (Pure meet ConditionallyDomainSpecificSideEffectFree(Set.empty)) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "Pure meet ConditionallyDomainSpecificSideEffectFree(Set.empty) was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${Pure meet ConditionallyDomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (Pure meet ConditionallyDomainSpecificExternallyPure(Set.empty)) == ConditionallyDomainSpecificExternallyPure(Set.empty),
            "Pure meet ConditionallyDomainSpecificExternallyPure(Set.empty) was not ConditionallyDomainSpecificExternallyPure(Set.empty)" +
                s" (was ${Pure meet ConditionallyDomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (Pure meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "Pure meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${Pure meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (Pure meet MaybePure) == MaybePure,
            "Pure meet MaybePure was not MaybePure" +
                s" (was ${Pure meet MaybePure})"
        )
        assert(
            (Pure meet Impure) == Impure,
            "Pure meet Impure was not Impure" +
                s" (was ${Pure meet Impure})"
        )
        assert(
            (Pure meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "Pure meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${Pure meet ImpureDueToSynchronization})"
        )
        assert(
            (Pure meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "Pure meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${Pure meet ImpureDueToHeapModification})"
        )
        assert(
            (Pure meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "Pure meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${Pure meet ImpureDueToFutureExtension})"
        )
        assert(
            (Pure meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "Pure meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${Pure meet ImpureDueToUnknownEntity})"
        )
        assert(
            (Pure meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "Pure meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${Pure meet ImpureDueToUnknownProperty})"
        )

        assert(
            (SideEffectFree meet ExternallyPure) == ExternallySideEffectFree,
            "SideEffectFree meet ExternallyPure was not ExternallySideEffectFree" +
                s" (was ${SideEffectFree meet ExternallyPure})"
        )
        assert(
            (SideEffectFree meet ExternallySideEffectFree) == ExternallySideEffectFree,
            "SideEffectFree meet ExternallySideEffectFree was not ExternallySideEffectFree" +
                s" (was ${SideEffectFree meet ExternallySideEffectFree})"
        )
        assert(
            (SideEffectFree meet DomainSpecificPure(Set.empty)) == DomainSpecificSideEffectFree(Set.empty),
            "SideEffectFree meet DomainSpecificPure(Set.empty) was not DomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${SideEffectFree meet DomainSpecificPure(Set.empty)})"
        )
        assert(
            (SideEffectFree meet DomainSpecificSideEffectFree(Set.empty)) == DomainSpecificSideEffectFree(Set.empty),
            "SideEffectFree meet DomainSpecificSideEffectFree(Set.empty) was not DomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${SideEffectFree meet DomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (SideEffectFree meet DomainSpecificExternallyPure(Set.empty)) == DomainSpecificExternallySideEffectFree(Set.empty),
            "SideEffectFree meet DomainSpecificExternallyPure(Set.empty) was not DomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${SideEffectFree meet DomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (SideEffectFree meet DomainSpecificExternallySideEffectFree(Set.empty)) == DomainSpecificExternallySideEffectFree(Set.empty),
            "SideEffectFree meet DomainSpecificExternallySideEffectFree(Set.empty) was not DomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${SideEffectFree meet DomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (SideEffectFree meet ConditionallyPureWithoutAllocations) == ConditionallySideEffectFree,
            "SideEffectFree meet ConditionallyPureWithoutAllocations was not ConditionallySideEffectFree" +
                s" (was ${SideEffectFree meet ConditionallyPureWithoutAllocations})"
        )
        assert(
            (SideEffectFree meet ConditionallySideEffectFreeWithoutAllocations) == ConditionallySideEffectFree,
            "SideEffectFree meet ConditionallySideEffectFreeWithoutAllocations was not ConditionallySideEffectFree" +
                s" (was ${SideEffectFree meet ConditionallySideEffectFreeWithoutAllocations})"
        )
        assert(
            (SideEffectFree meet ConditionallyPure) == ConditionallySideEffectFree,
            "SideEffectFree meet ConditionallyPure was not ConditionallySideEffectFree" +
                s" (was ${SideEffectFree meet ConditionallyPure})"
        )
        assert(
            (SideEffectFree meet ConditionallySideEffectFree) == ConditionallySideEffectFree,
            "SideEffectFree meet ConditionallySideEffectFree was not ConditionallySideEffectFree" +
                s" (was ${SideEffectFree meet ConditionallySideEffectFree})"
        )
        assert(
            (SideEffectFree meet ConditionallyExternallyPure) == ConditionallyExternallySideEffectFree,
            "SideEffectFree meet ConditionallyExternallyPure was not ConditionallyExternallySideEffectFree" +
                s" (was ${SideEffectFree meet ConditionallyExternallyPure})"
        )
        assert(
            (SideEffectFree meet ConditionallyExternallySideEffectFree) == ConditionallyExternallySideEffectFree,
            "SideEffectFree meet ConditionallyExternallySideEffectFree was not ConditionallyExternallySideEffectFree" +
                s" (was ${SideEffectFree meet ConditionallyExternallySideEffectFree})"
        )
        assert(
            (SideEffectFree meet ConditionallyDomainSpecificPure(Set.empty)) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "SideEffectFree meet ConditionallyDomainSpecificPure(Set.empty) was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${SideEffectFree meet ConditionallyDomainSpecificPure(Set.empty)})"
        )
        assert(
            (SideEffectFree meet ConditionallyDomainSpecificSideEffectFree(Set.empty)) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "SideEffectFree meet ConditionallyDomainSpecificSideEffectFree(Set.empty) was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${SideEffectFree meet ConditionallyDomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (SideEffectFree meet ConditionallyDomainSpecificExternallyPure(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "SideEffectFree meet ConditionallyDomainSpecificExternallyPure(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${SideEffectFree meet ConditionallyDomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (SideEffectFree meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "SideEffectFree meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${SideEffectFree meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (SideEffectFree meet MaybePure) == MaybePure,
            "SideEffectFree meet MaybePure was not MaybePure" +
                s" (was ${SideEffectFree meet MaybePure})"
        )
        assert(
            (SideEffectFree meet Impure) == Impure,
            "SideEffectFree meet Impure was not Impure" +
                s" (was ${SideEffectFree meet Impure})"
        )
        assert(
            (SideEffectFree meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "SideEffectFree meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${SideEffectFree meet ImpureDueToSynchronization})"
        )
        assert(
            (SideEffectFree meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "SideEffectFree meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${SideEffectFree meet ImpureDueToHeapModification})"
        )
        assert(
            (SideEffectFree meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "SideEffectFree meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${SideEffectFree meet ImpureDueToFutureExtension})"
        )
        assert(
            (SideEffectFree meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "SideEffectFree meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${SideEffectFree meet ImpureDueToUnknownEntity})"
        )
        assert(
            (SideEffectFree meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "SideEffectFree meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${SideEffectFree meet ImpureDueToUnknownProperty})"
        )

        assert(
            (ExternallyPure meet ExternallySideEffectFree) == ExternallySideEffectFree,
            "ExternallyPure meet ExternallySideEffectFree was not ExternallySideEffectFree" +
                s" (was ${ExternallyPure meet ExternallySideEffectFree})"
        )
        assert(
            (ExternallyPure meet DomainSpecificPure(Set.empty)) == DomainSpecificExternallyPure(Set.empty),
            "ExternallyPure meet DomainSpecificPure(Set.empty) was not DomainSpecificExternallyPure(Set.empty)" +
                s" (was ${ExternallyPure meet DomainSpecificPure(Set.empty)})"
        )
        assert(
            (ExternallyPure meet DomainSpecificSideEffectFree(Set.empty)) == DomainSpecificExternallySideEffectFree(Set.empty),
            "ExternallyPure meet DomainSpecificSideEffectFree(Set.empty) was not DomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ExternallyPure meet DomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (ExternallyPure meet DomainSpecificExternallyPure(Set.empty)) == DomainSpecificExternallyPure(Set.empty),
            "ExternallyPure meet DomainSpecificExternallyPure(Set.empty) was not DomainSpecificExternallyPure(Set.empty)" +
                s" (was ${ExternallyPure meet DomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (ExternallyPure meet DomainSpecificExternallySideEffectFree(Set.empty)) == DomainSpecificExternallySideEffectFree(Set.empty),
            "ExternallyPure meet DomainSpecificExternallySideEffectFree(Set.empty) was not DomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ExternallyPure meet DomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (ExternallyPure meet ConditionallyPureWithoutAllocations) == ConditionallyExternallyPure,
            "ExternallyPure meet ConditionallyPureWithoutAllocations was not ConditionallyExternallyPure" +
                s" (was ${ExternallyPure meet ConditionallyPureWithoutAllocations})"
        )
        assert(
            (ExternallyPure meet ConditionallySideEffectFreeWithoutAllocations) == ConditionallyExternallySideEffectFree,
            "ExternallyPure meet ConditionallySideEffectFreeWithoutAllocations was not ConditionallyExternallySideEffectFree" +
                s" (was ${ExternallyPure meet ConditionallySideEffectFreeWithoutAllocations})"
        )
        assert(
            (ExternallyPure meet ConditionallyPure) == ConditionallyExternallyPure,
            "ExternallyPure meet ConditionallyPure was not ConditionallyExternallyPure" +
                s" (was ${ExternallyPure meet ConditionallyPure})"
        )
        assert(
            (ExternallyPure meet ConditionallySideEffectFree) == ConditionallyExternallySideEffectFree,
            "ExternallyPure meet ConditionallySideEffectFree was not ConditionallyExternallySideEffectFree" +
                s" (was ${ExternallyPure meet ConditionallySideEffectFree})"
        )
        assert(
            (ExternallyPure meet ConditionallyExternallyPure) == ConditionallyExternallyPure,
            "ExternallyPure meet ConditionallyExternallyPure was not ConditionallyExternallyPure" +
                s" (was ${ExternallyPure meet ConditionallyExternallyPure})"
        )
        assert(
            (ExternallyPure meet ConditionallyExternallySideEffectFree) == ConditionallyExternallySideEffectFree,
            "ExternallyPure meet ConditionallyExternallySideEffectFree was not ConditionallyExternallySideEffectFree" +
                s" (was ${ExternallyPure meet ConditionallyExternallySideEffectFree})"
        )
        assert(
            (ExternallyPure meet ConditionallyDomainSpecificPure(Set.empty)) == ConditionallyDomainSpecificExternallyPure(Set.empty),
            "ExternallyPure meet ConditionallyDomainSpecificPure(Set.empty) was not ConditionallyDomainSpecificExternallyPure(Set.empty)" +
                s" (was ${ExternallyPure meet ConditionallyDomainSpecificPure(Set.empty)})"
        )
        assert(
            (ExternallyPure meet ConditionallyDomainSpecificSideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ExternallyPure meet ConditionallyDomainSpecificSideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ExternallyPure meet ConditionallyDomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (ExternallyPure meet ConditionallyDomainSpecificExternallyPure(Set.empty)) == ConditionallyDomainSpecificExternallyPure(Set.empty),
            "ExternallyPure meet ConditionallyDomainSpecificExternallyPure(Set.empty) was not ConditionallyDomainSpecificExternallyPure(Set.empty)" +
                s" (was ${ExternallyPure meet ConditionallyDomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (ExternallyPure meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ExternallyPure meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ExternallyPure meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (ExternallyPure meet MaybePure) == MaybePure,
            "ExternallyPure meet MaybePure was not MaybePure" +
                s" (was ${ExternallyPure meet MaybePure})"
        )
        assert(
            (ExternallyPure meet Impure) == Impure,
            "ExternallyPure meet Impure was not Impure" +
                s" (was ${ExternallyPure meet Impure})"
        )
        assert(
            (ExternallyPure meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "ExternallyPure meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${ExternallyPure meet ImpureDueToSynchronization})"
        )
        assert(
            (ExternallyPure meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "ExternallyPure meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${ExternallyPure meet ImpureDueToHeapModification})"
        )
        assert(
            (ExternallyPure meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "ExternallyPure meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${ExternallyPure meet ImpureDueToFutureExtension})"
        )
        assert(
            (ExternallyPure meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "ExternallyPure meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${ExternallyPure meet ImpureDueToUnknownEntity})"
        )
        assert(
            (ExternallyPure meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "ExternallyPure meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${ExternallyPure meet ImpureDueToUnknownProperty})"
        )

        assert(
            (ExternallySideEffectFree meet DomainSpecificPure(Set.empty)) == DomainSpecificExternallySideEffectFree(Set.empty),
            "ExternallySideEffectFree meet DomainSpecificPure(Set.empty) was not DomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ExternallySideEffectFree meet DomainSpecificPure(Set.empty)})"
        )
        assert(
            (ExternallySideEffectFree meet DomainSpecificSideEffectFree(Set.empty)) == DomainSpecificExternallySideEffectFree(Set.empty),
            "ExternallySideEffectFree meet DomainSpecificSideEffectFree(Set.empty) was not DomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ExternallySideEffectFree meet DomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (ExternallySideEffectFree meet DomainSpecificExternallyPure(Set.empty)) == DomainSpecificExternallySideEffectFree(Set.empty),
            "ExternallySideEffectFree meet DomainSpecificExternallyPure(Set.empty) was not DomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ExternallySideEffectFree meet DomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (ExternallySideEffectFree meet DomainSpecificExternallySideEffectFree(Set.empty)) == DomainSpecificExternallySideEffectFree(Set.empty),
            "ExternallySideEffectFree meet DomainSpecificExternallySideEffectFree(Set.empty) was not DomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ExternallySideEffectFree meet DomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (ExternallySideEffectFree meet ConditionallyPureWithoutAllocations) == ConditionallyExternallySideEffectFree,
            "ExternallySideEffectFree meet ConditionallyPureWithoutAllocations was not ConditionallyExternallySideEffectFree" +
                s" (was ${ExternallySideEffectFree meet ConditionallyPureWithoutAllocations})"
        )
        assert(
            (ExternallySideEffectFree meet ConditionallySideEffectFreeWithoutAllocations) == ConditionallyExternallySideEffectFree,
            "ExternallySideEffectFree meet ConditionallySideEffectFreeWithoutAllocations was not ConditionallyExternallySideEffectFree" +
                s" (was ${ExternallySideEffectFree meet ConditionallySideEffectFreeWithoutAllocations})"
        )
        assert(
            (ExternallySideEffectFree meet ConditionallyPure) == ConditionallyExternallySideEffectFree,
            "ExternallySideEffectFree meet ConditionallyPure was not ConditionallyExternallySideEffectFree" +
                s" (was ${ExternallySideEffectFree meet ConditionallyPure})"
        )
        assert(
            (ExternallySideEffectFree meet ConditionallySideEffectFree) == ConditionallyExternallySideEffectFree,
            "ExternallySideEffectFree meet ConditionallySideEffectFree was not ConditionallyExternallySideEffectFree" +
                s" (was ${ExternallySideEffectFree meet ConditionallySideEffectFree})"
        )
        assert(
            (ExternallySideEffectFree meet ConditionallyExternallyPure) == ConditionallyExternallySideEffectFree,
            "ExternallySideEffectFree meet ConditionallyExternallyPure was not ConditionallyExternallySideEffectFree" +
                s" (was ${ExternallySideEffectFree meet ConditionallyExternallyPure})"
        )
        assert(
            (ExternallySideEffectFree meet ConditionallyExternallySideEffectFree) == ConditionallyExternallySideEffectFree,
            "ExternallySideEffectFree meet ConditionallyExternallySideEffectFree was not ConditionallyExternallySideEffectFree" +
                s" (was ${ExternallySideEffectFree meet ConditionallyExternallySideEffectFree})"
        )
        assert(
            (ExternallySideEffectFree meet ConditionallyDomainSpecificPure(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ExternallySideEffectFree meet ConditionallyDomainSpecificPure(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ExternallySideEffectFree meet ConditionallyDomainSpecificPure(Set.empty)})"
        )
        assert(
            (ExternallySideEffectFree meet ConditionallyDomainSpecificSideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ExternallySideEffectFree meet ConditionallyDomainSpecificSideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ExternallySideEffectFree meet ConditionallyDomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (ExternallySideEffectFree meet ConditionallyDomainSpecificExternallyPure(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ExternallySideEffectFree meet ConditionallyDomainSpecificExternallyPure(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ExternallySideEffectFree meet ConditionallyDomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (ExternallySideEffectFree meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ExternallySideEffectFree meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ExternallySideEffectFree meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (ExternallySideEffectFree meet MaybePure) == MaybePure,
            "ExternallySideEffectFree meet MaybePure was not MaybePure" +
                s" (was ${ExternallySideEffectFree meet MaybePure})"
        )
        assert(
            (ExternallySideEffectFree meet Impure) == Impure,
            "ExternallySideEffectFree meet Impure was not Impure" +
                s" (was ${ExternallySideEffectFree meet Impure})"
        )
        assert(
            (ExternallySideEffectFree meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "ExternallySideEffectFree meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${ExternallySideEffectFree meet ImpureDueToSynchronization})"
        )
        assert(
            (ExternallySideEffectFree meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "ExternallySideEffectFree meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${ExternallySideEffectFree meet ImpureDueToHeapModification})"
        )
        assert(
            (ExternallySideEffectFree meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "ExternallySideEffectFree meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${ExternallySideEffectFree meet ImpureDueToFutureExtension})"
        )
        assert(
            (ExternallySideEffectFree meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "ExternallySideEffectFree meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${ExternallySideEffectFree meet ImpureDueToUnknownEntity})"
        )
        assert(
            (ExternallySideEffectFree meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "ExternallySideEffectFree meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${ExternallySideEffectFree meet ImpureDueToUnknownProperty})"
        )

        assert(
            (DomainSpecificPure(Set.empty) meet DomainSpecificSideEffectFree(Set.empty)) == DomainSpecificSideEffectFree(Set.empty),
            "DomainSpecificPure(Set.empty) meet DomainSpecificSideEffectFree(Set.empty) was not DomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificPure(Set.empty) meet DomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet DomainSpecificExternallyPure(Set.empty)) == DomainSpecificExternallyPure(Set.empty),
            "DomainSpecificPure(Set.empty) meet DomainSpecificExternallyPure(Set.empty) was not DomainSpecificExternallyPure(Set.empty)" +
                s" (was ${DomainSpecificPure(Set.empty) meet DomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet DomainSpecificExternallySideEffectFree(Set.empty)) == DomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificPure(Set.empty) meet DomainSpecificExternallySideEffectFree(Set.empty) was not DomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificPure(Set.empty) meet DomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet ConditionallyPureWithoutAllocations) == ConditionallyDomainSpecificPure(Set.empty),
            "DomainSpecificPure(Set.empty) meet ConditionallyPureWithoutAllocations was not ConditionallyDomainSpecificPure(Set.empty)" +
                s" (was ${DomainSpecificPure(Set.empty) meet ConditionallyPureWithoutAllocations})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet ConditionallySideEffectFreeWithoutAllocations) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "DomainSpecificPure(Set.empty) meet ConditionallySideEffectFreeWithoutAllocations was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificPure(Set.empty) meet ConditionallySideEffectFreeWithoutAllocations})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet ConditionallyPure) == ConditionallyDomainSpecificPure(Set.empty),
            "DomainSpecificPure(Set.empty) meet ConditionallyPure was not ConditionallyDomainSpecificPure(Set.empty)" +
                s" (was ${DomainSpecificPure(Set.empty) meet ConditionallyPure})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet ConditionallySideEffectFree) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "DomainSpecificPure(Set.empty) meet ConditionallySideEffectFree was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificPure(Set.empty) meet ConditionallySideEffectFree})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet ConditionallyExternallyPure) == ConditionallyDomainSpecificExternallyPure(Set.empty),
            "DomainSpecificPure(Set.empty) meet ConditionallyExternallyPure was not ConditionallyDomainSpecificExternallyPure(Set.empty)" +
                s" (was ${DomainSpecificPure(Set.empty) meet ConditionallyExternallyPure})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet ConditionallyExternallySideEffectFree) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificPure(Set.empty) meet ConditionallyExternallySideEffectFree was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificPure(Set.empty) meet ConditionallyExternallySideEffectFree})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificPure(Set.empty)) == ConditionallyDomainSpecificPure(Set.empty),
            "DomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificPure(Set.empty) was not ConditionallyDomainSpecificPure(Set.empty)" +
                s" (was ${DomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificPure(Set.empty)})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificSideEffectFree(Set.empty)) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "DomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificSideEffectFree(Set.empty) was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty)) == ConditionallyDomainSpecificExternallyPure(Set.empty),
            "DomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty) was not ConditionallyDomainSpecificExternallyPure(Set.empty)" +
                s" (was ${DomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet MaybePure) == MaybePure,
            "DomainSpecificPure(Set.empty) meet MaybePure was not MaybePure" +
                s" (was ${DomainSpecificPure(Set.empty) meet MaybePure})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet Impure) == Impure,
            "DomainSpecificPure(Set.empty) meet Impure was not Impure" +
                s" (was ${DomainSpecificPure(Set.empty) meet Impure})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "DomainSpecificPure(Set.empty) meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${DomainSpecificPure(Set.empty) meet ImpureDueToSynchronization})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "DomainSpecificPure(Set.empty) meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${DomainSpecificPure(Set.empty) meet ImpureDueToHeapModification})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "DomainSpecificPure(Set.empty) meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${DomainSpecificPure(Set.empty) meet ImpureDueToFutureExtension})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "DomainSpecificPure(Set.empty) meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${DomainSpecificPure(Set.empty) meet ImpureDueToUnknownEntity})"
        )
        assert(
            (DomainSpecificPure(Set.empty) meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "DomainSpecificPure(Set.empty) meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${DomainSpecificPure(Set.empty) meet ImpureDueToUnknownProperty})"
        )

        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet DomainSpecificExternallyPure(Set.empty)) == DomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificSideEffectFree(Set.empty) meet DomainSpecificExternallyPure(Set.empty) was not DomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet DomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet DomainSpecificExternallySideEffectFree(Set.empty)) == DomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificSideEffectFree(Set.empty) meet DomainSpecificExternallySideEffectFree(Set.empty) was not DomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet DomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet ConditionallyPureWithoutAllocations) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "DomainSpecificSideEffectFree(Set.empty) meet ConditionallyPureWithoutAllocations was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet ConditionallyPureWithoutAllocations})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet ConditionallySideEffectFreeWithoutAllocations) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "DomainSpecificSideEffectFree(Set.empty) meet ConditionallySideEffectFreeWithoutAllocations was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet ConditionallySideEffectFreeWithoutAllocations})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet ConditionallyPure) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "DomainSpecificSideEffectFree(Set.empty) meet ConditionallyPure was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet ConditionallyPure})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet ConditionallySideEffectFree) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "DomainSpecificSideEffectFree(Set.empty) meet ConditionallySideEffectFree was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet ConditionallySideEffectFree})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet ConditionallyExternallyPure) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificSideEffectFree(Set.empty) meet ConditionallyExternallyPure was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet ConditionallyExternallyPure})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet ConditionallyExternallySideEffectFree) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificSideEffectFree(Set.empty) meet ConditionallyExternallySideEffectFree was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet ConditionallyExternallySideEffectFree})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificPure(Set.empty)) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "DomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificPure(Set.empty) was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificPure(Set.empty)})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificSideEffectFree(Set.empty)) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "DomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificSideEffectFree(Set.empty) was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet MaybePure) == MaybePure,
            "DomainSpecificSideEffectFree(Set.empty) meet MaybePure was not MaybePure" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet MaybePure})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet Impure) == Impure,
            "DomainSpecificSideEffectFree(Set.empty) meet Impure was not Impure" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet Impure})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "DomainSpecificSideEffectFree(Set.empty) meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet ImpureDueToSynchronization})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "DomainSpecificSideEffectFree(Set.empty) meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet ImpureDueToHeapModification})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "DomainSpecificSideEffectFree(Set.empty) meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet ImpureDueToFutureExtension})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "DomainSpecificSideEffectFree(Set.empty) meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet ImpureDueToUnknownEntity})"
        )
        assert(
            (DomainSpecificSideEffectFree(Set.empty) meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "DomainSpecificSideEffectFree(Set.empty) meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${DomainSpecificSideEffectFree(Set.empty) meet ImpureDueToUnknownProperty})"
        )

        assert(
            (DomainSpecificExternallyPure(Set.empty) meet DomainSpecificExternallySideEffectFree(Set.empty)) == DomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificExternallyPure(Set.empty) meet DomainSpecificExternallySideEffectFree(Set.empty) was not DomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet DomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (DomainSpecificExternallyPure(Set.empty) meet ConditionallyPureWithoutAllocations) == ConditionallyDomainSpecificExternallyPure(Set.empty),
            "DomainSpecificExternallyPure(Set.empty) meet ConditionallyPureWithoutAllocations was not ConditionallyDomainSpecificExternallyPure(Set.empty)" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet ConditionallyPureWithoutAllocations})"
        )
        assert(
            (DomainSpecificExternallyPure(Set.empty) meet ConditionallySideEffectFreeWithoutAllocations) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificExternallyPure(Set.empty) meet ConditionallySideEffectFreeWithoutAllocations was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet ConditionallySideEffectFreeWithoutAllocations})"
        )
        assert(
            (DomainSpecificExternallyPure(Set.empty) meet ConditionallyPure) == ConditionallyDomainSpecificExternallyPure(Set.empty),
            "DomainSpecificExternallyPure(Set.empty) meet ConditionallyPure was not ConditionallyDomainSpecificExternallyPure(Set.empty)" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet ConditionallyPure})"
        )
        assert(
            (DomainSpecificExternallyPure(Set.empty) meet ConditionallySideEffectFree) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificExternallyPure(Set.empty) meet ConditionallySideEffectFree was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet ConditionallySideEffectFree})"
        )
        assert(
            (DomainSpecificExternallyPure(Set.empty) meet ConditionallyExternallyPure) == ConditionallyDomainSpecificExternallyPure(Set.empty),
            "DomainSpecificExternallyPure(Set.empty) meet ConditionallyExternallyPure was not ConditionallyDomainSpecificExternallyPure(Set.empty)" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet ConditionallyExternallyPure})"
        )
        assert(
            (DomainSpecificExternallyPure(Set.empty) meet ConditionallyExternallySideEffectFree) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificExternallyPure(Set.empty) meet ConditionallyExternallySideEffectFree was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet ConditionallyExternallySideEffectFree})"
        )
        assert(
            (DomainSpecificExternallyPure(Set.empty) meet ConditionallyDomainSpecificPure(Set.empty)) == ConditionallyDomainSpecificExternallyPure(Set.empty),
            "DomainSpecificExternallyPure(Set.empty) meet ConditionallyDomainSpecificPure(Set.empty) was not ConditionallyDomainSpecificExternallyPure(Set.empty)" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet ConditionallyDomainSpecificPure(Set.empty)})"
        )
        assert(
            (DomainSpecificExternallyPure(Set.empty) meet ConditionallyDomainSpecificSideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificExternallyPure(Set.empty) meet ConditionallyDomainSpecificSideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet ConditionallyDomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (DomainSpecificExternallyPure(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty)) == ConditionallyDomainSpecificExternallyPure(Set.empty),
            "DomainSpecificExternallyPure(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty) was not ConditionallyDomainSpecificExternallyPure(Set.empty)" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (DomainSpecificExternallyPure(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificExternallyPure(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (DomainSpecificExternallyPure(Set.empty) meet MaybePure) == MaybePure,
            "DomainSpecificExternallyPure(Set.empty) meet MaybePure was not MaybePure" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet MaybePure})"
        )
        assert(
            (DomainSpecificExternallyPure(Set.empty) meet Impure) == Impure,
            "DomainSpecificExternallyPure(Set.empty) meet Impure was not Impure" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet Impure})"
        )
        assert(
            (DomainSpecificExternallyPure(Set.empty) meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "DomainSpecificExternallyPure(Set.empty) meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet ImpureDueToSynchronization})"
        )
        assert(
            (DomainSpecificExternallyPure(Set.empty) meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "DomainSpecificExternallyPure(Set.empty) meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet ImpureDueToHeapModification})"
        )
        assert(
            (DomainSpecificExternallyPure(Set.empty) meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "DomainSpecificExternallyPure(Set.empty) meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet ImpureDueToFutureExtension})"
        )
        assert(
            (DomainSpecificExternallyPure(Set.empty) meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "DomainSpecificExternallyPure(Set.empty) meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet ImpureDueToUnknownEntity})"
        )
        assert(
            (DomainSpecificExternallyPure(Set.empty) meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "DomainSpecificExternallyPure(Set.empty) meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${DomainSpecificExternallyPure(Set.empty) meet ImpureDueToUnknownProperty})"
        )

        assert(
            (DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyPureWithoutAllocations) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyPureWithoutAllocations was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyPureWithoutAllocations})"
        )
        assert(
            (DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallySideEffectFreeWithoutAllocations) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallySideEffectFreeWithoutAllocations was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallySideEffectFreeWithoutAllocations})"
        )
        assert(
            (DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyPure) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyPure was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyPure})"
        )
        assert(
            (DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallySideEffectFree) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallySideEffectFree was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallySideEffectFree})"
        )
        assert(
            (DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyExternallyPure) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyExternallyPure was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyExternallyPure})"
        )
        assert(
            (DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyExternallySideEffectFree) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyExternallySideEffectFree was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyExternallySideEffectFree})"
        )
        assert(
            (DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyDomainSpecificPure(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyDomainSpecificPure(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyDomainSpecificPure(Set.empty)})"
        )
        assert(
            (DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyDomainSpecificSideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyDomainSpecificSideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyDomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${DomainSpecificExternallySideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (DomainSpecificExternallySideEffectFree(Set.empty) meet MaybePure) == MaybePure,
            "DomainSpecificExternallySideEffectFree(Set.empty) meet MaybePure was not MaybePure" +
                s" (was ${DomainSpecificExternallySideEffectFree(Set.empty) meet MaybePure})"
        )
        assert(
            (DomainSpecificExternallySideEffectFree(Set.empty) meet Impure) == Impure,
            "DomainSpecificExternallySideEffectFree(Set.empty) meet Impure was not Impure" +
                s" (was ${DomainSpecificExternallySideEffectFree(Set.empty) meet Impure})"
        )
        assert(
            (DomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "DomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${DomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToSynchronization})"
        )
        assert(
            (DomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "DomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${DomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToHeapModification})"
        )
        assert(
            (DomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "DomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${DomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToFutureExtension})"
        )
        assert(
            (DomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "DomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${DomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToUnknownEntity})"
        )
        assert(
            (DomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "DomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${DomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToUnknownProperty})"
        )
    }

    it should "return the correct purity levels for conditional levels" in {
        assert(
            (ConditionallyPureWithoutAllocations meet ConditionallySideEffectFreeWithoutAllocations) == ConditionallySideEffectFreeWithoutAllocations,
            "ConditionallyPureWithoutAllocations meet ConditionallySideEffectFreeWithoutAllocations was not ConditionallySideEffectFreeWithoutAllocations" +
                s" (was ${ConditionallyPureWithoutAllocations meet ConditionallySideEffectFreeWithoutAllocations})"
        )
        assert(
            (ConditionallyPureWithoutAllocations meet ConditionallyPure) == ConditionallyPure,
            "ConditionallyPureWithoutAllocations meet ConditionallyPure was not ConditionallyPure" +
                s" (was ${ConditionallyPureWithoutAllocations meet ConditionallyPure})"
        )
        assert(
            (ConditionallyPureWithoutAllocations meet ConditionallySideEffectFree) == ConditionallySideEffectFree,
            "ConditionallyPureWithoutAllocations meet ConditionallySideEffectFree was not ConditionallySideEffectFree" +
                s" (was ${ConditionallyPureWithoutAllocations meet ConditionallySideEffectFree})"
        )
        assert(
            (ConditionallyPureWithoutAllocations meet ConditionallyExternallyPure) == ConditionallyExternallyPure,
            "ConditionallyPureWithoutAllocations meet ConditionallyExternallyPure was not ConditionallyExternallyPure" +
                s" (was ${ConditionallyPureWithoutAllocations meet ConditionallyExternallyPure})"
        )
        assert(
            (ConditionallyPureWithoutAllocations meet ConditionallyExternallySideEffectFree) == ConditionallyExternallySideEffectFree,
            "ConditionallyPureWithoutAllocations meet ConditionallyExternallySideEffectFree was not ConditionallyExternallySideEffectFree" +
                s" (was ${ConditionallyPureWithoutAllocations meet ConditionallyExternallySideEffectFree})"
        )
        assert(
            (ConditionallyPureWithoutAllocations meet ConditionallyDomainSpecificPure(Set.empty)) == ConditionallyDomainSpecificPure(Set.empty),
            "ConditionallyPureWithoutAllocations meet ConditionallyDomainSpecificPure(Set.empty) was not ConditionallyDomainSpecificPure(Set.empty)" +
                s" (was ${ConditionallyPureWithoutAllocations meet ConditionallyDomainSpecificPure(Set.empty)})"
        )
        assert(
            (ConditionallyPureWithoutAllocations meet ConditionallyDomainSpecificSideEffectFree(Set.empty)) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "ConditionallyPureWithoutAllocations meet ConditionallyDomainSpecificSideEffectFree(Set.empty) was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${ConditionallyPureWithoutAllocations meet ConditionallyDomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (ConditionallyPureWithoutAllocations meet ConditionallyDomainSpecificExternallyPure(Set.empty)) == ConditionallyDomainSpecificExternallyPure(Set.empty),
            "ConditionallyPureWithoutAllocations meet ConditionallyDomainSpecificExternallyPure(Set.empty) was not ConditionallyDomainSpecificExternallyPure(Set.empty)" +
                s" (was ${ConditionallyPureWithoutAllocations meet ConditionallyDomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (ConditionallyPureWithoutAllocations meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ConditionallyPureWithoutAllocations meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ConditionallyPureWithoutAllocations meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (ConditionallyPureWithoutAllocations meet MaybePure) == MaybePure,
            "ConditionallyPureWithoutAllocations meet MaybePure was not MaybePure" +
                s" (was ${ConditionallyPureWithoutAllocations meet MaybePure})"
        )
        assert(
            (ConditionallyPureWithoutAllocations meet Impure) == Impure,
            "ConditionallyPureWithoutAllocations meet Impure was not Impure" +
                s" (was ${ConditionallyPureWithoutAllocations meet Impure})"
        )
        assert(
            (ConditionallyPureWithoutAllocations meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "ConditionallyPureWithoutAllocations meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${ConditionallyPureWithoutAllocations meet ImpureDueToSynchronization})"
        )
        assert(
            (ConditionallyPureWithoutAllocations meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "ConditionallyPureWithoutAllocations meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${ConditionallyPureWithoutAllocations meet ImpureDueToHeapModification})"
        )
        assert(
            (ConditionallyPureWithoutAllocations meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "ConditionallyPureWithoutAllocations meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${ConditionallyPureWithoutAllocations meet ImpureDueToFutureExtension})"
        )
        assert(
            (ConditionallyPureWithoutAllocations meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "ConditionallyPureWithoutAllocations meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${ConditionallyPureWithoutAllocations meet ImpureDueToUnknownEntity})"
        )
        assert(
            (ConditionallyPureWithoutAllocations meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "ConditionallyPureWithoutAllocations meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${ConditionallyPureWithoutAllocations meet ImpureDueToUnknownProperty})"
        )

        assert(
            (ConditionallySideEffectFreeWithoutAllocations meet ConditionallyPure) == ConditionallySideEffectFree,
            "ConditionallySideEffectFreeWithoutAllocations meet ConditionallyPure was not ConditionallySideEffectFree" +
                s" (was ${ConditionallySideEffectFreeWithoutAllocations meet ConditionallyPure})"
        )
        assert(
            (ConditionallySideEffectFreeWithoutAllocations meet ConditionallySideEffectFree) == ConditionallySideEffectFree,
            "ConditionallySideEffectFreeWithoutAllocations meet ConditionallySideEffectFree was not ConditionallySideEffectFree" +
                s" (was ${ConditionallySideEffectFreeWithoutAllocations meet ConditionallySideEffectFree})"
        )
        assert(
            (ConditionallySideEffectFreeWithoutAllocations meet ConditionallyExternallyPure) == ConditionallyExternallySideEffectFree,
            "ConditionallySideEffectFreeWithoutAllocations meet ConditionallyExternallyPure was not ConditionallyExternallySideEffectFree" +
                s" (was ${ConditionallySideEffectFreeWithoutAllocations meet ConditionallyExternallyPure})"
        )
        assert(
            (ConditionallySideEffectFreeWithoutAllocations meet ConditionallyExternallySideEffectFree) == ConditionallyExternallySideEffectFree,
            "ConditionallySideEffectFreeWithoutAllocations meet ConditionallyExternallySideEffectFree was not ConditionallyExternallySideEffectFree" +
                s" (was ${ConditionallySideEffectFreeWithoutAllocations meet ConditionallyExternallySideEffectFree})"
        )
        assert(
            (ConditionallySideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificPure(Set.empty)) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "ConditionallySideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificPure(Set.empty) was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${ConditionallySideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificPure(Set.empty)})"
        )
        assert(
            (ConditionallySideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificSideEffectFree(Set.empty)) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "ConditionallySideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificSideEffectFree(Set.empty) was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${ConditionallySideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (ConditionallySideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificExternallyPure(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ConditionallySideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificExternallyPure(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ConditionallySideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (ConditionallySideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ConditionallySideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ConditionallySideEffectFreeWithoutAllocations meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (ConditionallySideEffectFreeWithoutAllocations meet MaybePure) == MaybePure,
            "ConditionallySideEffectFreeWithoutAllocations meet MaybePure was not MaybePure" +
                s" (was ${ConditionallySideEffectFreeWithoutAllocations meet MaybePure})"
        )
        assert(
            (ConditionallySideEffectFreeWithoutAllocations meet Impure) == Impure,
            "ConditionallySideEffectFreeWithoutAllocations meet Impure was not Impure" +
                s" (was ${ConditionallySideEffectFreeWithoutAllocations meet Impure})"
        )
        assert(
            (ConditionallySideEffectFreeWithoutAllocations meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "ConditionallySideEffectFreeWithoutAllocations meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${ConditionallySideEffectFreeWithoutAllocations meet ImpureDueToSynchronization})"
        )
        assert(
            (ConditionallySideEffectFreeWithoutAllocations meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "ConditionallySideEffectFreeWithoutAllocations meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${ConditionallySideEffectFreeWithoutAllocations meet ImpureDueToHeapModification})"
        )
        assert(
            (ConditionallySideEffectFreeWithoutAllocations meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "ConditionallySideEffectFreeWithoutAllocations meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${ConditionallySideEffectFreeWithoutAllocations meet ImpureDueToFutureExtension})"
        )
        assert(
            (ConditionallySideEffectFreeWithoutAllocations meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "ConditionallySideEffectFreeWithoutAllocations meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${ConditionallySideEffectFreeWithoutAllocations meet ImpureDueToUnknownEntity})"
        )
        assert(
            (ConditionallySideEffectFreeWithoutAllocations meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "ConditionallySideEffectFreeWithoutAllocations meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${ConditionallySideEffectFreeWithoutAllocations meet ImpureDueToUnknownProperty})"
        )

        assert(
            (ConditionallyPure meet ConditionallySideEffectFree) == ConditionallySideEffectFree,
            "ConditionallyPure meet ConditionallySideEffectFree was not ConditionallySideEffectFree" +
                s" (was ${ConditionallyPure meet ConditionallySideEffectFree})"
        )
        assert(
            (ConditionallyPure meet ConditionallyExternallyPure) == ConditionallyExternallyPure,
            "ConditionallyPure meet ConditionallyExternallyPure was not ConditionallyExternallyPure" +
                s" (was ${ConditionallyPure meet ConditionallyExternallyPure})"
        )
        assert(
            (ConditionallyPure meet ConditionallyExternallySideEffectFree) == ConditionallyExternallySideEffectFree,
            "ConditionallyPure meet ConditionallyExternallySideEffectFree was not ConditionallyExternallySideEffectFree" +
                s" (was ${ConditionallyPure meet ConditionallyExternallySideEffectFree})"
        )
        assert(
            (ConditionallyPure meet ConditionallyDomainSpecificPure(Set.empty)) == ConditionallyDomainSpecificPure(Set.empty),
            "ConditionallyPure meet ConditionallyDomainSpecificPure(Set.empty) was not ConditionallyDomainSpecificPure(Set.empty)" +
                s" (was ${ConditionallyPure meet ConditionallyDomainSpecificPure(Set.empty)})"
        )
        assert(
            (ConditionallyPure meet ConditionallyDomainSpecificSideEffectFree(Set.empty)) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "ConditionallyPure meet ConditionallyDomainSpecificSideEffectFree(Set.empty) was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${ConditionallyPure meet ConditionallyDomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (ConditionallyPure meet ConditionallyDomainSpecificExternallyPure(Set.empty)) == ConditionallyDomainSpecificExternallyPure(Set.empty),
            "ConditionallyPure meet ConditionallyDomainSpecificExternallyPure(Set.empty) was not ConditionallyDomainSpecificExternallyPure(Set.empty)" +
                s" (was ${ConditionallyPure meet ConditionallyDomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (ConditionallyPure meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ConditionallyPure meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ConditionallyPure meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (ConditionallyPure meet MaybePure) == MaybePure,
            "ConditionallyPure meet MaybePure was not MaybePure" +
                s" (was ${ConditionallyPure meet MaybePure})"
        )
        assert(
            (ConditionallyPure meet Impure) == Impure,
            "ConditionallyPure meet Impure was not Impure" +
                s" (was ${ConditionallyPure meet Impure})"
        )
        assert(
            (ConditionallyPure meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "ConditionallyPure meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${ConditionallyPure meet ImpureDueToSynchronization})"
        )
        assert(
            (ConditionallyPure meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "ConditionallyPure meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${ConditionallyPure meet ImpureDueToHeapModification})"
        )
        assert(
            (ConditionallyPure meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "ConditionallyPure meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${ConditionallyPure meet ImpureDueToFutureExtension})"
        )
        assert(
            (ConditionallyPure meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "ConditionallyPure meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${ConditionallyPure meet ImpureDueToUnknownEntity})"
        )
        assert(
            (ConditionallyPure meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "ConditionallyPure meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${ConditionallyPure meet ImpureDueToUnknownProperty})"
        )

        assert(
            (ConditionallySideEffectFree meet ConditionallyExternallyPure) == ConditionallyExternallySideEffectFree,
            "ConditionallySideEffectFree meet ConditionallyExternallyPure was not ConditionallyExternallySideEffectFree" +
                s" (was ${ConditionallySideEffectFree meet ConditionallyExternallyPure})"
        )
        assert(
            (ConditionallySideEffectFree meet ConditionallyExternallySideEffectFree) == ConditionallyExternallySideEffectFree,
            "ConditionallySideEffectFree meet ConditionallyExternallySideEffectFree was not ConditionallyExternallySideEffectFree" +
                s" (was ${ConditionallySideEffectFree meet ConditionallyExternallySideEffectFree})"
        )
        assert(
            (ConditionallySideEffectFree meet ConditionallyDomainSpecificPure(Set.empty)) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "ConditionallySideEffectFree meet ConditionallyDomainSpecificPure(Set.empty) was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${ConditionallySideEffectFree meet ConditionallyDomainSpecificPure(Set.empty)})"
        )
        assert(
            (ConditionallySideEffectFree meet ConditionallyDomainSpecificSideEffectFree(Set.empty)) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "ConditionallySideEffectFree meet ConditionallyDomainSpecificSideEffectFree(Set.empty) was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${ConditionallySideEffectFree meet ConditionallyDomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (ConditionallySideEffectFree meet ConditionallyDomainSpecificExternallyPure(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ConditionallySideEffectFree meet ConditionallyDomainSpecificExternallyPure(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ConditionallySideEffectFree meet ConditionallyDomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (ConditionallySideEffectFree meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ConditionallySideEffectFree meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ConditionallySideEffectFree meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (ConditionallySideEffectFree meet MaybePure) == MaybePure,
            "ConditionallySideEffectFree meet MaybePure was not MaybePure" +
                s" (was ${ConditionallySideEffectFree meet MaybePure})"
        )
        assert(
            (ConditionallySideEffectFree meet Impure) == Impure,
            "ConditionallySideEffectFree meet Impure was not Impure" +
                s" (was ${ConditionallySideEffectFree meet Impure})"
        )
        assert(
            (ConditionallySideEffectFree meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "ConditionallySideEffectFree meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${ConditionallySideEffectFree meet ImpureDueToSynchronization})"
        )
        assert(
            (ConditionallySideEffectFree meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "ConditionallySideEffectFree meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${ConditionallySideEffectFree meet ImpureDueToHeapModification})"
        )
        assert(
            (ConditionallySideEffectFree meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "ConditionallySideEffectFree meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${ConditionallySideEffectFree meet ImpureDueToFutureExtension})"
        )
        assert(
            (ConditionallySideEffectFree meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "ConditionallySideEffectFree meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${ConditionallySideEffectFree meet ImpureDueToUnknownEntity})"
        )
        assert(
            (ConditionallySideEffectFree meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "ConditionallySideEffectFree meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${ConditionallySideEffectFree meet ImpureDueToUnknownProperty})"
        )

        assert(
            (ConditionallyExternallyPure meet ConditionallyExternallySideEffectFree) == ConditionallyExternallySideEffectFree,
            "ConditionallyExternallyPure meet ConditionallyExternallySideEffectFree was not ConditionallyExternallySideEffectFree" +
                s" (was ${ConditionallyExternallyPure meet ConditionallyExternallySideEffectFree})"
        )
        assert(
            (ConditionallyExternallyPure meet ConditionallyDomainSpecificPure(Set.empty)) == ConditionallyDomainSpecificExternallyPure(Set.empty),
            "ConditionallyExternallyPure meet ConditionallyDomainSpecificPure(Set.empty) was not ConditionallyDomainSpecificExternallyPure(Set.empty)" +
                s" (was ${ConditionallyExternallyPure meet ConditionallyDomainSpecificPure(Set.empty)})"
        )
        assert(
            (ConditionallyExternallyPure meet ConditionallyDomainSpecificSideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ConditionallyExternallyPure meet ConditionallyDomainSpecificSideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ConditionallyExternallyPure meet ConditionallyDomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (ConditionallyExternallyPure meet ConditionallyDomainSpecificExternallyPure(Set.empty)) == ConditionallyDomainSpecificExternallyPure(Set.empty),
            "ConditionallyExternallyPure meet ConditionallyDomainSpecificExternallyPure(Set.empty) was not ConditionallyDomainSpecificExternallyPure(Set.empty)" +
                s" (was ${ConditionallyExternallyPure meet ConditionallyDomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (ConditionallyExternallyPure meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ConditionallyExternallyPure meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ConditionallyExternallyPure meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (ConditionallyExternallyPure meet MaybePure) == MaybePure,
            "ConditionallyExternallyPure meet MaybePure was not MaybePure" +
                s" (was ${ConditionallyExternallyPure meet MaybePure})"
        )
        assert(
            (ConditionallyExternallyPure meet Impure) == Impure,
            "ConditionallyExternallyPure meet Impure was not Impure" +
                s" (was ${ConditionallyExternallyPure meet Impure})"
        )
        assert(
            (ConditionallyExternallyPure meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "ConditionallyExternallyPure meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${ConditionallyExternallyPure meet ImpureDueToSynchronization})"
        )
        assert(
            (ConditionallyExternallyPure meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "ConditionallyExternallyPure meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${ConditionallyExternallyPure meet ImpureDueToHeapModification})"
        )
        assert(
            (ConditionallyExternallyPure meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "ConditionallyExternallyPure meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${ConditionallyExternallyPure meet ImpureDueToFutureExtension})"
        )
        assert(
            (ConditionallyExternallyPure meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "ConditionallyExternallyPure meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${ConditionallyExternallyPure meet ImpureDueToUnknownEntity})"
        )
        assert(
            (ConditionallyExternallyPure meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "ConditionallyExternallyPure meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${ConditionallyExternallyPure meet ImpureDueToUnknownProperty})"
        )

        assert(
            (ConditionallyExternallySideEffectFree meet ConditionallyDomainSpecificPure(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ConditionallyExternallySideEffectFree meet ConditionallyDomainSpecificPure(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ConditionallyExternallySideEffectFree meet ConditionallyDomainSpecificPure(Set.empty)})"
        )
        assert(
            (ConditionallyExternallySideEffectFree meet ConditionallyDomainSpecificSideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ConditionallyExternallySideEffectFree meet ConditionallyDomainSpecificSideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ConditionallyExternallySideEffectFree meet ConditionallyDomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (ConditionallyExternallySideEffectFree meet ConditionallyDomainSpecificExternallyPure(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ConditionallyExternallySideEffectFree meet ConditionallyDomainSpecificExternallyPure(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ConditionallyExternallySideEffectFree meet ConditionallyDomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (ConditionallyExternallySideEffectFree meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ConditionallyExternallySideEffectFree meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ConditionallyExternallySideEffectFree meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (ConditionallyExternallySideEffectFree meet MaybePure) == MaybePure,
            "ConditionallyExternallySideEffectFree meet MaybePure was not MaybePure" +
                s" (was ${ConditionallyExternallySideEffectFree meet MaybePure})"
        )
        assert(
            (ConditionallyExternallySideEffectFree meet Impure) == Impure,
            "ConditionallyExternallySideEffectFree meet Impure was not Impure" +
                s" (was ${ConditionallyExternallySideEffectFree meet Impure})"
        )
        assert(
            (ConditionallyExternallySideEffectFree meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "ConditionallyExternallySideEffectFree meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${ConditionallyExternallySideEffectFree meet ImpureDueToSynchronization})"
        )
        assert(
            (ConditionallyExternallySideEffectFree meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "ConditionallyExternallySideEffectFree meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${ConditionallyExternallySideEffectFree meet ImpureDueToHeapModification})"
        )
        assert(
            (ConditionallyExternallySideEffectFree meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "ConditionallyExternallySideEffectFree meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${ConditionallyExternallySideEffectFree meet ImpureDueToFutureExtension})"
        )
        assert(
            (ConditionallyExternallySideEffectFree meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "ConditionallyExternallySideEffectFree meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${ConditionallyExternallySideEffectFree meet ImpureDueToUnknownEntity})"
        )
        assert(
            (ConditionallyExternallySideEffectFree meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "ConditionallyExternallySideEffectFree meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${ConditionallyExternallySideEffectFree meet ImpureDueToUnknownProperty})"
        )

        assert(
            (ConditionallyDomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificSideEffectFree(Set.empty)) == ConditionallyDomainSpecificSideEffectFree(Set.empty),
            "ConditionallyDomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificSideEffectFree(Set.empty) was not ConditionallyDomainSpecificSideEffectFree(Set.empty)" +
                s" (was ${ConditionallyDomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificSideEffectFree(Set.empty)})"
        )
        assert(
            (ConditionallyDomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty)) == ConditionallyDomainSpecificExternallyPure(Set.empty),
            "ConditionallyDomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty) was not ConditionallyDomainSpecificExternallyPure(Set.empty)" +
                s" (was ${ConditionallyDomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (ConditionallyDomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ConditionallyDomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ConditionallyDomainSpecificPure(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (ConditionallyDomainSpecificPure(Set.empty) meet MaybePure) == MaybePure,
            "ConditionallyDomainSpecificPure(Set.empty) meet MaybePure was not MaybePure" +
                s" (was ${ConditionallyDomainSpecificPure(Set.empty) meet MaybePure})"
        )
        assert(
            (ConditionallyDomainSpecificPure(Set.empty) meet Impure) == Impure,
            "ConditionallyDomainSpecificPure(Set.empty) meet Impure was not Impure" +
                s" (was ${ConditionallyDomainSpecificPure(Set.empty) meet Impure})"
        )
        assert(
            (ConditionallyDomainSpecificPure(Set.empty) meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "ConditionallyDomainSpecificPure(Set.empty) meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${ConditionallyDomainSpecificPure(Set.empty) meet ImpureDueToSynchronization})"
        )
        assert(
            (ConditionallyDomainSpecificPure(Set.empty) meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "ConditionallyDomainSpecificPure(Set.empty) meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${ConditionallyDomainSpecificPure(Set.empty) meet ImpureDueToHeapModification})"
        )
        assert(
            (ConditionallyDomainSpecificPure(Set.empty) meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "ConditionallyDomainSpecificPure(Set.empty) meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${ConditionallyDomainSpecificPure(Set.empty) meet ImpureDueToFutureExtension})"
        )
        assert(
            (ConditionallyDomainSpecificPure(Set.empty) meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "ConditionallyDomainSpecificPure(Set.empty) meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${ConditionallyDomainSpecificPure(Set.empty) meet ImpureDueToUnknownEntity})"
        )
        assert(
            (ConditionallyDomainSpecificPure(Set.empty) meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "ConditionallyDomainSpecificPure(Set.empty) meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${ConditionallyDomainSpecificPure(Set.empty) meet ImpureDueToUnknownProperty})"
        )

        assert(
            (ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallyPure(Set.empty)})"
        )
        assert(
            (ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (ConditionallyDomainSpecificSideEffectFree(Set.empty) meet MaybePure) == MaybePure,
            "ConditionallyDomainSpecificSideEffectFree(Set.empty) meet MaybePure was not MaybePure" +
                s" (was ${ConditionallyDomainSpecificSideEffectFree(Set.empty) meet MaybePure})"
        )
        assert(
            (ConditionallyDomainSpecificSideEffectFree(Set.empty) meet Impure) == Impure,
            "ConditionallyDomainSpecificSideEffectFree(Set.empty) meet Impure was not Impure" +
                s" (was ${ConditionallyDomainSpecificSideEffectFree(Set.empty) meet Impure})"
        )
        assert(
            (ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ImpureDueToSynchronization})"
        )
        assert(
            (ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ImpureDueToHeapModification})"
        )
        assert(
            (ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ImpureDueToFutureExtension})"
        )
        assert(
            (ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ImpureDueToUnknownEntity})"
        )
        assert(
            (ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${ConditionallyDomainSpecificSideEffectFree(Set.empty) meet ImpureDueToUnknownProperty})"
        )

        assert(
            (ConditionallyDomainSpecificExternallyPure(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)) == ConditionallyDomainSpecificExternallySideEffectFree(Set.empty),
            "ConditionallyDomainSpecificExternallyPure(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) was not ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)" +
                s" (was ${ConditionallyDomainSpecificExternallyPure(Set.empty) meet ConditionallyDomainSpecificExternallySideEffectFree(Set.empty)})"
        )
        assert(
            (ConditionallyDomainSpecificExternallyPure(Set.empty) meet MaybePure) == MaybePure,
            "ConditionallyDomainSpecificExternallyPure(Set.empty) meet MaybePure was not MaybePure" +
                s" (was ${ConditionallyDomainSpecificExternallyPure(Set.empty) meet MaybePure})"
        )
        assert(
            (ConditionallyDomainSpecificExternallyPure(Set.empty) meet Impure) == Impure,
            "ConditionallyDomainSpecificExternallyPure(Set.empty) meet Impure was not Impure" +
                s" (was ${ConditionallyDomainSpecificExternallyPure(Set.empty) meet Impure})"
        )
        assert(
            (ConditionallyDomainSpecificExternallyPure(Set.empty) meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "ConditionallyDomainSpecificExternallyPure(Set.empty) meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${ConditionallyDomainSpecificExternallyPure(Set.empty) meet ImpureDueToSynchronization})"
        )
        assert(
            (ConditionallyDomainSpecificExternallyPure(Set.empty) meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "ConditionallyDomainSpecificExternallyPure(Set.empty) meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${ConditionallyDomainSpecificExternallyPure(Set.empty) meet ImpureDueToHeapModification})"
        )
        assert(
            (ConditionallyDomainSpecificExternallyPure(Set.empty) meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "ConditionallyDomainSpecificExternallyPure(Set.empty) meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${ConditionallyDomainSpecificExternallyPure(Set.empty) meet ImpureDueToFutureExtension})"
        )
        assert(
            (ConditionallyDomainSpecificExternallyPure(Set.empty) meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "ConditionallyDomainSpecificExternallyPure(Set.empty) meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${ConditionallyDomainSpecificExternallyPure(Set.empty) meet ImpureDueToUnknownEntity})"
        )
        assert(
            (ConditionallyDomainSpecificExternallyPure(Set.empty) meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "ConditionallyDomainSpecificExternallyPure(Set.empty) meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${ConditionallyDomainSpecificExternallyPure(Set.empty) meet ImpureDueToUnknownProperty})"
        )

        assert(
            (ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet MaybePure) == MaybePure,
            "ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet MaybePure was not MaybePure" +
                s" (was ${ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet MaybePure})"
        )
        assert(
            (ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet Impure) == Impure,
            "ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet Impure was not Impure" +
                s" (was ${ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet Impure})"
        )
        assert(
            (ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToSynchronization) == ImpureDueToSynchronization,
            "ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToSynchronization was not ImpureDueToSynchronization" +
                s" (was ${ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToSynchronization})"
        )
        assert(
            (ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToHeapModification) == ImpureDueToHeapModification,
            "ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToHeapModification was not ImpureDueToHeapModification" +
                s" (was ${ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToHeapModification})"
        )
        assert(
            (ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToFutureExtension) == ImpureDueToFutureExtension,
            "ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToFutureExtension was not ImpureDueToFutureExtension" +
                s" (was ${ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToFutureExtension})"
        )
        assert(
            (ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToUnknownEntity) == ImpureDueToUnknownEntity,
            "ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToUnknownEntity was not ImpureDueToUnknownEntity" +
                s" (was ${ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToUnknownEntity})"
        )
        assert(
            (ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToUnknownProperty) == ImpureDueToUnknownProperty,
            "ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToUnknownProperty was not ImpureDueToUnknownProperty" +
                s" (was ${ConditionallyDomainSpecificExternallySideEffectFree(Set.empty) meet ImpureDueToUnknownProperty})"
        )

        assert(
            (MaybePure meet Impure) == MaybePure,
            "MaybePure meet Impure was not MaybePure" +
                s" (was ${MaybePure meet Impure})"
        )
        assert(
            (MaybePure meet ImpureDueToSynchronization) == MaybePure,
            "MaybePure meet ImpureDueToSynchronization was not MaybePure" +
                s" (was ${MaybePure meet ImpureDueToSynchronization})"
        )
        assert(
            (MaybePure meet ImpureDueToHeapModification) == MaybePure,
            "MaybePure meet ImpureDueToHeapModification was not MaybePure" +
                s" (was ${MaybePure meet ImpureDueToHeapModification})"
        )
        assert(
            (MaybePure meet ImpureDueToFutureExtension) == MaybePure,
            "MaybePure meet ImpureDueToFutureExtension was not MaybePure" +
                s" (was ${MaybePure meet ImpureDueToFutureExtension})"
        )
        assert(
            (MaybePure meet ImpureDueToUnknownEntity) == MaybePure,
            "MaybePure meet ImpureDueToUnknownEntity was not MaybePure" +
                s" (was ${MaybePure meet ImpureDueToUnknownEntity})"
        )
        assert(
            (MaybePure meet ImpureDueToUnknownProperty) == MaybePure,
            "MaybePure meet ImpureDueToUnknownProperty was not MaybePure" +
                s" (was ${MaybePure meet ImpureDueToUnknownProperty})"
        )
    }
}
