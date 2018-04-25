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
package ai

import org.opalj.collection.immutable.{Chain ⇒ List}
import org.opalj.br.Code
import org.opalj.br.instructions.Instruction
import org.opalj.collection.mutable.IntArrayStack

/**
 * A tracer that forwards every call to all registered tracers.
 *
 * @author Michael Eichberg
 */
class MultiTracer(val tracers: AITracer*) extends AITracer {

    override def initialLocals(domain: Domain)(locals: domain.Locals): Unit = {
        tracers foreach { tracer ⇒ tracer.initialLocals(domain)(locals) }
    }

    override def continuingInterpretation(
        code:   Code,
        domain: Domain
    )(
        initialWorkList:                  List[Int /*PC*/ ],
        alreadyEvaluatedPCs:              IntArrayStack,
        operandsArray:                    domain.OperandsArray,
        localsArray:                      domain.LocalsArray,
        memoryLayoutBeforeSubroutineCall: List[(PC, domain.OperandsArray, domain.LocalsArray)]
    ): Unit = {
        tracers foreach { tracer ⇒
            tracer.continuingInterpretation(code, domain)(
                initialWorkList, alreadyEvaluatedPCs,
                operandsArray, localsArray, memoryLayoutBeforeSubroutineCall
            )
        }
    }

    override def instructionEvalution(
        domain: Domain
    )(
        pc:          Int,
        instruction: Instruction,
        operands:    domain.Operands,
        locals:      domain.Locals
    ): Unit = {
        tracers foreach { tracer ⇒
            tracer.instructionEvalution(domain)(pc, instruction, operands, locals)
        }
    }

    override def flow(
        domain: Domain
    )(
        currentPC:                Int,
        targetPC:                 Int,
        isExceptionalControlFlow: Boolean
    ): Unit = {
        tracers foreach { _.flow(domain)(currentPC, targetPC, isExceptionalControlFlow) }
    }

    override def deadLocalVariable(domain: Domain)(pc: Int, lvIndex: Int): Unit = {
        tracers foreach { _.deadLocalVariable(domain)(pc, lvIndex) }
    }

    override def noFlow(
        domain: Domain
    )(
        currentPC: Int, targetPC: Int
    ): Unit = {
        tracers foreach { _.noFlow(domain)(currentPC, targetPC) }
    }

    override def rescheduled(
        domain: Domain
    )(
        sourcePC: Int, targetPC: Int, isExceptionalControlFlow: Boolean, worklist: List[Int /*PC*/ ]
    ): Unit = {
        tracers foreach { tracer ⇒
            tracer.rescheduled(domain)(sourcePC, targetPC, isExceptionalControlFlow, worklist)
        }
    }

    override def join(
        domain: Domain
    )(
        pc:            Int,
        thisOperands:  domain.Operands,
        thisLocals:    domain.Locals,
        otherOperands: domain.Operands,
        otherLocals:   domain.Locals,
        result:        Update[(domain.Operands, domain.Locals)]
    ): Unit = {
        tracers foreach { tracer ⇒
            tracer.join(
                domain
            )(
                pc, thisOperands, thisLocals, otherOperands, otherLocals, result
            )
        }
    }

    override def abruptMethodExecution(
        domain: Domain
    )(
        pc: Int, exception: domain.ExceptionValue
    ): Unit = {
        tracers foreach { _.abruptMethodExecution(domain)(pc, exception) }
    }

    override def ret(
        domain: Domain
    )(
        pc:              Int,
        returnAddressPC: Int,
        oldWorklist:     List[Int /*PC*/ ],
        newWorklist:     List[Int /*PC*/ ]
    ): Unit = {
        tracers foreach { tracer ⇒
            tracer.ret(domain)(pc, returnAddressPC, oldWorklist, newWorklist)
        }
    }

    override def jumpToSubroutine(
        domain: Domain
    )(
        pc: Int, target: Int, nestingLevel: Int
    ): Unit = {
        tracers foreach { _.jumpToSubroutine(domain)(pc, target, nestingLevel) }
    }

    override def returnFromSubroutine(
        domain: Domain
    )(
        pc: Int, returnAddress: Int, subroutineInstructions: List[Int]
    ): Unit = {
        tracers foreach { tracer ⇒
            tracer.returnFromSubroutine(domain)(pc, returnAddress, subroutineInstructions)
        }
    }

    override def abruptSubroutineTermination(
        domain: Domain
    )(
        details:  String,
        sourcePC: Int, targetPC: Int, jumpToSubroutineId: Int,
        terminatedSubroutinesCount: Int,
        forceScheduling:            Boolean,
        oldWorklist:                List[Int /*PC*/ ],
        newWorklist:                List[Int /*PC*/ ]
    ): Unit = {
        tracers foreach { tracer ⇒
            tracer.abruptSubroutineTermination(domain)(
                details,
                sourcePC, targetPC,
                jumpToSubroutineId, terminatedSubroutinesCount,
                forceScheduling,
                oldWorklist, newWorklist
            )
        }
    }

    override def result(result: AIResult): Unit = {
        tracers foreach { _.result(result) }
    }

    override def establishedConstraint(
        domain: Domain
    )(
        pc:          Int,
        effectivePC: Int,
        operands:    domain.Operands,
        locals:      domain.Locals,
        newOperands: domain.Operands,
        newLocals:   domain.Locals
    ): Unit = {
        tracers foreach { tracer ⇒
            tracer.establishedConstraint(domain)(
                pc, effectivePC, operands, locals, newOperands, newLocals
            )
        }
    }

    override def domainMessage(
        domain: Domain,
        source: Class[_], typeID: String,
        pc: Option[Int /*PC*/ ], message: ⇒ String
    ): Unit = {
        tracers foreach { tracer ⇒
            tracer.domainMessage(domain, source, typeID, pc, message)
        }
    }
}
