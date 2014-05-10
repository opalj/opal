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
package ai

import instructions._

/**
 * A tracer that forwards every call to all registered tracers.
 *
 * @author Michael Eichberg
 */
class MultiTracer(val tracers: AITracer*) extends AITracer {

    override def continuingInterpretation(
        code: Code,
        domain: Domain)(
            initialWorkList: List[PC],
            alreadyEvaluated: List[PC],
            operandsArray: Array[List[domain.DomainValue]],
            localsArray: Array[Array[domain.DomainValue]]): Unit = {
        tracers foreach { tracer ⇒
            tracer.continuingInterpretation(code, domain)(
                initialWorkList, alreadyEvaluated, operandsArray, localsArray
            )
        }
    }

    override def instructionEvalution(
        domain: Domain)(
            pc: PC,
            instruction: Instruction,
            operands: List[domain.DomainValue],
            locals: Array[domain.DomainValue]): Unit = {
        tracers foreach { tracer ⇒
            tracer.instructionEvalution(domain)(pc, instruction, operands, locals)
        }
    }

    override def flow(
        domain: Domain)(
            currentPC: PC,
            targetPC: PC,
            isExceptionalControlFlow: Boolean): Unit = {
        tracers foreach { _.flow(domain)(currentPC, targetPC, isExceptionalControlFlow) }
    }

    override def rescheduled(
        domain: Domain)(
            sourcePC: PC,
            targetPC: PC,
            isExceptionalControlFlow: Boolean): Unit = {
        tracers foreach { _.rescheduled(domain)(sourcePC, targetPC, isExceptionalControlFlow) }

    }

    override def join(
        domain: Domain)(
            pc: PC,
            thisOperands: domain.Operands,
            thisLocals: domain.Locals,
            otherOperands: domain.Operands,
            otherLocals: domain.Locals,
            result: Update[(domain.Operands, domain.Locals)]): Unit = {
        tracers foreach { tracer ⇒
            tracer.join(domain)(
                pc, thisOperands, thisLocals, otherOperands, otherLocals, result
            )
        }
    }

    override def abruptMethodExecution(
        domain: Domain)(
            pc: Int,
            exception: domain.DomainValue): Unit = {
        tracers foreach { _.abruptMethodExecution(domain)(pc, exception) }
    }

    override def ret(
        domain: Domain)(
            pc: PC,
            returnAddress: PC,
            oldWorklist: List[PC],
            newWorklist: List[PC]): Unit = {
        tracers foreach { tracer ⇒
            tracer.ret(domain)(pc, returnAddress, oldWorklist, newWorklist)
        }
    }

    override def jumpToSubroutine(domain: Domain)(pc: PC): Unit = {
        tracers foreach { tracer ⇒
            tracer.jumpToSubroutine(domain)(pc)
        }
    }

    /**
     * Called when the evaluation of a subroutine (JSR/RET) is completed.
     */
    override def returnFromSubroutine(
        domain: Domain)(
            pc: Int,
            returnAddress: Int,
            subroutineInstructions: List[Int]): Unit = {
        tracers foreach { tracer ⇒
            tracer.returnFromSubroutine(domain)(
                pc, returnAddress, subroutineInstructions
            )
        }
    }

    override def result(result: AIResult): Unit = {
        tracers foreach { _.result(result) }
    }
}
