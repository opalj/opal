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
package ai
package util

import org.scalatest.ParallelTestExecution
import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Tests the utility methods.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class LocalsTest
        extends FlatSpec
        with Matchers
        with ParallelTestExecution {

    import org.opalj.ai.util._

    behavior of "a Locals data structure"

    it should ("be empty if it has size 0") in {
        Locals(0).isEmpty should be(true)

        Locals(0).nonEmpty should be(false)
    }

    it should ("be non-empty if it has more than one element") in {
        for {
            i ← 1 to 100
            v = Locals[Integer](i)
        } {
            v.isEmpty should be(false)
            v.nonEmpty should be(true)
        }
    }

    it should ("return null for each field after initialization") in {
        for {
            i ← 1 to 100
            v = Locals[Integer](i)
            j ← 0 until i
        } {
            v(j) should be(null)
        }
    }

    it should ("be able to return the value stored at an index") in {
        for {
            i ← 1 to 100
            v = Locals[Integer](i)
            j ← 0 until i
        } {
            v.updated(j, j).apply(j) should equal(j)
        }
    }

    it should ("iterate over all values") in {
        for {
            i ← 1 to 100
        } {
            var v = Locals[Integer](i)
            for { j ← 0 until i } v = v.updated(j, j + 1)
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
            size ← 1 to 100
        } {
            var v = Locals[Integer](size)
            for {
                i ← 0 until size
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
            size ← 10 to 50
        } {
            var v1 = Locals[Integer](size)
            var v2 = Locals[Integer](size)
            var v3 = Locals[Integer](size)
            for {
                i ← 0 until size
            } {
                v1 = v1.updated(i, i)
                v2 = v2.updated(i, i)
                v3 = v3.updated(i, i + 1)
            }
            v1.merge(v2, (a, b) ⇒ { a should equal(b); a })
            val vm = v1.merge(v3, (a, b) ⇒ { a should not equal (b); -1 })
            vm.foreach { v ⇒
                if (v == null || v != -1)
                    fail("null is not -1 (size="+size+"; va="+v1.toString ++ "; vb="+v3.toString+"; vm="+vm.toString+")")
            }
        }
    }

    it should ("be able to set a locals' value") in {
        for {
            size ← 10 to 50
        } {
            var v = Locals[Integer](size)
            for {
                i ← 0 until size
            } {
                v.set(i, i)
                v(i) should be(i)
            }
        }
    }

    it should ("be able to update the locals in-place") in {
        for {
            size ← 10 to 50
        } {
            var v = Locals[Integer](size)
            for { i ← 0 until size } { v.set(i, i) }
            
            v.update(_ + 100)

            for { i ← 0 until size } {
                v(i) should be(i + 100)
            }
        }
    }

}