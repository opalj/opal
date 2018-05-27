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
 * Tests the [[Purity]] property, especially correctness of the meet operator.
 *
 * @author Dominik Helm
 */
class PurityPropertyTest extends FlatSpec with Matchers {

    val allPurities: List[Purity] = List(
        CompileTimePure, Pure, SideEffectFree, ExternallyPure, ExternallySideEffectFree,
        DPure, DSideEffectFree, DExternallyPure, DExternallySideEffectFree,
        ContextuallyPure, ContextuallySideEffectFree, ImpureByAnalysis, ImpureByLackOfInformation
    )

    val doesntModifyReceiver: Set[Purity] = Set(
        CompileTimePure, Pure, SideEffectFree, DPure, DSideEffectFree
    )

    val doesntModifyParams: Set[Purity] = Set(
        CompileTimePure, Pure, SideEffectFree, ExternallyPure, ExternallySideEffectFree,
        DPure, DSideEffectFree, DExternallyPure, DExternallySideEffectFree
    )

    "purity levels" should "have the right properties" in {
        for (prop ← allPurities) {
            assert(
                prop.isCompileTimePure == (prop == CompileTimePure),
                s"$prop.isCompileTimePure was ${prop.isCompileTimePure}"
            )
        }

        val deterministic: Set[Purity] = Set(
            CompileTimePure, Pure, ExternallyPure, ContextuallyPure, DPure,
            DExternallyPure, DContextuallyPure
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
            CompileTimePure, Pure, SideEffectFree, ExternallyPure, ExternallySideEffectFree,
            ContextuallyPure, ContextuallySideEffectFree
        )

        for (prop ← allPurities) {
            assert(
                prop.usesDomainSpecificActions != doesntUseDomainSpecificActions.contains(prop),
                s"$prop.usesDomainSpecicifActions was ${prop.usesDomainSpecificActions}"
            )
        }
    }

    they should "be converted correctly" in {
        for { prop ← allPurities } {
            if (doesntModifyReceiver.contains(prop) || prop.isInstanceOf[ClassifiedImpure])
                assert(
                    prop.withoutExternal == prop,
                    s"$prop.withoutExternal modified $prop (was ${prop.withoutExternal})"
                )
            else
                assert(
                    prop.withoutExternal.flags ==
                        (prop.flags & ~Purity.ModifiesReceiver & ~Purity.ModifiesParameters),
                    s"$prop.withoutExternal was incorrect (was ${prop.withoutExternal})"
                )
            if (doesntModifyParams.contains(prop) || prop.isInstanceOf[ClassifiedImpure])
                assert(
                    prop.withoutContextual == prop,
                    s"$prop.withoutContextual modified $prop (was ${prop.withoutContextual})"
                )
            else
                assert(
                    prop.withoutContextual.flags == (prop.flags & ~Purity.ModifiesParameters),
                    s"$prop.withoutContextual was incorrect (was ${prop.withoutContextual})"
                )
        }
    }

    "the meet operator" should "be reflexive and symmetric" in {
        for (prop1 ← allPurities) {
            assert((prop1 meet prop1) == prop1, s"meet was not reflexive for $prop1")
            for (prop2 ← allPurities) {
                val meet12 = prop1 meet prop2
                val meet21 = prop2 meet prop1
                assert(
                    meet12 == meet21,
                    s"$prop1 meet $prop2 was not symmetric (was $meet12 / $meet21)"
                )
            }
        }
    }

