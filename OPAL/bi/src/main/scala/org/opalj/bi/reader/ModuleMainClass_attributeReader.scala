/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Generic parser for the ''ModuleMainClass'' attribute (Java 9).
 */
trait ModuleMainClass_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type ModuleMainClass_attribute <: Attribute

    /**
     * @param main_class_index Reference to a CONSTANT_Class_info.
     */
    def ModuleMainClass_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        main_class_index:     Constant_Pool_Index // CONSTANT_Class_info
    ): ModuleMainClass_attribute

    //
    // IMPLEMENTATION
    //

    /**
     * <pre>
     * MainClass_attribute {
     *     u2 attribute_name_index;
     *     u4 attribute_length;
     *     u2 main_class_index;
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
        ModuleMainClass_attribute(
            cp,
            ap_name_index,
            ap_descriptor_index,
            attribute_name_index,
            in.readUnsignedShort()
        )
    }

    registerAttributeReader(ModuleMainClassAttribute.Name -> parserFactory())
}
