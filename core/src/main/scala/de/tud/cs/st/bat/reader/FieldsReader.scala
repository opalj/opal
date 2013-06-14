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
package de.tud.cs.st.bat
package reader

import java.io.DataInputStream

import scala.reflect.ClassTag

import de.tud.cs.st.util.ControlAbstractions.repeat

/**
 *
 * @author Michael Eichberg
 */
trait FieldsReader extends Constant_PoolAbstractions {

    //
    // ABSTRACT DEFINITIONS
    //

    type Field_Info
    implicit val Field_InfoManifest: ClassTag[Field_Info]

    type Attributes

    protected def Attributes(ap: AttributesParent, cp: Constant_Pool, in: DataInputStream): Attributes

    def Field_Info(access_flags: Int,
                   name_index: Constant_Pool_Index,
                   descriptor_index: Constant_Pool_Index,
                   attributes: Attributes)(
                       implicit constant_pool: Constant_Pool): Field_Info

    //
    // IMPLEMENTATION
    //

    type Fields = IndexedSeq[Field_Info]

    private val NO_FIELDS = Vector.empty

    // We need the constant pool to look up the attributes' names and other information.
    def Fields(in: DataInputStream, cp: Constant_Pool): Fields = {
        val fields_count = in.readUnsignedShort
        if (fields_count == 0) return NO_FIELDS

        repeat(fields_count) {
            Field_Info(in, cp)
        }
    }

    private def Field_Info(in: DataInputStream, cp: Constant_Pool): Field_Info = {
        Field_Info(
            in.readUnsignedShort,
            in.readUnsignedShort,
            in.readUnsignedShort,
            Attributes(AttributesParent.Field, cp, in)
        )(cp)
    }
}
