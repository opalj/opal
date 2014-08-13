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

import org.opalj.ai.domain.Configuration
import org.opalj.ai.domain.DefaultDomainValueBinding

import org.opalj.br.ComputationalTypeInt

/**
 * Base implementation of the `TypeLevelIntegerValues` trait that requires that
 * the domain's Value` trait is not extended. This implementation satisfies
 * the requirements of OPAL w.r.t. the domain's computational type. Additionally,
 * it collects information about a value's range, if possible.
 *
 * This domain is highly efficient as it uses a single value domain value to represents
 * all values of the same primitive type.
 *
 * =Adaptation/Reusability=
 * This domain '''does not support constraint propagation''' – due to its reuse of the
 * the same instance of a DomainValue across all potential instantiations of such values –
 * and should not be used to implement such a domain as this requires the
 * reimplementation of basically '''all''' methods.
 *
 * @author Michael Eichberg
 */
trait DefaultTypeLevelIntegerValues
        extends DefaultDomainValueBinding
        with TypeLevelIntegerValues {
    this: Configuration ⇒

    //
    // IMPLEMENTATION NOTE
    //
    // It is safe to use singleton objects in this case since we do not propagate 
    // constraints.
    // I.e., all constraints that are stated by the AI (e.g., `intHasValue`) are
    // completely ignored.
    //

    case object ABooleanValue extends super.BooleanValue {

        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] =
            value match {
                case ABooleanValue ⇒ NoUpdate
                case _             ⇒ StructuralUpdate(AnIntegerValue)
            }

        override def abstractsOver(other: DomainValue): Boolean = other eq this

    }

    case object AByteValue extends super.ByteValue {

        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] =
            value match {
                case ABooleanValue | AByteValue ⇒ NoUpdate
                case _                          ⇒ StructuralUpdate(AnIntegerValue)
            }

        override def abstractsOver(other: DomainValue): Boolean =
            (other eq this) || (other eq ABooleanValue)

    }

    case object AShortValue extends super.ShortValue {

        override def doJoin(pc: PC, that: DomainValue): Update[DomainValue] =
            that match {
                case ABooleanValue | AByteValue | AShortValue ⇒
                    NoUpdate
                case _ ⇒
                    StructuralUpdate(AnIntegerValue)
            }

        override def abstractsOver(other: DomainValue): Boolean =
            (other eq this) || (other eq ABooleanValue) || (other eq AByteValue)
    }

    case object ACharValue extends super.CharValue {

        override def doJoin(pc: PC, that: DomainValue): Update[DomainValue] =
            that match {
                case ABooleanValue | ACharValue ⇒ NoUpdate
                case _                          ⇒ StructuralUpdate(AnIntegerValue)
            }

        override def abstractsOver(other: DomainValue): Boolean =
            (other eq this) || (other eq ABooleanValue)
    }

    case object AnIntegerValue extends super.IntegerValue {

        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] =
            // the other value also has computational type Int
            NoUpdate

        override def abstractsOver(other: DomainValue): Boolean =
            (other ne TheIllegalValue) &&
                other.computationalType == ComputationalTypeInt
    }

    override def BooleanValue(valueOrigin: ValueOrigin): BooleanValue = ABooleanValue
    override def BooleanValue(valueOrigin: ValueOrigin, value: Boolean): BooleanValue = ABooleanValue

    override def ByteValue(valueOrigin: ValueOrigin): ByteValue = AByteValue
    override def ByteValue(valueOrigin: ValueOrigin, value: Byte): ByteValue = AByteValue

    override def ShortValue(valueOrigin: ValueOrigin): ShortValue = AShortValue
    override def ShortValue(valueOrigin: ValueOrigin, value: Short): ShortValue = AShortValue

    override def CharValue(valueOrigin: ValueOrigin): CharValue = ACharValue
    override def CharValue(valueOrigin: ValueOrigin, value: Char): CharValue = ACharValue

    override def IntegerValue(valueOrigin: ValueOrigin): IntegerValue = AnIntegerValue
    override def IntegerValue(valueOrigin: ValueOrigin, value: Int): IntegerValue = AnIntegerValue
}

