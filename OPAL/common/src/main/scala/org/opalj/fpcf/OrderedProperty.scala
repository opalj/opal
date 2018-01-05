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
package org.opalj.fpcf

/**
 * Ordered properties makes the order between all properties regarding a respective kind explicit;
 * all properties that are of the same kind have to inherit from ordered property or none.
 *
 * This information is used by the property store when debugging is turned on to test if an
 * analysis which derives a new property always derives a more precise property. These tests
 * are only executed in-phase!
 *
 * @author Michael Eichberg
 */
trait OrderedProperty extends Property {

    /**
     * Returns `true`.
     */
    final override private[fpcf] def isOrdered: Boolean = true

    /**
     * Returns `this`.
     */
    final override private[fpcf] def asOrderedProperty: this.type = this

    /**
     * Tests if this property is a valid successor property of the other property; this
     * relation is typically reflexive, that is, a property is a valid success of itself.
     *
     * @return None if this property is a valid successor of the other property else
     *         `Some(description:String)` which describes the problem is returned.
     */
    def isValidSuccessorOf(other: OrderedProperty): Option[String]

}
