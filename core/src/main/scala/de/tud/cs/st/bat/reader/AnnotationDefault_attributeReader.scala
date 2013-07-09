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

/**
 * Reads in an annotation default attribute's data and passes it to a factory
 * method to create the attribute specific representation.
 *
 * '''From the Specification'''
 * The AnnotationDefault attribute is a variable-length attribute in the
 * attributes table of method_info structures representing elements of
 * annotation types.
 *
 * <pre>
 * AnnotationDefault_attribute {
 *  u2 attribute_name_index;
 *  u4 attribute_length;
 *  element_value default_value;
 * }
 * </pre>
 *
 * @author Michael Eichberg
 */
trait AnnotationDefault_attributeReader extends AttributeReader {

    //
    // ABSTRACT DEFINITIONS
    //

    type AnnotationDefault_attribute <: Attribute
    type ElementValue

    /**
     * Creates a new element value.
     */
    def ElementValue(in: DataInputStream, cp: Constant_Pool): ElementValue

    def AnnotationDefault_attribute(
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        element_value: ElementValue)(
            implicit constant_pool: Constant_Pool): AnnotationDefault_attribute

    //
    // IMPLEMENTATION
    //

    register(AnnotationDefault_attributeReader.ATTRIBTUE_NAME ->
        ((ap: AttributeParent, cp: Constant_Pool, attributeNameIndex: Constant_Pool_Index, in: DataInputStream) ⇒ {
            val attributeLength = in.readInt()
            AnnotationDefault_attribute(
                attributeNameIndex, attributeLength, ElementValue(in, cp)
            )(cp)
        }))
}

object AnnotationDefault_attributeReader {

    val ATTRIBTUE_NAME = "AnnotationDefault"

}