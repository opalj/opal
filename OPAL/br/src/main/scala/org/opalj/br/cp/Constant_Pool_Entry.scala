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

import org.opalj.bi.AttributeParent
import org.opalj.bytecode.BytecodeProcessingFailedException

/**
 * Represents a constant pool entry.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
trait Constant_Pool_Entry extends bi.reader.ConstantPoolEntry {

    def tag: Int = throw new UnknownError("tag not available")

    def asString: String = {
        throw new BytecodeProcessingFailedException(s"$this cannot be converted to string")
    }

    def asFieldType: FieldType = {
        throw new BytecodeProcessingFailedException("conversion to field type is not supported")
    }

    def asMethodDescriptor: MethodDescriptor =
        throw new BytecodeProcessingFailedException(
            "conversion to method descriptor is not supported"
        )

    def asFieldTypeSignature: FieldTypeSignature =
        throw new BytecodeProcessingFailedException(
            "conversion to field type signature is not supported"
        )

    def asSignature(ap: AttributeParent): Signature =
        throw new BytecodeProcessingFailedException(
            "conversion to signature attribute is not supported"
        )

    def asConstantValue(cp: Constant_Pool): ConstantValue[_] =
        throw new BytecodeProcessingFailedException(
            "conversion of "+this.getClass.getSimpleName+" to constant value is not supported"
        )

    def asConstantFieldValue(cp: Constant_Pool): ConstantFieldValue[_] =
        throw new BytecodeProcessingFailedException(
            "conversion of "+this.getClass.getSimpleName+" to constant field value is not supported"
        )

    def asFieldref(cp: Constant_Pool): (ObjectType, String, FieldType) =
        throw new BytecodeProcessingFailedException("conversion to field ref is not supported")

    def asMethodref(
        cp: Constant_Pool
    ): (ReferenceType, Boolean /*InterfaceMethodRef*/ , String, MethodDescriptor) =
        throw new BytecodeProcessingFailedException("conversion to method ref is not supported")

    def asObjectType(cp: Constant_Pool): ObjectType =
        throw new BytecodeProcessingFailedException("conversion to object type is not supported")

    def asReferenceType(cp: Constant_Pool): ReferenceType = {
        val message = "conversion to reference type is not supported"
        throw new BytecodeProcessingFailedException(message)
    }

    def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument = {
        val message = "conversion to bootstrap argument is not supported"
        throw new BytecodeProcessingFailedException(message)
    }

    def asMethodHandle(cp: Constant_Pool): MethodHandle = {
        val message = "conversion to method handle is not supported"
        throw new BytecodeProcessingFailedException(message)
    }

    def asNameAndType: CONSTANT_NameAndType_info = {
        val message = "conversion to name and type info is not supported"
        throw new BytecodeProcessingFailedException(message)
    }

    def asInvokeDynamic: CONSTANT_InvokeDynamic_info = {
        val message = "conversion to invoke dynamic info is not supported"
        throw new BytecodeProcessingFailedException(message)
    }

    def asModuleIdentifier(cp: Constant_Pool): String = {
        val message = "conversion to reference type is not supported"
        throw new BytecodeProcessingFailedException(message)
    }

    def asPackageIdentifier(cp: Constant_Pool): String = {
        val message = "conversion to reference type is not supported"
        throw new BytecodeProcessingFailedException(message)
    }
}
