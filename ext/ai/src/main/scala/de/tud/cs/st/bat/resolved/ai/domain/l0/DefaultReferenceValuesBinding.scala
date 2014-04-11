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
package l0

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }
import de.tud.cs.st.collection.immutable.UIDSet

/**
 * Default implementation for handling reference values.
 *
 * @author Michael Eichberg
 */
trait DefaultReferenceValuesBinding[+I] extends DefaultTypeLevelReferenceValues[I] {
    domain: Configuration with IntegerValuesComparison with ClassHierarchy ⇒

    type DomainReferenceValue = ReferenceValue
    type DomainNullValue = NullValue
    type DomainObjectValue = ObjectValue
    type DomainArrayValue = ArrayValue

    //
    // FACTORY METHODS
    //

    protected[this] val TheNullValue: DomainNullValue = new NullValue()

    /**
     * @inheritdoc
     * This implementation always returns the singleton instance `TheNullValue`.
     */
    override def NullValue(pc: PC): DomainNullValue = TheNullValue

    override protected[domain] def ObjectValue(pc: PC, objectType: ObjectType): DomainObjectValue =
        new SObjectValue(objectType)

    override protected[domain] def ObjectValue(pc: PC, upperTypeBound: UIDSet[ObjectType]): DomainObjectValue = {
        assume(upperTypeBound.nonEmpty)
        if (upperTypeBound.containsOneElement)
            ObjectValue(pc, upperTypeBound.first)
        else
            new MObjectValue(upperTypeBound)
    }

    override protected[domain] def ArrayValue(pc: PC, arrayType: ArrayType): DomainArrayValue =
        new ArrayValue(arrayType)

}
