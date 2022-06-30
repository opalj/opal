/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

/**
 * Specifies the bounds of the properties that may be used/derived.
 *
 * @author Michael Eichberg
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
