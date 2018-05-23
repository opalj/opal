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
package reader

import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.DEFAULT_INVOKEDYNAMIC

/**
 * Mixin this trait to resolve links between [[org.opalj.br.instructions.INVOKEDYNAMIC]]
 * instructions and the [[BootstrapMethodTable]].
 */
trait DeferredInvokedynamicResolution extends ConstantPoolBinding with CodeBinding {

    override type Constant_Pool = Array[Constant_Pool_Entry]

    /**
     * Resolves an [[org.opalj.br.instructions.INCOMPLETE_INVOKEDYNAMIC]] instruction using the
     * [[BootstrapMethodTable]] of the class.
     *
     * Deferred resolution is necessary since the [[BootstrapMethodTable]] – which
     * is an attribute of the class file – is loaded after the methods.
     *
     * @note    This method is called (back) after the class file was completely loaded.
     *          Registration as a callback method happens whenever an `invokedynamic`
     *          instruction is found in a method's byte code.
     *
     * ==Overriding this Method==
     * To perform additional analyses on `invokedynamic` instructions, e.g., to
     * fully resolve the call target, a subclass may override this method to do so.
     * When you override this method, you should call this method
     * (`super.deferredResolveInvokedynamicInstruction`) to ensure that the default resolution
     * is carried out.
     *
     * @param   classFile The [[ClassFile]] with which the deferred action was registered.
     * @param   cp The class file's [[Constant_Pool]].
     * @param   invokeDynamicInfo The [[org.opalj.br.instructions.INVOKEDYNAMIC]] instruction's
     *          constant pool entry.
     * @param   instructions This method's array of [[instructions.Instruction]]s.
     *          (The array returned by the [[#Instructions]] method.)
     * @param   pc The program counter of the `invokedynamic` instruction.
     */
    protected def deferredInvokedynamicResolution(
        classFile:         ClassFile,
        cp:                Constant_Pool,
        invokeDynamicInfo: CONSTANT_InvokeDynamic_info,
        instructions:      Array[Instruction],
        pc:                PC
    ): ClassFile = {

        val bootstrapMethods = classFile.attributes collectFirst {
            case BootstrapMethodTable(bms) ⇒ bms
        }
        val invokeDynamic = DEFAULT_INVOKEDYNAMIC(
            bootstrapMethods.get(invokeDynamicInfo.bootstrapMethodAttributeIndex),
            invokeDynamicInfo.methodName(cp),
            invokeDynamicInfo.methodDescriptor(cp)
        )
        instructions(pc) = invokeDynamic
        classFile
    }
}
