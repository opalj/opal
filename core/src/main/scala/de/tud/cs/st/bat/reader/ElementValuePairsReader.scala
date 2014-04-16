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
package reader

import reflect.ClassTag

import java.io.DataInputStream

/**
 * Generic parser for an annotation's element-value pairs.
 *
 * @author Michael Eichberg
 */
trait ElementValuePairsReader extends Constant_PoolAbstractions {

    //
    // ABSTRACT DEFINITIONS
    //

    type ElementValue
    implicit val ElementValueManifest: ClassTag[ElementValue]

    type ElementValuePair
    implicit val ElementValuePairManifest: ClassTag[ElementValuePair]

    type Annotation

    def Annotation(cp: Constant_Pool, in: DataInputStream): Annotation

    def ElementValuePair(
        constant_pool: Constant_Pool,
        element_name_index: Constant_Pool_Index,
        element_value: ElementValue): ElementValuePair

    def ByteValue(
        constant_pool: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue

    def CharValue(
        constant_pool: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue

    def DoubleValue(
        constant_pool: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue

    def FloatValue(
        constant_pool: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue

    def IntValue(
        constant_pool: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue

    def LongValue(
        constant_pool: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue

    def ShortValue(
        constant_pool: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue

    def BooleanValue(
        constant_pool: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue

    def StringValue(
        constant_pool: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue

    def ClassValue(
        constant_pool: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue

    def EnumValue(
        constant_pool: Constant_Pool,
        type_name_index: Constant_Pool_Index,
        const_name_index: Constant_Pool_Index): ElementValue

    def AnnotationValue(
        constant_pool: Constant_Pool,
        annotation: Annotation): ElementValue

    def ArrayValue(
        constant_pool: Constant_Pool,
        values: ElementValues): ElementValue

    //
    // IMPLEMENTATION
    //

    import util.ControlAbstractions.repeat

    type ElementValues = IndexedSeq[ElementValue]
    type ElementValuePairs = IndexedSeq[ElementValuePair]

    def ElementValuePairs(cp: Constant_Pool, in: DataInputStream): ElementValuePairs = {
        repeat(in.readUnsignedShort) {
            ElementValuePair(cp, in)
        }
    }

    def ElementValuePair(cp: Constant_Pool, in: DataInputStream): ElementValuePair =
        ElementValuePair(cp, in.readUnsignedShort, ElementValue(cp, in))

    /**
     * Parses an element value.
     *
     * '''From the Specification'''
     * <pre>
     * element_value {
     *    u1 tag;
     *    union {
     *      u2   const_value_index;
     *
     *      {
     *        u2 type_name_index;
     *        u2 const_name_index;
     *      } enum_const_value;
     *
     *      u2 class_info_index;
     *
     *      annotation annotation_value;
     *
     *      {
     *        u2    num_values;
     *        element_value values[num_values];
     *      } array_value;
     *    } value;
     * }
     * </pre>
     */
    def ElementValue(cp: Constant_Pool, in: DataInputStream): ElementValue = {
        val tag = in.readByte
        (tag: @scala.annotation.switch) match {
            case 'B' ⇒ ByteValue(cp, in.readUnsignedShort)
            case 'C' ⇒ CharValue(cp, in.readUnsignedShort)
            case 'D' ⇒ DoubleValue(cp, in.readUnsignedShort)
            case 'F' ⇒ FloatValue(cp, in.readUnsignedShort)
            case 'I' ⇒ IntValue(cp, in.readUnsignedShort)
            case 'J' ⇒ LongValue(cp, in.readUnsignedShort)
            case 'S' ⇒ ShortValue(cp, in.readUnsignedShort)
            case 'Z' ⇒ BooleanValue(cp, in.readUnsignedShort)
            case 's' ⇒ StringValue(cp, in.readUnsignedShort)
            case 'e' ⇒ EnumValue(cp, in.readUnsignedShort, in.readUnsignedShort)
            case 'c' ⇒ ClassValue(cp, in.readUnsignedShort)
            case '@' ⇒ AnnotationValue(cp, Annotation(cp, in))
            case '[' ⇒ ArrayValue(cp, repeat(in.readUnsignedShort) { ElementValue(cp, in) })
        }
    }
}
