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

import java.io.DataInputStream

/**
 * Implements the template method to read signature attributes.
 *
 * '''From the Specification'''
 *
 * The Signature attribute is an optional fixed-length attribute in the
 * attributes table of a ClassFile, field_info or method_info structure.
 *
 * <pre>
 * Signature_attribute {
 *    u2 attribute_name_index;
 *    u4 attribute_length;
 *    u2 signature_index;
 * }
 * </pre>
 * @author Michael Eichberg
 */
trait Signature_attributeReader extends AttributeReader {

    type Signature_attribute <: Attribute

    /**
     * '''From the Specification'''
     *
     * The constant pool entry at signature_index must be a CONSTANT_Utf8_info
     * structure representing either a class signature, if this signature
     * attribute is an attribute of a ClassFile structure, a method type
     * signature, if this signature is an attribute of a method_info structure,
     * or a field type signature otherwise.
     */
    def Signature_attribute(
        attribute_name_index: Constant_Pool_Index,
        signature_index: Constant_Pool_Index)(
            implicit constant_pool: Constant_Pool, ap: AttributeParent): Signature_attribute

    registerAttributeReader(
        Signature_attributeReader.ATTRIBUTE_NAME -> (
            (ap: AttributeParent, cp: Constant_Pool, attribute_name_index: Constant_Pool_Index, in: DataInputStream) ⇒ {
                val attribute_length = in.readInt
                Signature_attribute(
                    attribute_name_index,
                    in.readUnsignedShort
                )(cp, ap)
            }
        )
    )
}

object Signature_attributeReader {

    val ATTRIBUTE_NAME = "Signature"

}
