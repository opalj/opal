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

    def isEPK: Boolean
    def asEPK: EPK[E, P]

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

    def isEPS: Boolean
    /**
     * This EOptionP as an EPS object; defined iff at least a preliminary property exists.
     */
    def asEPS: EPS[E, P]

    def toEPS: Option[EPS[E, P]]

    /**
     * Returns `true` if and only if we have a property and the property was stored in the
     * store using `(Multi)Result`.
     */
    def isFinal: Boolean
    final def isRefinable: Boolean = !isFinal
    def asFinal: FinalEP[E, P]

    def asInterim: InterimEP[E, P]

    /**
     * Combines the test if we have a final property and – if we have one – if it is equal (by
     * means of an equality check) to the given one.
     */
    def is(p: AnyRef): Boolean = /*this.hasProperty && */ this.isFinal && this.ub == p

    /**
     * Converts an `InterimEP` to a `FinalEP`; fails otherwise.
     */
    private[fpcf] def toFinalEP: FinalEP[E, P]

    private[fpcf] def toFinalEUBP: FinalEP[E, P]

    private[fpcf] def toFinalELBP: FinalEP[E, P]

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
     * to be `Set(IllegalArgumentException,UnkownError)`, then the catch of the
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

    /**
     * Returns `true` if this `EOptionP` is updated when compared with the given `oldEOptionP`,
     * provided that this `EOptionP` is guaranteed to be at most as old as `oldEOptionP`.
     * That is, this EPS is considered to be newer if the properties are different.
     *
     * @note The caller has to ensure that this EOptionP and and the given EOptionP are
     *       comparable. That is, they define properties of the same kind associated with
     *       the same entity and same bounds.
     */
    private[fpcf] def isUpdatedComparedTo(oldEOptionP: EOptionP[Entity, Property]): Boolean

    @throws[IllegalArgumentException]("if the given eps is not a valid update")
    private[fpcf] def checkIsValidPropertiesUpdate(
        eps:          SomeEPS,
        newDependees: Iterable[SomeEOptionP]
    ): Unit

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

    final override def isEPK: Boolean = false
    def asEPK: EPK[E, P] = throw new ClassCastException();

    final override def toEPK: EPK[E, P] = EPK(e, pk)

    /**
     * Creates a [[FinalP]] object using the current ub if the ub is available. If the ub
     * is not available an exception is thrown.
     *
     * @note No check is done whether the property is actually final.
     */
    override def toFinalEUBP: FinalEP[E, P] = FinalEP(e, ub)

    /**
     * Creates a [[FinalP]] object using the current lb if the lb is available. If the lb
     * is not available an exception is thrown.
     *
     * @note No check is done whether the property is actually final.
     */
    override def toFinalELBP: FinalEP[E, P] = FinalEP(e, lb)

    final override def isEPS: Boolean = true
    final override def asEPS: EPS[E, P] = this

    final override def toEPS: Option[EPS[E, P]] = Some(this)
}

/**
 * Provides a factory for [[EPS]] objects.
 *
 * @author Michael Eichberg
 */
object EPS {

    def apply[E <: Entity, P <: Property](e: E, lb: P, ub: P): EPS[E, P] = {
        if (lb == ub) {
            if (lb == null /* && | || ub == null*/ ) {
                throw new IllegalArgumentException(s"lb and ub are null")
            } else {
                FinalEP(e, ub)
            }
        } else if (lb == null)
            InterimEUBP(e, ub)
        else if (ub == null)
            InterimELBP(e, lb)
        else
            InterimELUBP(e, lb, ub)
    }

    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[E] = Some(eps.e)
}

/**
 * Factory and extractor for [[EPS]] objects.
 *
 * @author Michael Eichberg
 */
object SomeEPS {

    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[(E, PropertyKey[P])] = {
        Some((eps.e, eps.pk))
    }
}

object LBPS {

    /**
     * Returns the tuple `(LowerBound, Boolean)`.
     *
     * @note Using LBPS to extract a property for which no lower bound was computed
     *       will (deliberately) result in an exception!
     */
    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[(P, Boolean)] = {
        Some((eps.lb, eps.isFinal))
    }

}

object UBPS {

    /**
     * Returns the tuple `(UpperBound, Boolean)`.
     *
     * @note Using UBPS to extract a property for which no upper bound was computed
     *       will (deliberately) result in an exception!
     */
    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[(P, Boolean)] = {
        Some((eps.ub, eps.isFinal))
    }
}

