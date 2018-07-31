/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.mutable

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Tests the AnyRefAccumulator.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class AnyRefAccumulatorTest extends FlatSpec with Matchers {

    behavior of "the AnyRefAccumulator data structure"

    it should "be empty if it is newly created" in {
        val l = AnyRefAccumulator.empty[String]
        l should be('empty)
        l.nonEmpty should be(false)
    }

    it should "be possible to add primitive values" in {
        val l = AnyRefAccumulator.empty[String]
        l += "s"
        l += "d"
        l should be('nonEmpty)
        l.pop() should be("d")
        l should be('nonEmpty)
        l.pop() should be("s")
        l should be('empty)
    }

    it should "be possible to add collections of primitive values" in {
        val l = AnyRefAccumulator.empty[String]
        l ++= List("s")
        l ++= Iterator("d", "c")
        l should be('nonEmpty)
        l.pop() should be("d")
        l.pop() should be("c")
        l should be('nonEmpty)
        l.pop() should be("s")
        l should be('empty)
    }

    it should "be possible to add collections of primitive values and also primitive values" in {
        val l = AnyRefAccumulator.empty[String]
        l ++= List("s")
        l += "x"
        l ++= Iterator("d", "c")
        l should be('nonEmpty)
        l.pop() should be("d")
        l.pop() should be("c")
        l should be('nonEmpty)
        l.pop() should be("x")
        l should be('nonEmpty)
        l.pop() should be("s")
        l should be('empty)
    }
}
