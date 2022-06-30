/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package mutable

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Tests the IntArrayStack.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class IntArrayStackTest extends AnyFlatSpec with Matchers {

    behavior of "an IntArrayStack"

    it should ("be empty if it is newly created") in {
        val stack = new IntArrayStack()
        stack.foreach { e => fail("non empty") }
        stack.isEmpty should be(true)
        stack.nonEmpty should be(false)
        stack.size should be(0)
        stack.length should be(0)

        (new IntArrayStack(100 /*sizeHint*/ )).foreach { e => fail("non empty") }
    }

    it should ("should only contain those elements that are added") in {
        val stack = new IntArrayStack()
        stack.push(4)
        stack.push(5)
        stack.push(2)

        stack.size should be(3)
        stack.length should be(3)
        stack.isEmpty should be(false)
        stack.nonEmpty should be(true)

        var values: List[Int] = Nil
        stack.foreach { v => values ::= v }
        values.size should be(3)
        values should contain(2)
        values should contain(5)
        values should contain(4)
    }

    it should ("only contain those elements that are added and not popped") in {
        val stack = new IntArrayStack()
        stack.push(4)
        stack.push(5)
        stack.push(2)
        stack.pop() should be(2)
        stack.pop() should be(5)

        stack.size should be(1)
        stack.length should be(1)
        stack.isEmpty should be(false)
        stack.nonEmpty should be(true)

        var values: List[Int] = Nil
        stack.foreach { v => values ::= v }
        values should be(List(4))
    }

    it should ("be empty if all values are popped") in {
        val stack = new IntArrayStack()
        stack.push(4)
        stack.push(5)
        stack.push(2)
        stack.pop() should be(2)
        stack.pop() should be(5)
        stack.pop() should be(4)

        stack.size should be(0)
        stack.length should be(0)
        stack.isEmpty should be(true)
        stack.nonEmpty should be(false)

        try {
            val v = stack.pop()
            fail(s"unexpectedly popped $v from a presumably empty stack")
        } catch {
            case _: Throwable => // everything is OK
        }
    }

    it should ("be possible to sum up the values using foldLeft") in {
        val stack = new IntArrayStack()
        stack.push(4)
        stack.push(5)
        stack.push(2)
        stack.foldLeft(0)(_ + _) should be(11)
    }

    it should ("be possible to call foldLeft even when the stack is empty") in {
        val stack = new IntArrayStack()
        stack.foldLeft(-3)(_ + _) should be(-3)
    }

    it should ("iterator overall values using intIterator") in {
        val stack = new IntArrayStack()
        stack.iterator.hasNext should be(false)

        stack.push(2)
        var it = stack.iterator
        it.hasNext should be(true)
        it.next() should be(2)
        it.hasNext should be(false)

        stack.push(3)
        it = stack.iterator
        it.hasNext should be(true)
        it.next() should be(3)
        it.hasNext should be(true)
        it.next() should be(2)
        it.hasNext should be(false)
    }

    it should ("be comparable to other IntArrayStacks") in {
        val stack1 = new IntArrayStack()
        stack1.push(4)
        stack1.push(5)
        stack1.push(2)

        val stack2 = new IntArrayStack(100)
        stack2.push(4)
        stack2.push(5)
        stack2.push(2)

        val stack3 = new IntArrayStack(50)
        stack3.push(4)
        stack3.push(5)

        stack1 should be(stack2)
        stack1 should not be (stack3)

    }

    it should ("should contain the correct values even if the initial stack size 0") in {
        val stack1 = new IntArrayStack(0)
        stack1.push(1000)
        stack1.pop() should be(1000)
    }

    it should ("should contain the correct values even if the initial stack size was too small") in {
        val Max = 50000
        val stack1 = new IntArrayStack(2)
        Range(start = 0, end = Max).foreach { stack1.push(_) }
        stack1.size should be(Max)

        { // test iteration
            var nextValue = Max - 1
            stack1.foreach { v => v should be(nextValue); nextValue -= 1 }
            stack1.size should be(Max)
        }

        { // test pop
            var nextValue = Max - 1
            Range(start = 0, end = Max).foreach { v =>
                stack1.pop() should be(nextValue); nextValue -= 1
            }
            stack1.size should be(0)
        }
    }

}
