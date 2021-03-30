/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.immutable

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

/**
 * Tests Long2List.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class Long2ListTest extends AnyFunSpec with Matchers {

    describe("properties") {

        it("forFirstN(N=0)") {
            var sum = 0L
            Long2List(1L).forFirstN(0)(sum += _)
            assert(sum == 0L)
        }

        it("forFirstN(N=1)") {
            var sum = 0L
            Long2List(1L).forFirstN(1)(sum += _)
            assert(sum == 1L)
        }

        it("forFirstN(N=2)") {
            var sum = 0L
            Long2List(1L, 2L).forFirstN(2)(sum += _)
            assert(sum == 3L)
        }

        it("forFirstN(N=3)") {
            var sum = 0L
            (4L +: Long2List(1L, 2L)).forFirstN(3)(sum += _)
            assert(sum == 7L)
        }

        it("forFirstN(N=4)") {
            var sum = 0L
            (5L +: 4L +: Long2List(1L, 2L)).forFirstN(4)(sum += _)
            assert(sum == 12L)
        }

        it("forFirstN(N=4) of a larger set") {
            var sum = 0L
            val l = (10L +: 10L +: 5L +: 4L +: Long2List(1L, 2L))
            l.forFirstN(4)(sum += _)
            assert(sum == 29L, l)
        }

    }

}
