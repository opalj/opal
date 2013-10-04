/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package ai
package domain

/**
 * Mixin this trait if you want to reify the stated constraints. This is particularly
 * useful for testing and debugging purposes.
 *
 * @todo Currently we only refine Is(Non)NullConstraints and nothing else...
 * @author Michael Eichberg
 */
trait ReifiedConstraints[+I] extends Domain[I] {

    /*abstract*/ def addConstraint(constraint: ReifiedConstraint)

    trait ReifiedConstraint {

        def pc: Int

        def constraint: String

    }

    case class ReifiedSingleValueConstraint(
        pc: Int,
        value: DomainValue,
        constraint: String) extends ReifiedConstraint

    case class ReifiedTwoValuesConstraint(
        pc: Int,
        value1: DomainValue, value2: DomainValue,
        constraint: String) extends ReifiedConstraint

    abstract override def establishIsNull(
        pc: Int,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        addConstraint(ReifiedSingleValueConstraint(pc, value, "is null"))
        super.establishIsNull(pc, value, operands, locals)
    }

    abstract override def establishIsNonNull(
        pc: Int,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        addConstraint(ReifiedSingleValueConstraint(pc, value, "is not null"))
        super.establishIsNonNull(pc, value, operands, locals)
    }

    abstract override def establishAreEqualReferences(
        pc: Int,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        addConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, "equals"))
        super.establishAreEqualReferences(pc, value1, value2, operands, locals)
    }

    abstract override def establishAreNotEqualReferences(
        pc: Int,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        addConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, "is not equal to"))
        super.establishAreNotEqualReferences(pc, value1, value2, operands, locals)
    }

    abstract override def establishUpperBound(
        pc: Int,
        bound: ReferenceType,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        addConstraint(ReifiedSingleValueConstraint(pc, value, "is subtype of "+bound.toJava))
        super.establishUpperBound(pc, bound, value, operands, locals)
    }

    //
    // W.r.t. Integer values

    abstract override def establishValue(
        pc: Int,
        theValue: Int,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        addConstraint(ReifiedSingleValueConstraint(pc, value, "is "+theValue))
        super.establishValue(pc, theValue, value, operands, locals)
    }

    abstract override def establishAreEqual(
        pc: Int,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        addConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, " == "))
        super.establishAreEqual(pc, value1, value2, operands, locals)
    }

    abstract override def establishAreNotEqual(
        pc: Int,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        addConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, " != "))
        super.establishAreNotEqual(pc, value1, value2, operands, locals)
    }

    abstract override def establishIsLessThan(
        pc: Int,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        addConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, " < "))
        super.establishIsLessThan(pc, value1, value2, operands, locals)
    }

    abstract override def establishIsLessThanOrEqualTo(
        pc: Int,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        addConstraint(ReifiedTwoValuesConstraint(pc, value1, value2, " <= "))
        super.establishIsLessThanOrEqualTo(pc, value1, value2, operands, locals)
    }
}

trait RecordConstraints[I] extends ReifiedConstraints[I] {

    var constraints: Set[ReifiedConstraint] = Set.empty

    def addConstraint(constraint: ReifiedConstraint) {
        constraints += constraint
    }
}



