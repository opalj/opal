/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

sealed abstract class PropertyBounds(val pk: PropertyKind) {

    def lowerBound: Boolean
    def upperBound: Boolean

    def toSpecification: String = {
        ((lowerBound, upperBound) match {
            case (true, true)   ⇒ "LBP+UBP"
            case (true, false)  ⇒ "LBP"
            case (false, true)  ⇒ "UBP"
            case (false, false) ⇒ "FinalP" // Used by transformers (only)
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
object PropertyBounds {

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
