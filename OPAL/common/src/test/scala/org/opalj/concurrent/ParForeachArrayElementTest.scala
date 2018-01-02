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
package org.opalj
package concurrent

import java.util.concurrent.atomic.AtomicInteger

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers

import scala.util.control.ControlThrowable

/**
 * Tests `parForeachArrayElement`.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ParForeachArrayElementTest extends FunSpec with Matchers {

    describe("parForeachArrayElement") {

        it("it should collect all exceptions that are thrown if we just use one thread") {
            val data = Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
            try {
                parForeachArrayElement(data, 1) { e ⇒ throw new RuntimeException(e.toString) }
            } catch {
                case ce: ConcurrentExceptions ⇒
                    assert(ce.getSuppressed.length == 16)
                    assert(ce.getSuppressed.forall(_.isInstanceOf[RuntimeException]))
            }
        }

        it("it should collect all exceptions that are thrown if we use multiple threads") {
            val data = Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
            try {
                parForeachArrayElement(data, 8) { e ⇒ throw new RuntimeException(e.toString) }
            } catch {
                case ce: ConcurrentExceptions ⇒
                    assert(ce.getSuppressed.length == 16)
                    assert(ce.getSuppressed.forall(_.isInstanceOf[RuntimeException]))
            }
        }

        it("it should catch a non-local return and report it") {
            val processed = new AtomicInteger(0)
            def test(): Unit = {
                val data = Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
                try {
                    parForeachArrayElement(data, 8) { e ⇒
                        if (e == 7) return ; else processed.incrementAndGet()
                    }
                } catch {
                    case ce: ConcurrentExceptions ⇒
                        assert(ce.getSuppressed().length == 1)
                        assert(ce.getSuppressed()(0).getCause.isInstanceOf[ControlThrowable])
                }
            }
            test()
            assert(processed.get() == 15)
        }

    }
}
