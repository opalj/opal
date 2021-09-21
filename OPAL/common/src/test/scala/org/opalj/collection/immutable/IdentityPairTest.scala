/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

/**
 * Tests IdentityPair.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class IdentityPairTest extends AnyFunSpec with Matchers {

    describe("an IdentityPair") {

        val a = new String("fooBar")
        val b = "foo"+"Bar"
        require(a ne b)
        require(a == b)
        val p1 = new IdentityPair(a, b) // #1
        val p2 = new IdentityPair(a, a) // #2
        val p3 = new IdentityPair(a, b) // #3

        it("should return the given values") {
            assert(p1._1 eq a)
            assert(p1._2 eq b)
        }

        it("should be equal to a pair containing the same values") {
            p1 should equal(p3)
            p1.hashCode should be(p3.hashCode)
        }

        it("should not be equal to a pair containing equal values") {
            p1 should not equal (p2)
        }

        it("should never be equal to another Product2 (Pair)") {
            val p = ((a, b))
            assert((p._1 eq p1._1) && (p._2 eq p1._2))

            p1 should not equal (p)
            (p) should not equal (p1)
        }
    }

}
