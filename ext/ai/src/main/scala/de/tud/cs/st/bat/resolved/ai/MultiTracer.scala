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
 *
 * @author Michael Eichberg
 */
class MultiTracer(val tracers: AITracer*) extends AITracer {

    def instructionEvalution[D <: SomeDomain](
        domain: D,
        pc: PC,
        instruction: Instruction,
        operands: List[D#DomainValue],
        locals: Array[D#DomainValue]) {
        tracers.foreach(
            _.instructionEvalution(
                domain,
                pc,
                instruction,
                operands,
                locals))
    }

    def flow(currentPC: PC, targetPC: PC) {
        tracers.foreach(_.flow(currentPC, targetPC))
    }

    def join[D <: SomeDomain](
        domain: D,
        pc: PC,
        thisOperands: D#Operands,
        thisLocals: D#Locals,
        otherOperands: D#Operands,
        otherLocals: D#Locals,
        result: Update[(D#Operands, D#Locals)],
        forcedContinuation: Boolean) {
        tracers.foreach(
            _.join[D](
                domain,
                pc,
                thisOperands,
                thisLocals,
                otherOperands,
                otherLocals,
                result,
                forcedContinuation)
        )
    }

    def abruptMethodExecution[D <: SomeDomain](
        domain: D,
        pc: Int,
        exception: D#DomainValue) {
        tracers.foreach(_.abruptMethodExecution(domain, pc, exception))
    }

    def ret[D <: SomeDomain](
        domain: D,
        pc: PC,
        returnAddress: PC,
        oldWorklist: List[PC],
        newWorklist: List[PC]) {
        tracers.foreach(
            _.ret(
                domain,
                pc,
                returnAddress,
                oldWorklist,
                newWorklist))
    }

    /**
     * Called when the evaluation of a subroutine (JSR/RET) is completed.
     */
    def returnFromSubroutine[D <: SomeDomain](
        domain: D,
        pc: Int,
        returnAddress: Int,
        subroutineInstructions: List[Int]) {
        tracers.foreach(
            _.returnFromSubroutine(
                domain,
                pc,
                returnAddress,
                subroutineInstructions))
    }

    def result[D <: SomeDomain](result: AIResult[D]) {
        tracers.foreach(_.result(result))
    }
}
