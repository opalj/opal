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

import org.opalj.br.Type
import org.opalj.br.BooleanType
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.br.ShortType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.FloatType
import org.opalj.br.DoubleType
import org.opalj.br.ReferenceType
import org.opalj.br.FieldType
import org.opalj.br.VoidType

/**
 * Defines additional, generally useful factory methods to create`DomainValue`s.
 *
 * @author Michael Eichberg
 */
trait TypedValuesFactory {
    domain: ValuesDomain with ReferenceValuesFactory with PrimitiveValuesFactory ⇒

    /**
     * Factory method to create domain values with a specific type. I.e., values for
     * which we have some type information but no value or source information.
     * However, the value is guaranteed to be proper initialized (if non-null).
     *
     * For example, if `valueType` is a reference type it may be possible
     * that the actual value is `null`, but such knowledge is not available.
     *
     * The framework uses this method when a method is to be analyzed, but no parameter
     * values are given and initial values need to be generated. This method is not
     * used elsewhere by the framework.
     */
    def TypedValue(vo: ValueOrigin, valueType: Type): DomainValue = valueType match {
        case BooleanType       ⇒ BooleanValue(vo)
        case ByteType          ⇒ ByteValue(vo)
        case ShortType         ⇒ ShortValue(vo)
        case CharType          ⇒ CharValue(vo)
        case IntegerType       ⇒ IntegerValue(vo)
        case FloatType         ⇒ FloatValue(vo)
        case LongType          ⇒ LongValue(vo)
        case DoubleType        ⇒ DoubleValue(vo)
        case rt: ReferenceType ⇒ ReferenceValue(vo, rt)
        case VoidType ⇒
            throw DomainException("a domain value cannot have the type void")
    }

    /**
     * Creates a `DomainValue` that represents a a value with the given type
     * and which is initialized using the JVM's default value for that type.
     * E.g., for `IntegerValue`s the value is set to `0`. In case of a
     * `ReferenceType` the value is the [[ReferenceValuesFactory#NullValue]].
     */
    final def DefaultValue(pc: PC, theType: FieldType): DomainValue = {
        theType match {
            case BooleanType      ⇒ BooleanValue(pc, false)
            case ByteType         ⇒ ByteValue(pc, 0)
            case CharType         ⇒ CharValue(pc, 0)
            case ShortType        ⇒ ShortValue(pc, 0)
            case IntegerType      ⇒ IntegerValue(pc, 0)
            case FloatType        ⇒ FloatValue(pc, 0.0f)
            case LongType         ⇒ LongValue(pc, 0l)
            case DoubleType       ⇒ DoubleValue(pc, 0.0d)
            case _: ReferenceType ⇒ NullValue(pc)
        }
    }
}
