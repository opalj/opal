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
package l0

import org.opalj.ai.Domain
import org.opalj.ai.IsLongValue
import org.opalj.ai.domain.Configuration
import org.opalj.br.ComputationalType
import org.opalj.br.ComputationalTypeLong

/**
 * This partial `Domain` performs all computations related to primitive long
 * values at the type level.
 *
 * This domain can be used as a foundation for building more complex domains.
 *
 * @author Michael Eichberg
 */
trait TypeLevelLongValues extends Domain { this: Configuration ⇒

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF LONG VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Common supertrait of all `DomainValue`s that represent long values.
     */
    trait LongValue extends Value with IsLongValue { this: DomainValue ⇒

        final override def computationalType: ComputationalType = ComputationalTypeLong

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
    override def lneg(pc: PC, value: DomainValue): DomainValue = LongValue(pc)

    //
    // RELATIONAL OPERATORS
    //

    /**
     * @inheritdoc
     *
     * @return The result of calling `IntegerValue(pc)`.
     */
    override def lcmp(pc: PC, left: DomainValue, right: DomainValue): DomainValue =
        IntegerValue(pc)

    //
    // BINARY EXPRESSIONS
    //

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    override def ladd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        LongValue(pc)

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    override def land(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        LongValue(pc)

    /**
     * @inheritdoc
     *
     * @return Either `ComputedValue(LongValue(pc))` if arithmetic exceptions should
     *      not be thrown if nothing is known about the precise value or – if the
     *      policy is to throw an ArithmeticException if in doubt – a
     *      `ComputedValueOrException(LongValue(pc), ArithmeticException(pc))`
     */
    override def ldiv(
        pc: PC,
        left: DomainValue,
        right: DomainValue): LongValueOrArithmeticException = {
        if (throwArithmeticExceptions)
            ComputedValueOrException(LongValue(pc), ArithmeticException(pc))
        else
            ComputedValue(LongValue(pc))
    }

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    override def lmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        LongValue(pc)

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    override def lor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        LongValue(pc)

    /**
     * @inheritdoc
     *
     * @return Either `ComputedValue(LongValue(pc))` if arithmetic exceptions should
     *      not be thrown if nothing is known about the precise value or – if the
     *      policy is to throw an ArithmeticException if in doubt – a
     *      `ComputedValueOrException(LongValue(pc), ArithmeticException(pc))`
     */
    override def lrem(
        pc: PC,
        left: DomainValue,
        right: DomainValue): LongValueOrArithmeticException = {
        if (throwArithmeticExceptions)
            ComputedValueOrException(LongValue(pc), ArithmeticException(pc))
        else
            ComputedValue(LongValue(pc))
    }

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    override def lshl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        LongValue(pc)

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    override def lshr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        LongValue(pc)

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    override def lsub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        LongValue(pc)

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    override def lushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        LongValue(pc)

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    override def lxor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        LongValue(pc)

    //
    // CONVERSION INSTRUCTIONS
    //

    /**
     * @inheritdoc
     *
     * @return The result of calling `DoubleValue(pc)`.
     */
    override def l2d(pc: PC, value: DomainValue): DomainValue = DoubleValue(pc)

    /**
     * @inheritdoc
     *
     * @return The result of calling `FloatValue(pc)`.
     */
    override def l2f(pc: PC, value: DomainValue): DomainValue = FloatValue(pc)

    /**
     * @inheritdoc
     *
     * @return The result of calling `IntegerValue(pc)`.
     */
    override def l2i(pc: PC, value: DomainValue): DomainValue = IntegerValue(pc)
}



