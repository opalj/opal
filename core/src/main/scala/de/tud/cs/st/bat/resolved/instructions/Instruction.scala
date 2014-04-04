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
package de.tud.cs.st
package bat
package resolved
package instructions

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
    def indexOfNextInstruction(currentPC: Int, code: Code): Int

    /**
     * Returns the set of instructions that may be executed next at runtime. This
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

    def unapply(instruction: Instruction): Option[(Int, String, List[ObjectType])] = {
        Some((instruction.opcode, instruction.mnemonic, instruction.runtimeExceptions))
    }

    import collection.mutable.UShortSet

    private[instructions] def nextInstructionOrExceptionHandlers(
        instruction: Instruction,
        currentPC: PC,
        code: Code,
        exceptions: List[ObjectType]): UShortSet /* <= mutable by purpose! */ = {

        var pcs = UShortSet(instruction.indexOfNextInstruction(currentPC, code))

        def processException(exception: ObjectType) {
            code.exceptionHandlersFor(currentPC) find { handler ⇒
                handler.catchType.isEmpty ||
                    Code.preDefinedClassHierarchy.isSubtypeOf(
                        exception,
                        handler.catchType.get).isYes
            } match {
                case Some(handler) ⇒ pcs +≈ handler.startPC
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

        code.exceptionHandlersFor(currentPC) find { handler ⇒
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