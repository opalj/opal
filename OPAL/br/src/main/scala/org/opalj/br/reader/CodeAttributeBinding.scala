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
package br
package reader

import reflect.ClassTag

import bi.reader.Code_attributeReader

/**
 * Binding for the code attribute.
 *
 * @author Michael Eichberg
 */
trait CodeAttributeBinding
        extends Code_attributeReader
        with ConstantPoolBinding
        with CodeBinding
        with AttributeBinding {

    type ExceptionTableEntry = br.ExceptionHandler
    val ExceptionTableEntryManifest: ClassTag[ExceptionTableEntry] = implicitly

    type Code_attribute = br.Code

    def Code_attribute(
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        max_stack: Int,
        max_locals: Int,
        instructions: Instructions,
        exception_handlers: ExceptionHandlers,
        attributes: Attributes) = {
        new Code(max_stack, max_locals, instructions, exception_handlers, attributes)
    }

    def ExceptionTableEntry(
        cp: Constant_Pool,
        start_pc: Int,
        end_pc: Int,
        handler_pc: Int,
        catch_type_index: Constant_Pool_Index): ExceptionTableEntry = {
        new ExceptionTableEntry(
            start_pc, end_pc, handler_pc,
            if (catch_type_index == 0)
                None
            else
                Some(cp(catch_type_index).asObjectType(cp))
        )
    }
}


