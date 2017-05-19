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
 * Common superclass of all instructions which are in their final form.
 *
 * @author Michael Eichberg
 */
trait Instruction extends InstructionLike {

    /**
     * @return  `this`.
     */
    override def resolveJumpTargets(pc: PC, pcs: Map[Symbol, PC]): this.type = this

    /**
     * Returns the pcs of the instructions that may be executed next at runtime. This
     * method takes potentially thrown exceptions into account. I.e., every instruction
     * that may throw an exception checks if it is handled locally and
     * – if so – checks if an appropriate handler exists and – if so – also returns
     * the first instruction of the handler. The chain may contain duplicates, iff the state
     * is potentially different when the target instruction is reached.
     *
     * @param   regularSuccessorsOnly If `true` only those instructions are returned
     *          which are not related to an exception thrown by this instruction.
     * @return  The absolute addresses of '''all instructions''' that may be executed next
     *          at runtime.
     */
    def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean = false
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = Code.BasicClassHierarchy
    ): Chain[PC]

    /**
     * Checks for structural equality of two instructions.
     *
     * @note   Implemted by using the underlying (compiler generated) equals methods.
     */
    def similar(other: Instruction): Boolean = this == other

}

/**
 * Functionality common to instructions.
 *
 * @author Michael Eichberg
 */
object Instruction {

    final val IllegalIndex: Int = 1

    /**
     * Facilitates the matching of [[Instruction]] objects.
     *
     * @return Returns the triple `Some((opcode,mnemonic,list of jvm exceptions))`.
     */
    def unapply(instruction: Instruction): Some[(Int, String, List[ObjectType])] = {
        Some((instruction.opcode, instruction.mnemonic, instruction.jvmExceptions))
    }

    /**
     * Determines if the instructions with the pcs `aPC` and `bPC` are isomorphic.
     *
     * @see [[Instruction.isIsomorphic]] for further details.
     */
    def areIsomorphic(aPC: PC, bPC: PC)(implicit code: Code): Boolean = {
        assert(aPC != bPC)

        code.instructions(aPC).isIsomorphic(aPC, bPC)
    }

    private[instructions] def nextInstructionOrExceptionHandlers(
        instruction: Instruction,
        currentPC:   PC,
        exceptions:  List[ObjectType]
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = Code.BasicClassHierarchy
    ): Chain[PC] = {
        var pcs = Chain.singleton(instruction.indexOfNextInstruction(currentPC))
        exceptions foreach { exception ⇒
            pcs = (code.handlersForException(currentPC, exception).map(_.handlerPC)) ++!: pcs
        }
        pcs
    }

    private[instructions] def nextInstructionOrExceptionHandler(
        instruction: Instruction,
        currentPC:   PC,
        exception:   ObjectType
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = Code.BasicClassHierarchy
    ): Chain[PC] = {
        val nextInstruction = instruction.indexOfNextInstruction(currentPC)
        nextInstruction :&: (code.handlersForException(currentPC, exception).map(_.handlerPC))
    }

    final val justNullPointerException: List[org.opalj.br.ObjectType] = {
        List(ObjectType.NullPointerException)
    }
}
