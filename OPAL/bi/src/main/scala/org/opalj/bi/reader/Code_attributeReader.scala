/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import org.opalj.control.fillArraySeq

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

/**
 * Defines a template method to read in the code attribute.
 *
 * '''From the Specification'''
 * The Code attribute is a variable-length attribute in the attributes table
 * of a method_info structure.
 */
trait Code_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type ExceptionTableEntry <: AnyRef
    implicit val exceptionTableEntryType: ClassTag[ExceptionTableEntry] // TODO: Replace in Scala 3 by `type ExceptionTableEntry : ClassTag`
    type ExceptionHandlers = ArraySeq[ExceptionTableEntry]

    type Instructions

    type Code_attribute <: Attribute

    def Instructions(
        cp:                  Constant_Pool,
        ap_name_index:       Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        in:                  DataInputStream
    ): Instructions

    protected def Attributes(
        cp:                  Constant_Pool,
        ap:                  AttributeParent,
        ap_name_index:       Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        in:                  DataInputStream
    ): Attributes

    def Code_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        max_stack:            Int,
        max_locals:           Int,
        instructions:         Instructions,
        exception_handlers:   ExceptionHandlers,
        attributes:           Attributes
    ): Code_attribute

    def ExceptionTableEntry(
        constant_pool: Constant_Pool,
        start_pc:      Int,
        end_pc:        Int,
        handler_pc:    Int,
        catch_type:    Constant_Pool_Index // may be "0" in case of "finally"
    ): ExceptionTableEntry

    //
    // IMPLEMENTATION
    //

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
        cp: Constant_Pool,
        ap: AttributeParent,
        ap_name_index: Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) => {
        /*val attribute_length = */ in.readInt()
        Code_attribute(
            cp,
            ap_name_index,
            ap_descriptor_index,
            attribute_name_index,
            in.readUnsignedShort(),
            in.readUnsignedShort(),
            Instructions(cp, ap_name_index, ap_descriptor_index, in),
            fillArraySeq(in.readUnsignedShort()) { // "exception_table_length" times
                ExceptionTableEntry(
                    cp,
                    in.readUnsignedShort, in.readUnsignedShort,
                    in.readUnsignedShort, in.readUnsignedShort
                )
            },
            Attributes(cp, AttributesParent.Code, ap_name_index, ap_descriptor_index, in)
        )
    }

    registerAttributeReader(CodeAttribute.Name -> parserFactory())

}
