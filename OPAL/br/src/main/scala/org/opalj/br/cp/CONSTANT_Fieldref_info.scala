/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package br
package cp

import org.opalj.bi.ConstantPoolTags

/**
 * Represents a field.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_Fieldref_info(
        class_index:         Constant_Pool_Index,
        name_and_type_index: Constant_Pool_Index
) extends Constant_Pool_Entry {

    override def tag: Int = ConstantPoolTags.CONSTANT_Fieldref_ID

    // We don't mind if the field is initialized more than once (if reading the classfile
    // should be parallelized) as it is just an optimization and the object reference
    // is of no importance; an equals check will return true. Hence, w.r.t. the
    // previous definition this code is thread-safe.
    private[this] var fieldref: (ObjectType, String, FieldType) = null // to cache the result
    override def asFieldref(cp: Constant_Pool): (ObjectType, String, FieldType) = {
        var fieldref = this.fieldref
        if (fieldref eq null) {
            val nameAndType = cp(name_and_type_index).asNameAndType
            fieldref =
                (
                    cp(class_index).asObjectType(cp),
                    nameAndType.name(cp),
                    nameAndType.fieldType(cp)
                )
            this.fieldref = fieldref
        }
        fieldref
    }
}
