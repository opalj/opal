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

    def isEPK : Boolean
    def asEPK : EPK[E,P]

    /** This EOptionP as a pair of an entity and a property key. */
    def toEPK: EPK[E, P]

    /**
     * @return  `true` if the entity is associated with a (preliminary) property which represents
     *          an upper bound. Always `true` in case of a `FinalP`.
     */
    def hasUBP: Boolean
    final def hasNoUBP: Boolean = !hasUBP

    /**
     * @return  `true` if the entity is associated with a (preliminary) property which represents
     *          a lower bound. Always `true` in case of a `FinalP`.
     */
    def hasLBP: Boolean
    final def hasNoLBP: Boolean = !hasLBP

    def isEPS : Boolean
    /**
     * This EOptionP as an EPS object; defined iff at least a preliminary property exists.
     */
    def asEPS: EPS[E, P]

    def toEPS : Option[EPS[E,P]]

    /**
     * Returns `true` if and only if we have a property and the property was stored in the
     * store using `(Multi)Result`.
     */
    def isFinal: Boolean
    final def isRefinable: Boolean = !isFinal
    def asFinal: FinalP[E, P]

    /**
     * Combines the test if we have a final property and – if we have one – if it is equal (by
     * means of an equality check) to the given one.
     */
    def is(p: AnyRef): Boolean = /*this.hasProperty && */ this.isFinal && this.ub == p

    private[fpcf] def toFinalP : FinalP[E,P]

    private[fpcf] def toFinalUBP: FinalP[E, P]

    private[fpcf] def toFinalLBP: FinalP[E, P]

    /**
     * Returns the upper bound of the property if it is available – [[hasUBP]] has to be
     * `true` – otherwise an `UnsupportedOperationException` is thrown.
     *
     * The upper bound always models the best/most precise result w.r.t. the underlying lattice.
     * Here, "best" means that the set of potentially reachable states/instructions that the
     * analyzed program can ever assume is potentially smaller when compared to a worse property.
     *
     * The upper bound models the sound and precise result under the assumption that the
     * properties of all explicitly and implicitly relevant entities is as last queried or
     * implicitly assumed. I.e., unless a dependee is updated the upper bound represents
     * the correct and most precise result.
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
     * In that case – assuming we do not perform context sensitive analyses –
     * if the lower bound for `f` for the set of thrown exceptions is determined
     * to be `Set(IllegalArgumentException,UnkownError)`, the catch of the
     * `NumberFormatException` can be ruled out and a final result for `m` can be
     * computed.
     *
     * @note If the property is final, lb (and ub) will return the final property `p`.
     */
    @throws[UnsupportedOperationException]("if no property is available")
    def ub: P

    /**
     * Returns the lower bound of the property if it is available,
     * otherwise an `UnsupportedOperationException` is thrown. For details regarding the
     * precise semantics see the discussion for [[ub]].
     *
     * @note If the property is final, lb (and ub) will return the final property `p`.
     */
    @throws[UnsupportedOperationException]("if no property is available")
    def lb: P

    @throws[IllegalArgumentException]("if the given eps is not a valid update")
    private[fpcf] def checkIsValidPropertiesUpdate(
                                                      eps : SomeEPS,
                                                      newDependees: Traversable[SomeEOptionP]
                                                  ) : Unit

    /**
     * @return `true` if the given eps's bounds are different.
     */
    private[fpcf] def hasDifferentProperties(eps : SomeEPS) : Boolean = {
        (this.hasLBP && this.lb != eps.lb) || (this.hasUBP && this.ub != eps.ub)
    }

}

/**
 * Factory and extractor for [[EOptionP]] objects.
 *
 * @author Michael Eichberg
 */
object EOptionP {

    def unapply[E <: Entity, P <: Property](eOptP: EOptionP[E, P]): Some[(E, PropertyKey[P])] = {
        Some((eOptP.e, eOptP.pk))
    }
}

/**
 * A pairing of an [[Entity]] and an associated [[Property]] along with its state.
 *
 * @note Entities and properties are compared using `equals`.
 *
 * @author Michael Eichberg
 */
sealed trait EPS[+E <: Entity, +P <: Property] extends EOptionP[E, P] {

