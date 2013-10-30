/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.st
package bat
package reader

import java.io.DataInputStream

import reflect.ClassTag

/**
 * Defines a template method to read in the code attribute.
 *
 * '''From the Specification'''
 * The Code attribute is a variable-length attribute in the attributes table
 * of a method_info structure.
 *
 * Format
 * <pre>
 * Code_attribute {
 * 	u2 attribute_name_index; u4 attribute_length;
 * 	u2 max_stack;
 * 	u2 max_locals;
 * 	u4 code_length;
 * 	u1 code[code_length];
 * 	u2 exception_table_length;
 * 	{	u2 start_pc;
 * 		u2 end_pc;
 * 		u2 handler_pc;
 * 		u2 catch_type;
 * 	} exception_table[exception_table_length];
 * 	u2 attributes_count;
 * 	attribute_info attributes[attributes_count];
 * }
 * </pre>
 *
 * @author Michael Eichberg
 */
trait Code_attributeReader extends AttributeReader {

    type ExceptionTableEntry
    implicit val ExceptionTableEntryManifest: ClassTag[ExceptionTableEntry]

    type Instructions

    type Code_attribute <: Attribute

    type Attributes

    def Instructions(in: DataInputStream, cp: Constant_Pool): Instructions

    protected def Attributes(
        ap: AttributeParent,
        cp: Constant_Pool, in: DataInputStream): Attributes

    def Code_attribute(
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        max_stack: Int,
        max_locals: Int,
        instructions: Instructions,
        exception_handlers: ExceptionHandlers,
        attributes: Attributes)(implicit constant_pool: Constant_Pool): Code_attribute

    def ExceptionTableEntry(
        start_pc: Int,
        end_pc: Int,
        handler_pc: Int,
        catch_type: Int)(
            implicit constant_pool: Constant_Pool): ExceptionTableEntry

    //
    // IMPLEMENTATION
    //

    import util.ControlAbstractions.repeat

    type ExceptionHandlers = IndexedSeq[ExceptionTableEntry]

    registerAttributeReader(
        Code_attributeReader.ATTRIBUTE_NAME -> (
            (ap: AttributeParent, cp: Constant_Pool, attribute_name_index: Constant_Pool_Index, in: DataInputStream) ⇒ {
                Code_attribute(
                    attribute_name_index,
                    in.readInt(),
                    in.readUnsignedShort(),
                    in.readUnsignedShort(),
                    Instructions(in, cp),
                    repeat(in.readUnsignedShort()) { // "exception_table_length" times
                        ExceptionTableEntry(
                            in.readUnsignedShort, in.readUnsignedShort,
                            in.readUnsignedShort, in.readUnsignedShort
                        )(cp)
                    },
                    Attributes(AttributesParent.Code, cp, in)
                )(cp)
            }
        )
    )
}

object Code_attributeReader {

    val ATTRIBUTE_NAME = "Code"

}

