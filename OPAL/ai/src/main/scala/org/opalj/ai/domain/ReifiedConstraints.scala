/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.opalj.br.ReferenceType

/**
 * Mixin this trait to reify the stated constraints. This trait
 * need to be mixed in after all traits that actually handle constraints.
 *
 * This is particularly useful for testing and debugging purposes.
 *
 * ==Core Properties==
 *  - Needs to be stacked upon a base implementation of the domains: [[IntegerValuesDomain]]
 *    and [[ReferenceValuesDomain]]].
 *  - Collects state directly associated with the analyzed code block.
 *  - '''Not''' thread-safe.
 *  - '''Not''' reusable (I.e., a new instance needs to be created per method.)
 *
 * @author Michael Eichberg
 */
trait ReifiedConstraints extends IntegerValuesDomain with ReferenceValuesDomain {
    domain: ValuesDomain =>

    /**
     * (Indirectly) called by OPAL for a new value-based constraint.
     */
    /*abstract*/ def nextConstraint(constraint: ReifiedConstraint): Unit

    /**
     * Representation of a reified constraint.
     */
    trait ReifiedConstraint {

        /**
         * The pc associated with the constraint.
         */
        def pc: Int

        /**
         * A textual description of the constraint.
         */
        def constraint: String

    }

    /**
     * Representation of a constraint related to a single value.
     */
    case class ReifiedSingleValueConstraint(
            pc:         Int,
            value:      DomainValue,
            constraint: String
    ) extends ReifiedConstraint

    /**
     * Representation of a constraint related to two values.
     */
    case class ReifiedTwoValuesConstraint(
            pc:     Int,
            value1: DomainValue, value2: DomainValue,
            constraint: String
    ) extends ReifiedConstraint

    abstract override def refEstablishIsNull(
        pc:       Int,
        value:    DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        nextConstraint(ReifiedSingleValueConstraint(pc, value, "is null"))
        super.refEstablishIsNull(pc, value, operands, locals)
    }

    abstract override def refEstablishIsNonNull(
        pc:       Int,
        value:    DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        nextConstraint(ReifiedSingleValueConstraint(pc, value, "is not null"))
        super.refEstablishIsNonNull(pc, value, operands, locals)
    }

    abstract override def refEstablishAreEqual(
        pc:       Int,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        nextConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, "equals"))
        super.refEstablishAreEqual(pc, value1, value2, operands, locals)
    }

    abstract override def refEstablishAreNotEqual(
        pc:       Int,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        nextConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, "is not equal to"))
        super.refEstablishAreNotEqual(pc, value1, value2, operands, locals)
    }

    abstract override def refSetUpperTypeBoundOfTopOperand(
        pc:       Int,
        bound:    ReferenceType,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        nextConstraint(
            ReifiedSingleValueConstraint(pc, operands.head, "is subtype of "+bound.toJava)
        )
        super.refSetUpperTypeBoundOfTopOperand(pc, bound, operands, locals)
    }

    abstract override def refTopOperandIsNull(
        pc:       Int,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {
        nextConstraint(
            ReifiedSingleValueConstraint(pc, operands.head, "is null")
        )
        super.refTopOperandIsNull(pc, operands, locals)
    }

    //
    // W.r.t. Integer values

    abstract override def intEstablishValue(
        pc:       Int,
        theValue: Int,
        value:    DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        nextConstraint(ReifiedSingleValueConstraint(pc, value, "is "+theValue))
        super.intEstablishValue(pc, theValue, value, operands, locals)
    }

    abstract override def intEstablishAreEqual(
        pc:       Int,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        nextConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, " == "))
        super.intEstablishAreEqual(pc, value1, value2, operands, locals)
    }

    abstract override def intEstablishAreNotEqual(
        pc:       Int,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        nextConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, " != "))
        super.intEstablishAreNotEqual(pc, value1, value2, operands, locals)
    }

    abstract override def intEstablishIsLessThan(
        pc:       Int,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        nextConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, " < "))
        super.intEstablishIsLessThan(pc, value1, value2, operands, locals)
    }

    abstract override def intEstablishIsLessThanOrEqualTo(
        pc:       Int,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        nextConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, " <= "))
        super.intEstablishIsLessThanOrEqualTo(pc, value1, value2, operands, locals)
    }
}

