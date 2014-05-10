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
package de.tud.cs.st.bat
package reader

import java.io.DataInputStream

/**
 * The Synthetic attribute is an attribute in the attributes table
 * of a ClassFile, field_info or method_info structure.
 *
 * @author Michael Eichberg
 */
trait Synthetic_attributeReader extends AttributeReader {

    type Synthetic_attribute <: Attribute

    def Synthetic_attribute(
        cp: Constant_Pool,
        attributeNameIndex: Constant_Pool_Index): Synthetic_attribute

    /* From the Specification
     *
     * <pre>
     * Synthetic_attribute {
     *  u2 attribute_name_index;
     *  u4 attribute_length;
     * }
     * </pre>
     */
    registerAttributeReader(
        Synthetic_attributeReader.ATTRIBUTE_NAME -> (
            (ap: AttributeParent, cp: Constant_Pool, attributeNameIndex: Constant_Pool_Index, in: DataInputStream) ⇒ {
                val attribute_length = in.readInt
                Synthetic_attribute(cp, attributeNameIndex)
            }
        )
    )
}

object Synthetic_attributeReader {

    val ATTRIBUTE_NAME = "Synthetic"

}
