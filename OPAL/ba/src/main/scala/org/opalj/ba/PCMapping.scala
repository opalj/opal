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
package org.opalj.ba

import java.util.Arrays.copyOf
import java.util.Arrays.fill

/**
 * Mapping of some pc to some new pc. If no mapping exists, Int.MaxValue == PCMapping.Invalid
 * is returned.
 */
class PCMapping(private[ba] var data: Array[Int]) extends (Int ⇒ Int) {

    def this(initialSize: Int) {
        this({
            val a = new Array[Int](Math.max(initialSize, 0))
            fill(a, Int.MaxValue)
            a
        })
    }

    def apply(key: Int): Int = {
        if (key >= data.length)
            PCMapping.Invalid
        else
            data(key)
    }

    def +=(key: Int, value: Int): Unit = {
        val oldLength = data.length
        if (key >= oldLength) {
            val newLength = key + 32
            data = copyOf(data, newLength)
            fill(data, oldLength, newLength, Int.MaxValue)
        }
        data(key) = value
    }

}

object PCMapping {
    final def Invalid = Int.MaxValue
}