object LUBPS {

    /**
     * Returns the triple `(LowerBound, UpperBound, Boolean)`.
     *
     * @note Using LUBPS to extract a property for which no lower or upper bound was computed
     *       will (deliberately) result in an exception!
     */
    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[(P, P, Boolean)] = {
        Some((eps.lb, eps.ub, eps.isFinal))
    }

}

/**
 * Provides an extractor for [[EPS]] objects.
 *
 * @author Michael Eichberg
 */
object ELUBPS {

    /**
     * Returns the quadruple `(Entity, LowerBound, UpperBound, Boolean)`.
     *
     * @note Using ELUBPS to extract a property for which no lower or upper bound was computed
     *       will (deliberately) result in an exception!
     */
    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[(E, P, P, Boolean)] = {
        Some((eps.e, eps.lb, eps.ub, eps.isFinal))
    }
}

object ELUBP {

    /**
     * Returns the triple `(Entity, lowerBound : Property, upperBound : Property)`.
     *
     * @note Using ELUBP to extract a property for which no lower or upper bound was computed
     *       will (deliberately) result in an exception!
     */
    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[(E, P, P)] = {
        Some((eps.e, eps.lb, eps.ub))
    }
}

object LUBP {

    /**
     * Returns the pair `(lowerBound : Property, upperBound : Property)`.
     *
     * @note Using LUBP to extract a property for which no lower or upper bound was computed
     *       will (deliberately) result in an exception!
     */
    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[(P, P)] = {
        Some((eps.lb, eps.ub))
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

object EUBP {

    /**
     * Returns the pair `(Entity, upperBound : Property)`.
     */
    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[(E, P)] = {
        Some((eps.e, eps.ub))
    }

}

object UBP {

    /**
     * Returns the `lowerBound : Property`.
     */
    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[P] = Some(eps.ub)

}

object ELBPS {

    /**
     * Returns the triple `(Entity, lowerBound : Property, isFinal : Boolean)`.
     */
    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[(E, P, Boolean)] = {
        Some((eps.e, eps.lb, eps.isFinal))
    }

}

object ELBP {

    /**
     * Returns the pair `(Entity, lowerBound : Property)`.
     */
    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[(E, P)] = {
        Some((eps.e, eps.lb))
    }

}

object LBP {

    /**
     * Returns the `lowerBound : Property`.
     */
    def unapply[E <: Entity, P <: Property](eps: EPS[E, P]): Some[P] = Some(eps.lb)

}

/**
 * Encapsulate the final property `P` for the entity `E`.
 *
 * For a detailed discussion of the semantics of `lb` and `ub` see [[EOptionP.ub]].
 */
final class FinalEP[+E <: Entity, +P <: Property](val e: E, val p: P) extends EPS[E, P] {

    override lazy val pk: PropertyKey[P] = p.key.asInstanceOf[PropertyKey[P]]

    override def isFinal: Boolean = true
    override def asFinal: FinalEP[E, P] = this

    override private[fpcf] def toFinalEP: FinalEP[E, P] = this

    override def hasLBP: Boolean = true
    override def lb: P = p
    override def toFinalELBP: FinalEP[E, P] = this

    override def hasUBP: Boolean = true
    override def ub: P = p
    override def toFinalEUBP: FinalEP[E, P] = this

    override def asInterim: InterimEP[E, P] = throw new ClassCastException();

    override private[fpcf] def isUpdatedComparedTo(
        oldEOptionP: EOptionP[Entity, Property]
    ): Boolean = {
        oldEOptionP.isRefinable
    }

    private[fpcf] def checkIsValidPropertiesUpdate(
        eps:          SomeEPS,
        newDependees: Iterable[SomeEOptionP]
    ): Unit = {
        throw new IllegalArgumentException("already final")
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: FinalEP[_, _] => (this eq that) || (that.e == this.e && this.p == that.p)
            case _                   => false
        }
    }

    override lazy val hashCode: Int = e.hashCode() * 727 + p.hashCode()

    override def toString: String = {
        s"FinalEP($e@${System.identityHashCode(e).toHexString},p=$p)"
    }

}

object FinalEP {

    def apply[E <: Entity, P <: Property](e: E, p: P): FinalEP[E, P] = new FinalEP(e, p)

