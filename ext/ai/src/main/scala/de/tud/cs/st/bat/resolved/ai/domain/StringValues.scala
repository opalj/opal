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
package de.tud.cs.st
package bat
package resolved
package ai
package domain

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

/**
 * This domain enables the tracing of string values and can, e.g., be used to
 * resolve "class.forName" calls.
 */
trait StringValues[I] extends DefaultTypeLevelReferenceValues[I] {

    class AStringValue(
        pc: Int, // sets the pc value of the superclass
        val value: String)
            extends AReferenceValue(pc, Set(ObjectType.String), No, true) {

        assume(value != null)

        override def adapt(domain: Domain[_ >: I]): domain.DomainValue =
            domain match {
                case d: StringValues[I] ⇒
                    // "this" value does not have a dependency on this domain instance  
                    this.asInstanceOf[domain.DomainValue]
                case _ ⇒ super.adapt(domain)
            }

        override def equals(other: Any): Boolean = {
            super.equals(other) &&
                other.asInstanceOf[AStringValue].value == this.value
        }

        override protected def canEqual(other: AReferenceValue): Boolean =
            other.isInstanceOf[AStringValue]

        override def hashCode: Int = super.hashCode + 41 * value.hashCode()

        override def toString(): String = "String(pc="+pc+", value=\""+value+"\")"

    }

    override def newStringValue(pc: Int, value: String): DomainValue =
        new AStringValue(pc, value)

}


