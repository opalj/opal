/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Generic parser for ''annotation default'' attributes.
 */
trait AnnotationDefault_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type ElementValue

    /**
     * Creates a new element value.
     */
    def ElementValue(cp: Constant_Pool, in: DataInputStream): ElementValue

    type AnnotationDefault_attribute <: Attribute

    def AnnotationDefault_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        element_value:        ElementValue
    ): AnnotationDefault_attribute

    //
    // IMPLEMENTATION
    //

    /**
     * <pre>
     * AnnotationDefault_attribute {
     *  u2 attribute_name_index;
     *  u4 attribute_length;
     *  element_value default_value;
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
        /* val attributeLength = */ in.readInt()
        AnnotationDefault_attribute(
            cp,
            ap_name_index,
            ap_descriptor_index,
            attribute_name_index,
            ElementValue(cp, in)
        )
    }

    registerAttributeReader(AnnotationDefaultAttribute.Name -> parserFactory())
}