    def unapply[E <: Entity, P <: Property](eps: FinalEP[E, P]): Some[(E, P)] = {
        Some((eps.e, eps.p))
    }

}

/**
 * Factory and extractor for [[FinalEP]] objects.
 *
 * @author Michael Eichberg
 */
object SomeFinalEP {

    def unapply[E <: Entity, P <: Property](finalEP: FinalEP[E, P]): Some[(E, PropertyKey[P])] = {
        Some((finalEP.e, finalEP.pk))
    }
}

object FinalP {

    def unapply[E <: Entity, P >: Null <: Property](eps: FinalEP[E, P]): Some[P] = Some(eps.p)

}

object FinalE {

    def unapply[E <: Entity, P <: Property](eps: FinalEP[E, P]): Some[E] = Some(eps.e)

}

sealed trait InterimEP[+E <: Entity, +P <: Property] extends EPS[E, P] {

    override def isFinal: Boolean = false
    override def asFinal: FinalEP[E, P] = throw new ClassCastException();

    override def asInterim: InterimEP[E, P] = this

    private[fpcf] def checkIsValidLBPropertyUpdate(eps: SomeEPS): Unit = {
        val newLBAsOP = eps.lb.asOrderedProperty
        val lbAsOP = lb.asInstanceOf[newLBAsOP.Self]
        newLBAsOP.checkIsEqualOrBetterThan(e, lbAsOP)
    }

    private[fpcf] def checkIsValidUBPropertyUpdate(eps: SomeEPS): Unit = {
        val ubAsOP = ub.asOrderedProperty
        val newUBAsOP = eps.ub.asInstanceOf[ubAsOP.Self]
        ubAsOP.checkIsEqualOrBetterThan(e, newUBAsOP)
    }

    override private[fpcf] def checkIsValidPropertiesUpdate(
        eps:          SomeEPS,
        newDependees: Iterable[SomeEOptionP]
    ): Unit = {
        try {
            if (eps.isRefinable && (hasLBP != eps.hasLBP || hasUBP != eps.hasUBP)) {
                throw new IllegalArgumentException(s"inconsistent property bounds: $this vs. $eps")
            }

            if (hasLBP && eps.lb.isOrderedProperty) {
                checkIsValidLBPropertyUpdate(eps)
            }
            if (hasUBP && eps.ub.isOrderedProperty) {
                checkIsValidUBPropertyUpdate(eps)
            }
        } catch {
            case t: Throwable =>
                val m = s"$e: illegal update oldLB: $lb vs. newLB=$eps.lb "+
                    newDependees.mkString("newDependees={", ", ", "}; cause=") + t.getMessage
                throw new IllegalArgumentException(m, t)
        }
    }
}

object InterimE {

    /**
     * Extracts the entity of an interim property.
     *
     * @note When we just have an InterimP object, we don't know which properties (ub, lb or both)
     *       are available.
     */
    def unapply[E <: Entity, P <: Property](interimP: InterimEP[E, P]): Some[E] = Some(interimP.e)

}

/**
 * Encapsulates the intermediate lower- and upper bound related to the computation of the respective
 * property kind for the entity `E`.
 *
 * For a detailed discussion of the semantics of `lb` and `ub` see [[EOptionP.ub]].
 */
final class InterimELUBP[+E <: Entity, +P <: Property](
        val e:  E,
        val lb: P,
        val ub: P
) extends InterimEP[E, P] {

    assert(lb != null)
    assert(ub != null)

    if (PropertyStore.Debug && lb /*or ub*/ .isOrderedProperty) {
        val ubAsOP = ub.asOrderedProperty
        ubAsOP.checkIsEqualOrBetterThan(e, lb.asInstanceOf[ubAsOP.Self])
    }

    override lazy val pk: PropertyKey[P] = ub /* or lb */ .key.asInstanceOf[PropertyKey[P]]

    override private[fpcf] def toFinalEP: FinalEP[E, P] = FinalEP(e, ub)

    override def hasLBP: Boolean = true
    override def hasUBP: Boolean = true

    override private[fpcf] def isUpdatedComparedTo(
        oldEOptionP: EOptionP[Entity, Property]
    ): Boolean = {
        oldEOptionP.isEPK || oldEOptionP.lb != this.lb || oldEOptionP.ub != this.ub
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: InterimELUBP[_, _] =>
                (this eq that) || (e == that.e && lb == that.lb && ub == that.ub)
            case _ =>
                false
        }
    }

