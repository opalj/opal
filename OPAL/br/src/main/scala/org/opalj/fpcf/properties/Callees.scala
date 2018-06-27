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

import org.opalj.br.Method
import org.opalj.br.analyses.MethodIDs
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntTrieSet

import scala.collection.immutable.IntMap

/**
 * For a given [[Method]], and for each call site (represented by the PC), the set of methods
 * that are possible call targets.
 *
 * @author Florian Kuebler
 */
sealed trait CalleesPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = Callees
}

sealed trait Callees extends Property with OrderedProperty with CalleesPropertyMetaInformation {

    def size: Int

    def callees: Map[Int /*PC*/ , Set[Method]]

    override def toString: String = {
        s"Callees(size=${this.size})"
    }

    final def key: PropertyKey[Callees] = Callees.key

    /**
     * Tests if this property is equal or better than the given one (better means that the
     * value is above the given value in the underlying lattice.)
     */
    override def checkIsEqualOrBetterThan(e: Entity, other: Callees): Unit = {
        if (other.size < size) //todo if (!pcMethodPairs.subsetOf(other.pcMethodPairs))
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
    }
}

final class CalleesImplementation(
        private[this] val calleesIds: IntMap[IntTrieSet],
        private[this] val methodIds:  MethodIDs
) extends Callees {

    override def callees: Map[Int /*PC*/ , Set[Method]] = calleesIds.map {
        case (pc, tgts) ⇒
            (pc, tgts.mapToAny[Method](methodIds.apply))
    }

    override val size: Int = {
        calleesIds.iterator.map(_._2.size).sum
    }
}

class FallbackCallees(project: SomeProject, method: Method) extends Callees {

    private[this] def allMethods = project.allMethods.toSet

    override def size: Int = {
        //callees.size * project.allMethods.size
        // todo this is for performance improvement only
        Int.MaxValue
    }

    override def callees: Map[Int, Set[Method]] = {
        method.body.get.withFilter(_.instruction.isInvocationInstruction).map(_.pc → allMethods)
    }
}

object Callees extends CalleesPropertyMetaInformation {

    final val key: PropertyKey[Callees] = {
        PropertyKey.create(
            "Callees",
            (ps: PropertyStore, m: Method) ⇒ Callees.fallback(m, ps.context[SomeProject]),
            (_: PropertyStore, eps: EPS[Method, Callees]) ⇒ eps.ub,
            (_: PropertyStore, _: Method) ⇒ None
        )
    }

    def fallback(m: Method, p: SomeProject): Callees = {
        new FallbackCallees(p, m)
    }
}
