/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import scala.language.higherKinds

/**
 * Encapsulate the information about the property bounds used or derived by an analysis.
 *
 * @note Equality is only based on the `PropertyKind` and not on the information about
 *       the concrete bounds.
 *
 * @author Michael Eichberg
 */
sealed abstract class PropertyBounds(val pk: PropertyKind) {

    def lowerBound: Boolean
    def upperBound: Boolean

    def toSpecification: String = {
        ((lowerBound, upperBound) match {
            case (true, true)   ⇒ "LBP+UBP"
            case (true, false)  ⇒ "LBP"
            case (false, true)  ⇒ "UBP"
            case (false, false) ⇒ "FinalP" // Intended to be used only by transformers
        }) + '(' + PropertyKey.name(pk) + ')'
    }

    final override def equals(other: Any): Boolean = {
        other match {
            case that: PropertyBounds ⇒ this.pk == that.pk
            case _                    ⇒ false
        }
    }

    final override def hashCode: Int = pk.id

    final override def toString: String = {
        s"PropertyBounds(pk=$pk,lowerBound=$lowerBound,upperBound=$upperBound)"
    }
}

/**
 * Specifies the bounds of the properties that may be used/derived.
 */
sealed abstract class PropertiesBoundType {
    type MatchedProperties[P]
    def unapply[E <: Entity, P <: Property](e: EPS[E, P]): Option[MatchedProperties[P]]
}
sealed abstract class SinglePropertiesBoundType extends PropertiesBoundType {
    final override type MatchedProperties[P] = P
}
case object LBProperties extends SinglePropertiesBoundType {
    def unapply[E <: Entity, P <: Property](e: EPS[E, P]): Option[P] = {
        if (e.hasLBP) Some(e.lb) else None
    }
}
case object UBProperties extends SinglePropertiesBoundType {
    def unapply[E <: Entity, P <: Property](e: EPS[E, P]): Option[P] = {
        if (e.hasUBP) Some(e.ub) else None
    }
}
case object FinalProperties extends SinglePropertiesBoundType {
    def unapply[E <: Entity, P <: Property](e: EPS[E, P]): Option[P] = {
        if (e.isFinal) Some(e.asFinal.p) else None
    }
}

case object LUBProperties extends PropertiesBoundType {
    override type MatchedProperties[P] = (P, P)
    def unapply[E <: Entity, P <: Property](e: EPS[E, P]): Option[(P, P)] = {
        if (e.hasLBP && e.hasUBP) Some((e.lb, e.ub)) else None
    }
}

object PropertyBounds {

    def apply(pbt: PropertiesBoundType, pks: Array[PropertyKind]): Set[PropertyBounds] = {
        pbt match {
            case LBProperties    ⇒ lbs(pks: _*)
            case UBProperties    ⇒ ubs(pks: _*)
            case LUBProperties   ⇒ lubs(pks: _*)
            case FinalProperties ⇒ finalPs(pks: _*)
        }
    }

    def apply(pbt: PropertiesBoundType, pk: PropertyKind): PropertyBounds = {
        pbt match {
            case LBProperties    ⇒ lb(pk)
            case UBProperties    ⇒ ub(pk)
            case LUBProperties   ⇒ lub(pk)
            case FinalProperties ⇒ finalP(pk)
        }
    }

    def finalP(pk: PropertyKind): PropertyBounds = {
        new PropertyBounds(pk) {
            override def lowerBound: Boolean = false
            override def upperBound: Boolean = false
        }
    }

    def finalPs(pks: PropertyKind*): Set[PropertyBounds] = pks.map(finalP).toSet

    def lub(pk: PropertyKind): PropertyBounds = {
        new PropertyBounds(pk) {
            override def lowerBound: Boolean = true
            override def upperBound: Boolean = true
        }
    }

    def lubs(pks: PropertyKind*): Set[PropertyBounds] = pks.map(lub).toSet

    def lb(pk: PropertyKind): PropertyBounds = {
        new PropertyBounds(pk) {
            override def lowerBound: Boolean = true
            override def upperBound: Boolean = false
        }
    }

    def lbs(pks: PropertyKind*): Set[PropertyBounds] = pks.map(lb).toSet

    def ub(pk: PropertyKind): PropertyBounds = {
        new PropertyBounds(pk) {
            override def lowerBound: Boolean = false
            override def upperBound: Boolean = true
        }
    }

    def ubs(pks: PropertyKind*): Set[PropertyBounds] = pks.map(ub).toSet

}
