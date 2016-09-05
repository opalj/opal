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
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * The TargetPlatform attribute is an attribute in the attributes table
 * of a module definition (Java 9).
 *
 * @author Michael Eichberg
 */
trait TargetPlatform_attributeReader extends AttributeReader {

    type TargetPlatform_attribute <: Attribute

    /**
     * @note if the indexes are zero then the field is empty!
     */
    def TargetPlatform_attribute(
        cp:                   Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        os_name_index:        Constant_Pool_Index, // CONSTANT_UTF8
        os_arch_index:        Constant_Pool_Index, // CONSTANT_UTF8
        os_version_index:     Constant_Pool_Index // CONSTANT_UTF8
    ): TargetPlatform_attribute

    /**
     * <pre>
     * TargetPlatform_attribute {
     *     u2 attribute_name_index;
     *     u4 attribute_length;
     *
     *     u2 os_name_index;
     *     u2 os_arch_index;
     *     u2 os_version_index;
     * }
     * </pre>
     */
    private[this] def parser(
        ap:                   AttributeParent,
        cp:                   Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in:                   DataInputStream
    ): TargetPlatform_attribute = {
        /*val attribute_length =*/ in.readInt
        TargetPlatform_attribute(
            cp, attribute_name_index,
            in.readUnsignedShort(),
            in.readUnsignedShort(),
            in.readUnsignedShort()
        )
    }

    registerAttributeReader(TargetPlatformAttribute.Name → parser)

}

object TargetPlatformAttribute {

    final val Name = "TargetPlatform"

}
