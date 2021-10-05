/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * Defines the public interface between the abstract interpreter and the domain
 * that implements the functionality related to the handling of `double` values.
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
trait DoubleValuesDomain extends DoubleValuesFactory { domain =>

    //
    // RELATIONAL OPERATORS
    //
    def dcmpg(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def dcmpl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    //
    // UNARY ARITHMETIC EXPRESSIONS
    //
    def dneg(pc: Int, value: DomainValue): DomainValue

    //
    // BINARY ARITHMETIC EXPRESSIONS
    //
    def dadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    def ddiv(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    def dmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    def drem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    def dsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

}
