/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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

/**
 * Common super trait of all constant pool entries that represent method refs.
 *
 * The created `MethodRef` is cached.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
import org.opalj.br.ReferenceType
import org.opalj.br.MethodDescriptor

trait AsMethodref extends Constant_Pool_Entry {

    def class_index: Constant_Pool_Index

    def name_and_type_index: Constant_Pool_Index

    def isInterfaceMethodRef: Boolean

    // to cache the result
    private[this] var methodref: (ReferenceType, Boolean /*InterfaceMethodRef*/ , String, MethodDescriptor) = null
    override def asMethodref(cp: Constant_Pool): (ReferenceType, Boolean, String, MethodDescriptor) = {
        // The following solution is sufficiently thread safe; i.e.,
        // it may happen that two or more methodref instances  
        // are created, but these instances are guaranteed to
        // be equal (`==`).

        var methodref = this.methodref
        if (methodref eq null) {
            val nameAndType = cp(name_and_type_index).asNameAndType
            methodref =
                (
                    cp(class_index).asReferenceType(cp),
                    isInterfaceMethodRef,
                    nameAndType.name(cp),
                    nameAndType.methodDescriptor(cp)
                )
            this.methodref = methodref
        }
        methodref
    }
}