/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.mutable

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Tests the RefAccumulator.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class RefAccumulatorTest extends AnyFlatSpec with Matchers {

    behavior of "the RefAccumulator data structure"

    it should "be empty if it is newly created" in {
        val l = RefAccumulator.empty[String]
        l should be(Symbol("empty"))
        l.nonEmpty should be(false)
    }

    it should "be possible to add reference values" in {
        val l = RefAccumulator.empty[String]
        l += "s"
        l += "d"
        l should be(Symbol("nonEmpty"))
        l.pop() should be("d")
        l should be(Symbol("nonEmpty"))
        l.pop() should be("s")
        l should be(Symbol("empty"))
    }

    it should "be possible to add collections of reference values" in {
        val l = RefAccumulator.empty[String]
        l ++= List("s")
        l ++= Iterator("d", "c")
        l should be(Symbol("nonEmpty"))
        l.pop() should be("d")
        l.pop() should be("c")
        l should be(Symbol("nonEmpty"))
        l.pop() should be("s")
        l should be(Symbol("empty"))
    }

    it should "be possible to add collections of reference values and also reference values" in {
        val l = RefAccumulator.empty[String]
        l ++= List("s")
        l += "x"
        l ++= Iterator("d", "c")
        l should be(Symbol("nonEmpty"))
        l.pop() should be("d")
        l.pop() should be("c")
        l should be(Symbol("nonEmpty"))
        l.pop() should be("x")
        l should be(Symbol("nonEmpty"))
        l.pop() should be("s")
        l should be(Symbol("empty"))
    }
}