    override lazy val hashCode: Int = ((e.hashCode() * 31 + lb.hashCode()) * 31) + ub.hashCode()

    override def toString: String = {
        s"InterimELUBP($e@${System.identityHashCode(e).toHexString},lb=$lb,ub=$ub)"
    }
}

object InterimELUBP {

    def apply[E <: Entity, P <: Property](e: E, lb: P, ub: P): InterimELUBP[E, P] = {
        assert(lb ne ub)
        new InterimELUBP(e, lb, ub)
    }

    def unapply[E <: Entity, P >: Null <: Property](eps: InterimELUBP[E, P]): Some[(E, P, P)] = {
        Some((eps.e, eps.lb, eps.ub))
    }
}

object InterimLUBP {

    def unapply[E <: Entity, P >: Null <: Property](eps: InterimELUBP[E, P]): Some[(P, P)] = {
        Some((eps.lb, eps.ub))
    }
}

final class InterimEUBP[+E <: Entity, +P <: Property](
        val e:  E,
        val ub: P
) extends InterimEP[E, P] {

    assert(ub != null)

    override lazy val pk: PropertyKey[P] = ub.key.asInstanceOf[PropertyKey[P]]

    override private[fpcf] def toFinalEP: FinalEP[E, P] = FinalEP(e, ub)

    override def hasLBP: Boolean = false
    override def hasUBP: Boolean = true

    override def lb: Nothing = throw new UnsupportedOperationException();

    override private[fpcf] def isUpdatedComparedTo(
        oldEOptionP: EOptionP[Entity, Property]
    ): Boolean = {
        oldEOptionP.isEPK || oldEOptionP.ub != this.ub
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: InterimEUBP[_, _] =>
                (this eq that) || (this.e == that.e && this.ub == that.ub)
            case _ =>
                false
        }
    }

    override lazy val hashCode: Int = e.hashCode() * 31 + ub.hashCode()

    override def toString: String = {
        s"InterimEUBP($e@${System.identityHashCode(e).toHexString},ub=$ub)"
    }
}

/**
 * Factory and extractor for `InterimEUBP` objects. The extractor also matches `InterimELUBP`
 * objects, but will throw an exception for `InterimELBP` objects. If you want to match
 * final and interim objects at the same time use the `E(LB|UB)PS` extractors.
 */
object InterimEUBP {

    def apply[E <: Entity, P <: Property](e: E, ub: P): InterimEUBP[E, P] = {
        new InterimEUBP(e, ub)
    }

    def unapply[E <: Entity, P >: Null <: Property](eps: InterimEP[E, P]): Some[(E, P)] = {
        Some((eps.e, eps.ub))
    }
}

/**
 * Factory and extractor for `InterimEUBP` objects. The extractor also matches `InterimELUBP`
 * objects, but will throw an exception for `InterimELBP` objects. If you want to match
 * final and interim objects at the same time use the `E(LB|UB)PS` extractors.
 */
object InterimEUB {

    def unapply[E <: Entity, P <: Property](eps: InterimEP[E, P]): Some[E] = {
        if (!eps.hasUBP)
            throw new IllegalArgumentException(s"$eps does not define an upper bound property");

        Some(eps.e)
    }
}

/**
 * Defines an extractor that matches EPKs and Interim properties where the latter only defines an
 * upper bound, but does not define a lower bound.
 *
 * For example, an analysis which declares that it can only handle lower bounds, but can't process
 * EPSs which only define an upper bound, may still see EPS which define only the upper bound
 * but can process them in the same way as an EPK; that is, it can basically ignore the
 * upper bound information. The scheduler ensures that the analysis scenario is a valid one
 * and no cyclic dependent computations may arise.
 * {{{
 * def continuation(eps : EPS) : ... = eps match {
 *   case FinalEP(...) => // Matches only final properties.
 *   case LBP(...)     => // Matches final  and interim properties which define a lower bound.
 *   case NoLBP(...)   => // Matches EPKs and EPS which just define an upper bound.
 * }
 * }}}
 */
object NoLBP {

    def unapply[E <: Entity, P >: Null <: Property](eps: EOptionP[E, P]): Option[EOptionP[E, P]] = {
        if (!eps.hasLBP)
            Some(eps)
        else
            None
    }
}

