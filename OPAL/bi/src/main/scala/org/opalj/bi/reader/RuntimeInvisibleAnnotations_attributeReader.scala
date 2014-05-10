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
 * Generic parser for the `RuntimeInvisibleAnnotations` attribute.
 *
 * @author Michael Eichberg
 */
trait RuntimeInvisibleAnnotations_attributeReader extends AttributeReader {

    type Annotations

    protected def Annotations(cp: Constant_Pool, in: DataInputStream): Annotations

    type RuntimeInvisibleAnnotations_attribute <: Attribute

    protected def RuntimeInvisibleAnnotations_attribute(
        constant_pool: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        annotations: Annotations): RuntimeInvisibleAnnotations_attribute

    //
    // IMPLEMENTATION
    //

    /*
     * '''From the Specification'''
     * <pre>
     * RuntimeInvisibleAnnotations_attribute {
     *  u2 attribute_name_index;
     *  u4 attribute_length;
     *  u2 num_annotations;
     *  annotation annotations[num_annotations];
     * }
     * </pre>
     */
    registerAttributeReader(
        RuntimeInvisibleAnnotations_attributeReader.ATTRIBUTE_NAME ->
            ((ap: AttributeParent, cp: Constant_Pool, attribute_name_index: Constant_Pool_Index, in: DataInputStream) ⇒ {
                val attribute_length = in.readInt()
                RuntimeInvisibleAnnotations_attribute(
                    cp, attribute_name_index, attribute_length, Annotations(cp, in)
                )
            })
    )
}

/**
 * Common properties of `RuntimeInvisibleAnnotations`.
 *
 * @author Michael Eichberg
 */
object RuntimeInvisibleAnnotations_attributeReader {

    val ATTRIBUTE_NAME = "RuntimeInvisibleAnnotations"

}

