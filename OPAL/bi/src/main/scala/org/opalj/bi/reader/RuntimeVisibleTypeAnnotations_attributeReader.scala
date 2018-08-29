/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import scala.collection.GenTraversableOnce

/**
 * Generic parser for Java 8's `RuntimeVisibleTypeAnnotations` attribute.
 */
trait RuntimeVisibleTypeAnnotations_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type TypeAnnotation

    type TypeAnnotations <: GenTraversableOnce[TypeAnnotation]
    def TypeAnnotations(cp: Constant_Pool, in: DataInputStream): TypeAnnotations

    type RuntimeVisibleTypeAnnotations_attribute >: Null <: Attribute
    def RuntimeVisibleTypeAnnotations_attribute(
        constant_pool:        Constant_Pool,
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
        ap: AttributeParent,
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) ⇒ {
        /*val attribute_length = */ in.readInt()
        val annotations = TypeAnnotations(cp, in)
        if (annotations.nonEmpty || reifyEmptyAttributes) {
            RuntimeVisibleTypeAnnotations_attribute(cp, attribute_name_index, annotations)
        } else {
            null
        }
    }

    registerAttributeReader(RuntimeVisibleTypeAnnotationsAttribute.Name → parserFactory())
}
