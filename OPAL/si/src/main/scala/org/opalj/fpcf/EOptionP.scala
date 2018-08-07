/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

/**
 * An entity associated with the current extension of a property or `None` if no (preliminary)
 * property is already computed.
 *
 * @author Michael Eichberg
 */
sealed trait EOptionP[+E <: Entity, +P <: Property] {

    /**
     * The entity. E.g., a class, method or field. In general, it is recommended
     * to use entities that stand for specific elements in the code, but which
     * are not the concrete source code entities themselves. This greatly facilitates
     * associating properties with entities where the respective code is not available.
     * For example, by using an object which acts as a representative for a concrete method
     * it is possible to associate (predetermined) properties with (selected) library methods
     * even if those methods are not part of the analysis.
     *
     * @note Entities have to implement `equals`/`hashCode` methods which are very efficient,
     *       because entity based comparisons happen very frequently!
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
    final def hasNoProperty: Boolean = !hasProperty

    /**
     * This EOptionP as an EPS object; defined iff at least a preliminary property exists.
     */
    def asEPS: EPS[E, P]

    /**
     * Returns `true` if and only if we have a property and the property was stored in the
     * store using `(Multi)Result`.
     */
    def isFinal: Boolean
    final def isRefinable: Boolean = !isFinal
    def asFinal: FinalEP[E, P]

    /**
     * Combines the test if we have a final property and – if we have one – if it is equal (by
     * means of equality check) to the given one.
     */
    def is(p: AnyRef): Boolean = /*this.hasProperty && */ this.isFinal && this.ub == p

    private[fpcf] def toUBEP: FinalEP[E, P]

    /**
     * Returns the upper bound of the property if it is available – [[hasProperty]] has to be
     * `true` – otherwise an `UnsupportedOperationException` is thrown.
     *
     * The upper bound always models the best/most precise result w.r.t. the underlying lattice.
     * Here, "best" means that the set of potentially reachable states/instructions that the
     * analyzed program can ever assume, is potentially smaller when compared to a worse property.
     *
     * The upper bound models the sound and precise result under the assumption that the
     * properties of all explicitly and implicitly relevant entities is as last queried or
     * implicitly assumed. I.e., unless a dependee is updated the upper bound represents
     * the correct and most precise result. The upper bound also models the extension of
     * simple properties.
     *
     * The lower bound models the worst case property that a specific entity can have under the
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
     * @note If the property is final, the lb (and ub) will return the final property `p`.
     */
    @throws[UnsupportedOperationException]("if no property is available")
    def ub: P

    /**
     * Returns the lower bound of the property if it is available,
     * otherwise an `UnsupportedOperationException` is thrown. For details regarding the
     * precise semantics see the discussion for [[ub]].
     *
     * @note If the property is final, the lb (and ub) will return the final property `p`.
     * @note For simple properties an [[IllegalArgumentException]] is thrown.
     */
    @throws[UnsupportedOperationException]("if no property is available")
    def lb: P

}

/**
 * Factory and extractor for [[EOptionP]] objects.
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

    final override def pk: PropertyKey[P] = ub.key.asInstanceOf[PropertyKey[P]]

    final override def toEPK: EPK[E, P] = EPK(e, pk)

    /**
     * Creates a [[FinalEP]] object using the current ub.
     *
     * @note No check is done whether the property is actually final.
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

object ESimplePS {

    def apply[E <: Entity, P <: Property](e: E, ub: P, isFinal: Boolean): EPS[E, P] = {
        if (isFinal)
            FinalEP(e, ub)
        else
            IntermediateESimpleP(e, ub)
    }

    /**
     * Returns the `(Entity, UpperBound : Property, isFinal : Boolean)`.
     */
    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[(E, P, Boolean)] = {
        Some((eps.e, eps.ub, eps.isFinal))
    }

}

/**
 * Encapsulates the intermediate lower- and upper bound related to the computation of the respective
 * property kind for the entity `E`.
 *
 * For a detailed discussion of the semantics of `lb` and `ub` see [[EOptionP.lb]].
 */
final class IntermediateEP[+E <: Entity, +P <: Property](
        val e:  E,
        val lb: P,
        val ub: P
) extends EPS[E, P] {

    override def isFinal: Boolean = false
    override def asFinal: FinalEP[E, P] = throw new ClassCastException();

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntermediateEP[_, _] ⇒ e == that.e && lb == that.lb && ub == that.ub
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
 * Encapsulates the intermediate simple property of the respective property kind for the entity `E`.
 *
 * For a more detailed discussion see [[EOptionP.ub]].
 */
final class IntermediateESimpleP[+E <: Entity, +P <: Property](
        val e:  E,
        val ub: P
) extends EPS[E, P] {

    override def lb: P = {
        throw new IllegalArgumentException("intermediate property of a simple property")
    }

    override def isFinal: Boolean = false
    override def asFinal: FinalEP[E, P] = throw new ClassCastException();

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntermediateESimpleP[_, _] ⇒ e == that.e && ub == that.ub
            case _                                ⇒ false
        }
    }

    override def hashCode: Int = e.hashCode() * 31 + ub.hashCode()

    override def toString: String = {
        s"IntermediateESimpleP($e@${System.identityHashCode(e).toHexString},ub=$ub)"
    }
}

object IntermediateESimpleP {

    def apply[E <: Entity, P <: Property](e: E, ub: P): IntermediateESimpleP[E, P] = {
        new IntermediateESimpleP(e, ub)
    }

    def unapply[E <: Entity, P <: Property](eps: IntermediateESimpleP[E, P]): Option[(E, P)] = {
        Some((eps.e, eps.ub))
    }
}

/**
 * Encapsulate the final property `P` for the entity `E`.
 *
 * For a detailed discussion of the semantics of `lb` and `ub` see [[EOptionP.ub]].
 */
final class FinalEP[+E <: Entity, +P <: Property](val e: E, val ub: P) extends EPS[E, P] {

    override def isFinal: Boolean = true
    override def asFinal: FinalEP[E, P] = this

    override def lb: P = ub

    def p: P = ub // or lb

    override def equals(other: Any): Boolean = {
        other match {
            case that: FinalEP[_, _] ⇒ that.e == this.e && this.p == that.p
            case _                   ⇒ false
        }
    }

    override def hashCode: Int = e.hashCode() * 727  + ub.hashCode()

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
 * @author Michael Eichberg
 */
final class EPK[+E <: Entity, +P <: Property](
        val e:  E,
        val pk: PropertyKey[P]
) extends EOptionP[E, P] {

    override def lb: Nothing = throw new UnsupportedOperationException();

    override def ub: Nothing = throw new UnsupportedOperationException();

    override def isFinal: Boolean = false
    override def asFinal: FinalEP[E, P] = throw new ClassCastException();

    private[fpcf] def toUBEP: FinalEP[E, P] = throw new UnsupportedOperationException();

    override def hasProperty: Boolean = false
    override def asEPS: EPS[E, P] = throw new ClassCastException();

    override def toEPK: this.type = this

    override def equals(other: Any): Boolean = {
        other match {
            case that: EPK[_, _] ⇒ that.e == this.e && this.pk == that.pk
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
