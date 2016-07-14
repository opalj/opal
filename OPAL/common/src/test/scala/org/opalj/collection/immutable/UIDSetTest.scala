/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers

/**
 * Tests UIDSet.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class UIDSetTest extends FunSpec with Matchers {

    import scala.language.implicitConversions

    case class MyUID(id: Int) extends UID

    implicit def intToUIDInt(i: Int): MyUID = MyUID(i)

    describe("a one element UIDSet") {

        val uidSet = UIDSet[MyUID](1)

        it("it should contain one value") {
            uidSet.mapToList(_.id) should be(List(1))
        }

    }

    describe("a UIDSet with two values") {

        val uidSet = UIDSet(List[MyUID](1, 4, 1, 4, 4))

        it("it should contain two value") {
            uidSet.mapToList(_.id) should be(List(4, 1))
        }

    }

    describe("a UIDSet with three values") {

        val uidSet = UIDSet(List[MyUID](5, 1, 4, 1, 4, 4, 5))

        it("it should contain three values") {
            uidSet.mapToList(_.id) should be(List(5, 4, 1))
        }

    }

    describe("a UIDSet with four values") {

        val uidSet = UIDSet(List[MyUID](5, 8, 1, 4, 8, 1, 4, 4, 5))

        it("it should contain four values") {
            uidSet.mapToList(_.id) should be(List(8, 5, 4, 1))
        }

    }

    describe("a UIDSet with multiple values") {

        val uidSet = UIDSet[UID](List[UID](1, 3, 1, 5, 7, 2, 8, 100, 55, 34, 645, 22, 1, 11, 1239, 23, 3, 21, 5))

        it("it should contain the values in order") {
            uidSet.mapToList(_.id) should be(List(1239, 645, 100, 55, 34, 23, 22, 21, 11, 8, 7, 5, 3, 2, 1))
        }

    }

}
