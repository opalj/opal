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

import org.opalj.collection.immutable.IntTrieSet
import org.scalatest.Matchers
import org.scalatest.FlatSpec

/**
 * Tests the [[Purity]] property, especially correctness of the meet operator.
 *
 * @author Dominik Helm
 */
class PurityPropertyTest extends FlatSpec with Matchers {

    val contextuallyPure = ContextuallyPure(IntTrieSet(1))
    val contextuallySideEffectFree = ContextuallySideEffectFree(IntTrieSet(1))
    val dContextuallyPure = DContextuallyPure(IntTrieSet(1))
    val dContextuallySideEffectFree = DContextuallySideEffectFree(IntTrieSet(1))

    val allPurities: List[Purity] = List(
        CompileTimePure, Pure, SideEffectFree, DPure, DSideEffectFree,
        contextuallyPure, contextuallySideEffectFree, ImpureByAnalysis, ImpureByLackOfInformation
    )

    val doesntModifyReceiver: Set[Purity] = Set(
        CompileTimePure, Pure, SideEffectFree, DPure, DSideEffectFree
    )

    val doesntModifyParams: Set[Purity] = Set(
        CompileTimePure, Pure, SideEffectFree, DPure, DSideEffectFree
    )

    "purity levels" should "have the right properties" in {
        for (prop ← allPurities) {
            assert(
                prop.isCompileTimePure == (prop == CompileTimePure),
                s"$prop.isCompileTimePure was ${prop.isCompileTimePure}"
            )
        }

        val deterministic: Set[Purity] = Set(
            CompileTimePure, Pure, contextuallyPure, DPure, dContextuallyPure
        )

        for (prop ← allPurities) {
            assert(
                prop.isDeterministic == deterministic.contains(prop),
                s"$prop.isDeterministic was ${prop.isDeterministic}"
            )
        }

        val doesntUseDomainSpecificActions: Set[Purity] = Set(
            CompileTimePure, Pure, SideEffectFree, contextuallyPure, contextuallySideEffectFree
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
        assert((Pure meet DPure) == DPure)
        assert((Pure meet DSideEffectFree) == DSideEffectFree)
        assert((Pure meet ImpureByAnalysis) == ImpureByAnalysis)

        assert((SideEffectFree meet DPure) == DSideEffectFree)
        assert((SideEffectFree meet DSideEffectFree) == DSideEffectFree)
        assert((SideEffectFree meet ImpureByAnalysis) == ImpureByAnalysis)

        assert(
            (DPure meet DSideEffectFree) == DSideEffectFree,
            "LBDPure meet LBDSideEffectFree was not LBDSideEffectFree"+
                s" (was ${DPure meet DSideEffectFree})"
        )
        assert(
            (DPure meet ImpureByAnalysis) == ImpureByAnalysis,
            "LBDPure meet LBImpure was not LBImpure"+
                s" (was ${DPure meet ImpureByAnalysis})"
        )

        assert(
            (DSideEffectFree meet ImpureByAnalysis) == ImpureByAnalysis,
            "LBDSideEffectFree meet LBImpure was not LBImpure"+
                s" (was ${DSideEffectFree meet ImpureByAnalysis})"
        )

    }
}
