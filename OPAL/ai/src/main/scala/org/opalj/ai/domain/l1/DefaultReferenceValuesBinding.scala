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
package l1

import scala.collection.SortedSet

import org.opalj.util.{ Answer, Yes, No, Unknown }
import org.opalj.collection.immutable.UIDSet

import br._

/**
 * @author Michael Eichberg
 */
trait DefaultReferenceValuesBinding extends l1.ReferenceValues {
    domain: Configuration with ClassHierarchy ⇒

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

    override protected[domain] def ObjectValue(
        pc: PC,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ObjectType): DomainObjectValue = {
        new SObjectValue(pc, isNull, isPrecise, theUpperTypeBound)
    }

    override protected[domain] def ObjectValue(
        pc: PC,
        isNull: Answer,
        upperTypeBound: UIDSet[ObjectType]): DomainObjectValue = {

        if (upperTypeBound.containsOneElement)
            ObjectValue(pc, isNull, false, upperTypeBound.first)
        else
            new MObjectValue(pc, isNull, upperTypeBound)
    }

    override protected[domain] def ArrayValue(
        pc: PC,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ArrayType): DomainArrayValue = {
        new ArrayValue(pc, isNull, isPrecise, theUpperTypeBound)
    }

    override protected[domain] def MultipleReferenceValues(
        values: SortedSet[SingleOriginReferenceValue]): MultipleReferenceValues = {
        new MultipleReferenceValues(values)
    }

}

