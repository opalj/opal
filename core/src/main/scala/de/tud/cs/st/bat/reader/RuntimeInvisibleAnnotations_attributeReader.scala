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
 *  '''From the Specification'''
 * {{{
 * RuntimeInvisibleAnnotations_attribute {
 * 	u2 attribute_name_index;
 * 	u4 attribute_length;
 * 	u2 num_annotations;
 * 	annotation annotations[num_annotations];
 * }
 * }}}
 * @author Michael Eichberg
 */
trait RuntimeInvisibleAnnotations_attributeReader extends AttributeReader {

    type RuntimeInvisibleAnnotations_attribute <: Attribute
    type Annotations

    protected def Annotations(in: DataInputStream, cp: Constant_Pool): Annotations

    protected def RuntimeInvisibleAnnotations_attribute(attribute_name_index: Constant_Pool_Index,
                                                        attribute_length: Int,
                                                        annotations: Annotations)(
                                                            implicit constant_pool: Constant_Pool): RuntimeInvisibleAnnotations_attribute

    //
    // IMPLEMENTATION
    //

    import util.ControlAbstractions.repeat

    register(
        RuntimeInvisibleAnnotations_attributeReader.ATTRIBUTE_NAME ->
            ((ap: AttributeParent, cp: Constant_Pool, attribute_name_index: Constant_Pool_Index, in: DataInputStream) ⇒ {
                val attribute_length = in.readInt()
                RuntimeInvisibleAnnotations_attribute(
                    attribute_name_index, attribute_length, Annotations(in, cp)
                )(cp)
            })
    )

}

object RuntimeInvisibleAnnotations_attributeReader {

    val ATTRIBUTE_NAME = "RuntimeInvisibleAnnotations"

}

