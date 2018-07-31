/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Parser for Java 8's `RuntimeInvisibleTypeAnnotations` attribute.
 */
trait RuntimeInvisibleTypeAnnotations_attributeReader extends AttributeReader {

    type TypeAnnotation

    type TypeAnnotations <: Traversable[TypeAnnotation]
    def TypeAnnotations(cp: Constant_Pool, in: DataInputStream): TypeAnnotations

    type RuntimeInvisibleTypeAnnotations_attribute >: Null <: Attribute

    protected def RuntimeInvisibleTypeAnnotations_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        annotations:          TypeAnnotations
    ): RuntimeInvisibleTypeAnnotations_attribute

    //
    // IMPLEMENTATION
    //

    /**
     * <pre>
     * RuntimeInvisibleTypeAnnotations_attribute {
     *      u2              attribute_name_index;
     *      u4              attribute_length;
     *      u2              num_annotations;
     *      type_annotation annotations[num_annotations];
     * }
     * </pre>
     */
    private[this] def parserFactory() = (
        ap: AttributeParent,
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) ⇒ {
        /*val attribute_length =*/ in.readInt()
        val annotations = TypeAnnotations(cp, in)
        if (annotations.nonEmpty || reifyEmptyAttributes) {
            RuntimeInvisibleTypeAnnotations_attribute(cp, attribute_name_index, annotations)
        } else {
            null
        }
    }

    registerAttributeReader(RuntimeInvisibleTypeAnnotationsAttribute.Name → parserFactory())

}
