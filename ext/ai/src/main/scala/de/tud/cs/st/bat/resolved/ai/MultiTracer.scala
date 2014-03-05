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
 * A tracer that forwards every call to all registered tracers.
 *
 * @author Michael Eichberg
 */
class MultiTracer(val tracers: AITracer*) extends AITracer {

    override def continuingInterpretation(
        code: Code,
        domain: SomeDomain)(
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
        domain: SomeDomain)(
            pc: PC,
            instruction: Instruction,
            operands: List[domain.DomainValue],
            locals: Array[domain.DomainValue]): Unit = {
        tracers foreach { tracer ⇒
            tracer.instructionEvalution(domain)(pc, instruction, operands, locals)
        }
    }

    override def flow(
        domain: SomeDomain)(
            currentPC: PC,
            targetPC: PC): Unit = {
        tracers foreach { _.flow(domain)(currentPC, targetPC) }
    }

    override def rescheduled(
        domain: SomeDomain)(
            sourcePC: PC,
            targetPC: PC): Unit = {
        tracers foreach { _.rescheduled(domain)(sourcePC, targetPC) }
    }

    override def join(
        domain: SomeDomain)(
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
        domain: SomeDomain)(
            pc: Int,
            exception: domain.DomainValue): Unit = {
        tracers foreach { _.abruptMethodExecution(domain)(pc, exception) }
    }

    override def ret(
        domain: SomeDomain)(
            pc: PC,
            returnAddress: PC,
            oldWorklist: List[PC],
            newWorklist: List[PC]): Unit = {
        tracers foreach { tracer ⇒
            tracer.ret(domain)(pc, returnAddress, oldWorklist, newWorklist)
        }
    }

    override def jumpToSubroutine(domain: SomeDomain)(pc: PC): Unit = {
        tracers foreach { tracer ⇒
            tracer.jumpToSubroutine(domain)(pc)
        }
    }

    /**
     * Called when the evaluation of a subroutine (JSR/RET) is completed.
     */
    override def returnFromSubroutine(
        domain: SomeDomain)(
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
