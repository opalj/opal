/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

import org.opalj.br.ComputationalTypeInt

/**
 * This domain implements the tracking of simple integer values.
 *
 * @see [[IntegerValues]] for more details.
 * @note   This domain uses a single object to represent some integer. I.e., this
 *         domain does not support the identification of values that may be equal.
 * @author Michael Eichberg
 */
trait DefaultIntegerValues extends DefaultDomainValueBinding with IntegerValues {
    domain: Configuration with ExceptionsFactory ⇒

    /**
     * Represents an unspecific, unknown Integer value.
     *
     * Two instances of
     */
    object AnIntegerValue extends super.AnIntegerValue {

        override def doJoin(pc: Int, value: DomainValue): Update[DomainValue] = NoUpdate

        override def abstractsOver(other: DomainValue): Boolean = {
            other.computationalType == ComputationalTypeInt
        }

        override def summarize(pc: Int): DomainValue = this

        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            target.IntegerValue(origin = pc)
        }

        override def hashCode: Int = 101

        override def equals(other: Any): Boolean = {
            other match {
                case that: AnyRef ⇒ that eq this
                case _            ⇒ false
            }
        }

        override def toString: String = "an int"
    }

    /**
     * Factory method to create a new instance of [[AnIntegerValue]].
     */
    //def AnIntegerValue(): AnIntegerValue.type = AnIntegerValue

    /**
     * Represents a specific integer value in the range [`lowerBound`,`upperBound`].
     */
    class TheIntegerValue(val value: Int) extends super.TheIntegerValue {

        override def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            other match {
                case AnIntegerValue ⇒ StructuralUpdate(AnIntegerValue)
                case that: TheIntegerValue ⇒
                    if (that.value == this.value)
                        NoUpdate
                    else
                        StructuralUpdate(AnIntegerValue)
            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            (this eq other) || (
                other match {
                    case that: TheIntegerValue ⇒ this.value == that.value
                    case _                     ⇒ false
                }
            )
        }

        override def summarize(pc: Int): DomainValue = this

        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            target.IntegerValue(pc, value)
        }

        override def hashCode = this.value * 13 + 11

        override def equals(other: Any): Boolean = {
            other match {
                case that: TheIntegerValue ⇒ this.value == that.value
                case _                     ⇒ false
            }
        }

        override def toString: String = "int = "+value

    }

    override def BooleanValue(origin: ValueOrigin): AnIntegerValue.type = AnIntegerValue
    override def BooleanValue(origin: ValueOrigin, value: Boolean): TheIntegerValue = {
        if (value) new TheIntegerValue(1) else new TheIntegerValue(0)
    }

    override def ByteValue(origin: ValueOrigin): AnIntegerValue.type = AnIntegerValue
    override def ByteValue(origin: ValueOrigin, value: Byte): TheIntegerValue = {
        new TheIntegerValue(value.toInt)
    }

    override def ShortValue(origin: ValueOrigin): AnIntegerValue.type = AnIntegerValue
    override def ShortValue(origin: ValueOrigin, value: Short): TheIntegerValue = {
        new TheIntegerValue(value.toInt)
    }

    override def CharValue(origin: ValueOrigin): AnIntegerValue.type = AnIntegerValue
    override def CharValue(origin: ValueOrigin, value: Char): TheIntegerValue = {
        new TheIntegerValue(value.toInt)
    }

    override def IntegerValue(origin: ValueOrigin): AnIntegerValue.type = AnIntegerValue
    override def IntegerValue(origin: ValueOrigin, value: Int): TheIntegerValue = {
        new TheIntegerValue(value)
    }

}