/**
 * Defines an extractor that matches EPKs and Interim properties where the latter only defines a
 * lower bound, but does not define an upper bound.
 */
object NoUBP {

    def unapply[E <: Entity, P >: Null <: Property](eps: EOptionP[E, P]): Option[EOptionP[E, P]] = {
        if (!eps.hasUBP)
            Some(eps)
        else
            None
    }
}

object InterimUBP {

    def unapply[E <: Entity, P <: Property](eps: InterimEP[E, P]): Some[P] = Some(eps.ub)

}

final class InterimELBP[+E <: Entity, +P <: Property](
        val e:  E,
        val lb: P
) extends InterimEP[E, P] {

    assert(lb != null)

    override lazy val pk: PropertyKey[P] = lb.key.asInstanceOf[PropertyKey[P]]

    override private[fpcf] def toFinalEP: FinalEP[E, P] = FinalEP(e, lb)

    override def hasLBP: Boolean = true
    override def hasUBP: Boolean = false

    override def ub: Nothing = throw new UnsupportedOperationException();

    override private[fpcf] def isUpdatedComparedTo(
        oldEOptionP: EOptionP[Entity, Property]
    ): Boolean = {
        oldEOptionP.isEPK || oldEOptionP.lb != this.lb
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: InterimELBP[_, _] =>
                (this eq that) || (this.lb == that.lb && this.e == that.e)
            case _ =>
                false
        }
    }

    override lazy val hashCode: Int = e.hashCode() * 31 + lb.hashCode()

    override def toString: String = {
        s"InterimELBP($e@${System.identityHashCode(e).toHexString},lb=$lb)"
    }
}

/**
 * Factory and extractor for `InterimLBP` objects. The extractor also matches `InterimLUBP`
 * objects, but will throw an exception for `InterimUBP` objects. If you want to match
 * final and interim objects at the same time use the `E(LB|UB)PS` extractors.
 */
object InterimELBP {

    def apply[E <: Entity, P <: Property](e: E, ub: P): InterimELBP[E, P] = {
        new InterimELBP(e, ub)
    }

    def unapply[E <: Entity, P >: Null <: Property](eps: InterimEP[E, P]): Some[(E, P)] = {
        Some((eps.e, eps.ub))
    }
}

object InterimLBP {

    def unapply[E <: Entity, P >: Null <: Property](eps: InterimEP[E, P]): Some[P] = Some(eps.ub)

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
    override private[fpcf] def toFinalELBP = throw new UnsupportedOperationException(toString);

    override def hasUBP: Boolean = false
    override def ub: Nothing = throw new UnsupportedOperationException();
    override private[fpcf] def toFinalEUBP = throw new UnsupportedOperationException(toString);

    override def isFinal: Boolean = false
    override def asFinal: FinalEP[E, P] = throw new ClassCastException();

    override private[fpcf] def toFinalEP: FinalEP[E, P] = {
        throw new UnsupportedOperationException(toString)
    };

    override def isEPS: Boolean = false
    override def asEPS: EPS[E, P] = {
        throw new ClassCastException()
    };

    override def isEPK: Boolean = true
    override def asEPK: EPK[E, P] = this

    override def toEPK: this.type = this

    override def toEPS: Option[EPS[E, P]] = None

    override def asInterim: InterimEP[E, P] = throw new ClassCastException();

    override private[fpcf] def isUpdatedComparedTo(
        oldEOptionP: EOptionP[Entity, Property]
    ): Boolean = {
        false
    }

    override private[fpcf] def checkIsValidPropertiesUpdate(
        eps:          SomeEPS,
        newDependees: Iterable[SomeEOptionP]
    ): Unit = {}

    override def equals(other: Any): Boolean = {
        other match {
            case that: EPK[_, _] => (this eq that) || (this.pk == that.pk && that.e == this.e)
            case _               => false
        }
    }

    override lazy val hashCode: Int = e.hashCode() * 31 + pk.id

    override def toString: String = {
        val pkId = pk.id
        val pkName = PropertyKey.name(pkId)
        s"EPK($e@${System.identityHashCode(e).toHexString},$pkName#$pkId)"
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

    def unapply[E <: Entity, P >: Null <: Property](epk: EPK[E, P]): Some[(E, PropertyKey[P])] = {
        Some((epk.e, epk.pk))
    }
}
