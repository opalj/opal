/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import org.opalj.control.fillArrayOfInt

/**
 * Generic parser for a code block's ''exceptions'' attribute.
 */
trait Exceptions_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type Exceptions_attribute >: Null <: Attribute

    type ExceptionIndexTable = Array[Constant_Pool_Index]

    def Exceptions_attribute(
        cp:                    Constant_Pool,
        ap_name_index:         Constant_Pool_Index,
        ap_descriptor_index:   Constant_Pool_Index,
        attribute_name_index:  Constant_Pool_Index,
        exception_index_table: ExceptionIndexTable
    ): Exceptions_attribute

    //
    // IMPLEMENTATION
    //

    /* From The Specification
     *
     * <pre>
     * Exceptions_attribute {
     *  u2 attribute_name_index;
     *  u4 attribute_length;
     *  u2 number_of_exceptions;
     *  u2 exception_index_table[number_of_exceptions];
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
        /*val attribute_length =*/ in.readInt()
        val number_of_exceptions = in.readUnsignedShort
        if (number_of_exceptions > 0 || reifyEmptyAttributes) {
            val exceptions = fillArrayOfInt(number_of_exceptions) { in.readUnsignedShort }
            Exceptions_attribute(
                cp,
                ap_name_index,
                ap_descriptor_index,
                attribute_name_index,
                exceptions
            )
        } else {
            null
        }
    }

    registerAttributeReader(ExceptionsAttribute.Name -> parserFactory())
}
