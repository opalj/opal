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
package fpcf
package properties

import scala.collection.Set
import scala.collection.Map
import org.opalj.br.Method

/**
 * TODO
 * @author Florian Kuebler
 */
sealed trait CalleesPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = Callees
}

class Callees(
        val callees: Map[Int /*PC*/ , Set[Method]]
) extends Property with OrderedProperty with CalleesPropertyMetaInformation {
    final def key: PropertyKey[Callees] = Callees.key

    override def toString: String = {
        s"Callees(size=${this.size}):${callees}"
    }

    def size: Int = {
        callees.map(_._2.size).sum
    }

    def pcMethodPairs: Set[(Int /*PC*/ , Method)] = callees.toSet.flatMap {
        pcToTgts: (Int, Set[Method]) ⇒ pcToTgts._2.map((tgt: Method) ⇒ (pcToTgts._1, tgt))
    }

    def canEqual(other: Any): Boolean = other.isInstanceOf[Callees]

    override def equals(other: Any): Boolean = other match {
        case that: Callees ⇒
            (that canEqual this) &&
                callees == that.callees
        case _ ⇒ false
    }

    override def hashCode(): Int = {
        val state = Seq(callees)
        state.map(_.hashCode()).foldLeft(0)((a, b) ⇒ 31 * a + b)
    }

    /**
     * Tests if this property is equal or better than the given one (better means that the
     * value is above the given value in the underlying lattice.)
     */
    override def checkIsEqualOrBetterThan(e: Entity, other: Callees): Unit = {
        if (!pcMethodPairs.subsetOf(other.pcMethodPairs))
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
    }
}

object Callees extends CalleesPropertyMetaInformation {

    final val key: PropertyKey[Callees] = {
        PropertyKey.create(
            "Callees",
            Callees(Map.empty.withDefaultValue(Set.empty)),
            (_: PropertyStore, eps: EPS[Method, Callees]) ⇒ eps.toUBEP
        )
    }

    def apply(callees: Map[Int /*PC*/ , Set[Method]]): Callees = new Callees(callees)

    def unapply(callees: Callees): Option[Map[Int /*PC*/ , Set[Method]]] = Some(callees.callees)
}
