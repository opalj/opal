/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package fixtures

/**
 * Models a property related to palindromes and "super palindromes"
 *
 * @note Only intended to be used as a test fixture.
 */
object Palindromes {

    final val PalindromeKey: PropertyKey[PalindromeProperty] = {
        def fallback(
            ps:     PropertyStore,
            reason: FallbackReason,
            e:      Entity
        ): PalindromeProperty = reason match {
            case PropertyIsNotComputedByAnyAnalysis               => NoAnalysisForPalindromeProperty
            case PropertyIsNotDerivedByPreviouslyExecutedAnalysis => PalindromePropertyNotAnalyzed
        }
        PropertyKey.create[Entity, PalindromeProperty]("Palindrome", fallback _)
    }

    sealed trait PalindromeProperty extends Property {
        type Self = PalindromeProperty
        def key: PropertyKey[PalindromeProperty] = PalindromeKey
    }
    case object Palindrome extends PalindromeProperty
    case object NoPalindrome extends PalindromeProperty
    case object PalindromePropertyNotAnalyzed extends PalindromeProperty
    case object NoAnalysisForPalindromeProperty extends PalindromeProperty

    /**
     * Here, a palindrome is considered to be a super palindrome if also the first half
     * of the palindrome is another palindrome; e.g., aacaa is a super palindrome while
     * abcba is not. If the entities' size is odd, the middle element is ignored.
     *
     * @note This definition is totally arbitrary and only used for testing purposes; it is
     *       not a general definition.
     */
    final val SuperPalindromeKey = {
        PropertyKey.create[Entity, SuperPalindromeProperty]("SuperPalindrome", NoSuperPalindrome)
    }

    sealed trait SuperPalindromeProperty extends Property {
        type Self = SuperPalindromeProperty
        def key: PropertyKey[SuperPalindromeProperty] = SuperPalindromeKey
    }
    case object SuperPalindrome extends SuperPalindromeProperty
    case object NoSuperPalindrome extends SuperPalindromeProperty

    /**
     * A collection of the first halfs of palindromes.
     */
    final val PalindromeFragmentsKey = {
        PropertyKey.create[String, PalindromeFragments]("PalindromeFragments")
    }

    case class PalindromeFragments(fragments: Set[String]) extends Property {
        type Self = PalindromeFragments
        def key: PropertyKey[PalindromeFragments] = PalindromeFragmentsKey
    }

}
