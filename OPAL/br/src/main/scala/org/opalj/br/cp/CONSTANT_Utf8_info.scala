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

import org.opalj.bi.AttributeParent
import org.opalj.bi.AttributesParent
import org.opalj.br.reader.SignatureParser
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.bi.ConstantPoolTags

/**
 * Represents a constant string value.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_Utf8_info(value: String) extends Constant_Pool_Entry {

    override def tag: Int = ConstantPoolTags.CONSTANT_Utf8_ID

    override def asString = value

    private[this] var methodDescriptor: MethodDescriptor = null // to cache the result
    override def asMethodDescriptor = {
        if (methodDescriptor eq null) { methodDescriptor = MethodDescriptor(value) };
        methodDescriptor
    }

    private[this] var fieldType: FieldType = null // to cache the result
    override def asFieldType = {
        if (fieldType eq null) { fieldType = FieldType(value) };
        fieldType
    }

    override def asFieldTypeSignature =
        // should be called at most once => caching doesn't make sense
        SignatureParser.parseFieldTypeSignature(value)

    override def asSignature(ap: AttributeParent): Signature =
        // should be called at most once => caching doesn't make sense
        ap match {
            case AttributesParent.Field     ⇒ SignatureParser.parseFieldTypeSignature(value)
            case AttributesParent.ClassFile ⇒ SignatureParser.parseClassSignature(value)
            case AttributesParent.Method    ⇒ SignatureParser.parseMethodTypeSignature(value)
            case AttributesParent.Code ⇒
                val message = s"code attribute has an unexpected signature attribute: $value"
                throw new BytecodeProcessingFailedException(message)
        }

    override def asConstantValue(cp: Constant_Pool): ConstantString =
        // required to support annotations; should be called at most once => caching doesn't make sense
        ConstantString(value)
}
