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
package org.opalj.fpcf

/**
 * An entity and a specific associated property if it is available.
 *
 * @author Michael Eichberg
 */
abstract class EOptionP[+E <: Entity, +P <: Property] private[fpcf] () {

    /**
     * The entity.
     */
    val e: E

    /**
     * The property key of the optionally available property.
     */
    def pk: PropertyKey[P]

    /** This EOptionP as a pair of an entity and a property key. */
    def toEPK: EPK[E, P]

    /**
     * Returns `true` if and only if we have a property and the property was stored in the
     * store using (Immediate)(Multi)Result or if the property is final by itself.
     */
    def isPropertyFinal: Boolean

    /**
     * @return `true` if the entity is associated with a property.
     */
    def hasProperty: Boolean

    final def hasNoProperty: Boolean = !hasProperty

    /**
     * Combines the test if we have a property and if we have one if it is equal to the given one.
     */
    final def is[T >: P](p: T): Boolean = this.hasProperty && p == this.p

    /**
     * Returns the property if it is available otherwise an `UnsupportedOperationException` is
     * thrown.
     */
    @throws[UnsupportedOperationException]("if no property is available")
    def p: P

    override def toString: String = s"EOptionP($e,$p)"
}

/**
 * Factory object to create [[EP]] and [[EPK]] objects.
 */
object EOptionP {

    def apply[E <: Entity, P <: Property](
        e:       E,
        pk:      PropertyKey[P],
        pOption: Option[P]
    ): EOptionP[E, P] = {
        pOption match {
            case Some(p) ⇒ EP(e, p)
            case None    ⇒ EPK(e, pk)
        }
    }
}

/**
 * A pairing of an [[Entity]] and an associated [[Property]].
 *
 * @note entities are compared using reference equality and properties are compared using `equals`.
 *
 * @author Michael Eichberg
 */
// TODO Add property state information
sealed class EP[+E <: Entity, +P <: Property](
        val e: E,
        val p: P
) extends EOptionP[E, P] with Product2[E, P] {

    override def _1: E = e
    override def _2: P = p

    def isPropertyFinal: Boolean = p.isFinal

    def hasProperty: Boolean = true

    override def equals(other: Any): Boolean = {
        other match {
            case that: EP[_, _] ⇒ (that.e eq this.e) && this.p == that.p
            case _              ⇒ false
        }
    }

    override def canEqual(that: Any): Boolean = that.isInstanceOf[EP[_, _]]

    override def hashCode: Int = e.hashCode() * 727 + p.hashCode()

    def pk: PropertyKey[P] = p.key.asInstanceOf[PropertyKey[P]]

    def toEPK: EPK[E, P] = EPK(e, pk)

    override def toString: String = {
        s"EP(${e}@${System.identityHashCode(e).toHexString},$p)"
    }
}

/**
 * Provides a factory and an extractor for [[EP]] objects.
 *
 * @author Michael Eichberg
 */
object EP {

    def apply[E <: Entity, P <: Property](e: E, p: P): EP[E, P] = new EP(e, p)

    def unapply[E <: Entity, P <: Property](ep: EP[E, P]): Option[(E, P)] = Some((ep.e, ep.p))

}

final class FinalEP[+E <: Entity, +P <: Property](e: E, p: P) extends EP[E, P](e, p) {

    override def isPropertyFinal: Boolean = true

}

object FinalEP {

    def apply[E <: Entity, P <: Property](e: E, p: P): FinalEP[E, P] = new FinalEP(e, p)

}

object SomeProperty {

    def unapply[P <: Property](ep: EP[_, P]): Option[P] = Some(ep.p)

}

/**
 * A simple pair consisting of an [[Entity]] and a [[PropertyKey]].
 *
 * Compared to a standard `Tuple2` the entities are compared using reference comparison
 * and not equality based on `equals` checks.
 *
 * @author Michael Eichberg
 */
final class EPK[+E <: Entity, +P <: Property](
        val e:  E,
        val pk: PropertyKey[P]
) extends EOptionP[E, P] with Product2[E, PropertyKey[P]] {

    override def _1: E = e
    override def _2: PropertyKey[P] = pk

    def isPropertyFinal: Boolean = false

    def hasProperty: Boolean = false

    def p: Nothing = throw new UnsupportedOperationException()

    def toEPK: this.type = this

    override def equals(other: Any): Boolean = {
        other match {
            case that: EPK[_, _] ⇒ (that.e eq this.e) && this.pk == that.pk
            case _               ⇒ false
        }
    }

    override def canEqual(that: Any): Boolean = that.isInstanceOf[EPK[_, _]]

    override def hashCode: Int = e.hashCode() * 511 + pk.id

    override def toString: String = {
        val pkId = pk.id
        val pkName = PropertyKey.name(pkId)
        s"EPK(${e}@${System.identityHashCode(e).toHexString},pkName=$pkName,pkId=$pkId)"
    }
}

/**
 * Factory and extractor for [[EPK]] objects.
 *
 * @author Michael Eichberg
 */
object EPK {

    def apply[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): EPK[E, P] = new EPK(e, pk)

    def unapply[E <: Entity, P <: Property](epk: EPK[E, P]): Option[(E, PropertyKey[P])] = {
        Some((epk.e, epk.pk))
    }
}

object NoProperty {

    def unapply(eOptP: EOptionP[_, _]): Boolean = eOptP.hasNoProperty

}
