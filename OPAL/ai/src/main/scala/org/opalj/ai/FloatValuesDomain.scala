/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * Defines the public interface between the abstract interpreter and the domain
 * that implements the functionality related to the handling of `float` values.
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
trait FloatValuesDomain extends FloatValuesFactory { this: ValuesDomain =>

    //
    // RELATIONAL OPERATORS
    //
    def fcmpg(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def fcmpl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    //
    // UNARY ARITHMETIC EXPRESSIONS
    //
    def fneg(pc: Int, value: DomainValue): DomainValue

    //
    // BINARY ARITHMETIC EXPRESSIONS
    //
    def fadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def fdiv(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def fmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def frem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def fsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
}
