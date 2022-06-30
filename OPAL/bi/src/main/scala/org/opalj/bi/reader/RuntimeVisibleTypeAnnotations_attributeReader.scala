/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Generic parser for Java 8's `RuntimeVisibleTypeAnnotations` attribute.
 */
trait RuntimeVisibleTypeAnnotations_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type TypeAnnotation

    type TypeAnnotations <: IterableOnce[TypeAnnotation]
    def TypeAnnotations(cp: Constant_Pool, in: DataInputStream): TypeAnnotations

    type RuntimeVisibleTypeAnnotations_attribute >: Null <: Attribute
    def RuntimeVisibleTypeAnnotations_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        annotations:          TypeAnnotations
    ): RuntimeVisibleTypeAnnotations_attribute

    //
    // IMPLEMENTATION
    //

    /**
     * <pre>
     * RuntimeVisibleTypeAnnotations_attribute {
     *      u2              attribute_name_index;
     *      u4              attribute_length;
     *      u2              num_annotations;
     *      type_annotation annotations[num_annotations];
     * }
     * <pre>
     */
    private[this] def parserFactory() = (
        cp: Constant_Pool,
        ap: AttributeParent,
        ap_name_index: Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) => {
        /*val attribute_length = */ in.readInt()
        val annotations = TypeAnnotations(cp, in)
        if (annotations.iterator.nonEmpty || reifyEmptyAttributes) {
            RuntimeVisibleTypeAnnotations_attribute(
                cp, ap_name_index, ap_descriptor_index, attribute_name_index, annotations
            )
        } else {
            null
        }
    }

    registerAttributeReader(RuntimeVisibleTypeAnnotationsAttribute.Name -> parserFactory())
}
