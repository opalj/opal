/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import org.scalatest.FunSuite

import org.opalj.fpcf.fixtures.NilProperty
import org.opalj.fpcf.fixtures.Palindromes.NoPalindrome
import org.opalj.fpcf.fixtures.Palindromes.Palindrome

class PropertyComputationResultsTest extends FunSuite {

    test("the ids of the different types of results are different") {
        val resultTypeIds =
            List(
                NoResult.id,
                Result.id,
                MultiResult.id,
                IncrementalResult.id,
                InterimResult.id,
                Results.id,
                PartialResult.id,
                IdempotentResult.id
            )

        assert(resultTypeIds.toSet.toList.sorted === resultTypeIds.sorted)
    }

}

class NoResultTest extends FunSuite {

    test("a NoResult is not a proper result") {
        val r: PropertyComputationResult = NoResult
        assert(!r.isInstanceOf[ProperPropertyComputationResult])
    }

    test("a NoResult is not an InterimResult") {
        assert(!NoResult.isInterimResult)
    }

    test("a NoResult cannot be cast to an InterimResult") {
        assertThrows[ClassCastException](NoResult.asInterimResult)
    }

}

class ResultTest extends FunSuite {

    test("a Result is a proper result") {
        val r: PropertyComputationResult = Result(new Object, NilProperty)
        assert(r.isInstanceOf[ProperPropertyComputationResult])
        assert(r.isInstanceOf[FinalPropertyComputationResult])
    }

    test("a Result is not an InterimResult") {
        val r = Result(new Object, NilProperty)
        assert(!r.isInterimResult)
    }

    test("a Result cannot be cast to an InterimResult") {
        val r = Result(new Object, NilProperty)
        assertThrows[ClassCastException](r.asInterimResult)
    }

}

class MultiResultTest extends FunSuite {

    test("a MultiResult is a proper result") {
        val r: PropertyComputationResult = MultiResult(List(FinalEP(new Object, NilProperty)))
        assert(r.isInstanceOf[ProperPropertyComputationResult])
        assert(r.isInstanceOf[FinalPropertyComputationResult])
    }

    test("a MultiResult is not an InterimResult") {
        val r = MultiResult(List(FinalEP(new Object, NilProperty)))
        assert(!r.isInterimResult)
    }

    test("a MultiResult cannot be cast to an InterimResult") {
        val r = MultiResult(List(FinalEP(new Object, NilProperty)))
        assertThrows[ClassCastException](r.asInterimResult)
    }

}

class InterimResultTest extends FunSuite {

    test("an InterimResult with an upper bound is a proper result") {
        val r: PropertyComputationResult =
            InterimResult(
                InterimEUBP(new Object, NilProperty),
                List(EPK(new Object, NilProperty.key)),
                _ ⇒ ???
            )
        assert(r.isInstanceOf[ProperPropertyComputationResult])
        assert(!r.isInstanceOf[FinalPropertyComputationResult])
    }

    test("an InterimResult with a lower bound is a proper result") {
        val r: PropertyComputationResult =
            InterimResult(
                InterimELBP(new Object, NilProperty),
                List(EPK(new Object, NilProperty.key)),
                _ ⇒ ???
            )
        assert(r.isInstanceOf[ProperPropertyComputationResult])
        assert(!r.isInstanceOf[FinalPropertyComputationResult])
    }

    test("an InterimResult with a lower and an upper bound is a proper result") {
        val r: PropertyComputationResult =
            InterimResult(
                InterimELUBP("c", NoPalindrome, Palindrome),
                List(EPK(new Object, NilProperty.key)),
                _ ⇒ ???
            )
        assert(r.isInstanceOf[ProperPropertyComputationResult])
        assert(!r.isInstanceOf[FinalPropertyComputationResult])
    }

    test("an InterimResult is an InterimResult") {
        val r: PropertyComputationResult =
            InterimResult(
                InterimELUBP("c", NoPalindrome, Palindrome),
                List(EPK(new Object, NilProperty.key)),
                _ ⇒ ???
            )
        assert(r.isInterimResult)
    }

    test("an InterimResult can be cast to an InterimResult using asInterimResult") {
        val r: PropertyComputationResult =
            InterimResult(
                InterimELUBP("c", NoPalindrome, Palindrome),
                List(EPK(new Object, NilProperty.key)),
                _ ⇒ ???
            )
        assert(r.asInterimResult eq r)
    }

    test("the specialized factory method for InterimResults with a lower and and upper bound creates the same EPS as if the EPS was created explicitly") {
        val dependees = List(EPK(new Object, NilProperty.key))
        val r: PropertyComputationResult =
            InterimResult(InterimELUBP("c", NoPalindrome, Palindrome), dependees, _ ⇒ ???)
        val rFactory: PropertyComputationResult =
            InterimResult("c", NoPalindrome, Palindrome, dependees, _ ⇒ ???)
        assert(r == rFactory)
    }

    test("two InterimResults for upper bounds are equal when property and dependency lists are equal") {
        val dependees = List(EPK(new Object, NilProperty.key))
        val r: PropertyComputationResult =
            InterimResult(InterimEUBP("c", Palindrome), dependees, _ ⇒ ???)
        val rFactory: PropertyComputationResult =
            InterimResult.forUB("c", Palindrome, dependees, _ ⇒ ???)
        assert(r == rFactory)
    }

    test("two InterimResults for lower bounds are equal when property and dependency lists are equal") {
        val dependees = List(EPK(new Object, NilProperty.key))
        val r: PropertyComputationResult =
            InterimResult(InterimELBP("c", Palindrome), dependees, _ ⇒ ???)
        val rFactory: PropertyComputationResult =
            InterimResult.forLB("c", Palindrome, dependees, _ ⇒ ???)
        assert(r == rFactory)
    }

    // TOOD test the Debug functionality...
}
