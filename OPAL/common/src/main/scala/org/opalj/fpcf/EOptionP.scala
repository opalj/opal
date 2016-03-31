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
package org.opalj.fpcf

/**
 * An entity and (optionally) a specific associated property.
 *
 * @author Michael Eichberg
 */
sealed trait EOptionP[+E <: Entity, +P <: Property] {

    /**
     * The entity.
     */
    val e: E

    /**
     * The property key of the optionally available property.
     */
    def pk: PropertyKey[P]

    def toEPK: EPK[E, P]

    /**
     * @return `true` if the entity is associated with a property.
     */
    def hasProperty: Boolean

    final def hasNoProperty: Boolean = !hasProperty

    /**
     * Returns the property if it is available otherwise an `UnsupportedOperationException` is
     * thrown.
     */
    @throws[UnsupportedOperationException]("if no property is available")
    def p: P

    override def toString: String = s"EOptionP($e,$p)"
}

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
 * Compared to a standard `Tuple2` the equality of two `EP` objects is based on
 * comparing the entities using reference equality.
 *
 * @author Michael Eichberg
 */
final class EP[+E <: Entity, +P <: Property](
        val e: E,
        val p: P
) extends EOptionP[E, P] with Product2[E, P] {

    override def _1: E = e
    override def _2: P = p

    def hasProperty = true

    override def equals(other: Any): Boolean = {
        other match {
            case that: EP[_, _] ⇒ (that.e eq this.e) && this.p == that.p
            case _              ⇒ false
        }
    }

    override def canEqual(that: Any): Boolean = that.isInstanceOf[EP[_, _]]

    def pk: PropertyKey[P] = p.key.asInstanceOf[PropertyKey[P]]

    def toEPK: EPK[E, P] = EPK(e, pk)

    override def hashCode: Int = e.hashCode() * 727 + p.hashCode()

    override def toString: String = s"EP($e,$p)"
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

    override def _1 = e
    override def _2 = pk

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

    override def toString: String = s"EPK($e,pkName=${PropertyKey.name(pk.id)},pkId=${pk.id})"
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
