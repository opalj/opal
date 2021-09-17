/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.immutable

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.util.PerformanceEvaluation

/**
 * Tests LongList.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class LongListTest extends AnyFunSpec with Matchers {

    describe("performance") {

        it("memory usage and runtime performance (add)") {
            val Elements = 3000000
            val seed = 123456789L
            val rngGen = new java.util.Random(seed)

            for { i <- 1 to 10 } {
                var l = LongList.empty

                PerformanceEvaluation.memory {
                    PerformanceEvaluation.time {
                        var i = Elements
                        do {
                            l = Math.abs(rngGen.nextLong()) +: l
                            i -= 1
                        } while (i > 0)
                    } { t => info(s"creation took ${t.toSeconds}") }
                } { mu => info(s"required $mu bytes") }

                var sumForeach = 0L
                PerformanceEvaluation.time {
                    l.foreach(sumForeach += _)
                } { t => info(s"foreach sum took ${t.toSeconds}") }

                var sumForFirstThird = 0L
                PerformanceEvaluation.time {
                    l.forFirstN(Elements / 3)(sumForFirstThird += _)
                } { t => info(s"forFirstN(1/3*Elements) sum took ${t.toSeconds}") }

                val sumIterator =
                    PerformanceEvaluation.time {
                        l.iterator.sum
                    } { t => info(s"iterator sum took ${t.toSeconds}") }

                assert(sumForeach == sumIterator)
                info(s"summarized value: ${sumIterator}")
            }

        }
    }
}
