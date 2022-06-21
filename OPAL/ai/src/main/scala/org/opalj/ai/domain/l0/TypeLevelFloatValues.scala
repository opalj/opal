/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import org.opalj.value.IsFloatValue
import org.opalj.br.FloatType

/**
 * This partial `Domain` performs all computations related to primitive float
 * values at the type level.
 *
 * This domain can be used as a foundation to build more complex domains.
 *
 * @author Michael Eichberg
 * @author David Becker
 */
trait TypeLevelFloatValues extends FloatValuesDomain {
    domain: IntegerValuesFactory =>

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF FLOAT VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over all values with computational type `float`.
     */
    trait FloatValue extends TypedValue[FloatType] with IsFloatValue {
        this: DomainTypedValue[FloatType] =>

        final override def leastUpperType: Option[FloatType] = Some(FloatType)

    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // UNARY EXPRESSIONS
    //
    override def fneg(pc: Int, value: DomainValue): DomainValue = FloatValue(pc)

    //
    // RELATIONAL OPERATORS
    //
    override def fcmpg(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        IntegerValue(pc)
    }

    override def fcmpl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        IntegerValue(pc)
    }

    //
    // BINARY EXPRESSIONS
    //
    override def fadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        FloatValue(pc)
    }

    override def fdiv(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        FloatValue(pc)
    }

    override def fmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        FloatValue(pc)
    }

    override def frem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        FloatValue(pc)
    }

    override def fsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        FloatValue(pc)
    }

}
