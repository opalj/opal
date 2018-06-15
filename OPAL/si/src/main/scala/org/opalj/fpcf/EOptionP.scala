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

/**
 * An entity and an associated property - if it is (already) available.
 *
 * @author Michael Eichberg
 */
sealed trait EOptionP[+E <: Entity, +P <: Property] {

    /**
     * The entity. E.g., a class, method or field. In general, it is recommended
     * to use entities that stand for specific elements in the code, but which
     * are not the concrete source code entities themselves. This greatly facilitates
     * associating properties with entities where the respective code is not available.
     * For example, by using "declared methods" it is possible to associate (predetermined)
     * properties with (selected) library methods even if those methods are not part of
     * the analysis.
     */
    val e: E

    /**
     * The property key of the optionally available property.
     */
    def pk: PropertyKey[P]

    /** This EOptionP as a pair of an entity and a property key. */
    def toEPK: EPK[E, P]

    /**
     * @return `true` if the entity is associated with a (preliminary) property.
     */
    def hasProperty: Boolean
    def hasNoProperty: Boolean = !hasProperty
    def asEPS: EPS[E, P]

    /**
     * Returns `true` if and only if we have a property and the property was stored in the
     * store using `(Multi)Result`.
     */
    def isFinal: Boolean
    def asFinal: FinalEP[E, P]

    final def isRefinable: Boolean = !isFinal

    /**
     * Combines the test if we have a final property and – if we have one – if it is equal (by
     * means of equality check) to the given one.
     */
    def is(p: AnyRef): Boolean = this.hasProperty && isFinal && this.ub == p

    /**
     * Returns the upper bound of the property if it is available – [[hasProperty]] has to be
     * `true` – otherwise an `UnsupportedOperationException` is thrown.
     *
     * The upper bound always models the best/most precise result w.r.t. the underlying lattice.
     * Here, "best" means that the set of potentially reachable states/instructions that the
     * analyzed program can ever have is potentially smaller when compared to a worse property.
     *
     * The upper bound models the sound and precise result under the assumption that the
     * properties of all explicitly and implicitly relevant entities is as last queried or
     * implicitly assumed. I.e., unless a dependee is updated the upper bound represents
     * the correct and most precise result.
     *
     * The lower bound models the worst case property that a specific entity can have under
     * assumption that all other relevant properties will get their worst properties. This
     * can – but does not have to be – the underlying lattice's bottom value.
     * The lower bound is generally helpful for client analyses to determine final
     * results quicker. For example, imagine the following code:
     * {{{
     * def f(a : AnyRef) : Unit = a match {
     *   case a : List[_] => if (a.exists( _ == null)) throw  new IllegalArgumentException
     *   case _ => throw new UnknownError
     * }
     * def m(){
     *   try {
     *     f(List(1,2,3))
     *   } catch {
     *     case nfe:  NumberFormatException => ...
     *   }
     * }
     * }}}
     * In that case (assuming we do not perform context sensitive analyses),
     * if the lower bound for `f` for the set of thrown exceptions is determined
     * to be `Set(IllegalArgumentException,UnkownError)`, the catch of the
     * `NumberFormatException` can be ruled out and a final result for `m` can be
     * computed.
     *
     *
     * @note If the property is final, the lb (and ub) will return the final property `p`.
     */
    @throws[UnsupportedOperationException]("if no property is available")
    def ub: P

    /**
     * Returns the lower bound of the property if it is available
     * otherwise an `UnsupportedOperationException` is thrown. For details regarding the
     * precise semantics see the discussion for [[ub]].
     *
     * @note If the property is final, the lb (and ub) will return the final property `p`.
     */
    @throws[UnsupportedOperationException]("if no property is available")
    def lb: P

}

/**
 * Factory and extractor for [[EPK]] objects.
 *
 * @author Michael Eichberg
 */
object EOptionP {

    def unapply[E <: Entity, P <: Property](eOptP: EOptionP[E, P]): Option[(E, PropertyKey[P])] = {
        Some((eOptP.e, eOptP.pk))
    }
}

/**
 * A pairing of an [[Entity]] and an associated [[Property]] along with its state.
 *
 * @note entities are compared using reference equality and properties are compared using `equals`.
 *
 * @author Michael Eichberg
 */
sealed trait EPS[+E <: Entity, +P <: Property] extends EOptionP[E, P] {

    final override def pk: PropertyKey[P] = lb.key.asInstanceOf[PropertyKey[P]]

    final override def toEPK: EPK[E, P] = EPK(e, pk)

    /**
     * Creates a [[FinalEP]] object using the current ub.
     *
     * No check is done whether the current state is actually final.
     */
    final def toUBEP: FinalEP[E, P] = FinalEP(e, ub)

