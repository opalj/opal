/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
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
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat.reader

import java.io.DataInputStream

import de.tud.cs.st.util.ControlAbstractions.repeat

/**
 *
 * @author Michael Eichberg
 */
trait Code_attributeReader extends AttributeReader {

  type ExceptionTableEntry
  implicit val ExceptionTableEntryManifest: ClassManifest[ExceptionTableEntry]

  type Code

  type Code_attribute <: Attribute

  type Attributes

  type ExceptionTable = IndexedSeq[ExceptionTableEntry]

  def Code(in: DataInputStream, cp: Constant_Pool): Code

  def Attributes(in: DataInputStream, cp: Constant_Pool): Attributes

  def Code_attribute(attribute_name_index: Int,
                     attribute_length: Int,
                     max_stack: Int,
                     max_locals: Int,
                     code: Code,
                     exception_table: ExceptionTable,
                     attributes: Attributes)(implicit constant_pool: Constant_Pool): Code_attribute

  def ExceptionTableEntry(start_pc: Int,
                          end_pc: Int,
                          handler_pc: Int,
                          catch_type: Int)(
                            implicit constant_pool: Constant_Pool): ExceptionTableEntry

  //
  // IMPLEMENTATION
  //

  register(de.tud.cs.st.bat.canonical.Code_attribute.name ->
    ((in: DataInputStream, cp: Constant_Pool, attribute_name_index: Int) ⇒ {

      Code_attribute(
        attribute_name_index,
        in.readInt(),
        in.readUnsignedShort(),
        in.readUnsignedShort(),
        Code(in, cp),
        repeat(in.readUnsignedShort()) { // "exception_table_length" times
          ExceptionTableEntry(
            in.readUnsignedShort, in.readUnsignedShort,
            in.readUnsignedShort, in.readUnsignedShort
          )(cp)
        },
        Attributes(in, cp) // read the code attribute's attributes 
      )(cp)
    }
    )
  )
}
