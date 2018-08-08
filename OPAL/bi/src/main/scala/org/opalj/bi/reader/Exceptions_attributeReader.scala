/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import scala.reflect.ClassTag

import org.opalj.control.repeat

/**
 * Generic parser for a code block's ''exceptions'' attribute.
 */
trait Exceptions_attributeReader extends AttributeReader {

    type Exceptions_attribute >: Null <: Attribute
    implicit val Exceptions_attributeManifest: ClassTag[Exceptions_attribute]

    type ExceptionIndexTable = IndexedSeq[Constant_Pool_Index]

    def Exceptions_attribute(
        constant_pool:         Constant_Pool,
        attribute_name_index:  Constant_Pool_Index,
        exception_index_table: ExceptionIndexTable,
        // The scope in which the attribute is defined
        as_name_index:       Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index
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
        ap: AttributeParent,
        as_name_index: Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index,
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) ⇒ {
        /*val attribute_length =*/ in.readInt()
        val number_of_exceptions = in.readUnsignedShort
        if (number_of_exceptions > 0 || reifyEmptyAttributes) {
            val exceptions = repeat(number_of_exceptions) { in.readUnsignedShort }
            Exceptions_attribute(
                cp,
                attribute_name_index,
                exceptions,
                as_name_index,
                as_descriptor_index
            )
        } else
            null
    }

    registerAttributeReader(ExceptionsAttribute.Name → parserFactory())
}
