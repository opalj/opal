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
package instructions

import org.opalj.collection.mutable.UShortSet

/**
 * Common superclass of all instructions.
 *
 * @author Michael Eichberg
 */
trait Instruction {

    /**
     *  The opcode of the instruction as defined by the JVM specification. The
     *  opcode is a value in the range [0..255].
     */
    def opcode: Int

    /**
     *  The mnemonic of the instruction as defined by the JVM specification.
     */
    def mnemonic: String

    /**
     * The exceptions that may be thrown by the JVM at runtime if the execution of
     * this instruction fails.
     * I.e., these are neither exceptions that are explicitly created and then thrown
     * by user code nor errors that my arise due to an invalid code base (e.g.
     * `LinkageError`s).
     */
    def runtimeExceptions: List[ObjectType]

    /**
     * The index of the next instruction in the code array.
     */
    def indexOfNextInstruction(currentPC: PC, code: Code): Int

    /**
     * The index of the next instruction in the code array.
     */
    def indexOfNextInstruction(currentPC: PC, modifiedByWide: Boolean): Int

    /**
     * Returns the pcs of the instructions that may be executed next at runtime. This
     * method takes potentially thrown exceptions into account. I.e., every instruction
     * that may throw an exception checks if it occurs within a catch block and
     * – if so – checks if an appropriate handler exists and – if so – also returns
     * the first instruction of the handler.
     *
     * @return The absolute addresses of '''all instructions''' that may be executed next
     *      at runtime.
     */
    def nextInstructions(currentPC: PC, code: Code): PCs

    /**
     * The number of values that are popped from the operand stack. Here, long and
     * double values are also counted as one value though they use to stack slots!
     *
     * @param ctg A function that returns the computational type category of
     *          the value on the operand stack with a given index. The top value on
     *          the operand stack has index '0' and may occupy one (for category 1 values)
     *          or two stack slots (for category 2 values.)
     * @note Several stack management instructions manipulate the stack in a generic
     *          manner and the precise effect depends on the type. E.g., the [[POP2]]
     *          instruction may just pop one ''categeory 2'' value (of type `long` or `double`)
     *          or two ''category 1'' values.
     */
    def numberOfPoppedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int

    /**
     * The number of values that are put onto the operand stack.
     *
     * @param ctg A function that returns the computational type category of
     *          the value on the operand stack with a given index. The top value on
     *          the operand stack has index '0' and may occupy one (for category 1 values)
     *          or two stack slots (for category 2 values.)
     * @note Several stack management instructions manipulate the stack in a generic
     *          manner and the precise effect depends on the type. E.g., the [[DUP2]]
     *          instruction may just duplicate one ''categeory 2'' value (of type long or double)
     *          or two ''category 1'' values.
     */
    def numberOfPushedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int

    /**
     * Returns `true` if this instruction reads/uses a local variable.
     */
    def readsLocal: Boolean

    @throws[UnsupportedOperationException]("thrown if no local variables are read")
    def indexOfReadLocal: Int

    /**
     * Returns `true` if this instruction writes/updates a local variable.
     */
    def writesLocal: Boolean

    @throws[UnsupportedOperationException]("thrown if no local variable is written")
    def indexOfWrittenLocal: Int

    /**
     * Returns a string representation of this instruction. If this instruction is a
     * (conditional) jump instruction then the PCs of the target instructions should
     * be given using absolute PCs. The string representation should be compact
     * and suitable for output on the console and should represent the instruction
     * in its entirety.
     *
     * @param currentPC The program counter of this instruction. Used to resolve relative
     *      jump targets.
     */
    def toString(currentPC: Int): String = toString()

}
/**
 * Functionality common to instructions.
 *
 * @author Michael Eichberg
 */
object Instruction {

    final val ILLEGAL_INDEX = -1

    def unapply(instruction: Instruction): Option[(Int, String, List[ObjectType])] = {
        Some((instruction.opcode, instruction.mnemonic, instruction.runtimeExceptions))
    }

    private[instructions] def nextInstructionOrExceptionHandlers(
        instruction: Instruction,
        currentPC: PC,
        code: Code,
        exceptions: List[ObjectType]): UShortSet /* <= mutable by purpose! */ = {

        var pcs = UShortSet(instruction.indexOfNextInstruction(currentPC, code))

        def processException(exception: ObjectType) {
            code.handlersFor(currentPC) find { handler ⇒
                handler.catchType.isEmpty ||
                    Code.preDefinedClassHierarchy.isSubtypeOf(
                        exception,
                        handler.catchType.get).isYes
            } match {
                case Some(handler) ⇒ handler.startPC +≈: pcs
                case _             ⇒ /* exception is not handled */
            }
        }

        exceptions foreach processException

        pcs
    }

    private[instructions] def nextInstructionOrExceptionHandler(
        instruction: Instruction,
        currentPC: PC,
        code: Code,
        exception: ObjectType): UShortSet /* <= mutable by purpose! */ = {

        val nextInstruction = instruction.indexOfNextInstruction(currentPC, code)

        code.handlersFor(currentPC) find { handler ⇒
            handler.catchType.isEmpty ||
                Code.preDefinedClassHierarchy.isSubtypeOf(
                    exception,
                    handler.catchType.get).isYes
        } match {
            case Some(handler) ⇒ UShortSet(nextInstruction, handler.startPC)
            case None          ⇒ UShortSet(nextInstruction)
        }
    }
}