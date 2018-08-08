/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import scala.reflect.ClassTag

import java.io.DataInputStream

import org.opalj.control.repeat

/**
 * Defines a template method to read in the code attribute.
 *
 * '''From the Specification'''
 * The Code attribute is a variable-length attribute in the attributes table
 * of a method_info structure.
 */
trait Code_attributeReader extends AttributeReader {

    type ExceptionTableEntry
    implicit val ExceptionTableEntryManifest: ClassTag[ExceptionTableEntry]

    type Instructions

    type Code_attribute <: Attribute

    type Attributes

    def Instructions(
        as_name_index:       Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index,
        cp:                  Constant_Pool,
        in:                  DataInputStream
    ): Instructions

    protected def Attributes(
        ap: AttributeParent,
        // The scope in which the attribute is defined
        as_name_index:       Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index,
        cp:                  Constant_Pool,
        in:                  DataInputStream
    ): Attributes

    def Code_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        max_stack:            Int,
        max_locals:           Int,
        instructions:         Instructions,
        exception_handlers:   ExceptionHandlers,
        // The scope in which the attribute is defined
        as_name_index:       Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index,
        attributes:          Attributes
    ): Code_attribute

    def ExceptionTableEntry(
        constant_pool: Constant_Pool,
        start_pc:      Int,
        end_pc:        Int,
        handler_pc:    Int,
        catch_type:    Int
    ): ExceptionTableEntry

    //
    // IMPLEMENTATION
    //

    type ExceptionHandlers = IndexedSeq[ExceptionTableEntry]

    /**
     * '''From the Specification'''
     * <pre>
     * Code_attribute {
     *  u2 attribute_name_index; u4 attribute_length;
     *  u2 max_stack;
     *  u2 max_locals;
     *  u4 code_length;
     *  u1 code[code_length];
     *  u2 exception_table_length;
     *  {   u2 start_pc;
     *      u2 end_pc;
     *      u2 handler_pc;
     *      u2 catch_type;
     *  } exception_table[exception_table_length];
     *  u2 attributes_count;
     *  attribute_info attributes[attributes_count];
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
        /*val attribute_length = */ in.readInt()
        Code_attribute(
            cp,
            attribute_name_index,
            in.readUnsignedShort(),
            in.readUnsignedShort(),
            Instructions(as_name_index, as_descriptor_index, cp, in),
            repeat(in.readUnsignedShort()) { // "exception_table_length" times
                ExceptionTableEntry(
                    cp,
                    in.readUnsignedShort, in.readUnsignedShort,
                    in.readUnsignedShort, in.readUnsignedShort
                )
            },
            as_name_index,
            as_descriptor_index,
            Attributes(AttributesParent.Code, as_name_index, as_descriptor_index, cp, in)
        )
    }

    registerAttributeReader(CodeAttribute.Name → parserFactory())

}
