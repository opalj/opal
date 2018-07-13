/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Template method to read in the SourceDebugExtension attribute.
 *
 * The SourceDebugExtension attribute is an optional attribute in the
 * attributes table of a ClassFile structure.
 */
trait SourceDebugExtension_attributeReader extends AttributeReader {

    type SourceDebugExtension_attribute <: Attribute

    def SourceDebugExtension_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        debug_extension:      Array[Byte]
    ): SourceDebugExtension_attribute

    /**
     * The SourceDebugExtension attribute is an optional attribute in the
     * attributes table of a ClassFile structure.
     *
     * <pre>
     * SourceDebugExtension_attribute {
     *  u2 attribute_name_index;
     *  u4 attribute_length;
     *  u1 debug_extension[attribute_length];
     *  // <- which is a modified UTF 8 String... but – as stated in the spec –
     *  // Note that the debug_extension array may denote a string longer than
     *  // that which can be represented with an instance of class String.
     * }
     * </pre>
     */
    private[this] def parserFactory() = (
        ap: AttributeParent,
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) ⇒ {
        val attribute_length = in.readInt
        val data = new Array[Byte](attribute_length)
        in.readFully(data)

        SourceDebugExtension_attribute(cp, attribute_name_index, data)
    }

    registerAttributeReader(SourceDebugExtensionAttribute.Name → parserFactory())

}
