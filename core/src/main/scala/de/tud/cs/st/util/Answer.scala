/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package util

/**
 * Models a three state answer (`Yes`, `No`, `Unknown`).
 *
 * @author Michael Eichberg
 */
sealed trait Answer {

    /**
     * Returns `true` if and only if this answer is `Yes`.
     *
     * Effectively the same as a comparison with "Yes".
     */
    def isYes: Boolean

    /**
     * Returns `true` if and only if this answer is `No`.
     *
     * Effectively the same as a comparison with "No".
     */
    def isNo: Boolean

    /**
     * Returns `true` if and only if this answer is `Unknown`.
     *
     * Effectively the same as a comparison with "Unknown".
     */
    def isUnknown: Boolean

    /**
     * Returns `true` if this answer is `Yes` or `Unknown`, `false` otherwise.
     */
    def isNotNo: Boolean

    /**
     * Returns `true` if this answer is `Yes` or `Unknown`, `false` otherwise.
     */
    final def isYesOrUnknown: Boolean = isNotNo

    /**
     * Returns `true` if this answer is `No` or `Unknown`, `false` otherwise.
     */
    def isNotYes: Boolean

    /**
     * Returns `true` if this answer is `No` or `Unknown`, `false` otherwise.
     */
    final def isNoOrUnknown: Boolean = isNotYes

    /**
     * Returns `true` if this answer is either `Yes` or `No`; false if this answer
     * is `Unknown`.
     */
    def isYesOrNo: Boolean

    /**
     * The negation of this `Answer`. If the answer is `Unknown` the negation is
     * still `Unknown`.
     */
    def negate: Answer

    /**
     * Joins this answer and the given answer.
     *
     * If the other `Answer` is identical to this answer `this` answer is returned,
     * otherwise `Unknown` is returned.
     *
     * Facilitates the concatenation of answers using `&=`.
     */
    def &(other: Answer): Answer

    /**
     * If this answer is unknown the given function is evaluated and that
     * result is returned, otherwise `this` answer is returned.
     */
    def ifUnknown(f: ⇒ Answer): Answer = this
}

/**
 * Factory for `Answer` objects.
 *
 * @author Michael Eichberg
 */
object Answer {
    /**
     * Returns [[de.tud.cs.st.util.Yes]] if `value` is `true` and
     * [[de.tud.cs.st.util.No]] otherwise.
     */
    def apply(value: Boolean): Answer = if (value) Yes else No
}

/**
 * Represents a `Yes` answer to a question.
 */
final case object Yes extends Answer {
    override def isYes: Boolean = true
    override def isNo: Boolean = false
    override def isUnknown: Boolean = false

    override def isNotNo: Boolean = true
    override def isNotYes: Boolean = false
    override def isYesOrNo: Boolean = true

    override def negate = No
    override def &(other: Answer) = if (other eq this) this else Unknown
}
/**
 * Represents a `No` answer to a question.
 */
final case object No extends Answer {
    override def isYes: Boolean = false
    override def isNo: Boolean = true
    override def isUnknown: Boolean = false

    override def isNotNo: Boolean = false
    override def isNotYes: Boolean = true
    override def isYesOrNo: Boolean = true

    override def negate = Yes
    override def &(other: Answer) = if (other eq this) this else Unknown
}
/**
 * Represents an `Unknown` answer to a question.
 */
final case object Unknown extends Answer {
    override def isYes: Boolean = false
    override def isNo: Boolean = false
    override def isUnknown: Boolean = true

    override def isNotNo: Boolean = true
    override def isNotYes: Boolean = true
    override def isYesOrNo: Boolean = false

    override def negate = this
    override def &(other: Answer) = Unknown
    override def ifUnknown(f: ⇒ Answer): Answer = f
}



