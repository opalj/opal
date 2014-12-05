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
package org.opalj
package ai
package domain
package l1

/**
 * This domain is able to track constant long values and to perform mathematical
 * operations related to constant long values.
 *
 * @author Michael Eichberg
 * @author Riadh Chtara
 * @author David Becker
 */
trait DefaultLongValues extends DefaultDomainValueBinding with LongValues {
    domain: IntegerValuesFactory with VMLevelExceptionsFactory with Configuration ⇒

    /**
     * Represents a specific, but unknown long value.
     */
    case object ALongValue extends super.ALongValue {

        override def doJoin(pc: PC, other: DomainValue): Update[DomainValue] = NoUpdate

        override def abstractsOver(other: DomainValue): Boolean =
            other.isInstanceOf[IsLongValue]

        override def summarize(origin: ValueOrigin): DomainValue = this

        override def adapt(target: TargetDomain, origin: ValueOrigin): target.DomainValue =
            target.LongValue(origin)

    }

    /**
     * Represents a concrete long value.
     */
    class TheLongValue(override val value: Long) extends super.TheLongValue {

        override def doJoin(pc: PC, other: DomainValue): Update[DomainValue] =
            other match {
                case ConcreteLongValue(thatValue) ⇒
                    if (this.value == thatValue) {
                        NoUpdate
                    } else {
                        StructuralUpdate(LongValue(pc))
                    }
                case _ ⇒ StructuralUpdate(other)
            }

        override def abstractsOver(other: DomainValue): Boolean =
            other match {
                case ConcreteLongValue(`value`) ⇒ true
                case _                          ⇒ false
            }

        override def summarize(origin: ValueOrigin): DomainValue = this

        override def adapt(target: TargetDomain, origin: ValueOrigin): target.DomainValue =
            target.LongValue(origin, value)

        override def equals(other: Any): Boolean = {
            other match {
                case that: TheLongValue ⇒ that.value == this.value
                case _                  ⇒ false
            }
        }

        override def hashCode: Int = (value ^ (value >>> 32)).toInt

        override def toString: String = "LongValue(value="+value+")"
    }

    //
    // FACTORY METHODS
    //

    override def LongValue(origin: ValueOrigin): DomainValue =
        ALongValue

    override def LongValue(origin: ValueOrigin, value: Long): DomainValue =
        new TheLongValue(value)

}