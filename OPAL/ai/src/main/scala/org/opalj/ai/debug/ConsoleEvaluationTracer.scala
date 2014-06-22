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
package ai
package debug

import br.Code
import br.instructions.Instruction

/**
 * A tracer that primarily prints out the evaluation order of the instructions on the
 * console. This tracer is particularly useful to understand the handling of JSR/RET
 * instructions.
 *
 * If you want to reuse this tracer to trace evaluations of different methods
 * you should call the `reset` method between two calls.
 *
 * ==Thread Safety==
 * This tracer has internal state that is dependent on the state of the evaluation.
 * Hence, '''this class is not thread safe and a new `AI` instance should be used per
 * method that is analyzed'''.
 *
 * @author Michael Eichberg
 */
trait ConsoleEvaluationTracer extends AITracer {

    import Console._

    private[this] var indent = 0
    private[this] def printIndent() { (0 until indent) foreach (i ⇒ print("\t")) }

    def reset(): Unit = { indent = 0 }

    override def instructionEvalution(
        domain: Domain)(
            pc: PC,
            instruction: Instruction,
            operands: domain.Operands,
            locals: domain.Locals): Unit = {
        print(pc+" ")
    }

    override def continuingInterpretation(
        code: Code,
        domain: Domain)(
            initialWorkList: List[PC],
            alreadyEvaluated: List[PC],
            operandsArray: domain.OperandsArray,
            localsArray: domain.LocalsArray,
            memoryLayoutBeforeSubroutineCall: List[(domain.OperandsArray, domain.LocalsArray)]): Unit = {
        /*EMPTY*/
    }

    override def rescheduled(
        domain: Domain)(
            sourcePC: PC,
            targetPC: PC,
            isExceptionalControlFlow: Boolean): Unit = { /*EMPTY*/ }

    override def flow(
        domain: Domain)(
            currentPC: PC,
            targetPC: PC,
            isExceptionalControlFlow: Boolean): Unit = { /*EMPTY*/ }

    override def noFlow(domain: Domain)(currentPC: PC, targetPC: PC): Unit = { /*EMPTY*/ }

    override def join(
        domain: Domain)(
            pc: PC,
            thisOperands: domain.Operands,
            thisLocals: domain.Locals,
            otherOperands: domain.Operands,
            otherLocals: domain.Locals,
            result: Update[(domain.Operands, domain.Locals)]): Unit = { /*EMPTY*/ }

    override def abruptMethodExecution(
        domain: Domain)(
            pc: Int,
            exception: domain.DomainValue): Unit = { /*EMPTY*/ }

    override def jumpToSubroutine(
        domain: Domain)(
            pc: PC, target: PC, nestingLevel: Int): Unit = {
        println
        printIndent
        print(BOLD+"↳\t︎"+RESET)
        indent += 1
    }

    override def returnFromSubroutine(
        domain: Domain)(
            pc: PC,
            returnAddress: PC,
            subroutineInstructions: List[PC]): Unit = {
        indent -= 1

        println(BOLD+"✓"+"(Resetting: "+subroutineInstructions.mkString(", ")+")"+RESET)
        printIndent
    }

    override def ret(
        domain: Domain)(
            pc: PC,
            returnAddress: PC,
            oldWorklist: List[PC],
            newWorklist: List[PC]): Unit = { /*EMPTY*/ }

    override def establishedConstraint(
        domain: Domain)(
            pc: PC,
            effectivePC: PC,
            operands: domain.Operands,
            locals: domain.Locals,
            newOperands: domain.Operands,
            newLocals: domain.Locals): Unit = { /*EMPTY*/ }

    override def result(result: AIResult): Unit = { /*EMPTY*/ }
}
