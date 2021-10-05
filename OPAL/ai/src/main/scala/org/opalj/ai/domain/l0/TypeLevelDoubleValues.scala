/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import org.opalj.value.IsDoubleValue
import org.opalj.br.DoubleType

/**
 * This partial `Domain` performs all computations related to primitive double
 * values at the type level.
 *
 * This domain can be used as a foundation to build more complex domains.
 *
 * @author Michael Eichberg
 * @author David Becker
 */
trait TypeLevelDoubleValues extends DoubleValuesDomain {
    domain: IntegerValuesFactory =>

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF DOUBLE VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over double values at the type level.
     */
    trait DoubleValue extends TypedValue[DoubleType] with IsDoubleValue {
        this: DomainTypedValue[DoubleType] =>

        final override def leastUpperType = Some(DoubleType)

    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // UNARY EXPRESSIONS
    //
    override def dneg(pc: Int, value: DomainValue): DomainValue = DoubleValue(pc)

    //
    // RELATIONAL OPERATORS
    //
    override def dcmpg(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        IntegerValue(pc)
    }

    override def dcmpl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        IntegerValue(pc)
    }

    //
    // BINARY EXPRESSIONS
    //
    override def dadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        DoubleValue(pc)
    }

    override def ddiv(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        DoubleValue(pc)
    }

    override def drem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        DoubleValue(pc)
    }

    override def dmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        DoubleValue(pc)
    }

    override def dsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        DoubleValue(pc)
    }

}
