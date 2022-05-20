/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.AnnotationsAbstractions
import org.opalj.bi.reader.ElementValuePairsReader
import scala.reflect.ClassTag

/**
 * Factory methods to create representations of Java annotations.
 *
 * @author Michael Eichberg
 */
trait AnnotationsBinding
    extends AnnotationsAbstractions
    with ElementValuePairsReader
    with ConstantPoolBinding {

    type Annotation = br.Annotation
    override implicit val annotationType: ClassTag[Annotation] = ClassTag(classOf[br.Annotation])

    type ElementValue = br.ElementValue
    override implicit val elementValueType: ClassTag[ElementValue] = ClassTag(classOf[br.ElementValue])

    type EnumValue = br.EnumValue

    type AnnotationValue = br.AnnotationValue

    type ByteValue = br.ByteValue

    type CharValue = br.CharValue

    type ShortValue = br.ShortValue

    type IntValue = br.IntValue

    type LongValue = br.LongValue

    type FloatValue = br.FloatValue

    type DoubleValue = br.DoubleValue

    type ArrayValue = br.ArrayValue

    type ClassValue = br.ClassValue

    type BooleanValue = br.BooleanValue

    type ElementValuePair = br.ElementValuePair
    override implicit val elementValuePairType: ClassTag[ElementValuePair] = ClassTag(classOf[br.ElementValuePair])

    def ElementValuePair(
        cp:                 Constant_Pool,
        element_name_index: Constant_Pool_Index,
        element_value:      ElementValue
    ): ElementValuePair = {
        new ElementValuePair(cp(element_name_index).asString, element_value)
    }

    def ByteValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new ByteValue(cv.toByte)
    }

    def CharValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new CharValue(cv.toChar)
    }

    def DoubleValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new DoubleValue(cv.toDouble)
    }

    def FloatValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new FloatValue(cv.toFloat)
    }

    def IntValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new IntValue(cv.toInt)
    }

    def LongValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new LongValue(cv.toLong)
    }

    def ShortValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new ShortValue(cv.toShort)
    }

    def BooleanValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new BooleanValue(cv.toBoolean)
    }

    def StringValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new StringValue(cv.toUTF8)
    }

    def ClassValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        val rt: String = cp(const_value_index).asString
        new ClassValue(ReturnType(rt))
    }

    def EnumValue(
        cp:               Constant_Pool,
        type_name_index:  Constant_Pool_Index,
        const_name_index: Constant_Pool_Index
    ): ElementValue = {
        new EnumValue(
            cp(type_name_index).asFieldType /*<= triggers the lookup in the CP*/ .asObjectType,
            cp(const_name_index).asString
        )
    }

    def AnnotationValue(cp: Constant_Pool, annotation: Annotation): ElementValue = {
        new AnnotationValue(annotation)
    }

    def ArrayValue(cp: Constant_Pool, values: ElementValues): ElementValue = {
        new ArrayValue(values)
    }

    def Annotation(
        cp:                  Constant_Pool,
        type_index:          Constant_Pool_Index,
        element_value_pairs: ElementValuePairs
    ): Annotation = {
        new Annotation(cp(type_index).asFieldType, element_value_pairs)
    }

}

