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

import instructions._

/**
 * Defines the interface between the abstract interpreter and a module for
 * tracing the interpreter's behavior. In general, a tracer is first registered with an
 * abstract interpreter. After that, when a method is analyzed, BATAI calls the
 * tracer's methods at the respective points in time.
 *
 * A tracer is registered with an abstract interpreter by creating a new subclass of
 * `AI` and overriding the method `tracer`.
 *
 * @note In general, all mutable data structures (e.g. the current locals) passed
 *      to the tracer must not be mutated by it.
 *
 * @author Michael Eichberg
 */
trait AITracer {

    /**
     * Called by BATAI immediately before the (abstract) interpretation of the
     * specified code is performed.
     *
     * The tracer is not expected to make any changes to the data structures
     * (operandsArray and localsArray). If the tracer makes such changes, it is
     * the responsibility of the tracer to ensure that the updates are meaningful.
     * BATAI will not perform any checks.
     */
    def continuingInterpretation[D <: SomeDomain with Singleton](
        code: Code,
        domain: D,
        initialWorkList: List[PC],
        alreadyEvaluated: List[PC],
        operandsArray: Array[List[D#DomainValue]],
        localsArray: Array[Array[D#DomainValue]])

    /**
     * Called by BATAI before an instruction is evaluated.
     *
     * This enables the tracer to precisely log the behavior of the abstract
     * interpreter, but also enables the tracer to interrupt the evaluation
     * to, e.g., enable stepping through a program.
     *
     * @param operands The operand stack before the execution of the instruction.
     * @param locals The registers before the execution of the instruction. '''The Array
     *      must not be mutated.'''
     */
    def instructionEvalution[D <: SomeDomain with Singleton](
        domain: D,
        pc: PC,
        instruction: Instruction,
        operands: List[D#DomainValue],
        locals: Array[D#DomainValue]): Unit

    /**
     * Called by BATAI after an instruction (`currentPC`) was evaluated and before the
     * `targetPC` may be evaluated.
     *
     * This method is only called if the instruction with the program counter
     * `targetPC` will be evaluated. I.e., when the abstract interpreter
     * determines that the evaluation of an instruction does not change the abstract
     * state (associated with the successor instruction) and, therefore, will not
     * schedule the successor instruction this method is not called.
     *
     * In case of `if` or `switch` instructions `flow` may be
     * called multiple times before the method `instructionEvaluation` is called again.
     *
     * Recall that BATAI performs a depth-first exploration.
     */
    def flow[D <: SomeDomain with Singleton](
        domain: D,
        currentPC: PC,
        targetPC: PC): Unit

    /**
     * Called if the instruction with the targetPC was rescheduled. I.e., the
     * instruction was already scheduled for evaluation in the future, but was now
     * rescheduled for a more immediate evaluation. I.e., it was moved to the first
     * position in the list that contains the instructions that will be evaluated.
     * However, further instructions may be appended to the list before the
     * next `instructionEvaluation` takes place.
     *
     * Recall that BATAI performs a depth-first exploration.
     */
    def rescheduled[D <: SomeDomain with Singleton](
        domain: D,
        sourcePC: PC,
        targetPC: PC): Unit

    /**
     * Called by BATAI whenever two paths converge and the values on the operand stack
     * and the registers are joined.
     *
     * @param thisOperands The operand stack as it was used the last time when the
     * 		instruction with the given program counter was evaluated.
     * @param thisLocals The registers as they were used the last time when the
     * 		instruction with the given program counter was evaluated.
     * @param otherOperands The current operand stack when we re-reach the instruction
     * @param otherLocals The current registers.
     * @param result The result of joining the operand stacks and register
     * 		assignment.
     */
    def join[D <: SomeDomain with Singleton](
        domain: D,
        pc: PC,
        thisOperands: D#Operands,
        thisLocals: D#Locals,
        otherOperands: D#Operands,
        otherLocals: D#Locals,
        result: Update[(D#Operands, D#Locals)])

    /**
     * Called when the analyzed method throws an exception (i.e., the interpreter
     * evaluates an `athrow` instruction) that is not caught within
     * the method.
     */
    def abruptMethodExecution[D <: SomeDomain with Singleton](
        domain: D,
        pc: Int,
        exception: D#DomainValue)

    /**
     * Called when a `RET` instruction is encountered.
     */
    def ret[D <: SomeDomain with Singleton](
        domain: D,
        pc: PC,
        returnAddress: PC,
        oldWorklist: List[PC],
        newWorklist: List[PC])

    /**
     * Called when the evaluation of a subroutine (JSR/RET) is completed.
     */
    def returnFromSubroutine[D <: SomeDomain with Singleton](
        domain: D,
        pc: Int,
        returnAddress: Int,
        subroutineInstructions: List[Int])

    /**
     * Called by BATAI when the abstract interpretation of a method has completed/was
     * interrupted.
     */
    def result[D <: SomeDomain with Singleton](result: AIResult[D])
}
