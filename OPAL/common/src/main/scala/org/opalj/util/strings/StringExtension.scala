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
package strings

import java.lang.{StringBuilder ⇒ JStringBuilder}

/**
 * This is the most common trait to represent String during analysis. A string can be represented
 * as concrete string, a partial string or by a generic placeholder. All subclasses have to model
 * the behaviour of java.lang.StringBuilder
 *
 * @author Michael Reif
 */
trait StringExtension {

    def equals(other: Any): Boolean

    /**
     * Compares two StringExtensions, returning true when the current string is more general than the
     * `other` one.
     */
    def leftIncludes(other: StringExtension): Answer

    /**
     * Compares two StringExtensions, returning true when the `other` string is more general than the
     * current one.
     */
    def rightIncludes(other: StringExtension): Answer = other.leftIncludes(this)

    // The following methods represent the StringBuilder/StringBuffer api. All of those methods can
    // be used to manipulate the Builder/Buffer.

    def insert(index: Int, str: String, offset: Int, len: Int): StringExtension

    def insert(offset: Int, str: String): StringExtension

    def replace(start: Int, end: Int, str: String): StringExtension

    def append(str: String): StringExtension

    def append(chars: Array[Char], offset: Int, len: Int): StringExtension

    def append(chars: CharSequence, start: Int, end: Int): StringExtension

    def appendCodePoint(cp: Int): StringExtension

    def delete(start: Int, end: Int): StringExtension

    def deleteCharAt(offset: Int): StringExtension

    def reverse(): StringExtension

    def setLength(capacity: Int): StringExtension

    def toString(): String

    def join(other: StringExtension): StringExtension
}

/**
 * Represents all arbitrary strings.
 */
case object AnyString extends StringExtension {

    override def insert(index: UShort, str: String, offset: UShort, len: UShort): StringExtension = this

    override def insert(offset: Int, str: String): StringExtension = this //FIXME we now have a known prefix if offsett == 0

    override def replace(start: Int, end: Int, str: String): StringExtension = this

    override def append(str: String): StringExtension = this //FIXME we now have a known suffix

    override def append(chars: Array[Char], offset: Int, len: Int): StringExtension = this //FIXME we now have a known suffix

    override def append(chars: CharSequence, start: Int, end: Int): StringExtension = this //FIXME we now have a known suffix

    override def appendCodePoint(cp: Int): StringExtension = this //FIXME we now have a known suffix

    override def delete(start: Int, end: Int): StringExtension = this

    override def deleteCharAt(offset: Int): StringExtension = this

    override def reverse(): StringExtension = this

    override def setLength(capacity: Int): StringExtension = {
        if (capacity == 0)
            ConcreteString("")
        else
            // in theory we now more then any, i.e. the max string length.
            this
    }

    override def leftIncludes(other: StringExtension): Answer = Yes

    override def join(other: StringExtension) = other match {
        case cs @ ConcreteString(_)   ⇒ StringExtensions(Set(cs, this))
        case StringExtensions(values) ⇒ StringExtensions(values + this)
        case _                        ⇒ this
    }
}

/**
 * Represents a concrete string constant that is known to be final.
 *
 * @param cs The final value of the string.
 */
case class ConcreteString(cs: String) extends StringExtension {

    override def insert(index: Int, str: String, offset: UShort, len: UShort): StringExtension = {
        val sb = new JStringBuilder(cs)
        ConcreteString(sb.insert(index, str, offset, len))
    }

    override def insert(offset: Int, str: String): StringExtension = {
        val sb = new JStringBuilder(cs)
        ConcreteString(sb.insert(offset, str))
    }

    override def replace(start: Int, end: Int, str: String): StringExtension = {
        val sb = new JStringBuilder(cs)
        ConcreteString(sb.replace(start, end, str))
    }

    override def append(str: String): StringExtension = {
        val sb = new JStringBuilder(cs)
        ConcreteString(sb.append(str))
    }

    override def append(chars: Array[Char], offset: Int, len: Int): StringExtension = {
        val sb = new JStringBuilder(cs)
        ConcreteString(sb.append(chars, offset, len))
    }

    override def append(charSeq: CharSequence, start: Int, end: Int): StringExtension = {
        val sb = new JStringBuilder(cs)
        ConcreteString(sb.append(charSeq, start, end))
    }

    override def appendCodePoint(cp: Int): StringExtension = {
        val sb = new JStringBuilder(cs)
        ConcreteString(sb.appendCodePoint(cp))
    }

    override def delete(start: Int, end: Int): StringExtension = {
        val sb = new JStringBuilder(cs)
        ConcreteString(sb.delete(start, end))
    }

    override def deleteCharAt(offset: Int): StringExtension = {
        val sb = new JStringBuilder(cs)
        ConcreteString(sb.deleteCharAt(offset))
    }

    override def reverse(): StringExtension = {
        val sb = new JStringBuilder(cs)
        ConcreteString(sb.reverse)
    }

    override def setLength(capacity: Int): StringExtension = {
        val sb = new JStringBuilder(cs)
        sb.setLength(capacity)
        ConcreteString(sb.toString)
    }

    override def toString: String = cs.toString

    override def equals(other: Any): Boolean = {
        other match {
            case ConcreteString(value) ⇒ this.cs == value
            case _                     ⇒ false
        }
    }

    override def leftIncludes(other: StringExtension): Answer = {
        other match {
            case ConcreteString(`cs`) ⇒ Yes
            case _                    ⇒ No
        }
    }

    override def join(other: StringExtension) = other match {
        case oCs @ ConcreteString(v) if v != cs  => StringExtensions(Set(oCs,this))
        case StringExtensions(values) ⇒ StringExtensions(values + this)
        case _                        ⇒ this
    }
}

