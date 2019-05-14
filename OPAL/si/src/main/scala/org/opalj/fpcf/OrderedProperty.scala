/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

/**
 * Ordered properties make the order between all properties of a specific kind explicit;
 * all properties that are of the same kind have to inherit from ordered property or none.
 *
 * This information is used by the property store when assertions/debugging is turned on to test
 * if an analysis, which derives a new property, always derives a more precise property.
 *
 * @author Michael Eichberg
 */
trait OrderedProperty extends Property {

    override type Self <: OrderedProperty

    /**
     * Tests if this property is equal or better than the given one (better means that the
     * value is above the given value in the underlying lattice.)
     */
    @throws[IllegalArgumentException]("if this property is not more precise than the given one")
    def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit

    /**
     * Returns a value between zero and 9 which describes how close the value is to its absolute
     * bottom. Zero means the value is very close to the bottom and 9 means the value is
     * far away from the bottom.
     *
     * The default value is "5".
     */
    def bottomness: Int = 5
}

object OrderedProperty {

    /** The value is the lattice's top value. */
    final val TopBottomness = 9

    /** The value is the lattice's bottom value. */
    final val BottomBottomness = 0

    final val DefaultBottomness = TopBottomness

}