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
package tracing

import org.opalj.util.{ Answer, Yes, No, Unknown }

import br._
import br.instructions.ReturnInstruction

/**
 * Enables the tracing of a single boolean property where the precise semantics
 * is determined by the user.
 *
 * @author Michael Eichberg
 */
trait SimpleBooleanPropertyTracing
        extends PropertyTracing
        with RecordReturnFromMethodInstructions { domain: Domain with TheCode ⇒

    /**
     * A name associated with the property. Used for debugging purposes only.
     */
    def propertyName: String

    class BooleanProperty private[SimpleBooleanPropertyTracing] (
        val state: Boolean)
            extends Property {

        def join(otherProperty: DomainProperty): Update[DomainProperty] =
            this.state & otherProperty.state match {
                case `state`  ⇒ NoUpdate
                case newState ⇒ StructuralUpdate(new BooleanProperty(newState))
            }

        override def toString: String = domain.propertyName+"("+state+")"
    }

    def updateProperty(pc: Int, newState: Boolean) {
        setProperty(pc, new BooleanProperty(newState))
    }

    final type DomainProperty = BooleanProperty

    final val DomainPropertyTag: reflect.ClassTag[DomainProperty] = implicitly

    def initialPropertyValue: DomainProperty = new BooleanProperty(false)

    def hasPropertyOnExit: Boolean = {
        allReturnFromMethodInstructions forall { pc ⇒ getProperty(pc).state }
    }

    def hasPropertyOnNormalReturn: Boolean = {
        allReturnFromMethodInstructions forall { pc ⇒
            !code.instructions(pc).isInstanceOf[ReturnInstruction] || getProperty(pc).state
        }
    }
}
