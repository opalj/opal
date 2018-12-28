/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.AnnotationsReader
import org.opalj.bi.reader.RuntimeInvisibleAnnotations_attributeReader
import org.opalj.bi.reader.RuntimeVisibleAnnotations_attributeReader
import org.opalj.bi.reader.RuntimeInvisibleParameterAnnotations_attributeReader
import org.opalj.bi.reader.RuntimeVisibleParameterAnnotations_attributeReader
import org.opalj.bi.reader.ParametersAnnotationsReader
import org.opalj.bi.reader.AnnotationDefault_attributeReader

/**
 * Factory methods to create representations of the attributes related to
 * Java annotations.
 *
 * @author Michael Eichberg
 */
trait AnnotationAttributesBinding
    extends AnnotationsBinding
    with AnnotationsReader
    with ParametersAnnotationsReader
    with RuntimeInvisibleAnnotations_attributeReader
    with RuntimeVisibleAnnotations_attributeReader
    with RuntimeInvisibleParameterAnnotations_attributeReader
    with RuntimeVisibleParameterAnnotations_attributeReader
    with AnnotationDefault_attributeReader
    with AttributeBinding {

    type AnnotationDefault_attribute = ElementValue

    type RuntimeVisibleAnnotations_attribute = RuntimeVisibleAnnotationTable

    type RuntimeInvisibleAnnotations_attribute = RuntimeInvisibleAnnotationTable

    type RuntimeVisibleParameterAnnotations_attribute = RuntimeVisibleParameterAnnotationTable

    type RuntimeInvisibleParameterAnnotations_attribute = RuntimeInvisibleParameterAnnotationTable

    def AnnotationDefault_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        element_value:        ElementValue
    ): AnnotationDefault_attribute = {
        element_value
    }

    def RuntimeVisibleAnnotations_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        annotations:          Annotations
    ): RuntimeVisibleAnnotations_attribute = {
        new RuntimeVisibleAnnotations_attribute(annotations)
    }

    def RuntimeInvisibleAnnotations_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        annotations:          Annotations
    ): RuntimeInvisibleAnnotations_attribute = {
        new RuntimeInvisibleAnnotations_attribute(annotations)
    }

    def RuntimeVisibleParameterAnnotations_attribute(
        cp:                     Constant_Pool,
        ap_name_index:          Constant_Pool_Index,
        ap_descriptor_index:    Constant_Pool_Index,
        attribute_name_index:   Constant_Pool_Index,
        parameters_annotations: ParametersAnnotations
    ): RuntimeVisibleParameterAnnotations_attribute = {
        new RuntimeVisibleParameterAnnotations_attribute(parameters_annotations)
    }

    def RuntimeInvisibleParameterAnnotations_attribute(
        cp:                     Constant_Pool,
        ap_name_index:          Constant_Pool_Index,
        ap_descriptor_index:    Constant_Pool_Index,
        attribute_name_index:   Constant_Pool_Index,
        parameters_annotations: ParametersAnnotations
    ): RuntimeInvisibleParameterAnnotations_attribute = {
        new RuntimeInvisibleParameterAnnotations_attribute(parameters_annotations)
    }

}

