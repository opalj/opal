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
trait BaseReferenceValuesBinding[+I] extends l1.ReferenceValues[I] {
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

    override def NullValue(pc: PC): DomainNullValue = new NullValue(pc)

    override def NonNullReferenceValue(pc: PC, objectType: ObjectType): DomainReferenceValue =
        ObjectValue(pc, No, false, objectType)

    def ArrayValue(
        pc: PC,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ArrayType): SingleOriginReferenceValue = {
        new ArrayValue(pc, isNull, isPrecise, theUpperTypeBound)
    }

    override def ObjectValue(pc: PC, objectType: ObjectType): DomainSingleOriginReferenceValue =
        ObjectValue(pc, Unknown, false, objectType)

    def ObjectValue(
        pc: PC,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ObjectType): DomainSingleOriginReferenceValue = {
        new SObjectValue(pc, isNull, isPrecise, theUpperTypeBound)
    }

    def MultipleReferenceValues(
        values: scala.collection.Set[SingleOriginReferenceValue]): DomainMultipleReferenceValues = {
        new MultipleReferenceValues(values)
    }

    def ReferenceValue(
        pc: PC,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ReferenceType): ReferenceValue = {
        theUpperTypeBound match {
            case arrayType: ArrayType   ⇒ ArrayValue(pc, isNull, isPrecise, arrayType)
            case objectType: ObjectType ⇒ ObjectValue(pc, isNull, isPrecise, objectType)
        }
    }

    override def ReferenceValue(pc: PC, upperTypeBound: UIDList[ObjectType]): ReferenceValue = {
        assume(upperTypeBound.nonEmpty)
        if (upperTypeBound.tail.isEmpty)
            ReferenceValue(pc, upperTypeBound.head)
        else
            ReferenceValue(pc, Unknown, upperTypeBound)
    }

    def ReferenceValue(
        pc: PC,
        isNull: Answer,
        upperTypeBound: UIDList[ObjectType]): SingleOriginReferenceValue = {

        new MObjectValue(pc, isNull, upperTypeBound)
    }

    override def NewObject(pc: PC, objectType: ObjectType): ReferenceValue =
        ObjectValue(pc, No, true, objectType)

    override def InitializedObject(pc: PC, referenceType: ReferenceType): ReferenceValue =
        if (referenceType.isArrayType)
            ArrayValue(pc, No, true, referenceType.asArrayType)
        else
            ObjectValue(pc, No, true, referenceType.asObjectType)

    override def StringValue(pc: PC, value: String): DomainValue =
        ObjectValue(pc, No, true, ObjectType.String)

    override def ClassValue(pc: PC, t: Type): DomainValue =
        ObjectValue(pc, No, true, ObjectType.Class)

}
