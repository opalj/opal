/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Generic parser for the ''ModuleMainClass'' attribute (Java 9).
 */
trait ModuleMainClass_attributeReader extends AttributeReader {

    type ModuleMainClass_attribute <: Attribute

    /**
     * @param main_class_index Reference to a CONSTANT_Class_info.
     */
    def ModuleMainClass_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        main_class_index:     Constant_Pool_Index, // CONSTANT_Class_info
        // The scope in which the attribute is defined
        as_name_index:       Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index
    ): ModuleMainClass_attribute

    /**
     * <pre>
     * MainClass_attribute {
     *     u2 attribute_name_index;
     *     u4 attribute_length;
     *
     *     u2 main_class_index;
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
        /*val attribute_length =*/ in.readInt
        ModuleMainClass_attribute(
            cp,
            attribute_name_index,
            in.readUnsignedShort(),
            as_name_index,
            as_descriptor_index
        )
    }

    registerAttributeReader(ModuleMainClassAttribute.Name → parserFactory())
}
