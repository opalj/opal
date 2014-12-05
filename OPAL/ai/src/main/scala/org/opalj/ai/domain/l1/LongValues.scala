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
package l1

import org.opalj.ai.Domain
import org.opalj.ai.IsLongValue
import org.opalj.ai.domain.Configuration
import org.opalj.br.ComputationalType
import org.opalj.br.ComputationalTypeLong

/**
 * Foundation for domains that trace long values. This domain can directly be used
 * to trace simple computations related to long values.
 *
 * @author Michael Eichberg
 * @author Riadh Chtara
 * @author David Becker
 */
trait LongValues extends LongValuesDomain with ConcreteLongValues {
    domain: IntegerValuesFactory with VMLevelExceptionsFactory with Configuration ⇒

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF LONG VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over all values with computational type `long`.
     */
    sealed trait LongValue extends Value with IsLongValue { this: DomainValue ⇒

        final def computationalType: ComputationalType = ComputationalTypeLong

        def newInstance: DomainValue
    }

    /**
     * Represents an (unknown) long value.
     *
     * Models the top value of this domain's lattice.
     */
    trait ALongValue extends LongValue { this: DomainValue ⇒ }

    /**
     * Represents one concrete long value.
     */
    trait DefiniteLongValue extends LongValue { this: DomainValue ⇒
        val value: Long
    }

    object DefiniteLongValue {
        def unapply(v: DefiniteLongValue): Option[Long] =
            Some(v.value)
    }

    // -----------------------------------------------------------------------------------
    //
    // COMPUTATIONS RELATED TO LONG VALUES
    //
    // -----------------------------------------------------------------------------------

    //
    // QUESTIONS ABOUT VALUES
    //

    @inline final override def longValue[T](
        value: DomainValue)(
            f: Long ⇒ T)(orElse: ⇒ T): T =
        value match {
            case v: DefiniteLongValue ⇒ f(v.value)
            case _                    ⇒ orElse
        }

    @inline final override def longValueOption(value: DomainValue): Option[Long] =
        value match {
            case v: DefiniteLongValue ⇒ Some(v.value)
            case _                    ⇒ None
        }

    @inline protected final def longValues[T](
        value1: DomainValue, value2: DomainValue)(
            f: (Long, Long) ⇒ T)(
                orElse: ⇒ T): T = {
        longValue(value1) { v1 ⇒
            longValue(value2) { v2 ⇒ f(v1, v2) } { orElse }
        } {
            orElse
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // UNARY EXPRESSIONS
    //

    override def lneg(pc: PC, value: DomainValue): DomainValue = value match {
        case v: DefiniteLongValue ⇒ LongValue(pc, -v.value)
        case _                    ⇒ LongValue(vo = pc)
    }

    //
    // RELATIONAL OPERATORS
    //

    override def lcmp(pc: PC, left: DomainValue, right: DomainValue): DomainValue = {
        (left, right) match {
            case (DefiniteLongValue(l), DefiniteLongValue(r)) ⇒
                if (l > r)
                    IntegerValue(pc, 1)
                else if (l == r)
                    IntegerValue(pc, 0)
                else
                    IntegerValue(pc, -1)
            case _ ⇒
                IntegerValue(vo = pc)
        }
    }

    //
    // BINARY EXPRESSIONS
    //

    override def ladd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (DefiniteLongValue(l), DefiniteLongValue(r)) ⇒ LongValue(pc, l + r)
            case _ ⇒ LongValue(vo = pc)
        }
    }

    override def lsub(pc: PC, left: DomainValue, right: DomainValue): DomainValue = {
        (left, right) match {
            case (DefiniteLongValue(l), DefiniteLongValue(r)) ⇒ LongValue(pc, l - r)
            case _ ⇒ LongValue(vo = pc)
        }
    }

    override def lmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (_, DefiniteLongValue(0)) ⇒ value2
            case (_, DefiniteLongValue(1)) ⇒ value1
            case (DefiniteLongValue(0), _) ⇒ value1
            case (DefiniteLongValue(1), _) ⇒ value2

            case (DefiniteLongValue(l), DefiniteLongValue(r)) ⇒
                LongValue(pc, l * r)

            case _ ⇒ LongValue(vo = pc)
        }
    }

    override def ldiv(
        pc: PC,
        numerator: DomainValue,
        denominator: DomainValue): LongValueOrArithmeticException = {

        (numerator, denominator) match {
            case (_, DefiniteLongValue(v)) ⇒
                if (v == 0)
                    ThrowsException(ArithmeticException(pc))
                else
                    ComputedValue(LongValue(vo = pc))
            case (DefiniteLongValue(n), DefiniteLongValue(d)) ⇒ ComputedValue(LongValue(pc, n / d))
            case _ ⇒
                if (throwArithmeticExceptions)
                    ComputedValueOrException(LongValue(vo = pc), ArithmeticException(pc))
                else
                    ComputedValue(LongValue(vo = pc))
        }
    }

    override def lrem(
        pc: PC,
        left: DomainValue,
        right: DomainValue): LongValueOrArithmeticException = {
        (left, right) match {
            case (_, DefiniteLongValue(v)) ⇒
                if (v == 0)
                    ThrowsException(ArithmeticException(pc))
                else
                    ComputedValue(LongValue(vo = pc))
            case (DefiniteLongValue(n), DefiniteLongValue(d)) ⇒ ComputedValue(LongValue(pc, n % d))
            case _ ⇒
                if (throwArithmeticExceptions)
                    ComputedValueOrException(LongValue(vo = pc), ArithmeticException(pc))
                else
                    ComputedValue(LongValue(vo = pc))
        }
    }

    override def land(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (_, DefiniteLongValue(-1)) ⇒ value1
            case (_, DefiniteLongValue(0))  ⇒ value2
            case (DefiniteLongValue(-1), _) ⇒ value2
            case (DefiniteLongValue(0), _)  ⇒ value1

            case (DefiniteLongValue(l), DefiniteLongValue(r)) ⇒
                LongValue(pc, l & r)

            case _ ⇒ LongValue(vo = pc)
        }
    }

    override def lor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (_, DefiniteLongValue(-1)) ⇒ value2
            case (_, DefiniteLongValue(0))  ⇒ value1
            case (DefiniteLongValue(-1), _) ⇒ value1
            case (DefiniteLongValue(0), _)  ⇒ value2

            case (DefiniteLongValue(l), DefiniteLongValue(r)) ⇒
                LongValue(pc, l | r)

            case _ ⇒ LongValue(vo = pc)
        }
    }

    override def lxor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (DefiniteLongValue(l), DefiniteLongValue(r)) ⇒ LongValue(pc, l ^ r)
            case _ ⇒ LongValue(vo = pc)
        }
    }
}

