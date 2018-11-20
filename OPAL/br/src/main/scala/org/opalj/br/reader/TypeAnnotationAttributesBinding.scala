/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.RuntimeInvisibleTypeAnnotations_attributeReader
import org.opalj.bi.reader.RuntimeVisibleTypeAnnotations_attributeReader

/**
 * Factory methods to create representations of the attributes related to
 * Java type annotations.
 *
 * @author Michael Eichberg
 */
trait TypeAnnotationAttributesBinding
    extends TypeAnnotationsBinding
    with RuntimeInvisibleTypeAnnotations_attributeReader
    with RuntimeVisibleTypeAnnotations_attributeReader
    with AttributeBinding {

    type RuntimeInvisibleTypeAnnotations_attribute = RuntimeInvisibleTypeAnnotationTable

    type RuntimeVisibleTypeAnnotations_attribute = RuntimeVisibleTypeAnnotationTable

    protected def RuntimeInvisibleTypeAnnotations_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        annotations:          TypeAnnotations
    ): RuntimeInvisibleTypeAnnotations_attribute = {
        new RuntimeInvisibleTypeAnnotationTable(annotations)
    }

    def RuntimeVisibleTypeAnnotations_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        annotations:          TypeAnnotations
    ): RuntimeVisibleTypeAnnotations_attribute = {
        new RuntimeVisibleTypeAnnotationTable(annotations)
    }

}

