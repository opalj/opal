/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.mutable

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Tests the RefAppendChain
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class RefAppendChainTest extends FlatSpec with Matchers {

    behavior of "the RefAppendChain data structure"

    it should ("be empty if it is newly created") in {
        val l = new RefAppendChain[AnyRef]()
        l should be('empty)
        l.nonEmpty should be(false)

    }

    it should ("throw an exception if head is called on an empy list") in {
        assertThrows[NullPointerException](new RefAppendChain[AnyRef]().head)
    }

    it should ("throw an exception if last is called on an empy list") in {
        assertThrows[NullPointerException](new RefAppendChain[AnyRef]().last)
    }

    it should ("return the prepended elements in reverse order") in {
        val c = new RefAppendChain[AnyRef]()
        c.prepend("a").prepend("b").prepend("c")
        c should be('nonEmpty)

        c.head should be("c")
        c.last should be("a")
        c.take() should be("c")
        c.head should be("b")
        c.last should be("a")
        c.take() should be("b")
        c.head should be("a")
        c.last should be("a")
        c.take() should be("a")

        c should be('empty)
        assertThrows[NullPointerException](new RefAppendChain[AnyRef]().head)
        assertThrows[NullPointerException](new RefAppendChain[AnyRef]().last)
    }

    it should ("return the appended elements in order") in {
        val c = new RefAppendChain[AnyRef]()
        c.append("a").append("b").append("c")
        c should be('nonEmpty)

        c.head should be("a")
        c.last should be("c")
        c.take() should be("a")
        c.head should be("b")
        c.last should be("c")
        c.take() should be("b")
        c.head should be("c")
        c.last should be("c")
        c.take() should be("c")

        c should be('empty)
        assertThrows[NullPointerException](new RefAppendChain[AnyRef]().head)
        assertThrows[NullPointerException](new RefAppendChain[AnyRef]().last)
    }

    it should ("after removing pre-/appended elements the list should be empty") in {
        val c = new RefAppendChain[AnyRef]()
        c.append("a").prepend("b").append("c")
        c.take() should be("b")
        c.take() should be("a")
        c.take() should be("c")

        c should be('empty)
        c should not be ('nonEmpty)
        assertThrows[NullPointerException](new RefAppendChain[AnyRef]().head)
        assertThrows[NullPointerException](new RefAppendChain[AnyRef]().last)
    }
}
