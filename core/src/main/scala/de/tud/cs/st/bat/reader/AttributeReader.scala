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
 * Supertrait of all attribute readers.
 *
 * @author Michael Eichberg
 */
trait AttributeReader
        extends Constant_PoolAbstractions
        with AttributesAbstractions {

    type Attribute >: Null

    /**
     * Register a reader for a concrete attribute. This function is intended to
     * be provided/implemented by an `AttributesReader` that manages the attributes of a
     * class, method_info, field_info or code_attribute structure.
     *
     * @param reader A map where the key is the name of an attribute and the value is
     *  a function that given a data input stream that is positioned directly
     *  at the beginning of the attribute, the constant pool, the index of the attribute's
     *  name and the parent of the attribute reads in the attribute and returns it.
     */
    def registerAttributeReader(reader: (String, (AttributeParent, Constant_Pool, /* attribute name */ Constant_Pool_Index, DataInputStream) ⇒ Attribute)): Unit

    /**
     * Registers a new processor for the list of all attributes of a given class file
     * structure (class, field_info, method_info, code_attribute). This can be used to
     * post-process attributes. E.g., to merge multiple line number tables if they exist
     * or to removed attributes if they are completely resolved.
     */
    def registerAttributesPostProcessor(p: (Attributes) ⇒ Attributes): Unit
}
