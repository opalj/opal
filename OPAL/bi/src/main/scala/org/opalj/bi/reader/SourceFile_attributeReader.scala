/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * The SourceFile attribute is an optional attribute in the
 * attributes table of a ClassFile structure.
 */
trait SourceFile_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type SourceFile_attribute <: Attribute

    def SourceFile_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        sourcefile_index:     Constant_Pool_Index,
        // The scope in which the attribute is defined
        as_name_index:       Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index
    ): SourceFile_attribute

    //
    // IMPLEMENTATION
    //

    /**
     * The SourceFile attribute is an optional fixed-length attribute in the
     * attributes table of a ClassFile structure.
     *
     * <pre>
     * SourceFile_attribute {
     *    u2 attribute_name_index;
     *    u4 attribute_length;
     *    u2 sourcefile_index;
     * }
     * </pre>
     */
    private[this] def parserFactory() = (
        ap: AttributeParent,
        // The scope in which the attribute is defined
        as_name_index: Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index,
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) ⇒ {
        /*val attribute_length =*/ in.readInt
        SourceFile_attribute(
            cp, attribute_name_index, in.readUnsignedShort, as_name_index, as_descriptor_index
        )
    }

    registerAttributeReader(SourceFileAttribute.Name → parserFactory())
}
