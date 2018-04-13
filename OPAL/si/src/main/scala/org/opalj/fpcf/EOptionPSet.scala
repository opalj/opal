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
/*
package org.opalj
package fpcf

/**
 * An efficient implementation of a set of EOptionP values where the underlying E/PK pair
 * is used as a key. This enables O(1) updates when an EPK/EPS is updated; i.e., an
 * EPK/EPS uses the "same" the same key.
 *
 * '''This set should be treated as an immutable set w.r.t. the behavior of the update or
 * remove methods. But, whenever efficient/possible the current set is mutated. Hence,
 * reusing EOptionP sets in different contexts is NOT possible.'''
 *
 * @tparam E
 * @tparam P
 */
sealed trait EOptionPSet[E <: Entity, P <: Property] {

    def isEmpty: Boolean

    def nonEmpty: Boolean = !isEmpty

    /**
     * Adds the given value to this set. Replaces a previous value with the same
     * entity/property key pair.
     *
     * @return '''This''' set if the value was already stored in the set otherwise this set
     *        or a new set - depending on what is more efficient.
     */
    def put(eOptionP: EOptionP[E, P]): EOptionPSet[E, P]

    /** Removes the '''entity with the property key(!)''' of the given eOptionP. */
    def remove(eOptionP: SomeEOptionP): EOptionPSet[E, P]

    def remove(e: Entity): EOptionPSet[E, P]

    def remove(pk: SomePropertyKey): EOptionPSet[E, P]

    def foreach[U](f: (EOptionP[E, P]) ⇒ U): Unit

    def iterator: Iterator[EOptionP[E, P]]
}

private[fpcf] class E0OptionP0Set private () extends EOptionPSet[Entity, Property] {

    def isEmpty: Boolean = true

    def put(eOptionP: EOptionP[Entity, Property]): EOptionPSet[Entity, Property] = {
        new E1OptionP1Set(eOptionP)
    }

    def remove(eOptionP: SomeEOptionP): this.type = this

    def remove(e: Entity): this.type = this

    def remove(pk: SomePropertyKey): this.type = this

    def foreach[U](f: (EOptionP[Entity, Property]) ⇒ U): Unit = {}

    def iterator: Iterator[EOptionP[Entity, Property]] = Iterator.empty
}

private[fpcf] object E0OptionP0Set {

    private[this] val Empty = new E0OptionP0Set()

    def empty[E <: Entity, P <: Property]: EOptionPSet[E, P] = {
        Empty.asInstanceOf[E0OptionP0Set[E, P]]
    }
}

private[fpcf] final class E1OptionP1Set[E <: Entity, P <: Property](
        value: EOptionP[E, P]
) extends EOptionPSet[E, P] {

    override def isEmpty: Boolean = false

    def put(eOptionP: EOptionP[E, P]): EOptionPSet[E, P] = new E1OptionP1Set(eOptionP)

    def remove(eOptionP: SomeEOptionP): EOptionPSet[E, P] = {
        if ((value.e eq eOptionP.e) && value.pk == eOptionP.pk) E0OptionP0Set.empty else this
    }

    def remove(e: Entity): EOptionPSet[E, P] = {
        if (value.e eq e) E0OptionP0Set.empty else this
    }

    def remove(pk: SomePropertyKey): EOptionPSet[E, P] = {
        if (value.pk == pk) E0OptionP0Set.empty else this
    }

    def foreach[U](f: (EOptionP[E, P]) ⇒ U): Unit = f(value)

    def iterator: Iterator[EOptionP[E, P]] = Iterator.single(value)
}

/*
class EOptionPSetN[E <: Entity, P <: Property]extends EOptionPSet[E,P](
                                             private Any
                                             ){

    def put(eOptionP : EOptionP[E,P]) : Boolean = {

}

    def foreach




}

trait Properties

case object Properties0 extends Properties
case class Properties1 extends Properties
*/

*/
