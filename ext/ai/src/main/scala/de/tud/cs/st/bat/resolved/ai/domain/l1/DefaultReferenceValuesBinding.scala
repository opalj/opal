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
package l1

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

import scala.collection.SortedSet

/**
 * @author Michael Eichberg
 */
trait DefaultReferenceValuesBinding[+I] extends l1.ReferenceValues[I] {
    domain: Configuration with IntegerValuesComparison with ClassHierarchy ⇒

    // Let's fix the type hierarchy

    type DomainReferenceValue = ReferenceValue


    type DomainSingleOriginReferenceValue = SingleOriginReferenceValue
    type DomainNullValue = NullValue
    type DomainObjectValue = ObjectValue
    type DomainArrayValue = ArrayValue

    type DomainMultipleReferenceValues = MultipleReferenceValues

    //
    // FACTORY METHODS
    //

    override def NullValue(pc: PC): DomainNullValue =
        new NullValue(pc)

    override protected[domain] def ObjectValue(
        pc: PC,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ObjectType): DomainObjectValue =
        new SObjectValue(pc, isNull, isPrecise, theUpperTypeBound)

    override protected[domain] def ObjectValue(
        pc: PC,
        isNull: Answer,
        upperTypeBound: UIDList[ObjectType]): DomainObjectValue = {
        assume(upperTypeBound.nonEmpty)
        if (upperTypeBound.tail.isEmpty)
            ObjectValue(pc, isNull, false, upperTypeBound.head)
        else
            new MObjectValue(pc, isNull, upperTypeBound)
    }

    override protected[domain] def ArrayValue(
        pc: PC,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ArrayType): DomainArrayValue =
        new ArrayValue(pc, isNull, isPrecise, theUpperTypeBound)

    override protected[domain] def MultipleReferenceValues(
        values: scala.collection.Set[SingleOriginReferenceValue]): DomainMultipleReferenceValues =
        new MultipleReferenceValues(values)

}

