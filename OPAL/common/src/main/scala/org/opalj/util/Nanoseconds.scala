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
package util

/**
 * Represents a time span of `n` nanoseconds.
 *
 * @author Michael Eichberg
 */
class Nanoseconds(val timeSpan: Long) extends AnyVal {

    final def +(other: Nanoseconds): Nanoseconds =
        new Nanoseconds(this.timeSpan + other.timeSpan)

    final def -(other: Nanoseconds): Nanoseconds =
        new Nanoseconds(this.timeSpan - other.timeSpan)

    /**
     * Converts the specified number of nanoseconds into seconds.
     */
    final def toSeconds: Seconds =
        new Seconds(timeSpan.toDouble / 1000.0d / 1000.0d / 1000.0d)

    override def toString: String = timeSpan+" ns"
}
/**
 * Defines factory methods and constants related to time spans in [[Nanoseconds]].
 *
 * @author Michael Eichberg
 */
object Nanoseconds {

    final val None: Nanoseconds = new Nanoseconds(0l)

    def apply(timeSpan: Long): Nanoseconds = new Nanoseconds(timeSpan)

    /**
     * Converts the specified time span and converts it into seconds.
     */
    final def TimeSpan(
        startTimeInNanoseconds: Long,
        endTimeInNanoseconds:   Long
    ): Nanoseconds =
        new Nanoseconds(endTimeInNanoseconds - startTimeInNanoseconds)

}
