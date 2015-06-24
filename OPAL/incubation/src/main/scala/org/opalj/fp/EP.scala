/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package org.opalj.fp

/**
 * A pairing of an [[Entity]] and an associated [[Property]].
 *
 * Compared to a standard `Tuple2` the equality of two EP objects is based on
 * comparing the entities using reference equality.
 *
 * @author Michael Eichberg
 */
final class EP(val e: Entity, val p: Property) extends Product2[Entity, Property] {

    def _1 = e
    def _2 = p

    override def equals(other: Any): Boolean = {
        other match {
            case that: EP ⇒ (that.e eq this.e) && this.p == that.p
            case _        ⇒ false
        }
    }

    override def canEqual(that: Any): Boolean = that.isInstanceOf[EP]

    def pk: PropertyKey = p.key

    override def hashCode: Int = e.hashCode() * 727 + p.hashCode()

    override def toString: String = s"EK($e,$p)"
}

/**
 * Provides a factory and an extractor for [[EP]] objects.
 *
 * @author Michael Eichberg
 */
object EP {

    def apply(e: Entity, p: Property): EP = new EP(e, p)

    def unapply(that: EP): Option[(Entity, Property)] = {
        that match {
            case null ⇒ None
            case ep   ⇒ Some((ep.e, ep.p))
        }
    }
}
