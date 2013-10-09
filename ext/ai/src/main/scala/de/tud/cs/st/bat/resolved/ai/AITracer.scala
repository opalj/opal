/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package ai

/**
 * Defines the interface between the abstract interpreter and a module for
 * tracing the interpreter's behavior. In general, a tracer is first registered with an
 * abstract interpreter. After that, when a method is analyzed, BATAI calls the
 * tracer's methods at the respective point in time.
 *
 * @note In general, all mutable data structures (.e.g. the current locals) passed
 * 		to the tracer must not be mutated by it.
 *
 * @author Michael Eichberg
 */
trait AITracer {

    /**
     * Called by BATAI before an instruction is evaluated.
     *
     * This enables the tracer to precisely log the behavior of the abstract
     * interpreter, but also enables the tracer to interrupt the evaluation
     * to, e.g., enable stepping through a program.
     *
     * @param operands The operand stack before the execution of the instruction.
     * @param locals The registers before the execution of the instruction. '''The Array
     * 		must not be mutated.'''
     */
    def instructionEvalution[D <: Domain[_]](
        domain: D,
        pc: Int,
        instruction: Instruction,
        operands: List[D#DomainValue],
        locals: Array[D#DomainValue]): Unit

    /**
     * Called by BATAI whenever two paths converge and the values on the operand stack
     * and the registers are merged.
     *
     * @param thisOperands The operand stack as it was used the last time when the
     * 		instruction with the given program counter was evaluated.
     * @param thisLocals The registers as they were used the last time when the
     * 		instruction with the given program counter was evaluated.
     * @param otherOperands The current operand stack when we re-reach the instruction
     *
     * @param otherLocals The current registers.
     *
     * @param result The result of merging the operand stacks and register
     * 		assignment.
     */
    def merge[D <: Domain[_]](
        domain: D,
        pc: Int,
        thisOperands: D#Operands,
        thisLocals: D#Locals,
        otherOperands: D#Operands,
        otherLocals: D#Locals,
        result: Update[(D#Operands, D#Locals)],
        forcedContinuation: Boolean)

    /**
     * Called when the analyzed method throws an exception that is not caught within
     * the method.
     */
    def abruptMethodExecution[D <: Domain[_]](
        domain: D,
        pc: Int,
        exception: D#DomainValue)

    /**
     * Called when the evaluation of a subroutine (JSR/RET) is completed.
     */
    def returnFromSubroutine[D <: Domain[_]](
        domain: D,
        pc: Int,
        returnAddress: Int,
        subroutineInstructions: List[Int])
}
