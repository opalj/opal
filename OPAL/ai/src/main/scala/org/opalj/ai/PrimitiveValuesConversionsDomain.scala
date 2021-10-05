/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * Defines the methods that performs type conversions between primitive values with different
 * computational types.
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
trait PrimitiveValuesConversionsDomain { domain: ValuesDomain =>

    def i2d(pc: Int, value: DomainValue): DomainValue
    def i2f(pc: Int, value: DomainValue): DomainValue
    def i2l(pc: Int, value: DomainValue): DomainValue

    /** Conversion of the given long value to a double value. */
    def l2d(pc: Int, value: DomainValue): DomainValue
    /** Conversion of the given long value to a float value. */
    def l2f(pc: Int, value: DomainValue): DomainValue
    /** Conversion of the given long value to an integer value. */
    def l2i(pc: Int, value: DomainValue): DomainValue

    def f2d(pc: Int, value: DomainValue): DomainValue
    def f2i(pc: Int, value: DomainValue): DomainValue
    def f2l(pc: Int, value: DomainValue): DomainValue

    def d2f(pc: Int, value: DomainValue): DomainValue
    def d2i(pc: Int, value: DomainValue): DomainValue
    def d2l(pc: Int, value: DomainValue): DomainValue

}
