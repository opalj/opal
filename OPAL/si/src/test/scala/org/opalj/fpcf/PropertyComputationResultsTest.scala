/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import org.scalatest.funsuite.AnyFunSuite

import org.opalj.fpcf.fixtures.NilProperty
import org.opalj.fpcf.fixtures.Palindromes.NoPalindrome
import org.opalj.fpcf.fixtures.Palindromes.Palindrome

class PropertyComputationResultsTest extends AnyFunSuite {

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
                InterimPartialResult.id
            )

        assert(resultTypeIds.toSet.toList.sorted === resultTypeIds.sorted)
    }

}

class NoResultTest extends AnyFunSuite {

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

class ResultTest extends AnyFunSuite {

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

class MultiResultTest extends AnyFunSuite {

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

class InterimResultTest extends AnyFunSuite {

    test("an InterimResult with an upper bound is a proper result") {
        val r: PropertyComputationResult =
            InterimResult(
                InterimEUBP(new Object, NilProperty),
                Set(EPK(new Object, NilProperty.key)),
                _ => ???
            )
        assert(r.isInstanceOf[ProperPropertyComputationResult])
        assert(!r.isInstanceOf[FinalPropertyComputationResult])
    }

    test("an InterimResult with a lower bound is a proper result") {
        val r: PropertyComputationResult =
            InterimResult(
                InterimELBP(new Object, NilProperty),
                Set(EPK(new Object, NilProperty.key)),
                _ => ???
            )
        assert(r.isInstanceOf[ProperPropertyComputationResult])
        assert(!r.isInstanceOf[FinalPropertyComputationResult])
    }

    test("an InterimResult with a lower and an upper bound is a proper result") {
        val r: PropertyComputationResult =
            InterimResult(
                InterimELUBP("c", NoPalindrome, Palindrome),
                Set(EPK(new Object, NilProperty.key)),
                _ => ???
            )
        assert(r.isInstanceOf[ProperPropertyComputationResult])
        assert(!r.isInstanceOf[FinalPropertyComputationResult])
    }

    test("an InterimResult is an InterimResult") {
        val r: PropertyComputationResult =
            InterimResult(
                InterimELUBP("c", NoPalindrome, Palindrome),
                Set(EPK(new Object, NilProperty.key)),
                _ => ???
            )
        assert(r.isInterimResult)
    }

    test("an InterimResult can be cast to an InterimResult using asInterimResult") {
        val r: PropertyComputationResult =
            InterimResult(
                InterimELUBP("c", NoPalindrome, Palindrome),
                Set(EPK(new Object, NilProperty.key)),
                _ => ???
            )
        assert(r.asInterimResult eq r)
    }

    test("the specialized factory method for InterimResults with a lower and and upper bound creates the same EPS as if the EPS was created explicitly") {
        val dependees: Set[SomeEOptionP] = Set(EPK(new Object, NilProperty.key))
        val r: PropertyComputationResult =
            InterimResult(InterimELUBP("c", NoPalindrome, Palindrome), dependees, _ => ???)
        val rFactory: PropertyComputationResult =
            InterimResult("c", NoPalindrome, Palindrome, dependees, _ => ???)
        assert(r == rFactory)
    }

    test("two InterimResults for upper bounds are equal when property and dependency lists are equal") {
        val dependees: Set[SomeEOptionP] = Set(EPK(new Object, NilProperty.key))
        val r: PropertyComputationResult =
            InterimResult(InterimEUBP("c", Palindrome), dependees, _ => ???)
        val rFactory: PropertyComputationResult =
            InterimResult.forUB("c", Palindrome, dependees, _ => ???)
        assert(r == rFactory)
    }

    test("two InterimResults for lower bounds are equal when property and dependency lists are equal") {
        val dependees: Set[SomeEOptionP] = Set(EPK(new Object, NilProperty.key))
        val r: PropertyComputationResult =
            InterimResult(InterimELBP("c", Palindrome), dependees, _ => ???)
        val rFactory: PropertyComputationResult =
            InterimResult.forLB("c", Palindrome, dependees, _ => ???)
        assert(r == rFactory)
    }

    // TODO test the Debug functionality...
}

class PartialResultTest extends AnyFunSuite {
    test("a PartialResult is a proper result") {
        val r: PropertyComputationResult = PartialResult(new Object, NilProperty.key, null)
        assert(r.isInstanceOf[ProperPropertyComputationResult])
        assert(!r.isInstanceOf[FinalPropertyComputationResult])
    }

    test("a PartialResult is not an InterimResult") {
        val r: PropertyComputationResult = PartialResult(new Object, NilProperty.key, null)
        assert(!r.isInterimResult)
    }

    test("a PartialResult cannot be cast to an InterimResult") {
        val r: PropertyComputationResult = PartialResult(new Object, NilProperty.key, null)
        assertThrows[ClassCastException](r.asInterimResult)
    }

    test("a PartialResult cannot be cast to a Result") {
        val r: PropertyComputationResult = PartialResult(new Object, NilProperty.key, null)
        assertThrows[ClassCastException](r.asResult)
    }
}

class InterimPartialResultTest extends AnyFunSuite {
    test("an InterimPartialResult is a proper result") {
        val dependees: Set[SomeEOptionP] = Set(EPK(new Object, NilProperty.key))
        val r: PropertyComputationResult = InterimPartialResult(dependees, _ => ???)
        assert(r.isInstanceOf[ProperPropertyComputationResult])
        assert(!r.isInstanceOf[FinalPropertyComputationResult])
    }

    test("an InterimPartialResult is not an InterimResult") {
        val dependees: Set[SomeEOptionP] = Set(EPK(new Object, NilProperty.key))
        val r: PropertyComputationResult = InterimPartialResult(dependees, _ => ???)
        assert(!r.isInterimResult)
    }

    test("an InterimPartialResult cannot be cast to an InterimResult") {
        val dependees: Set[SomeEOptionP] = Set(EPK(new Object, NilProperty.key))
        val r: PropertyComputationResult = InterimPartialResult(dependees, _ => ???)
        assertThrows[ClassCastException](r.asInterimResult)
    }

    test("an InterimPartialResult cannot be cast to a Result") {
        val dependees: Set[SomeEOptionP] = Set(EPK(new Object, NilProperty.key))
        val r: PropertyComputationResult = InterimPartialResult(dependees, _ => ???)
        assertThrows[ClassCastException](r.asResult)
    }
}