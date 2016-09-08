/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package collection
package mutable

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Tests the IntArrayStack.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class IntArrayStackTest extends FlatSpec with Matchers {

    behavior of "a the IntArrayStack data structure"

    it should ("be empty if it is newly created") in {
        val stack = new IntArrayStack()
        stack.foreach { e ⇒ fail("non empty") }
        stack.isEmpty should be(true)
        stack.nonEmpty should be(false)
        stack.size should be(0)
        stack.length should be(0)

        (new IntArrayStack(100 /*sizeHint*/ )).foreach { e ⇒ fail("non empty") }
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
        stack.foreach { v ⇒ values ::= v }
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
        stack.foreach { v ⇒ values ::= v }
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
            case t: Throwable ⇒ // everything is OK 
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
            stack1.foreach { v ⇒ v should be(nextValue); nextValue -= 1 }
            stack1.size should be(Max)
        }

        { // test pop
            var nextValue = Max - 1
            Range(start = 0, end = Max).foreach { v ⇒
                stack1.pop should be(nextValue); nextValue -= 1
            }
            stack1.size should be(0)
        }
    }

}
