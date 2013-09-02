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
package bat

/**
 * Models a three state answer.
 *
 * @author Michael Eichberg
 */
sealed trait Answer {
    def negate: Answer
    def maybeYes: Boolean
    def maybeNo: Boolean
    def yes: Boolean
    def no: Boolean
    def isDefined: Boolean
    def isUndefined: Boolean

    def merge(other: Answer): Answer
}
/**
 * Defines factory methods for answer objects.
 */
object Answer {
    def apply(value: Boolean): Answer = if (value) Yes else No
}
final case object Yes extends Answer {
    def negate = No
    def maybeYes: Boolean = true
    def maybeNo: Boolean = false
    def yes: Boolean = true
    def no: Boolean = false
    def isDefined: Boolean = true
    def isUndefined: Boolean = false

    def merge(other: Answer) = if (other eq Yes) Yes else Unknown
}
final case object No extends Answer {
    def negate = Yes
    def maybeYes: Boolean = false
    def maybeNo: Boolean = true
    def yes: Boolean = false
    def no: Boolean = true
    def isDefined: Boolean = true
    def isUndefined: Boolean = false

    def merge(other: Answer) = if (other eq No) No else Unknown
}
final case object Unknown extends Answer {
    def negate = this
    def maybeYes: Boolean = true
    def maybeNo: Boolean = true
    def yes: Boolean = false
    def no: Boolean = false
    def isDefined: Boolean = false
    def isUndefined: Boolean = true

    def merge(other: Answer) = Unknown
}



