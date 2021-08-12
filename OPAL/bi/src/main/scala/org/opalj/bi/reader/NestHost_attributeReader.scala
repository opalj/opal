/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Generic parser for the ''NestHost'' attribute (Java 11).
 *
 * The NestHost attribute is a fixed-length attribute in the attributes table of a ClassFile
 * structure. The NestHost attribute records the nest host of the nest to which the current class or
 * interface claims to belong (§5.4.4).
 */
trait NestHost_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type NestHost_attribute <: Attribute

    /**
     * @param host_class_index Reference to a CONSTANT_Class_info.
     */
    def NestHost_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        host_class_index:     Constant_Pool_Index // CONSTANT_Class_info
    ): NestHost_attribute

    //
    // IMPLEMENTATION
    //

    /**
     * <pre>
     * NestHost_attribute {
     *     u2 attribute_name_index;
     *     u4 attribute_length;
     *     u2 host_class_index;
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
        /*val attribute_length =*/ in.readInt
        NestHost_attribute(
            cp,
            ap_name_index,
            ap_descriptor_index,
            attribute_name_index,
            in.readUnsignedShort()
        )
    }

    registerAttributeReader(NestHostAttribute.Name -> parserFactory())
}
