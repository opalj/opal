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
package ba

import org.opalj.bi.ACC_STATIC
import org.opalj.br.JVMMethod
import org.opalj.br.instructions.Instruction

/**
 * Builder for the [[org.opalj.br.Code]] attribute with all its properties. Instantiation is only
 * possible with the [[CODE]] factory. The `max_stack` and
 * `max_locals` values will be calculated if not explicitly defined.
 *
 * @author Malte Limmeroth
 */
class CodeAttributeBuilder[T] private[ba] (
        private val instructions:      Array[Instruction],
        private val annotations:       Map[br.PC, T],
        private var maxStack:          Option[Int],
        private var maxLocals:         Option[Int],
        private var exceptionHandlers: br.ExceptionHandlers,
        private var attributes:        br.Attributes
) extends br.CodeAttributeBuilder[(Map[br.PC, T], List[String])] {

    /**
     * Defines the max_stack value.
     *
     * (This overrides/disables the automatic computation of this value.)
     */
    def MAXSTACK(value: Int): this.type = {
        maxStack = Some(value)
        this
    }

    /**
     * Defines the max_locals value.
     *
     * (This overrides/disables the automatic computation of this value.)
     */
    def MAXLOCALS(value: Int): this.type = {
        maxLocals = Some(value)
        this
    }

    /** Creates a `Code` attribute w.r.t. the given method. */
    def apply(jvmMethod: JVMMethod): (br.Code, (Map[br.PC, T], List[String])) = {
        this(jvmMethod.accessFlags, jvmMethod.name, jvmMethod.descriptor)
    }

    /**
     * @param  accessFlags The declaring method's access flags, required during code validation or
     *         when MAXSTACK/MAXLOCALS should be computed.
     * @param  descriptor The declaring method's descriptor; required during code valiation or
     *         when MAXSTACK/MAXLOCALS should be computed.
     *
     * @return The tuple:
     *         `(the code attribute, (the extracted meta information, the list of warnings))`.
     */
    def apply(
        accessFlags: Int,
        name:        String,
        descriptor:  br.MethodDescriptor
    ): (br.Code, (Map[br.PC, T], List[String])) = {

        import CodeAttributeBuilder.warnMessage
        var warnings = List.empty[String]

        val computedMaxLocals = br.Code.computeMaxLocals(
            !ACC_STATIC.isSet(accessFlags),
            descriptor,
            instructions
        )
        if (maxLocals.isDefined && maxLocals.get < computedMaxLocals) {
            warnings ::=
                warnMessage.format(
                    descriptor.toJVMDescriptor,
                    "max_locals",
                    maxLocals.get,
                    computedMaxLocals
                )
        }

        val computedMaxStack = br.Code.computeMaxStack(
            instructions = instructions,
            exceptionHandlers = exceptionHandlers
        )

        if (maxStack.isDefined && maxStack.get < computedMaxStack) {
            warnings ::= warnMessage.format(
                descriptor.toJVMDescriptor,
                "max_stack",
                maxStack.get,
                computedMaxStack
            )
        }

        val code = br.Code(
            maxStack = maxStack.getOrElse(computedMaxStack),
            maxLocals = maxLocals.getOrElse(computedMaxLocals),
            instructions = instructions,
            exceptionHandlers = exceptionHandlers,
            attributes = attributes
        )

        (code, (annotations, warnings))
    }
}

object CodeAttributeBuilder {

    val warnMessage = s"%s: %s is too small %d < %d"
}
