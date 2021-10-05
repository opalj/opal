/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.collection.immutable.IntTrieSet

/**
 * Tests the [[Purity]] property, especially correctness of the meet operator.
 *
 * @author Dominik Helm
 */
class PurityPropertyTest extends AnyFlatSpec with Matchers {

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
        for (prop <- allPurities) {
            assert(
                prop.isCompileTimePure == (prop == CompileTimePure),
                s"$prop.isCompileTimePure was ${prop.isCompileTimePure}"
            )
        }

        val deterministic: Set[Purity] = Set(
            CompileTimePure, Pure, contextuallyPure, DPure, dContextuallyPure
        )

        for (prop <- allPurities) {
            assert(
                prop.isDeterministic == deterministic.contains(prop),
                s"$prop.isDeterministic was ${prop.isDeterministic}"
            )
        }

        val doesntUseDomainSpecificActions: Set[Purity] = Set(
            CompileTimePure, Pure, SideEffectFree, contextuallyPure, contextuallySideEffectFree
        )

        for (prop <- allPurities) {
            assert(
                prop.usesDomainSpecificActions != doesntUseDomainSpecificActions.contains(prop),
                s"$prop.usesDomainSpecicifActions was ${prop.usesDomainSpecificActions}"
            )
        }
    }

    they should "be converted correctly" in {
        for { prop <- allPurities } {
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
        for (prop1 <- allPurities) {
            assert((prop1 meet prop1) == prop1, s"meet was not reflexive for $prop1")
            for (prop2 <- allPurities) {
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
        for (prop <- allPurities) {
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
