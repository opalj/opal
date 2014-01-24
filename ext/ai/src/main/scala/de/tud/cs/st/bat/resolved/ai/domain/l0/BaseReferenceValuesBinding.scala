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
package l0

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

/**
 * Default implementation for handling reference values.
 *
 * @author Michael Eichberg
 */
trait BaseReferenceValuesBinding[+I] extends DefaultTypeLevelReferenceValues[I] {
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

    /**
     * @inheritdoc
     * This implementation always directly creates a new `SObjectValue`.
     */
    override def NonNullReferenceValue(pc: PC, objectType: ObjectType): DomainObjectValue =
        new SObjectValue(objectType)

    override def NewArray(pc: PC, count: DomainValue, arrayType: ArrayType): DomainArrayValue =
        new ArrayValue(arrayType)

    override def NewArray(pc: PC, counts: List[DomainValue], arrayType: ArrayType): DomainArrayValue =
        new ArrayValue(arrayType)

    override def ArrayValue(pc: PC, arrayType: ArrayType): DomainArrayValue =
        new ArrayValue(arrayType)

    def ObjectValue(pc: PC, objectType: ObjectType): DomainObjectValue =
        new SObjectValue(objectType)

    /**
     * @inheritdoc
     *
     * Depending on the kind of reference type (array or class/interface type) this method
     * just calls the respective factory method: `ArrayValue(PC,ArrayType)`
     * or `ObjectValue(PC,ObjectType)`.
     *
     * @note It is generally not necessary to override this method.
     */
    override def ReferenceValue(pc: PC, referenceType: ReferenceType): DomainReferenceValue =
        referenceType match {
            case ot: ObjectType ⇒ ObjectValue(pc, ot)
            case at: ArrayType  ⇒ ArrayValue(pc, at)
        }

    /**
     * Factory method to create a `DomainValue` that represents ''either a reference
     * value that has the given type bound and is initialized or the value `null`''.
     * However, the information whether the value is `null` or not is not available.
     * Furthermore, the type may also be an upper bound. I.e., we may have multiple types
     * and the runtime type is guaranteed to be a subtype of all given types.
     *
     * ==Summary==
     * The properties of the domain value are:
     *
     *  - Initialized: '''yes''' (the constructor was called)
     *  - Type: '''Upper Bound'''
     *  - Null: '''MayBe''' (It is unknown whether the value is `null` or not.)
     */
    def ReferenceValue(pc: PC, upperTypeBound: UIDList[ObjectType]): DomainReferenceValue = {
        assume(upperTypeBound.nonEmpty)
        if (upperTypeBound.tail.isEmpty)
            ReferenceValue(pc, upperTypeBound.head)
        else
            new MObjectValue(upperTypeBound)
    }

    override def NewObject(pc: PC, objectType: ObjectType): DomainObjectValue =
        new SObjectValue(objectType)

    /**
     * @inheritdoc
     *
     * Depending on the kind of reference type (array or class type) this method
     * just calls the respective factory method: `ArrayValue(PC,ArrayType)`
     * or `ObjectValue(PC,ObjectType)`.
     *
     * @note It is generally necessary to override this method when you want to track
     *      a value`s properties ('''type''' and '''isPrecise''') more precisely.
     */
    override def InitializedObject(pc: PC, referenceType: ReferenceType): DomainReferenceValue =
        if (referenceType.isArrayType)
            ArrayValue(pc, referenceType.asArrayType)
        else
            ObjectValue(pc, referenceType.asObjectType)

    override def StringValue(pc: PC, value: String): DomainObjectValue =
        new SObjectValue(ObjectType.String)

    override def ClassValue(pc: PC, t: Type): DomainObjectValue =
        new SObjectValue(ObjectType.Class)

}
