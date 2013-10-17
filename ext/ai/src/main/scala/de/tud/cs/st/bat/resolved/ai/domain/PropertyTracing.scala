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
 * Representation of some arbitrary property that should be tracked during
 * the abstract interpretation of the method.
 *
 * @author Michael Eichberg
 */
trait PropertyTracing[+I] extends Domain[I] { domain ⇒

    trait Property {
        def merge(otherProperty: DomainProperty): Update[DomainProperty]
    }

    type DomainProperty <: Property

    def initialPropertyValue(): DomainProperty

    implicit val DomainPropertyTag: reflect.ClassTag[DomainProperty]

    /**
     * The array which stores the value the property has when the respective.
     * Instruction is executed. As in case of BATAI
     */
    protected var propertiesArray: Array[DomainProperty] = _

    def initProperties(
        code: Code,
        operandsArray: List[this.type#DomainValue],
        localsArray: Array[this.type#DomainValue]) = {

        this.propertiesArray = new Array(code.instructions.size)
        this.propertiesArray(0) = initialPropertyValue()
    }

    def getProperty(pc: Int): DomainProperty = propertiesArray(pc)
    
    override def properties(pc: Int): Option[String] =
        Option(propertiesArray(pc)).map(_.toString())

    override def flow(currentPC: Int, successorPC: Int): Boolean = {
        if (propertiesArray(successorPC) eq null) {
            propertiesArray(successorPC) = propertiesArray(currentPC)
            true
        } else {
            propertiesArray(successorPC) merge propertiesArray(currentPC) match {
                case NoUpdate ⇒ false
                case StructuralUpdate(property) ⇒
                    propertiesArray(successorPC) = property
                    true
                case MetaInformationUpdate(property) ⇒
                    propertiesArray(successorPC) = property
                    false
            }
        }
    }
}
