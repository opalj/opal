/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Generic infrastructure used by specific parsers of class file attributes to register
 * with the overall framework ([[org.opalj.bi.reader.AttributesReader]]).
 */
trait AttributeReader extends Constant_PoolAbstractions with AttributesAbstractions {

    //
    // General framework to read attributes.
    //

    /**
     * Called (typically by subclasses) to register a reader for a concrete attribute.
     * This function is intended to be provided/implemented by an `AttributesReader`
     * that manages the attributes of a class, method_info, field_info or
     * code_attribute structure.
     *
     * @param reader A map where the key is the name of an attribute and the value is
     *  a function that given a data input stream that is positioned directly
     *  at the beginning of the attribute, the constant pool, the index of the attribute's
     *  name and the parent of the attribute reads in the attribute and returns it.
     */
    def registerAttributeReader(
        reader: (String, (Constant_Pool, AttributeParent, /* the (class|field|method)name index of the attribute parent */ Constant_Pool_Index, /* the (field|method)descriptor index of the parent or -1 in case of a class */ Constant_Pool_Index, /* attribute_name_index */ Constant_Pool_Index, DataInputStream) => Attribute)
    ): Unit

    /**
     * Registers a new processor for the list of all attributes of a given class file
     * structure (class, field_info, method_info, code_attribute). This can be used to
     * post-process attributes. E.g., to merge multiple line number tables if they exist
     * or to remove attributes if they are completely resolved.
     *
     * @see The implementation of
     *      [[org.opalj.br.reader.UnpackedLineNumberTable_attributeBinding]]
     *      for a concrete example.
     */
    def registerAttributesPostProcessor(p: Attributes => Attributes): Unit

    /**
     * Controls whether empty attributes (e.g., a LocalVariableTypeTable with no entries)
     * should be reified or should be dropped.
     */
    def reifyEmptyAttributes: Boolean = false
}
