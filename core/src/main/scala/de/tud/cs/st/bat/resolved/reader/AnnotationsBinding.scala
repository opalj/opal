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
package reader

import reflect.ClassTag

import de.tud.cs.st.bat.reader.AnnotationsReader
import de.tud.cs.st.bat.reader.RuntimeInvisibleAnnotations_attributeReader
import de.tud.cs.st.bat.reader.RuntimeVisibleAnnotations_attributeReader
import de.tud.cs.st.bat.reader.RuntimeInvisibleParameterAnnotations_attributeReader
import de.tud.cs.st.bat.reader.RuntimeVisibleParameterAnnotations_attributeReader
import de.tud.cs.st.bat.reader.ParameterAnnotationsReader
import de.tud.cs.st.bat.reader.AnnotationDefault_attributeReader
import de.tud.cs.st.bat.reader.ElementValuePairsReader

/**
 * Factory methods to create representations of Java annotations.
 *
 * @author Michael Eichberg
 */
trait AnnotationsBinding
        extends AnnotationsReader
        with RuntimeInvisibleAnnotations_attributeReader
        with RuntimeVisibleAnnotations_attributeReader
        with RuntimeInvisibleParameterAnnotations_attributeReader
        with RuntimeVisibleParameterAnnotations_attributeReader
        with ParameterAnnotationsReader
        with AnnotationDefault_attributeReader
        with ElementValuePairsReader
        with ConstantPoolBinding
        with AttributeBinding {

    type Annotation = resolved.Annotation
    type AnnotationDefault_attribute = resolved.ElementValue
    type ElementValue = resolved.ElementValue
    val ElementValueManifest: ClassTag[ElementValue] = implicitly
    type EnumValue = resolved.EnumValue
    type AnnotationValue = resolved.AnnotationValue
    type ByteValue = resolved.ByteValue
    type CharValue = resolved.CharValue
    type ShortValue = resolved.ShortValue
    type IntValue = resolved.IntValue
    type LongValue = resolved.LongValue
    type FloatValue = resolved.FloatValue
    type DoubleValue = resolved.DoubleValue
    type ArrayValue = resolved.ArrayValue
    type ClassValue = resolved.ClassValue
    type BooleanValue = resolved.BooleanValue
    type ElementValuePair = resolved.ElementValuePair
    val ElementValuePairManifest: ClassTag[ElementValuePair] = implicitly
    type RuntimeVisibleAnnotations_attribute = resolved.RuntimeVisibleAnnotationTable
    type RuntimeInvisibleAnnotations_attribute = resolved.RuntimeInvisibleAnnotationTable
    type RuntimeVisibleParameterAnnotations_attribute = resolved.RuntimeVisibleParameterAnnotationTable
    type RuntimeInvisibleParameterAnnotations_attribute = resolved.RuntimeInvisibleParameterAnnotationTable

    val AnnotationManifest: ClassTag[Annotation] = implicitly

    def ElementValuePair(element_name_index: Constant_Pool_Index,
                         element_value: ElementValue)(
                             implicit cp: Constant_Pool): ElementValuePair = {
        new ElementValuePair(element_name_index.asString, element_value)
    }

    def ByteValue(const_value_index: Constant_Pool_Index)(
        implicit cp: Constant_Pool): ElementValue = {
        val cv: ConstantValue[_] = const_value_index.asConstantValue
        new ByteValue(cv.toByte)
    }

    def CharValue(const_value_index: Constant_Pool_Index)(
        implicit cp: Constant_Pool): ElementValue = {
        val cv: ConstantValue[_] = const_value_index.asConstantValue
        new CharValue(cv.toChar)
    }

    def DoubleValue(const_value_index: Constant_Pool_Index)(
        implicit cp: Constant_Pool): ElementValue = {
        val cv: ConstantValue[_] = const_value_index.asConstantValue
        new DoubleValue(cv.toDouble)
    }

    def FloatValue(const_value_index: Constant_Pool_Index)(
        implicit cp: Constant_Pool): ElementValue = {
        val cv: ConstantValue[_] = const_value_index.asConstantValue
        new FloatValue(cv.toFloat)
    }

    def IntValue(const_value_index: Constant_Pool_Index)(
        implicit cp: Constant_Pool): ElementValue = {
        val cv: ConstantValue[_] = const_value_index.asConstantValue
        new IntValue(cv.toInt)
    }

    def LongValue(const_value_index: Constant_Pool_Index)(
        implicit cp: Constant_Pool): ElementValue = {
        val cv: ConstantValue[_] = const_value_index.asConstantValue
        new LongValue(cv.toLong)
    }

    def ShortValue(const_value_index: Constant_Pool_Index)(
        implicit cp: Constant_Pool): ElementValue = {
        val cv: ConstantValue[_] = const_value_index.asConstantValue
        new ShortValue(cv.toShort)
    }

    def BooleanValue(const_value_index: Constant_Pool_Index)(
        implicit cp: Constant_Pool): ElementValue = {
        val cv: ConstantValue[_] = const_value_index.asConstantValue
        new BooleanValue(cv.toBoolean)
    }

    def StringValue(const_value_index: Constant_Pool_Index)(
        implicit cp: Constant_Pool): ElementValue = {
        val cv: ConstantValue[_] = const_value_index.asConstantValue
        new StringValue(cv.toUTF8)
    }

    def ClassValue(const_value_index: Constant_Pool_Index)(
        implicit cp: Constant_Pool): ElementValue = {
        val rt: String = const_value_index.asString
        new ClassValue(ReturnType(rt))
    }

    def EnumValue(type_name_index: Constant_Pool_Index,
                  const_name_index: Constant_Pool_Index)(
                      implicit cp: Constant_Pool): ElementValue = {
        new EnumValue(
            type_name_index.asFieldType /*<= triggers the lookup in the CP*/ .asObjectType,
            const_name_index.asString)
    }

    def AnnotationValue(
        annotation: Annotation)(
            implicit cp: Constant_Pool): ElementValue =
        new AnnotationValue(annotation)

    def ArrayValue(
        values: ElementValues)(
            implicit cp: Constant_Pool): ElementValue =
        new ArrayValue(values)

    def Annotation(
        type_index: Constant_Pool_Index,
        element_value_pairs: ElementValuePairs)(
            implicit cp: Constant_Pool) =
        new Annotation(type_index.asFieldType, element_value_pairs)

    def AnnotationDefault_attribute(
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        element_value: ElementValue)(
            implicit cp: Constant_Pool) = {
        element_value
    }

    def RuntimeVisibleAnnotations_attribute(
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        annotations: Annotations)(
            implicit cp: Constant_Pool) =
        new RuntimeVisibleAnnotations_attribute(annotations)

    def RuntimeInvisibleAnnotations_attribute(
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        annotations: Annotations)(
            implicit cp: Constant_Pool) =
        new RuntimeInvisibleAnnotations_attribute(annotations)

    def RuntimeVisibleParameterAnnotations_attribute(
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        parameter_annotations: ParameterAnnotations)(
            implicit cp: Constant_Pool) =
        new RuntimeVisibleParameterAnnotations_attribute(parameter_annotations)

    def RuntimeInvisibleParameterAnnotations_attribute(
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        parameter_annotations: ParameterAnnotations)(
            implicit cp: Constant_Pool) =
        new RuntimeInvisibleParameterAnnotations_attribute(parameter_annotations)

}


