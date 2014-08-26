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

import org.opalj.br.{ ComputationalType, ComputationalTypeFloat }

/**
 * This partial `Domain` performs all computations related to primitive float
 * values at the type level.
 *
 * This domain can be used as a foundation to build more complex domains.
 *
 * @author Michael Eichberg
 */
trait TypeLevelFloatValues extends FloatValuesDomain {
    this: ValuesDomain with Configuration with IntegerValuesFactory ⇒

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF FLOAT VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over all values with computational type `float`.
     */
    trait FloatValue extends Value with IsFloatValue { this: DomainValue ⇒

        final override def computationalType: ComputationalType = ComputationalTypeFloat

    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // UNARY EXPRESSIONS
    //
    override def fneg(pc: PC, value: DomainValue): DomainValue = FloatValue(pc)

    //
    // RELATIONAL OPERATORS
    //
    override def fcmpg(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def fcmpl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    //
    // BINARY EXPRESSIONS
    //
    override def fadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        FloatValue(pc)

    override def fdiv(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        FloatValue(pc)

    override def fmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        FloatValue(pc)

    override def frem(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        FloatValue(pc)

    override def fsub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        FloatValue(pc)

}

