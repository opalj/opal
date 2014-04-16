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
 * A generic reader for Java 8's `MethodParameters` attribute.
 *
 * <pre>
 * MethodParameters_attribute {
 *      u2 attribute_name_index;
 *      u4 attribute_length;
 *       u1 parameters_count;
 *       {   u2 name_index;
 *           u2 access_flags;
 *       } parameters[parameters_count];
 * }
 * </pre>
 *
 * @author Michael Eichberg
 */
trait MethodParameters_attributeReader extends AttributeReader {

    type MethodParameters_attribute <: Attribute

    type MethodParameter
    implicit val MethodParameterManifest: ClassTag[MethodParameter]

    def MethodParameters_attribute(
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        parameters: MethodParameters)(
            implicit constant_pool: Constant_Pool): MethodParameters_attribute

    def MethodParameter(
        name_index: Constant_Pool_Index,
        access_flags: Int)(
            implicit constant_pool: Constant_Pool): MethodParameter

    //
    // IMPLEMENTATION
    //
    import util.ControlAbstractions.repeat

    type MethodParameters = IndexedSeq[MethodParameter]

    registerAttributeReader(
        MethodParameters_attributeReader.ATTRIBUTE_NAME -> (
            (ap: AttributeParent, cp: Constant_Pool, attribute_name_index: Constant_Pool_Index, in: DataInputStream) ⇒ {
                val attribute_length = in.readInt()
                MethodParameters_attribute(
                    attribute_name_index,
                    attribute_length,
                    repeat(in.readUnsignedByte) {
                        MethodParameter(in.readUnsignedShort, in.readUnsignedShort)(cp)
                    }
                )(cp)
            }
        )
    )
}

object MethodParameters_attributeReader {

    val ATTRIBUTE_NAME = "MethodParameters"

}

