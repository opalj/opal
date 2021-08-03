/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package collection

/**
 * An efficient representation of a set of types if some types are actually upper type bounds
 * and hence already represent sets of types.
 *
 * @author Michael Eichberg
 */
abstract class TypesSet /*extends Set[(ObjectType,...)]*/ {

    def concreteTypes: Set[ObjectType] // IMPROVE [L2] Use UIDSet
    def upperTypeBounds: Set[ObjectType] // IMPROVE [L2] Use UIDSet

    /**
     * Returns `true` if this set is empty.
     * @see [[size]]
     */
    def isEmpty: Boolean = concreteTypes.isEmpty && upperTypeBounds.isEmpty

    /**
     * Returns `true` if this set contains at least one type.
     * @see [[size]]
     */
    def nonEmpty: Boolean = concreteTypes.nonEmpty || upperTypeBounds.nonEmpty

    /**
     * The number of types explicitly stored in the set. This number is '''independent'''
     * of the number of represented types. E.g., if `java.lang.Object` is stored in this set,
     * then the size of this set is 1 even though it represents all known types.
     */
    def size: Int = concreteTypes.size + upperTypeBounds.size

    /**
     * @param f A call back function will be called for each type stored in the set along with
     *      the information if type represents an upper type bound (`true`) or refers to a
     *      concrete class/interface type (the second parameter is then `false`).
     */
    def foreach[U](f: (ObjectType, Boolean) => U): Unit = {
        concreteTypes.foreach { tpe => f(tpe, false) }
        upperTypeBounds.foreach { tpe => f(tpe, true) }
    }

    /**
     * Returns a pair where the first set contains all concrete types and the second set
     * contains all upper type bounds.
     */
    def types: (Set[ObjectType], Set[ObjectType]) = (concreteTypes, upperTypeBounds)

    final override def equals(other: Any): Boolean = {

        other match {
            case that: TypesSet =>
                concreteTypes == that.concreteTypes && upperTypeBounds == that.upperTypeBounds
            case _ => false
        }
    }

    final override lazy val hashCode: Int = {
        concreteTypes.hashCode() * 111 + upperTypeBounds.hashCode()
    }

    override def toString: String = {
        if (upperTypeBounds.isEmpty && concreteTypes.isEmpty)
            return "EmptyTypesSet";

        if (upperTypeBounds.isEmpty)
            return concreteTypes.map(_.toJava).mkString("PreciseTypesSet(", ",", ")")

        if (concreteTypes.isEmpty)
            return upperTypeBounds.map(_.toJava).mkString("UpperTypeBoundsSet(", ",", ")")

        upperTypeBounds.map(_.toJava).mkString(
            concreteTypes.map(_.toJava).mkString(
                "TypesSet(preciseTypes={",
                ",",
                "},upperTypeBounds={"
            ),
            ",",
            "})"
        )
    }
}

object TypesSet {

    def empty: EmptyTypesSet.type = EmptyTypesSet

    final val SomeException: TypesSet = UpperTypeBounds(Set(ObjectType.Throwable))
}

case object EmptyTypesSet extends TypesSet {

    def concreteTypes: Set[ObjectType] = Set.empty
    def upperTypeBounds: Set[ObjectType] = Set.empty

}

case class TheTypes( final val concreteTypes: Set[ObjectType]) extends TypesSet {

    final override def upperTypeBounds = Set.empty

}

case class UpperTypeBounds( final val upperTypeBounds: Set[ObjectType]) extends TypesSet {

    final override def concreteTypes = Set.empty

}
