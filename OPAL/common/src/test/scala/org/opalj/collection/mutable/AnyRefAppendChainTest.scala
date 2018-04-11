/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package org.opalj.collection.mutable

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Tests the AnyRefAppendChain
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class AnyRefAppendChainTest extends FlatSpec with Matchers {

    behavior of "the AnyRefAppendChain data structure"

    it should ("be empty if it is newly created") in {
        val l = new AnyRefAppendChain[AnyRef]()
        l should be('empty)
        l.nonEmpty should be(false)

    }

    it should ("throw an exception if head is called on an empy list") in {
        assertThrows[NullPointerException](new AnyRefAppendChain[AnyRef]().head)
    }

    it should ("throw an exception if last is called on an empy list") in {
        assertThrows[NullPointerException](new AnyRefAppendChain[AnyRef]().last)
    }

    it should ("return the prepended elements in reverse order") in {
        val c = new AnyRefAppendChain[AnyRef]()
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
        assertThrows[NullPointerException](new AnyRefAppendChain[AnyRef]().head)
        assertThrows[NullPointerException](new AnyRefAppendChain[AnyRef]().last)
    }

    it should ("return the appended elements in order") in {
        val c = new AnyRefAppendChain[AnyRef]()
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
        assertThrows[NullPointerException](new AnyRefAppendChain[AnyRef]().head)
        assertThrows[NullPointerException](new AnyRefAppendChain[AnyRef]().last)
    }

    it should ("after removing pre-/appended elements the list should be empty") in {
        val c = new AnyRefAppendChain[AnyRef]()
        c.append("a").prepend("b").append("c")
        c.take() should be("b")
        c.take() should be("a")
        c.take() should be("c")

        c should be('empty)
        c should not be ('nonEmpty)
        assertThrows[NullPointerException](new AnyRefAppendChain[AnyRef]().head)
        assertThrows[NullPointerException](new AnyRefAppendChain[AnyRef]().last)
    }
}