object ConcreteString {

    def apply(sb: JStringBuilder): ConcreteString = {
        new ConcreteString(sb.toString)
    }
}

/*case class StringPrefix(prefix: String) extends StringExtension {
    /**
     * Compares two StringExtensions, returning true when the current string is more general than the
     * `other` one.
     */
    override def leftIncludes(other: StringExtension): Answer = ???

    /**
     * Compares two StringExtensions, returning true when the `other` string is more general than the
     * current one.
     */
    override def rightIncludes(other: StringExtension): Answer = ???

    override def insert(index: Int, str: String, offset: Int, len: Int): StringExtension = {
        val sb = new JStringBuilder(prefix)
        if (index <= prefix.length) {
            sb.insert(index, str, offset, len)
            StringPrefix(sb.toString)
        } else {
            this
        }

    }

    override def insert(offset: UShort, str: String): StringExtension = ???

    override def replace(start: UShort, end: UShort, str: String): StringExtension = ???

    override def append(str: String): StringExtension = ???

    override def append(chars: Array[Char], offset: UShort, len: UShort): StringExtension = ???

    override def append(chars: CharSequence, start: UShort, end: UShort): StringExtension = ???

    override def appendCodePoint(cp: UShort): StringExtension = ???

    override def delete(start: UShort, end: UShort): StringExtension = ???

    override def deleteCharAt(offset: UShort): StringExtension = ???

    override def reverse(): StringExtension = ???

    override def setLength(capacity: UShort): StringExtension = ???
}

/**
 * Represents a partial string. It is unknown whether the fragment is a pre- or suffix or
 * at an arbitrary position.
 *
 * @param fragment The string fragment that represents the partial string.
 */
case class StringFragment(fragment: String) extends StringExtension {

    override def insert(index: UShort, str: String, offset: UShort, len: UShort): StringExtension = {
        if (index == 0)
            this // FIXME we know it is an prefix
        else
            AnyString // the insert could destroy our fragment
    }

    override def insert(offset: Int, str: String): StringExtension = {
        insert(offset, str, 0, str.size)
    }

    override def replace(start: UShort, end: UShort, str: String): StringExtension = {
        val sb = new JStringBuilder(fragment)
        StringFragment(sb.replace(start, end, str).toString) //TODO this assumption is probably not safe
    }

    override def append(str: String): StringExtension = ??? // fragment + ANY + suffix

    override def append(chars: Array[Char], offset: UShort, len: UShort): StringExtension = ??? // fragment + ANY + suffix

    override def append(chars: CharSequence, start: UShort, end: UShort): StringExtension = ??? // fragment + ANY + suffix

    override def appendCodePoint(cp: UShort): StringExtension = ??? // fragment + ANY + suffix

    override def delete(start: UShort, end: UShort): StringExtension = AnyString

    override def deleteCharAt(offset: UShort): StringExtension = AnyString

    override def reverse(): StringExtension = {
        val sb = new JStringBuilder
        StringFragment(sb.reverse.toString)
    }

    override def setLength(capacity: UShort): StringExtension = {
        capacity match {
            case 0                            ⇒ ConcreteString("")
            case len if len < fragment.length ⇒ StringFragment(fragment.substring(0, len))
            case _                            ⇒ this /* semantic not obvious, fragment could be removed or partially removed */
        }
    }

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
}*/ 