    it should "return the correct purity levels" in {
        for (prop ← allPurities) {
            val result = CompileTimePure meet prop
            assert(
                result == prop,
                s"CompileTimePure meet $prop was not $prop (was $result)"
            )
        }

        assert((Pure meet SideEffectFree) == SideEffectFree)
        assert((Pure meet ExternallyPure) == ExternallyPure)
        assert((Pure meet ExternallySideEffectFree) == ExternallySideEffectFree)
        assert((Pure meet DPure) == DPure)
        assert((Pure meet DSideEffectFree) == DSideEffectFree)
        assert((Pure meet DExternallyPure) == DExternallyPure)
        assert((Pure meet DExternallySideEffectFree) == DExternallySideEffectFree)
        assert((Pure meet ImpureByAnalysis) == ImpureByAnalysis)

        assert((SideEffectFree meet ExternallyPure) == ExternallySideEffectFree)
        assert((SideEffectFree meet ExternallySideEffectFree) == ExternallySideEffectFree)
        assert((SideEffectFree meet DPure) == DSideEffectFree)
        assert((SideEffectFree meet DSideEffectFree) == DSideEffectFree)
        assert((SideEffectFree meet DExternallyPure) == DExternallySideEffectFree)
        assert((SideEffectFree meet DExternallySideEffectFree) == DExternallySideEffectFree)
        assert((SideEffectFree meet ImpureByAnalysis) == ImpureByAnalysis)

        assert(
            (ExternallyPure meet ExternallySideEffectFree) == ExternallySideEffectFree,
            "LBExternallyPure meet LBExternallySideEffectFree was not LBExternallySideEffectFree"+
                s" (was ${ExternallyPure meet ExternallySideEffectFree})"
        )
        assert(
            (ExternallyPure meet DPure) == DExternallyPure,
            "LBExternallyPure meet LBDPure was not LBDExternallyPure"+
                s" (was ${ExternallyPure meet DPure})"
        )
        assert(
            (ExternallyPure meet DSideEffectFree) == DExternallySideEffectFree,
            "LBExternallyPure meet LBDSideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${ExternallyPure meet DSideEffectFree})"
        )
        assert(
            (ExternallyPure meet DExternallyPure) == DExternallyPure,
            "LBExternallyPure meet LBDExternallyPure was not LBDExternallyPure"+
                s" (was ${ExternallyPure meet DExternallyPure})"
        )
        assert(
            (ExternallyPure meet DExternallySideEffectFree) == DExternallySideEffectFree,
            "LBExternallyPure meet LBDExternallySideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${ExternallyPure meet DExternallySideEffectFree})"
        )
        assert(
            (ExternallyPure meet ImpureByAnalysis) == ImpureByAnalysis,
            "LBExternallyPure meet LBImpure was not LBImpure"+
                s" (was ${ExternallyPure meet ImpureByAnalysis})"
        )

        assert(
            (ExternallySideEffectFree meet DPure) == DExternallySideEffectFree,
            "LBExternallySideEffectFree meet LBDPure was not LBDExternallySideEffectFree"+
                s" (was ${ExternallySideEffectFree meet DPure})"
        )
        assert(
            (ExternallySideEffectFree meet DSideEffectFree) == DExternallySideEffectFree,
            "LBExternallySideEffectFree meet LBDSideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${ExternallySideEffectFree meet DSideEffectFree})"
        )
        assert(
            (ExternallySideEffectFree meet DExternallyPure) == DExternallySideEffectFree,
            "LBExternallySideEffectFree meet LBDExternallyPure was not LBDExternallySideEffectFree"+
                s" (was ${ExternallySideEffectFree meet DExternallyPure})"
        )
        assert(
            (ExternallySideEffectFree meet DExternallySideEffectFree) == DExternallySideEffectFree,
            "LBExternallySideEffectFree meet LBDExternallySideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${ExternallySideEffectFree meet DExternallySideEffectFree})"
        )
        assert(
            (ExternallySideEffectFree meet ImpureByAnalysis) == ImpureByAnalysis,
            "LBExternallySideEffectFree meet LBImpure was not LBImpure"+
                s" (was ${ExternallySideEffectFree meet ImpureByAnalysis})"
        )

        assert(
            (DPure meet DSideEffectFree) == DSideEffectFree,
            "LBDPure meet LBDSideEffectFree was not LBDSideEffectFree"+
                s" (was ${DPure meet DSideEffectFree})"
        )
        assert(
            (DPure meet DExternallyPure) == DExternallyPure,
            "LBDPure meet LBDExternallyPure was not LBDExternallyPure"+
                s" (was ${DPure meet DExternallyPure})"
        )
        assert(
            (DPure meet DExternallySideEffectFree) == DExternallySideEffectFree,
            "LBDPure meet LBDExternallySideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${DPure meet DExternallySideEffectFree})"
        )
        assert(
            (DPure meet ImpureByAnalysis) == ImpureByAnalysis,
            "LBDPure meet LBImpure was not LBImpure"+
                s" (was ${DPure meet ImpureByAnalysis})"
        )

        assert(
            (DSideEffectFree meet DExternallyPure) == DExternallySideEffectFree,
            "LBDSideEffectFree meet LBDExternallyPure was not LBDExternallySideEffectFree"+
                s" (was ${DSideEffectFree meet DExternallyPure})"
        )
        assert(
            (DSideEffectFree meet DExternallySideEffectFree) == DExternallySideEffectFree,
            "LBDSideEffectFree meet LBDExternallySideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${DSideEffectFree meet DExternallySideEffectFree})"
        )
        assert(
            (DSideEffectFree meet ImpureByAnalysis) == ImpureByAnalysis,
            "LBDSideEffectFree meet LBImpure was not LBImpure"+
                s" (was ${DSideEffectFree meet ImpureByAnalysis})"
        )

        assert(
            (DExternallyPure meet DExternallySideEffectFree) == DExternallySideEffectFree,
            "LBDExternallyPure meet LBDExternallySideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${DExternallyPure meet DExternallySideEffectFree})"
        )
        assert(
            (DExternallyPure meet ImpureByAnalysis) == ImpureByAnalysis,
            "LBDExternallyPure meet LBImpure was not LBImpure"+
                s" (was ${DExternallyPure meet ImpureByAnalysis})"
        )

        assert(
            (DExternallySideEffectFree meet ImpureByAnalysis) == ImpureByAnalysis,
            "LBDExternallySideEffectFree meet LBImpure was not LBImpure"+
                s" (was ${DExternallySideEffectFree meet ImpureByAnalysis})"
        )
    }
}
