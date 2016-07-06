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

import org.opalj.bi.ConstantPoolTags

/**
 * Is used by the invokedynamic instruction to specify a bootstrap method, the dynamic
 * invocation name, the argument and return types of the call, and optionally, a
 * sequence of additional constants called static arguments to the bootstrap method.
 *
 * @param bootstrapMethodAttributeIndex This is an index into the bootstrap table.
 *    Since the bootstrap table is a class level attribute it is only possible
 *    to resolve this reference after loading the entire class file (class level
 *    attributes are loaded last).
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_InvokeDynamic_info(
        bootstrapMethodAttributeIndex: Int,
        nameAndTypeIndex:              Constant_Pool_Index
) extends Constant_Pool_Entry {

    override def asInvokeDynamic: this.type = this

    override def tag: Int = ConstantPoolTags.CONSTANT_InvokeDynamic_ID

    def methodName(cp: Constant_Pool): String = cp(nameAndTypeIndex).asNameAndType.name(cp)

    def methodDescriptor(cp: Constant_Pool): MethodDescriptor = {
        cp(nameAndTypeIndex).asNameAndType.methodDescriptor(cp)
    }

}