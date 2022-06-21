/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package mutable

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Tests the ArrayMap.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class FixedSizeHashIDMapTest extends AnyFlatSpec with Matchers {

    case class Node(id: Int) {
        override def hashCode(): Int = id
    }

    behavior of "an ArrayMap data structure"

    it should "be empty if it is newly created" in {
        assert(FixedSizedHashIDMap[AnyRef, AnyRef](-10, 10).entries.isEmpty)
        assert(FixedSizedHashIDMap[AnyRef, AnyRef](-10, 10).keys.isEmpty)
        assert(FixedSizedHashIDMap[AnyRef, AnyRef](5, 10).keys.isEmpty)
        assert(FixedSizedHashIDMap[AnyRef, AnyRef](5, 10).entries.isEmpty)
    }

    it should "return the added keys" in {
        val m = FixedSizedHashIDMap[Node, Int](-10, 10)
        m.put(Node(1), 1)
        m.put(Node(8), 8)
        val keys = m.keys
        assert(keys.next() == Node(1))
        assert(keys.next() == Node(8))
    }

    it should "return the added entries using apply" in {
        val m = FixedSizedHashIDMap[Node, Int](-10, 10)
        m.put(Node(1), 1)
        m.put(Node(8), 8)
        m(Node(1)) should be(1)
        m(Node(8)) should be(8)
    }

    it should "return the added entries using entries" in {
        val m = FixedSizedHashIDMap[Node, Int](-10, 10)
        m.put(Node(1), 1)
        m.put(Node(8), 8)
        val entries = m.entries
        entries.next() should be((Node(1), 1))
        entries.next() should be((Node(8), 8))
    }

    it should "return the added entries using iterate" in {
        val m = FixedSizedHashIDMap[Node, Int](-10, 10)
        m.put(Node(1), 1)
        m.put(Node(8), 8)
        var node1Found = false
        var node8Found = false
        m iterate { (n, id) =>
            if (n == Node(1) && id == 1) node1Found = true
            if (n == Node(8) && id == 8) node8Found = true
        }
        assert(node1Found)
        assert(node8Found)
    }

    it should "return the added entries using foreach (explicitly)" in {
        val m = FixedSizedHashIDMap[Node, Int](-10, 10)
        m.put(Node(1), 1)
        m.put(Node(8), 8)
        var node1Found = false
        var node8Found = false
        m foreach { x =>
            val (n, id) = x
            if (n == Node(1) && id == 1) node1Found = true
            if (n == Node(8) && id == 8) node8Found = true
        }
        assert(node1Found)
        assert(node8Found)
    }

    it should "return the added entries using foreach (for comprehension)" in {
        val m = FixedSizedHashIDMap[Node, Int](-10, 10)
        m.put(Node(1), 1)
        m.put(Node(8), 8)
        var node1Found = false
        var node8Found = false
        for (e <- m) {
            val (n, id) = e
            if (n == Node(1) && id == 1) node1Found = true
            if (n == Node(8) && id == 8) node8Found = true
        }
        assert(node1Found)
        assert(node8Found)
    }

}
