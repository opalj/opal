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
 */
trait LongValues extends l0.TypeLevelLongValues with ConcreteLongValues {
    domain: IntegerValuesFactory with VMLevelExceptionsFactory with Configuration ⇒

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF LONG VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Represents an unknown long value.
     */
    trait ALongValue extends LongValue { this: DomainValue ⇒
    }

    /**
     * Represents some `long` which has the specified value.
     */
    trait ConcreteLongValue extends LongValue { this: DomainValue ⇒
        val value: Long
    }

    def withLongValuesOrElse[T](
        value1: DomainValue, value2: DomainValue)(
            ifThen: (Long, Long) ⇒ T)(orElse: ⇒ T): T =
        value1 match {
            case v1: ConcreteLongValue ⇒
                value2 match {
                    case v2: ConcreteLongValue ⇒ ifThen(v1.value, v2.value)
                    case _                     ⇒ orElse
                }
            case _ ⇒
                orElse
        }

    override def longValue[T](value: DomainValue)(ifThen: Long ⇒ T)(orElse: ⇒ T): T =
        value match {
            case v: ConcreteLongValue ⇒ ifThen(v.value)
            case _                    ⇒ orElse
        }

    override def longValueOption(value: DomainValue): Option[Long] =
        value match {
            case v: ConcreteLongValue ⇒ Some(v.value)
            case _                    ⇒ None
        }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // UNARY EXPRESSIONS
    //

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    override def lneg(pc: PC, value: DomainValue): DomainValue =
        value match {
            case v: ConcreteLongValue ⇒ LongValue(pc, -v.value)
            case _                    ⇒ LongValue(pc)
        }

    //
    // RELATIONAL OPERATORS
    //

    /**
     * @inheritdoc
     *
     * @return The result of calling `IntegerValue(pc)`.
     */
    override def lcmp(pc: PC, left: DomainValue, right: DomainValue): DomainValue =
        withLongValuesOrElse(left, right) { (l, r) ⇒
            if (l > r) IntegerValue(pc, 1)
            else if (l == r) IntegerValue(pc, 0)
            else IntegerValue(pc, -1)
        } {
            IntegerValue(pc)
        }

    //
    // BINARY EXPRESSIONS
    //

    override def ladd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        withLongValuesOrElse(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 + v2)
        } {
            LongValue(pc)
        }

    override def lsub(pc: PC, left: DomainValue, right: DomainValue): DomainValue =
        withLongValuesOrElse(left, right) { (l, r) ⇒
            LongValue(pc, l - r)
        } {
            LongValue(pc)
        }

    override def ldiv(
        pc: PC,
        left: DomainValue,
        right: DomainValue): LongValueOrArithmeticException = {
        withLongValuesOrElse(left, right) { (l, r) ⇒
            if (r == 0)
                ThrowsException(ArithmeticException(pc))
            else
                ComputedValue(LongValue(pc, l / r))
        } {
            if (throwArithmeticExceptions)
                ComputedValueOrException(LongValue(pc), ArithmeticException(pc))
            else
                ComputedValue(LongValue(pc))
        }
    }

    override def lmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        withLongValuesOrElse(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 & v2)
        } {
            LongValue(pc)
        }

    override def lrem(
        pc: PC,
        left: DomainValue,
        right: DomainValue): LongValueOrArithmeticException = {
        withLongValuesOrElse(left, right) { (l, r) ⇒
            if (r == 0)
                ThrowsException(ArithmeticException(pc))
            else
                ComputedValue(LongValue(pc, l % r))
        } {
            if (throwArithmeticExceptions)
                ComputedValueOrException(LongValue(pc), ArithmeticException(pc))
            else
                ComputedValue(LongValue(pc))
        }
    }

    override def land(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        withLongValuesOrElse(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 & v2)
        } {
            LongValue(pc)
        }

    override def lor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        withLongValuesOrElse(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 | v2)
        } {
            LongValue(pc)
        }

    override def lxor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        withLongValuesOrElse(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 ^ v2)
        } {
            LongValue(pc)
        }
}

