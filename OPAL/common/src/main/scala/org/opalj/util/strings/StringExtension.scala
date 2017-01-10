/*
 * BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
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
package strings

/**
 * This is the most common trait to represent String during analysis. A string can be represented
 * as concrete string, a partial string or by a generic placeholder.
 *
 * @author Michael Reif
 */
trait StringExtension {

    def equals(other: Any): Boolean

    /**
     * Compares two StringExtensions, returning true when the current string is more general than the
     * `other` one.
     *
     */
    def leftIncludes(other: StringExtension): Boolean

    /**
     * Compares two StringExtensions, returning true when the `other` string is more general than the
     * current one.
     */
    def rightIncludes(other: StringExtension): Boolean
}

/**
 * Represents all arbitrary strings.
 */
case object AnyString extends StringExtension {

    override def leftIncludes(other: StringExtension): Boolean = true

    override def rightIncludes(other: StringExtension): Boolean = other match {
        case AnyString ⇒ true
        case _         ⇒ false
    }
}

/**
 * Represents a concrete string constant that is known to be final.
 *
 * @param concreteString The final value of the string.
 */
case class ConcreteString(concreteString: String) extends StringExtension {

    override def equals(other: Any): Boolean = {
        other match {
            case ConcreteString(value) ⇒ this.concreteString == value
            case _                     ⇒ false
        }
    }

    override def leftIncludes(other: StringExtension): Boolean = {
        other match {
            case ConcreteString(`concreteString`) ⇒ true
            case _                                ⇒ false
        }
    }

    override def rightIncludes(other: StringExtension): Boolean = {
        other match {
            case AnyString          ⇒ true
            case StringFragment(sf) ⇒ concreteString.contains(sf)
            case ConcreteString(cs) ⇒ cs == this.concreteString
        }
    }
}

/**
 * Represents a partial string. It is unknown whether the fragment is a pre- or suffix or
 * at an arbitrary position.
 *
 * @param fragment The string fragment that represents the partial string.
 */
case class StringFragment(fragment: String) extends StringExtension {

    override def equals(other: Any): Boolean = {
        other match {
            case StringFragment(value) ⇒ this.fragment == value
            case _                     ⇒ false
        }
    }

    override def leftIncludes(other: StringExtension): Boolean = {
        other match {
            case ConcreteString(cs) ⇒ cs.contains(this.fragment)
            case StringFragment(sf) ⇒
                // if this fragment is included in the other fragment, this fragment is more general.
                sf.contains(this.fragment)
            case _ ⇒ false
        }
    }

    override def rightIncludes(other: StringExtension): Boolean = {
        other match {
            case AnyString          ⇒ true
            case ConcreteString(_)  ⇒ false
            case StringFragment(sf) ⇒ this.fragment.contains(sf)
        }
    }
}