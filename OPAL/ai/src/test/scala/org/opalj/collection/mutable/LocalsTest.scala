/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package mutable

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Tests the utility methods.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class LocalsTest extends AnyFlatSpec with Matchers {

    behavior of "a Locals data structure"

    it should ("be empty if it has size 0") in {
        Locals(0).isEmpty should be(true)

        Locals(0).nonEmpty should be(false)
    }

    it should ("be non-empty if it has more than one element") in {
        for {
            i <- 1 to 100
            v = Locals[Integer](i)
        } {
            v.isEmpty should be(false)
            v.nonEmpty should be(true)
        }
    }

    it should ("return null for each field after initialization") in {
        for {
            i <- 1 to 100
            v = Locals[Integer](i)
            j <- 0 until i
        } {
            v(j) should be(null)
        }
    }

    it should ("be able to return the value stored (upated(index,value)) at an index") in {
        for {
            i <- 1 to 100
            v = Locals[Integer](i)
            j <- 0 until i
        } {
            v.updated(j, j).apply(j) should equal(j)
        }
    }

    it should ("be able to return the values stored (upated(index,value1,value2)) at an index") in {
        for {
            i <- 2 to 100
            v = Locals[Integer](i)
            j <- 0 until i - 1
        } {
            val newV = v.updated(j, j, -3)
            newV.apply(j) should equal(j)
            newV.apply(j + 1) should equal(-3)
        }
    }

    it should ("iterate over all values") in {
        for {
            i <- 1 to 100
        } {
            var v = Locals[Integer](i)
            for { j <- 0 until i } v = v.updated(j, j + 1)
            v(i - 1) should be(i)

            {
                var sum = 0
                v.foreach(sum += _)
                sum should be(i * (i + 1) / 2)
            }
            {
                v.iterator.map(_.intValue).sum should be(i * (i + 1) / 2)
            }
        }
    }

    it should ("be able to map the values to some other data-type") in {
        for {
            size <- 1 to 100
        } {
            var v = Locals[Integer](size)
            for {
                i <- 0 until size
            } {
                v = v.updated(i, i)
            }
            val l1 = v.map(String.valueOf(_)).iterator.toList
            val l2 = (0 until size).map(String.valueOf(_))
            l1 should equal(l2)
        }
    }

    it should ("be able to merge two locals") in {
        for {
            size <- 1 to 25
        } {
            var v1 = Locals[Integer](size)
            var v2 = Locals[Integer](size)
            var v3 = Locals[Integer](size)
            for {
                i <- 0 until size
            } {
                v1 = v1.updated(i, i)
                v2 = v2.updated(i, i)
                v3 = v3.updated(i, i + 1)
            }
            v1.fuse(v2, (a, b) => { a should equal(b); a })
            val vm = v1.fuse(v3, (a, b) => { a should not equal (b); -1 })
            vm.foreach { v =>
                if (v == null || v != -1)
                    fail("null is not -1 (size="+size+"; va="+v1.toString ++ "; vb="+v3.toString+"; vm="+vm.toString+")")
            }
        }
    }

    it should ("return \"this\" locals if the merge results in values of \"this\" local") in {
        for {
            size <- 1 to 25
        } {
            var v1 = Locals[Integer](size)
            var v2 = Locals[Integer](size)
            var v3 = Locals[Integer](size)
            for {
                i <- 0 until size
            } {
                v1 = v1.updated(i, i)
                v2 = v2.updated(i, i)
                v3 = v3.updated(i, i + 1)
            }
            v1.fuse(v2, (a, b) => a) should be theSameInstanceAs v1

            v1.fuse(v3, (a, b) => b) should be theSameInstanceAs v3
            v1.fuse(v3, (a, b) => a) should be theSameInstanceAs v1
            v3.fuse(v1, (a, b) => b) should be theSameInstanceAs v1
            v3.fuse(v1, (a, b) => a) should be theSameInstanceAs v3
        }
    }

    it should ("be able to set a locals' value") in {
        for {
            size <- 1 to 25
        } {
            val v = Locals[Integer](size)
            for {
                i <- 0 until size
            } {
                v.set(i, i)
                v(i) should be(i)
            }
        }
    }

    it should ("be able to update the locals in-place") in {
        for {
            size <- 1 to 25
        } {
            val v = Locals[Integer](size)
            for { i <- 0 until size } { v.set(i, i) }

            v.update(_ + 100)

            for { i <- 0 until size } {
                v(i) should be(i + 100)
            }
        }
    }

    it should ("return the same locals if the mapConserve does not update a value") in {
        for {
            size <- 1 to 25
        } {
            val v = Locals[Integer](size)
            for { i <- 0 until size } { v.set(i, i) }

            (v eq v.mapConserve(id => id)) should be(true)

        }
    }

    it should ("be able to map the locals") in {
        for {
            size <- 1 to 25
        } {
            val v = Locals[Integer](size)
            for { i <- 0 until size } { v.set(i, i) }

            val newV = v.mapConserve(_ + 100)

            for { i <- 0 until size } {
                newV(i) should be(i + 100)
            }
        }
    }

    it should ("be able to map the locals using the index") in {
        for {
            size <- 1 to 25
        } {
            val v = Locals[Integer](size)
            for { i <- 0 until size } { v.set(i, i) }

            val newV = v.mapKV[Integer] { (i, v) => assert(i == v); i }

            for { i <- 0 until size } {
                newV(i) should be(i)
            }
        }
    }

    it should ("be able to calculate a hashCode even if it is non-full") in {
        val size = 25
        val v = Locals[Integer](size)
        for { i <- 0 until size } {
            if (i % 2 == 0) v.set(i, i)
        }

        v.hashCode should not be (0)
    }

    it should ("be compareable to a non-full Locals collections") in {
        val size = 25
        val v1 = Locals[Integer](size)
        val v2 = Locals[Integer](size)
        for { i <- 0 until size } {
            if (i % 2 == 0) v1.set(i, i)
            if (i % 2 == 0) v2.set(i, i)
        }

        v1 should equal(v2)
    }

    it should ("be able to get the nthValue") in {
        val ls = Locals[Integer](5)
        ls(0) = 5
        ls(2) = 2
        ls(4) = 4

        ls.nthValue(_ == 1) should be(-1)

        ls.nthValue(_ == 5) should be(0)
        ls.nthValue(_ == 2) should be(1)
        ls.nthValue(_ == 4) should be(2)
    }

}