    final override def isEPK : Boolean = false
    def asEPK : EPK[E,P] = throw new ClassCastException();

    final override def toEPK: EPK[E, P] = EPK(e, pk)

    /**
     * Creates a [[FinalP]] object using the current ub if the ub is available. If the ub
     * is not available an exception is thrown.
     *
     * @note No check is done whether the property is actually final.
     */
    override def toFinalUBP: FinalP[E, P] = FinalP(e, ub)

    /**
     * Creates a [[FinalP]] object using the current lb if the lb is available. If the lb
     * is not available an exception is thrown.
     *
     * @note No check is done whether the property is actually final.
     */
    override def toFinalLBP: FinalP[E, P] = FinalP(e, lb)

    final override def isEPS : Boolean = true
    final override def asEPS: EPS[E, P] = this

    final override  def toEPS : Option[EPS[E,P]] = Some(this)
}


/**
 * Provides a factory for [[EPS]] objects.
 *
 * @author Michael Eichberg
 */
object EPS {

    def apply[E <: Entity, P <: Property](e: E, lb: P, ub: P): EPS[E, P] = {
        if (lb == ub) {
            if(lb == null /* && | || ub == null*/) {
                throw new IllegalArgumentException(s"lb and ub are null")
            } else {
                FinalP(e, ub)
            }
        } else if(lb == null)
            InterimUBP(e,  ub)
        else if (ub == null)
            InterimLBP(e, lb)
            else
            InterimLBUBP(e, lb, ub)
    }
}

/**
 * Provides an extractor for [[EPS]] objects.
 *
 * @author Michael Eichberg
 */
object ELBUBPS {

    /**
     * Returns the quadruple `(Entity, LowerBound, UpperBound, Boolean)`.
     *
     * @note Using ELBUBPS to extract a property for which no lower or upper bound was computed
     *       will (deliberately) result in an exception!
     */
    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[(E, P, P,Boolean)] = {
        Some((eps.e, eps.lb, eps.ub, eps.isFinal))
    }
}

object EUBPS {

    /**
     * Returns the triple `(Entity, upperBound : Property, isFinal : Boolean)`.
     */
    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[(E, P, Boolean)] = {
        Some((eps.e, eps.ub, eps.isFinal))
    }

}

object ELBPS {

    /**
     * Returns the triple `(Entity, lowerBound : Property, isFinal : Boolean)`.
     */
    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[(E, P, Boolean)] = {
        Some((eps.e, eps.lb, eps.isFinal))
    }

}

/**
 * Encapsulate the final property `P` for the entity `E`.
 *
 * For a detailed discussion of the semantics of `lb` and `ub` see [[EOptionP.ub]].
 */
final class FinalP[+E <: Entity, +P <: Property](val e: E, val p: P) extends EPS[E, P] {

    override def pk: PropertyKey[P] = p.key.asInstanceOf[PropertyKey[P]]

    override def isFinal: Boolean = true
    override def asFinal: FinalP[E, P] = this

    override private[fpcf] def toFinalP : FinalP[E,P] = throw new UnsupportedOperationException();

    override def hasLBP: Boolean = true
    override def lb: P = p
    override def toFinalLBP: FinalP[E, P] = this

    override def hasUBP: Boolean = true
    override def ub: P = p
    override def toFinalUBP: FinalP[E, P] = this

    private[fpcf] def checkIsValidPropertiesUpdate(
                                                      eps : SomeEPS,
                                                   newDependees: Traversable[SomeEOptionP]
                                                  ) : Unit = {
        throw new IllegalArgumentException("already final")
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: FinalP[_, _] ⇒ that.e == this.e && this.p == that.p
            case _                  ⇒ false
        }
    }

    override def hashCode: Int = e.hashCode() * 727 + p.hashCode()

    override def toString: String = {
        s"FinalP($e@${System.identityHashCode(e).toHexString},p=$p)"
    }

}

object FinalP {

    def apply[E <: Entity, P <: Property](e: E, p: P): FinalP[E, P] = new FinalP(e, p)

    def unapply[E <: Entity, P <: Property](eps: FinalP[E, P]): Option[(E, P)] = {
        Some((eps.e, eps.lb))
    }

}

sealed trait InterimP[+E <: Entity, +P <: Property] extends EPS[E, P] {

