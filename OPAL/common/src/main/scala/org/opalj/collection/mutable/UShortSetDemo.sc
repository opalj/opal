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

/**
 * This is a scala worksheet to demonstrate how to use UShortSets.
 *
 * @author Michael Eichberg
 */
object UShortSetDemo {

    val empty = UShortSet.empty
    val just0 = UShortSet(0)
    val _0_2 = just0 + 2
    _0_2 + 0
    _0_2 + 2
    val _0_1_2 = _0_2 + 1
    val _0_1_2_65535 = _0_1_2 + 65535
    _0_1_2_65535 + 65535
    _0_1_2_65535 + 0
    _0_1_2_65535 + 1
    _0_1_2_65535 + 2

    val _10_20_30_40 = UShortSet(10) + 30 + 40 + 20
    val _10_30_35_40 = UShortSet(10) + 30 + 40 + 35
    val _5_10_30_40 = UShortSet(10) + 30 + 40 + 5

    val large = _5_10_30_40 + 35 + 500 + 2 + 90 + 5242 + 0 + 1 + 0 + 5 + 30
    large.contains(0)
    large.contains(1)
    large.contains(2)
    large.contains(5)
    large.contains(10)
    large.contains(30)
    large.contains(35)
    large.contains(40)
    large.contains(90)
    large.contains(500)
    large.contains(5242)
    !large.contains(4)
    !large.contains(6666)
    large.max

    _10_20_30_40 + 0
    _10_20_30_40 + 5
    _10_20_30_40 + 15
    _10_20_30_40 + 25
    _10_20_30_40 + 35
    _10_20_30_40 + 45

    UShortSet(12, 23)
    UShortSet(12, 12)
    UShortSet(23, 11)

    try {
        empty + 66666
    } catch {
        case _: IllegalArgumentException ⇒ "OK"
    }

    try {
        empty + -1
    } catch {
        case _: IllegalArgumentException ⇒ "OK"
    }

    UShortSet.create(0, 5, 3, 10, 19, 200, 65, 56, 56, 3, 0).size == 8

    _0_1_2_65535.map(_.toString)

    (_10_20_30_40 + 10) eq _10_20_30_40

    (_0_1_2.mutableCopy) ne _0_1_2
    (_10_20_30_40.mutableCopy) eq _10_20_30_40

    var x = _10_20_30_40
    x += 50
    x += 60
    x
    val y = 60 +≈: (50 +≈: (_10_20_30_40))
    x == y
    x ne y
}