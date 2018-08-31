/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import scala.collection.GenTraversableOnce

/**
 * Generic parser for the `RuntimeInvisibleAnnotations` attribute.
 */
trait RuntimeInvisibleAnnotations_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type Annotation

    type Annotations <: GenTraversableOnce[Annotation]
    def Annotations(cp: Constant_Pool, in: DataInputStream): Annotations

    type RuntimeInvisibleAnnotations_attribute >: Null <: Attribute

    protected def RuntimeInvisibleAnnotations_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        annotations:          Annotations,
        // The scope in which the attribute is defined
        as_name_index:       Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index
    ): RuntimeInvisibleAnnotations_attribute

    //
    // IMPLEMENTATION
    //

    /**
     * <pre>
     * RuntimeInvisibleAnnotations_attribute {
     *  u2 attribute_name_index;
     *  u4 attribute_length;
     *  u2 num_annotations;
     *  annotation annotations[num_annotations];
     * }
     * </pre>
     */
    private[this] def parserFactory() = (
        ap: AttributeParent,
        as_name_index: Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index,
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) ⇒ {
        /*val attribute_length = */ in.readInt()
        val annotations = Annotations(cp, in)
        if (annotations.nonEmpty || reifyEmptyAttributes) {
            RuntimeInvisibleAnnotations_attribute(
                cp,
                attribute_name_index,
                annotations,
                as_name_index,
                as_descriptor_index
            )
        } else {
            null
        }
    }

    registerAttributeReader(RuntimeInvisibleAnnotationsAttribute.Name → parserFactory())
}
