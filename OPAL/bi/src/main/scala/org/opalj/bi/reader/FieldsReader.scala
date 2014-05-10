/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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

import reflect.ClassTag

import java.io.DataInputStream

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

    protected def Attributes(
        ap: AttributeParent,
        cp: Constant_Pool,
        in: DataInputStream): Attributes

    def Field_Info(
        constant_pool: Constant_Pool,
        access_flags: Int,
        name_index: Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        attributes: Attributes): Field_Info

    //
    // IMPLEMENTATION
    //

    type Fields = IndexedSeq[Field_Info]

    // We need the constant pool to look up the attributes' names and other information.
    def Fields(cp: Constant_Pool, in: DataInputStream): Fields = {
        import util.ControlAbstractions.repeat
        val fields_count = in.readUnsignedShort
        repeat(fields_count) {
            Field_Info(cp, in)
        }
    }

    private def Field_Info(cp: Constant_Pool, in: DataInputStream): Field_Info =
        Field_Info(
            cp,
            in.readUnsignedShort,
            in.readUnsignedShort,
            in.readUnsignedShort,
            Attributes(AttributesParent.Field, cp, in)
        )

}
