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
package util

import play.api.libs.json.JsNumber
import play.api.libs.json.JsPath
import play.api.libs.json.Reads
import play.api.libs.json.Writes

/**
 * Represents a time span of `n` seconds.
 *
 * @author Michael Eichberg
 */
class Seconds(val timeSpan: Double) extends AnyVal with Serializable {

    def toString(withUnit: Boolean): String = {
        val time = f"$timeSpan%.4f"
        if (withUnit) {
            time+" s"
        } else {
            time
        }
    }

    def +(other: Seconds): Seconds = new Seconds(this.timeSpan + other.timeSpan)

    final def toNanoseconds: Nanoseconds = {
        new Nanoseconds((timeSpan * 1000.0d * 1000.0d * 1000.0d).toLong)
    }

    /**
     * Conversion to [[Milliseconds]].
     */
    final def toMilliseconds: Milliseconds = {
        new Milliseconds((timeSpan * 1000).toLong)
    }

    override def toString: String = toString(withUnit = true)

}
/**
 * Common constants related to seconds.
 *
 * @author Michael Eichberg
 */
object Seconds {

    implicit val secondsWrites = new Writes[Seconds] {
        def writes(second: Seconds) = JsNumber(second.timeSpan)
    }

    implicit val secondsReads: Reads[Seconds] = JsPath.read[Double].map(Seconds.apply)

    def apply(timeSpan: Double): Seconds = new Seconds(timeSpan)

    final val None: Seconds = new Seconds(0d)

}
