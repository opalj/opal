/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

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
        tracers foreach { tracer => tracer.initialLocals(domain)(locals) }
    }

    override def continuingInterpretation(
        code:   Code,
        domain: Domain
    )(
        initialWorkList:                  List[Int /*PC*/ ],
        alreadyEvaluatedPCs:              IntArrayStack,
        operandsArray:                    domain.OperandsArray,
        localsArray:                      domain.LocalsArray,
        memoryLayoutBeforeSubroutineCall: List[(Int /*PC*/ , domain.OperandsArray, domain.LocalsArray)]
    ): Unit = {
        tracers foreach { tracer =>
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
        tracers foreach { tracer =>
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
        tracers foreach { tracer =>
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
        tracers foreach { tracer =>
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
        tracers foreach { tracer =>
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
        pc: Int, returnAddress: Int, subroutinePCs: List[Int]
    ): Unit = {
        tracers foreach { tracer =>
            tracer.returnFromSubroutine(domain)(pc, returnAddress, subroutinePCs)
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
        tracers foreach { tracer =>
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
        tracers foreach { tracer =>
            tracer.establishedConstraint(domain)(
                pc, effectivePC, operands, locals, newOperands, newLocals
            )
        }
    }

    override def domainMessage(
        domain: Domain,
        source: Class[_], typeID: String,
        pc: Option[Int /*PC*/ ], message: => String
    ): Unit = {
        tracers foreach { tracer =>
            tracer.domainMessage(domain, source, typeID, pc, message)
        }
    }
}
