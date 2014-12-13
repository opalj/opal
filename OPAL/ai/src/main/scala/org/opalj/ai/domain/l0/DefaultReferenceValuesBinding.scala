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
package l0

import scala.reflect.ClassTag

import org.opalj.collection.immutable.UIDSet

import org.opalj.br.ArrayType
import org.opalj.br.ObjectType

import org.opalj.ai.IntegerValuesDomain
import org.opalj.ai.TypedValuesFactory
import org.opalj.ai.domain.ClassHierarchy
import org.opalj.ai.domain.Configuration
import org.opalj.ai.domain.DefaultVMLevelExceptionsFactory

/**
 * Default implementation for handling reference values.
 *
 * @author Michael Eichberg
 */
trait DefaultReferenceValuesBinding
        extends DefaultTypeLevelReferenceValues
        with DefaultVMLevelExceptionsFactory {
    domain: IntegerValuesDomain with TypedValuesFactory with Configuration with ClassHierarchy ⇒

    type AReferenceValue = ReferenceValue
    type DomainReferenceValue = AReferenceValue

    final val DomainReferenceValue: ClassTag[DomainReferenceValue] = implicitly

    type DomainNullValue = NullValue
    type DomainObjectValue = ObjectValue
    type DomainArrayValue = ArrayValue

    val TheNullValue: DomainNullValue = new NullValue()

    //
    // FACTORY METHODS
    //

    /**
     * @inheritdoc
     *
     * This implementation always returns the singleton instance [[TheNullValue]].
     */
    override def NullValue(origin: ValueOrigin): DomainNullValue = TheNullValue

    override def ObjectValue(
        origin: ValueOrigin,
        objectType: ObjectType): DomainObjectValue = {

        new SObjectValue(objectType)
    }

    override def ObjectValue(
        origin: ValueOrigin,
        upperTypeBound: UIDSet[ObjectType]): DomainObjectValue = {

        if (upperTypeBound.consistsOfOneElement)
            ObjectValue(origin, upperTypeBound.first)
        else
            new MObjectValue(upperTypeBound)
    }

    override protected[domain] def ArrayValue(
        origin: ValueOrigin,
        arrayType: ArrayType): DomainArrayValue = {

        new ArrayValue(arrayType)
    }

}
