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

import org.opalj.br.ObjectType
import scala.reflect.ClassTag

/**
 * Enables the tracing of concrete string values and can, e.g., be used to
 * resolve static "class.forName(...)" calls.
 *
 * @author Michael Eichberg
 */
trait StringValues extends ReferenceValues with JavaObjectConversion {
    domain: CorrelationalDomainSupport with IntegerValuesDomain with TypedValuesFactory with Configuration with ClassHierarchy ⇒

    type DomainStringValue <: StringValue with DomainObjectValue
    val DomainStringValue: ClassTag[DomainStringValue]

    protected class StringValue(
        origin: ValueOrigin,
        val value: String,
        t: Timestamp)
            extends SObjectValue(origin, No, true, ObjectType.String, t) {
        this: DomainStringValue ⇒

        require(value != null)

        override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: PC,
            other: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue] = {

            other match {
                case that: StringValue ⇒
                    if (this.value == that.value) {
                        if (this.t == that.t)
                            NoUpdate
                        else
                            MetaInformationUpdate(StringValue(origin, value, t))
                    } else {
                        // we have to drop the concrete information...
                        // given that the values are different the timestamp must
                        // be different too
                        val newValue = ObjectValue(origin, No, true, ObjectType.String, nextT())
                        StructuralUpdate(newValue)
                    }
                case _ ⇒
                    val result = super.doJoinWithNonNullValueWithSameOrigin(joinPC, other)
                    if (result.isStructuralUpdate) {
                        result
                    } else {
                        // This (string) value and the other value may have a corresponding
                        // abstract representation (w.r.t. the next abstraction level!)
                        // but we still need to drop the concrete information.
                        StructuralUpdate(result.value.update())
                    }
            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            if (this eq other)
                return true

            other match {
                case that: StringValue ⇒ that.value == this.value
                case _                 ⇒ false
            }
        }

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue =
            // The following method is provided by `CoreDomain` and, hence,
            // all possible target domains are automatically supported.
            target.StringValue(vo, this.value)

        override def equals(other: Any): Boolean = {
            other match {
                case sv: StringValue ⇒ sv.value == this.value && super.equals(other)
                case _               ⇒ false
            }
        }

        override protected def canEqual(other: SObjectValue): Boolean =
            other.isInstanceOf[StringValue]

        override def hashCode: Int = super.hashCode * 41 + value.hashCode()

        override def toString(): String =
            s"""String(origin=$origin;value="$value";t=$t)"""

    }

    object StringValue {
        def unapply(value: StringValue): Option[String] = Some(value.value)
    }

    abstract override def toJavaObject(pc: PC, value: DomainValue): Option[Object] = {
        value match {
            case StringValue(value) ⇒ Some(value)
            case _                  ⇒ super.toJavaObject(pc, value)
        }
    }

    abstract override def toDomainValue(pc: PC, value: Object): DomainReferenceValue = {
        value match {
            case s: String ⇒ StringValue(pc, s)
            case _         ⇒ super.toDomainValue(pc, value)
        }
    }

    // Needs to be implemented (the default implementation is now longer sufficient!)
    override def StringValue(origin: ValueOrigin, value: String): DomainObjectValue

    // todo use the same t for all constant JVM strings 
    def StringValue(origin: ValueOrigin, value: String, t: Timestamp): DomainStringValue
}

