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
        CompileTimePure, LBPure, LBSideEffectFree, LBExternallyPure, LBExternallySideEffectFree,
        LBDPure, LBDSideEffectFree, LBDExternallyPure, LBDExternallySideEffectFree,
        LBContextuallyPure, LBContextuallySideEffectFree, LBImpure, Impure
    )

    val doesntModifyReceiver: Set[Purity] = Set(
        CompileTimePure, LBPure, LBSideEffectFree, LBDPure, LBDSideEffectFree
    )

    val doesntModifyParams: Set[Purity] = Set(
        CompileTimePure, LBPure, LBSideEffectFree, LBExternallyPure, LBExternallySideEffectFree,
        LBDPure, LBDSideEffectFree, LBDExternallyPure, LBDExternallySideEffectFree
    )

    "purity levels" should "have the right properties" in {
        for (prop ← allPurities) {
            assert(
                prop.isCompileTimePure ^ prop == CompileTimePure,
                s"$prop.isCompileTimePure was ${prop.isCompileTimePure}"
            )
        }

        val deterministic: Set[Purity] = Set(
            CompileTimePure, LBPure, LBExternallyPure, LBContextuallyPure, LBDPure,
            LBDExternallyPure, LBDContextuallyPure
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
            CompileTimePure, LBPure, LBSideEffectFree, LBExternallyPure, LBExternallySideEffectFree,
            LBContextuallyPure, LBContextuallySideEffectFree
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

        assert((LBPure meet LBSideEffectFree) == LBSideEffectFree)
        assert((LBPure meet LBExternallyPure) == LBExternallyPure)
        assert((LBPure meet LBExternallySideEffectFree) == LBExternallySideEffectFree)
        assert((LBPure meet LBDPure) == LBDPure)
        assert((LBPure meet LBDSideEffectFree) == LBDSideEffectFree)
        assert((LBPure meet LBDExternallyPure) == LBDExternallyPure)
        assert((LBPure meet LBDExternallySideEffectFree) == LBDExternallySideEffectFree)
        assert((LBPure meet LBImpure) == LBImpure)

        assert((LBSideEffectFree meet LBExternallyPure) == LBExternallySideEffectFree)
        assert((LBSideEffectFree meet LBExternallySideEffectFree) == LBExternallySideEffectFree)
        assert((LBSideEffectFree meet LBDPure) == LBDSideEffectFree)
        assert((LBSideEffectFree meet LBDSideEffectFree) == LBDSideEffectFree)
        assert((LBSideEffectFree meet LBDExternallyPure) == LBDExternallySideEffectFree)
        assert((LBSideEffectFree meet LBDExternallySideEffectFree) == LBDExternallySideEffectFree)
        assert((LBSideEffectFree meet LBImpure) == LBImpure)

        assert(
            (LBExternallyPure meet LBExternallySideEffectFree) == LBExternallySideEffectFree,
            "LBExternallyPure meet LBExternallySideEffectFree was not LBExternallySideEffectFree"+
                s" (was ${LBExternallyPure meet LBExternallySideEffectFree})"
        )
        assert(
            (LBExternallyPure meet LBDPure) == LBDExternallyPure,
            "LBExternallyPure meet LBDPure was not LBDExternallyPure"+
                s" (was ${LBExternallyPure meet LBDPure})"
        )
        assert(
            (LBExternallyPure meet LBDSideEffectFree) == LBDExternallySideEffectFree,
            "LBExternallyPure meet LBDSideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${LBExternallyPure meet LBDSideEffectFree})"
        )
        assert(
            (LBExternallyPure meet LBDExternallyPure) == LBDExternallyPure,
            "LBExternallyPure meet LBDExternallyPure was not LBDExternallyPure"+
                s" (was ${LBExternallyPure meet LBDExternallyPure})"
        )
        assert(
            (LBExternallyPure meet LBDExternallySideEffectFree) == LBDExternallySideEffectFree,
            "LBExternallyPure meet LBDExternallySideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${LBExternallyPure meet LBDExternallySideEffectFree})"
        )
        assert(
            (LBExternallyPure meet LBImpure) == LBImpure,
            "LBExternallyPure meet LBImpure was not LBImpure"+
                s" (was ${LBExternallyPure meet LBImpure})"
        )

        assert(
            (LBExternallySideEffectFree meet LBDPure) == LBDExternallySideEffectFree,
            "LBExternallySideEffectFree meet LBDPure was not LBDExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree meet LBDPure})"
        )
        assert(
            (LBExternallySideEffectFree meet LBDSideEffectFree) == LBDExternallySideEffectFree,
            "LBExternallySideEffectFree meet LBDSideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree meet LBDSideEffectFree})"
        )
        assert(
            (LBExternallySideEffectFree meet LBDExternallyPure) == LBDExternallySideEffectFree,
            "LBExternallySideEffectFree meet LBDExternallyPure was not LBDExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree meet LBDExternallyPure})"
        )
        assert(
            (LBExternallySideEffectFree meet LBDExternallySideEffectFree) == LBDExternallySideEffectFree,
            "LBExternallySideEffectFree meet LBDExternallySideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${LBExternallySideEffectFree meet LBDExternallySideEffectFree})"
        )
        assert(
            (LBExternallySideEffectFree meet LBImpure) == LBImpure,
            "LBExternallySideEffectFree meet LBImpure was not LBImpure"+
                s" (was ${LBExternallySideEffectFree meet LBImpure})"
        )

        assert(
            (LBDPure meet LBDSideEffectFree) == LBDSideEffectFree,
            "LBDPure meet LBDSideEffectFree was not LBDSideEffectFree"+
                s" (was ${LBDPure meet LBDSideEffectFree})"
        )
        assert(
            (LBDPure meet LBDExternallyPure) == LBDExternallyPure,
            "LBDPure meet LBDExternallyPure was not LBDExternallyPure"+
                s" (was ${LBDPure meet LBDExternallyPure})"
        )
        assert(
            (LBDPure meet LBDExternallySideEffectFree) == LBDExternallySideEffectFree,
            "LBDPure meet LBDExternallySideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${LBDPure meet LBDExternallySideEffectFree})"
        )
        assert(
            (LBDPure meet LBImpure) == LBImpure,
            "LBDPure meet LBImpure was not LBImpure"+
                s" (was ${LBDPure meet LBImpure})"
        )

        assert(
            (LBDSideEffectFree meet LBDExternallyPure) == LBDExternallySideEffectFree,
            "LBDSideEffectFree meet LBDExternallyPure was not LBDExternallySideEffectFree"+
                s" (was ${LBDSideEffectFree meet LBDExternallyPure})"
        )
        assert(
            (LBDSideEffectFree meet LBDExternallySideEffectFree) == LBDExternallySideEffectFree,
            "LBDSideEffectFree meet LBDExternallySideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${LBDSideEffectFree meet LBDExternallySideEffectFree})"
        )
        assert(
            (LBDSideEffectFree meet LBImpure) == LBImpure,
            "LBDSideEffectFree meet LBImpure was not LBImpure"+
                s" (was ${LBDSideEffectFree meet LBImpure})"
        )

        assert(
            (LBDExternallyPure meet LBDExternallySideEffectFree) == LBDExternallySideEffectFree,
            "LBDExternallyPure meet LBDExternallySideEffectFree was not LBDExternallySideEffectFree"+
                s" (was ${LBDExternallyPure meet LBDExternallySideEffectFree})"
        )
        assert(
            (LBDExternallyPure meet LBImpure) == LBImpure,
            "LBDExternallyPure meet LBImpure was not LBImpure"+
                s" (was ${LBDExternallyPure meet LBImpure})"
        )

        assert(
            (LBDExternallySideEffectFree meet LBImpure) == LBImpure,
            "LBDExternallySideEffectFree meet LBImpure was not LBImpure"+
                s" (was ${LBDExternallySideEffectFree meet LBImpure})"
        )
    }
}
