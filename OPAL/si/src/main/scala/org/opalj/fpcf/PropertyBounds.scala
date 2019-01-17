/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

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
sealed abstract class PropertiesBoundType
case object LBProperties extends PropertiesBoundType
case object UBProperties extends PropertiesBoundType
case object LUBProperties extends PropertiesBoundType
case object FinalProperties extends PropertiesBoundType

object PropertyBounds {

    def apply(pbt : PropertiesBoundType, pks: Array[PropertyKind]): Set[PropertyBounds] = {
        pbt match {
            case LBProperties ⇒ lbs(pks :_*)
            case UBProperties ⇒ ubs(pks : _*)
            case LUBProperties ⇒ lubs(pks : _*)
            case FinalProperties ⇒ finalPs(pks : _*)
        }
    }

    def apply(pbt : PropertiesBoundType, pk: PropertyKind): PropertyBounds = {
        pbt match {
            case LBProperties ⇒ lb(pk)
            case UBProperties ⇒ ub(pk)
            case LUBProperties ⇒ lub(pk)
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
