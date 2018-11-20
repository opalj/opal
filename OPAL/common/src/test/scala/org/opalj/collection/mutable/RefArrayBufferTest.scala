/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.mutable

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Tests the RefArrayBuffer
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class RefArrayBufferTest extends FlatSpec with Matchers {

    behavior of "the RefArrayBuffer data structure"

    it should "be empty if it is newly created" in {
        val l1 = RefArrayBuffer.withInitialSize[String](16)
        l1 should be('empty)
        l1.nonEmpty should be(false)

        val l2 = RefArrayBuffer.empty[String]
        l2 should be('empty)
        l2.nonEmpty should be(false)
    }

    it should "be possible to add values" in {
        val l = RefArrayBuffer.empty[String]
        l ++= List("s")
        l ++= Iterator("d", "c")
        l should be('nonEmpty)
        l.size should be(3)

        l(0) should be("s")
        l(1) should be("d")
        l(2) should be("c")
    }

    it should "be possible to add null" in {
        val l = RefArrayBuffer.empty[String]
        l += null
        l ++= Iterator("d")
        l += null
        l should be('nonEmpty)
        l.size should be(3)

        l(0) should be(null)
        l(1) should be("d")
        l(2) should be(null)

        l.toArray should be(Array(null, "d", null))
    }

    it should "be possible to add arrays" in {
        val l = RefArrayBuffer.empty[String]
        l ++= Array(null, "a")
        l ++= Array(null, "b")

        l.toArray should be(Array(null, "a", null, "b"))
    }

}
