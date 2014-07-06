/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package ai
package domain

import br._

/**
 * Mixin this trait to reify the stated constraints. This trait
 * need to be mixed in after all traits that actually handle constraints.
 *
 * This is particularly useful for testing and debugging purposes.
 *
 * @author Michael Eichberg
 */
trait ReifiedConstraints extends Domain {

    /**
     * (Indirectly) called by OPAL-AI for a new value-based constraint.
     */
    /*abstract*/ def nextConstraint(constraint: ReifiedConstraint)

    /**
     * Representation of a reified constraint.
     */
    trait ReifiedConstraint {

        /**
         * The pc associated with the constraint.
         */
        def pc: PC

        /**
         * A textual description of the constraint.
         */
        def constraint: String

    }

    /**
     * Representation of a constraint related to a single value.
     */
    case class ReifiedSingleValueConstraint(
        pc: PC,
        value: DomainValue,
        constraint: String) extends ReifiedConstraint

    /**
     * Representation of a constraint related to two values.
     */
    case class ReifiedTwoValuesConstraint(
        pc: PC,
        value1: DomainValue, value2: DomainValue,
        constraint: String) extends ReifiedConstraint

    abstract override def refEstablishIsNull(
        pc: PC,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        nextConstraint(ReifiedSingleValueConstraint(pc, value, "is null"))
        super.refEstablishIsNull(pc, value, operands, locals)
    }

    abstract override def refEstablishIsNonNull(
        pc: PC,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        nextConstraint(ReifiedSingleValueConstraint(pc, value, "is not null"))
        super.refEstablishIsNonNull(pc, value, operands, locals)
    }

    abstract override def refEstablishAreEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        nextConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, "equals"))
        super.refEstablishAreEqual(pc, value1, value2, operands, locals)
    }

    abstract override def refEstablishAreNotEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        nextConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, "is not equal to"))
        super.refEstablishAreNotEqual(pc, value1, value2, operands, locals)
    }

    abstract override def refEstablishUpperBound(
        pc: PC,
        bound: ReferenceType,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        nextConstraint(
            ReifiedSingleValueConstraint(pc, operands.head, "is subtype of "+bound.toJava)
        )
        super.refEstablishUpperBound(pc, bound, operands, locals)
    }

    //
    // W.r.t. Integer values

    abstract override def intEstablishValue(
        pc: PC,
        theValue: Int,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        nextConstraint(ReifiedSingleValueConstraint(pc, value, "is "+theValue))
        super.intEstablishValue(pc, theValue, value, operands, locals)
    }

    abstract override def intEstablishAreEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        nextConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, " == "))
        super.intEstablishAreEqual(pc, value1, value2, operands, locals)
    }

    abstract override def intEstablishAreNotEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        nextConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, " != "))
        super.intEstablishAreNotEqual(pc, value1, value2, operands, locals)
    }

    abstract override def intEstablishIsLessThan(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        nextConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, " < "))
        super.intEstablishIsLessThan(pc, value1, value2, operands, locals)
    }

    abstract override def intEstablishIsLessThanOrEqualTo(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        nextConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, " <= "))
        super.intEstablishIsLessThanOrEqualTo(pc, value1, value2, operands, locals)
    }
}




