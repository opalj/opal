/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

/**
 * Ordered properties make the order between all properties regarding a respective kind explicit;
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
}