    override def isFinal: Boolean = false
    override def asFinal: FinalP[E, P] = throw new ClassCastException();

    private[fpcf] def checkIsValidLBPropertyUpdate(                                                      eps : SomeEPS                                                  ) : Unit = {
                            val newLBAsOP = eps.lb.asOrderedProperty
                val lbAsOP = lb.asInstanceOf[newLBAsOP.Self]
                newLBAsOP.checkIsEqualOrBetterThan(e,lbAsOP)

    }

     private[fpcf] def checkIsValidUBPropertyUpdate(                                                       eps : SomeEPS                                                   ) : Unit = {
         val ubAsOP = ub.asOrderedProperty
         val newUBAsOP = eps.ub.asInstanceOf[ubAsOP.Self]
         ubAsOP.checkIsEqualOrBetterThan(e,newUBAsOP)
    }

    override private[fpcf] def checkIsValidPropertiesUpdate(
                                                               eps : SomeEPS,
                                                               newDependees: Traversable[SomeEOptionP]
                                                           ) : Unit = {
try {
    if(hasLBP != eps.hasLBP || hasUBP != eps.hasUBP) {
        throw new IllegalArgumentException("inconsistent property bounds")
    }

        if(hasLBP && eps.lb.isOrderedProperty) {
            checkIsValidLBPropertyUpdate(eps)
        }
            if(hasUBP && eps.ub.isOrderedProperty) {
                checkIsValidUBPropertyUpdate(eps)
            }
} catch {
    case t: Throwable ⇒
        throw new IllegalArgumentException(
            s"$e: illegal update oldLB: $lb vs. newLB=$eps.lb "+
                newDependees.mkString("newDependees={", ", ", "}")+
                "; cause="+t.getMessage,
            t
        )
}

    }
}

object InterimP {

    def apply[E <: Entity, P <: Property](e: E, lb: P, ub: P): InterimP[E, P] = {
        if (lb == ub) {
                throw new IllegalArgumentException(s"lb and ub are equal ($lb)")
        } else if(lb == null)
            InterimUBP(e,  ub)
        else if (ub == null)
            InterimLBP(e, lb)
        else
            InterimLBUBP(e, lb, ub)
    }

}


/**
 * Encapsulates the intermediate lower- and upper bound related to the computation of the respective
 * property kind for the entity `E`.
 *
 * For a detailed discussion of the semantics of `lb` and `ub` see [[EOptionP.lb]].
 */
final class InterimLBUBP[+E <: Entity, +P <: Property](
                                                      val e:  E,
                                                      val lb: P,
                                                      val ub: P
                                                  ) extends InterimP[E, P] {

    assert(lb != null)
    assert(ub != null)

    if (PropertyStore.Debug && lb /*or ub*/ .isOrderedProperty) {
                val ubAsOP = ub.asOrderedProperty
                ubAsOP.checkIsEqualOrBetterThan(e, lb.asInstanceOf[ubAsOP.Self])
            }


    override def pk: PropertyKey[P] = ub/* or lb */.key.asInstanceOf[PropertyKey[P]]

    override private[fpcf] def toFinalP : FinalP[E,P] = FinalP(e,ub)

    override def hasLBP: Boolean = true
    override def hasUBP: Boolean = true

    override def equals(other: Any): Boolean = {
        other match {
            case that: InterimLBUBP[_, _] ⇒ e == that.e && lb == that.lb && ub == that.ub
            case _                          ⇒ false
        }
    }

    override def hashCode: Int = ((e.hashCode() * 31 + lb.hashCode()) * 31) + ub.hashCode()

    override def toString: String = {
        s"InterimLBUBP($e@${System.identityHashCode(e).toHexString},lb=$lb,ub=$ub)"
    }
}

object InterimLBUBP {

    def apply[E <: Entity, P <: Property](e: E, lb: P, ub: P): InterimP[E, P] = {
        assert(lb ne ub)
        new InterimLBUBP(e, lb, ub)
    }

    def unapply[E <: Entity, P <: Property](eps: InterimLBUBP[E, P]): Option[(E, P, P)] = {
        Some((eps.e, eps.lb, eps.ub))
    }
}

