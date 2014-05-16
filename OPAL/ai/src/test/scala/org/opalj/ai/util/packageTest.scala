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
class packageTest
        extends FlatSpec
        with Matchers
        with ParallelTestExecution {

    import org.opalj.ai.util._

    behavior of "the function removeFirstWhile"

    val shortList = List(1, 5, 4)
    val longList = List(1, 4, 5, 6, 4848, 34, 35, 35, 37)

    it should ("return the given list if no element matches") in {
        val newList = removeFirstUnless(longList, -1)(_ <= 0)
        newList should be(longList)
        System.identityHashCode(newList) should be(System.identityHashCode(longList))
    }

    it should ("return the given list if the matching element is not considered") in {
        val newList = removeFirstUnless(longList, 34)(_ >= 1000)
        newList should be(longList)
        System.identityHashCode(newList) should be(System.identityHashCode(longList))
    }

    it should ("return the list even if the head matches but it does not pass the filter") in {
        val newList = removeFirstUnless(longList, 1)(_ >= -1)
        newList should be(longList)
    }

    it should ("return the list without it's head if the head matches") in {
        val newList = removeFirstUnless(longList, 1)(_ >= 1000)
        newList should be(longList.tail)
    }

    it should ("return the list without it's second element if that element matches") in {
        val newList = removeFirstUnless(longList, 4)(_ >= 1000)
        newList should be(longList.head :: longList.tail.tail)
    }

    it should ("return the list without it's last element if that element matches") in {
        val newList = removeFirstUnless(shortList, 4)(_ >=1000)
        newList should be(List(1, 5))
    }
}