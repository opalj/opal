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
package org.opalj
package ai
package domain
package l1

import scala.reflect.ClassTag

import org.opalj.collection.immutable.Chain
import org.opalj.br.ArrayType

/**
 * @author Michael Eichberg
 */
trait DefaultArrayValuesBinding extends l1.DefaultReferenceValuesBinding with ArrayValues {
    domain: CorrelationalDomain with IntegerValuesDomain with ConcreteIntegerValues with TypedValuesFactory with Configuration with TheClassHierarchy with LogContextProvider ⇒

    type DomainInitializedArrayValue = InitializedArrayValue
    final val DomainInitializedArrayValue: ClassTag[DomainInitializedArrayValue] = implicitly

    type DomainConcreteArrayValue = ConcreteArrayValue
    final val DomainConcreteArrayValue: ClassTag[DomainConcreteArrayValue] = implicitly

    //
    // FACTORY METHODS
    //

    final override def ArrayValue(
        origin:  ValueOrigin,
        theType: ArrayType,
        values:  Array[DomainValue]
    ): DomainConcreteArrayValue = {
        ArrayValue(origin, theType, values, nextT())
    }

    override def ArrayValue(
        origin:  ValueOrigin,
        theType: ArrayType,
        values:  Array[DomainValue],
        t:       Timestamp
    ): DomainConcreteArrayValue = {
        new ConcreteArrayValue(origin, theType, values, t)
    }

    final override def InitializedArrayValue(
        origin:    ValueOrigin,
        arrayType: ArrayType,
        counts:    Chain[Int]
    ): DomainInitializedArrayValue = {
        InitializedArrayValue(origin, arrayType, counts, nextT())
    }

    override def InitializedArrayValue(
        origin:    ValueOrigin,
        arrayType: ArrayType,
        counts:    Chain[Int],
        t:         Timestamp
    ): DomainInitializedArrayValue = {
        // we currently support at most two-dimensional arrays
        new InitializedArrayValue(origin, arrayType, counts.takeUpTo(2), t)
    }

}