    final override def hasProperty: Boolean = true
    final override def asEPS: EPS[E, P] = this

}

/**
 * Provides a factory and an extractor for [[EPS]] objects.
 *
 * @author Michael Eichberg
 */
object EPS {

    def apply[E <: Entity, P <: Property](e: E, lb: P, ub: P): EPS[E, P] = {
        if (lb == ub)
            FinalEP(e, ub)
        else
            IntermediateEP(e, lb, ub)
    }

    /**
     * Returns the `(Entity, LowerBound, UpperBound)`.
     */
    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[(E, P, P)] = {
        Some((eps.e, eps.lb, eps.ub))
    }

}

/**
 * Encapsulate the intermediate state related to the computation of the property `P` of
 * the respective kind for the entity `E`.
 *
 * For a detailed discussion of the semantics of `lb` and `ub` see [[EOptionP.lb]].
 */
final class IntermediateEP[+E <: Entity, +P <: Property](
        val e:  E,
        val lb: P,
        val ub: P
) extends EPS[E, P] {

    override def isFinal: Boolean = false
    override def asFinal: FinalEP[E, P] = throw new ClassCastException()

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntermediateEP[_, _] ⇒ (e eq that.e) && lb == that.lb && ub == that.ub
            case _                          ⇒ false
        }
    }

    override def hashCode: Int = ((e.hashCode() * 31 + lb.hashCode()) * 31) + ub.hashCode()

    override def toString: String = {
        s"IntermediateEP($e@${System.identityHashCode(e).toHexString},lb=$lb,ub=$ub)"
    }
}

object IntermediateEP {

    def apply[E <: Entity, P <: Property](e: E, lb: P, ub: P): IntermediateEP[E, P] = {
        new IntermediateEP(e, lb, ub)
    }

    def unapply[E <: Entity, P <: Property](eps: IntermediateEP[E, P]): Option[(E, P, P)] = {
        Some((eps.e, eps.lb, eps.ub))
    }
}

/**
 * Encapsulate the final state related to the computation of the property `P` of
 * the respective kind for the entity `E`.
 *
 * For a detailed discussion of the semantics of `lb` and `ub` see [[EOptionP.lb]].
 */
final class FinalEP[+E <: Entity, +P <: Property](val e: E, val ub: P) extends EPS[E, P] {

    override def isFinal: Boolean = true
    override def asFinal: FinalEP[E, P] = this

    override def lb: P = ub

    def p: P = ub // or lb

    override def equals(other: Any): Boolean = {
        other match {
            case that: FinalEP[_, _] ⇒ (that.e eq this.e) && this.p == that.p
            case _                   ⇒ false
        }
    }

    override def hashCode: Int = ((e.hashCode() * 727 + lb.hashCode()) * 31) + ub.hashCode()

    override def toString: String = {
        s"FinalEP($e@${System.identityHashCode(e).toHexString},p=$p)"
    }

}

object FinalEP {

    def apply[E <: Entity, P <: Property](e: E, p: P): FinalEP[E, P] = new FinalEP(e, p)

    def unapply[E <: Entity, P <: Property](eps: FinalEP[E, P]): Option[(E, P)] = {
        Some((eps.e, eps.lb))
    }

}

/**
 * A simple pair consisting of an [[Entity]] and a [[PropertyKey]].
 *
 * Compared to a standard `Tuple2` the entities are compared using reference comparison
 * and not equality based on `equals` checks. `PropertyKey`s are compared using equals
 * (structural equality).
 *
 * @author Michael Eichberg
 */
final class EPK[+E <: Entity, +P <: Property](
        val e:  E,
        val pk: PropertyKey[P]
) extends EOptionP[E, P] {

    override def lb: Nothing = throw new UnsupportedOperationException()

    override def ub: Nothing = throw new UnsupportedOperationException()

    override def isFinal: Boolean = false
    override def asFinal: FinalEP[E, P] = throw new ClassCastException()

    override def hasProperty: Boolean = false
    override def asEPS: EPS[E, P] = throw new ClassCastException()

    override def toEPK: this.type = this

    override def equals(other: Any): Boolean = {
        other match {
            case that: EPK[_, _] ⇒ (that.e eq this.e) && this.pk == that.pk
            case _               ⇒ false
        }
    }

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

    def apply[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): EPK[e.type, P] = new EPK(e, pk)

    def apply[E <: Entity, P <: Property](e: E, p: P): EPK[E, P] = {
        new EPK(e, p.key.asInstanceOf[PropertyKey[P]])
    }

    def unapply[E <: Entity, P <: Property](epk: EPK[E, P]): Option[(E, PropertyKey[P])] = {
        Some((epk.e, epk.pk))
    }
}

object NoProperty {

    def unapply(eOptP: EOptionP[_, _]): Boolean = eOptP.hasNoProperty

}
