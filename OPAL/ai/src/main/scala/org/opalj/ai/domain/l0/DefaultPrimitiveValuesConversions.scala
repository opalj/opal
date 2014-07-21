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

/**
 * @author Michael Eichberg
 */
trait DefaultPrimitiveValuesConversions extends PrimitiveValuesConversionsDomain {
    this: CoreDomain with PrimitiveValuesFactory ⇒

    override def d2f(pc: PC, value: DomainValue): DomainValue = FloatValue(pc)
    override def d2i(pc: PC, value: DomainValue): DomainValue = IntegerValue(pc)
    override def d2l(pc: PC, value: DomainValue): DomainValue = LongValue(pc)

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

    override def i2d(pc: PC, value: DomainValue): DomainValue = DoubleValue(pc)
    override def i2f(pc: PC, value: DomainValue): DomainValue = FloatValue(pc)
    override def i2l(pc: PC, value: DomainValue): DomainValue = LongValue(pc)

    override def f2d(pc: PC, value: DomainValue): DomainValue = DoubleValue(pc)
    override def f2i(pc: PC, value: DomainValue): DomainValue = IntegerValue(pc)
    override def f2l(pc: PC, value: DomainValue): DomainValue = LongValue(pc)
}



