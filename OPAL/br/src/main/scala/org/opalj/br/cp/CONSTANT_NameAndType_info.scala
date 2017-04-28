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
 * Represents a field or a method without indicating which class or interface it belongs
 * to.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_NameAndType_info(
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index
) extends Constant_Pool_Entry {

    override def asNameAndType: this.type = this

    // this operation is very cheap and hence, it doesn't make sense to cache the result
    def name(cp: Constant_Pool): String = cp(name_index).asString

    def fieldType(cp: Constant_Pool): FieldType = cp(descriptor_index).asFieldType

    def methodDescriptor(cp: Constant_Pool): MethodDescriptor = {
        cp(descriptor_index).asMethodDescriptor
    }

    override def tag: Int = ConstantPoolTags.CONSTANT_NameAndType_ID
}