final class InterimUBP[+E <: Entity, +P <: Property](
                                                      val e:  E,
                                                      val ub: P
                                                    ) extends InterimP[E, P] {

    assert(ub != null)

     override def pk: PropertyKey[P] = ub.key.asInstanceOf[PropertyKey[P]]

    override private[fpcf] def toFinalP : FinalP[E,P] = FinalP(e,ub)

    override def hasLBP: Boolean = false
    override def hasUBP: Boolean = true

    override def lb: Nothing = throw new UnsupportedOperationException();

    override def equals(other: Any): Boolean = {
        other match {
            case that: InterimUBP[_, _] ⇒ e == that.e && ub == that.ub
            case _                          ⇒ false
        }
    }

    override def hashCode: Int = e.hashCode() * 31 + ub.hashCode()

    override def toString: String = {
        s"InterimUBP($e@${System.identityHashCode(e).toHexString},ub=$ub)"
    }
}

/**
 * Factory and extractor for `InterimUBP` objects. The extractor also matches `InterimLBUBP`
 * objects, but will throw an exception for `InterimLBP` objects. If you want to match
 * final and interim objects at the same time use the `E(LB|UB)PS` extractors.
 */
object InterimUBP {

    def apply[E <: Entity, P <: Property](e: E, ub: P): InterimP[E, P] = {
        new InterimUBP(e, ub)
    }

    def unapply[E <: Entity, P <: Property](eps: InterimP[E, P]): Option[(E, P)] = {
        Some((eps.e, eps.ub))
    }
}

final class InterimLBP[+E <: Entity, +P <: Property](
                                                        val e:  E,
                                                        val lb: P
                                                    ) extends InterimP[E, P] {

    assert(lb != null)

     override def pk: PropertyKey[P] = lb.key.asInstanceOf[PropertyKey[P]]

    override private[fpcf] def toFinalP : FinalP[E,P] = FinalP(e,lb)

    override def hasLBP: Boolean = true
    override def hasUBP: Boolean = false

    override def ub: Nothing = throw new UnsupportedOperationException();

    override def equals(other: Any): Boolean = {
        other match {
            case that: InterimLBP[_, _] ⇒ e == that.e && lb == that.lb
            case _                          ⇒ false
        }
    }

    override def hashCode: Int = e.hashCode() * 31 + lb.hashCode()

    override def toString: String = {
        s"InterimUBP($e@${System.identityHashCode(e).toHexString},lb=$lb)"
    }
}

/**
 * Factory and extractor for `InterimLBP` objects. The extractor also matches `InterimLBUBP`
 * objects, but will throw an exception for `InterimUBP` objects. If you want to match
 * final and interim objects at the same time use the `E(LB|UB)PS` extractors.
 */
object InterimLBP {

    def apply[E <: Entity, P <: Property](e: E, ub: P): InterimP[E, P] = {
        new InterimLBP(e, ub)
    }

    def unapply[E <: Entity, P <: Property](eps: InterimP[E, P]): Option[(E, P)] = {
        Some((eps.e, eps.ub))
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

    override def hasLBP: Boolean = false
    override def lb: Nothing = throw new UnsupportedOperationException();
    override private[fpcf] def toFinalLBP = throw new UnsupportedOperationException();

    override def hasUBP: Boolean = false
    override def ub: Nothing = throw new UnsupportedOperationException();
    override private[fpcf] def toFinalUBP = throw new UnsupportedOperationException();

    override def isFinal: Boolean = false
    override def asFinal: FinalP[E, P] = throw new ClassCastException();

    override private[fpcf] def toFinalP : FinalP[E,P] = throw new UnsupportedOperationException();

    override def isEPS: Boolean = false
    override def asEPS: EPS[E, P] = throw new ClassCastException();

    override def isEPK : Boolean = true
    override def asEPK : EPK[E,P] = this

    override def toEPK: this.type = this

    override def toEPS : Option[EPS[E,P]] = None

    override private[fpcf] def checkIsValidPropertiesUpdate(
                                                               eps : SomeEPS,
                                                               newDependees: Traversable[SomeEOptionP]
                                                           ) : Unit = {}

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
        s"EPK($e@${System.identityHashCode(e).toHexString},pkName=$pkName,pkId=$pkId)"
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
