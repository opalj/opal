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
package de.tud.cs.st
package bat
package resolved
package ai
package domain
package l1

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

/**
 * Enables the tracing of concrete string values and can, e.g., be used to
 * resolve static "class.forName(...)" calls.
 *
 * @author Michael Eichberg
 */
trait StringValues[+I] extends ReferenceValues[I] {
    domain: Configuration with IntegerValuesComparison with ClassHierarchy ⇒

    type DomainStringValue <: StringValue with DomainObjectValue

    class StringValue(
        pc: PC, // sets the pc value of the superclass
        val value: String)
            extends SObjectValue(pc, No, true, ObjectType.String) {
        this: DomainObjectValue ⇒

        override def adapt[TDI >: I](target: Domain[TDI], pc: Int): target.DomainValue =
            target.StringValue(pc, this.value)

        override def equals(other: Any): Boolean = {
            other match {
                case sv: DomainStringValue ⇒ super.equals(other) && sv.value == this.value
                case _                     ⇒ false
            }
        }

        override protected def canEqual(other: SObjectValue): Boolean =
            other.isInstanceOf[StringValue]

        override def hashCode: Int = super.hashCode * 41 + value.hashCode()

        override def toString(): String = "String(pc="+pc+", value=\""+value+"\")"

    }
    
    object StringValue {
        def unapply(value: StringValue): Option[String] = Some(value.value)
    }

    // Need to be implemented (the default implementation is now longer sufficient!)
    override def StringValue(pc: PC, value: String): DomainObjectValue
}


