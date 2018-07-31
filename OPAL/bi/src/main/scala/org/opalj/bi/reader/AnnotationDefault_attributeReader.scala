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
    // ABSTRACT DEFINITIONS
    //

    type ElementValue

    /**
     * Creates a new element value.
     */
    def ElementValue(cp: Constant_Pool, in: DataInputStream): ElementValue

    type AnnotationDefault_attribute <: Attribute

    def AnnotationDefault_attribute(
        constant_pool:        Constant_Pool,
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
        ap: AttributeParent,
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) ⇒ {
        /* val attributeLength =*/ in.readInt()
        AnnotationDefault_attribute(cp, attribute_name_index, ElementValue(cp, in))
    }

    registerAttributeReader(AnnotationDefaultAttribute.Name → parserFactory())
}
