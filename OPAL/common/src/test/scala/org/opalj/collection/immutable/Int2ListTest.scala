/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.immutable

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.util.PerformanceEvaluation

/**
 * Tests Int2List.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class Int2ListTest extends AnyFunSpec with Matchers {

    describe("properties") {

        it("forFirstN(N=0)") {
            var sum = 0
            Int2List(1).forFirstN(0)(sum += _)
            assert(sum == 0L)
        }

        it("forFirstN(N=1)") {
            var sum = 0
            Int2List(1).forFirstN(1)(sum += _)
            assert(sum == 1L)
        }

        it("forFirstN(N=2)") {
            var sum = 0
            Int2List(1, 2).forFirstN(2)(sum += _)
            assert(sum == 3L)
        }

        it("forFirstN(N=3)") {
            var sum = 0
            (4 +: Int2List(1, 2)).forFirstN(3)(sum += _)
            assert(sum == 7L)
        }

        it("forFirstN(N=4)") {
            var sum = 0L
            (5 +: 4 +: Int2List(1, 2)).forFirstN(4)(sum += _)
            assert(sum == 12L)
        }

        it("forFirstN(N=4) of a larger set") {
            var sum = 0L
            (10 +: -10 +: 5 +: 4 +: Int2List(1, 2)).forFirstN(4)(sum += _)
            assert(sum == 9L)
        }

    }

    describe("performance") {

        it("memory usage and runtime performance") {
            val Elements = 3000000
            val seed = 123456789L
            val rngGen = new java.util.Random(seed)

            for { i <- 1 to 10 } {
                var l = Int2List.empty

                PerformanceEvaluation.memory {
                    PerformanceEvaluation.time {
                        var i = Elements
                        do {
                            l = Math.abs(rngGen.nextInt()) +: l
                            i -= 1
                        } while (i > 0)
                    } { t => if (i >= 8) info(s"creation took ${t.toSeconds}") }
                } { mu => if (i >= 8) info(s"required $mu bytes") }

                var sumForeach = 0
                PerformanceEvaluation.time {
                    l.foreach(sumForeach += _)
                } { t => if (i >= 8) info(s"foreach sum took ${t.toSeconds}") }

                var sumForFirstThird = 0
                PerformanceEvaluation.time {
                    l.forFirstN(Elements / 3)(sumForFirstThird += _)
                } { t => if (i >= 8) info(s"forFirstN(1/3*Elements) sum took ${t.toSeconds}") }

                val sumIterator =
                    PerformanceEvaluation.time {
                        l.iterator.sum
                    } { t => if (i >= 8) info(s"iterator sum took ${t.toSeconds}") }

                assert(sumForeach == sumIterator)
                if (i >= 8) info(s"summarized value: $sumIterator")
            }

        }
    }
}
