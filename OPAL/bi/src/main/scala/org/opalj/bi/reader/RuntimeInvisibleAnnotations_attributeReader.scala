/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Generic parser for the `RuntimeInvisibleAnnotations` attribute.
 */
trait RuntimeInvisibleAnnotations_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type Annotation

    type Annotations <: IterableOnce[Annotation]
    def Annotations(cp: Constant_Pool, in: DataInputStream): Annotations

    type RuntimeInvisibleAnnotations_attribute >: Null <: Attribute

    protected def RuntimeInvisibleAnnotations_attribute(
        constant_pool:        Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        annotations:          Annotations
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
        cp: Constant_Pool,
        ap: AttributeParent,
        ap_name_index: Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) => {
        /*val attribute_length = */ in.readInt()
        val annotations = Annotations(cp, in)
        if (annotations.iterator.nonEmpty || reifyEmptyAttributes) {
            RuntimeInvisibleAnnotations_attribute(
                cp,
                ap_name_index,
                ap_descriptor_index,
                attribute_name_index,
                annotations
            )
        } else {
            null
        }
    }

    registerAttributeReader(RuntimeInvisibleAnnotationsAttribute.Name -> parserFactory())
}
