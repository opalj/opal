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
package instructions

import org.opalj.collection.immutable.Chain

/**
 * An instruction that "invokes" something. This can, e.g., be the invocation of a method
 * or – using [[INCOMPLETE_INVOKEDYNAMIC]] – the read of a field value.
 *
 * @author Michael Eichberg
 */
abstract class InvocationInstruction
    extends Instruction
    with ConstantLengthInstruction
    with NoLabels {

    final override def asInvocationInstruction: this.type = this

    final override def isInvocationInstruction: Boolean = true

    final def mayThrowExceptions: Boolean = true

    /**
     * The simple name of the called method.
     *
     * @note    This information is – in case of [[INCOMPLETE_INVOKEDYNAMIC]] instructions
     *          – only available after loading the entire class file.
     */
    def name: String

    /**
     * The method descriptor of the called method.
     *
     * @note    This information is – in case of [[INCOMPLETE_INVOKEDYNAMIC]] instructions
     *          – only available after loading the entire class file.
     */
    def methodDescriptor: MethodDescriptor

    /**
     * Returns `true` if this method takes an implicit parameter "this".
     */
    def isInstanceMethod: Boolean

    final def numberOfPushedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = {
        if (methodDescriptor.returnType.isVoidType) 0 else 1
    }

    final def stackSlotsChange: Int = {
        val returnType = methodDescriptor.returnType
        (if (isInstanceMethod) -1 /* pop "receiver object */ else 0) -
            methodDescriptor.parameterTypes.foldLeft(0)(_ + _.computationalType.operandSize) +
            (if (returnType.isVoidType) 0 else returnType.computationalType.operandSize)
    }

    final def expressionResult: ExpressionResultLocation = {
        if (methodDescriptor.returnType.isVoidType) NoExpression else Stack
    }

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        this == code.instructions(otherPC)
    }

    final def readsLocal: Boolean = false

    final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final def writesLocal: Boolean = false

    final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    /**
     * Given that we have – without any sophisticated analysis – no idea which
     * exceptions may be thrown by the called method, we make the safe assumption that
     * any handler is a potential successor!
     *
     * The result may contain duplicates iff multiple different exceptions are handled by
     * the same handler. E.g., as generated in case of "Java's multicatch instruction":
     * {{{
     * try {} catch(IOException | NullPointerException ex) {...}
     * }}}
     */
    final def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Chain[PC] = {
        if (regularSuccessorsOnly)
            Chain.singleton(indexOfNextInstruction(currentPC))
        else {
            val exceptionHandlerPCs = code.handlerInstructionsFor(currentPC)
            indexOfNextInstruction(currentPC) :&: exceptionHandlerPCs
        }
    }

    final override def toString(currentPC: Int): String = toString()
}
