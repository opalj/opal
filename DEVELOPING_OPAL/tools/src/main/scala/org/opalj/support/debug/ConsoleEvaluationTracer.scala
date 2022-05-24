/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.debug

import org.opalj.br.Code
import org.opalj.br.instructions.Instruction
import org.opalj.ai.AITracer
import org.opalj.ai.AIResult
import org.opalj.ai.Domain
import org.opalj.ai.Update
import org.opalj.collection.mutable.IntArrayStack

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
    private[this] def printIndent(): Unit = { (0 until indent) foreach (i => print("\t")) }

    def reset(): Unit = { indent = 0 }

    override def instructionEvalution(
        domain: Domain
    )(
        pc:          Int,
        instruction: Instruction,
        operands:    domain.Operands,
        locals:      domain.Locals
    ): Unit = {
        print(s"$pc ")
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
        /*EMPTY*/
    }

    override def rescheduled(
        domain: Domain
    )(
        sourcePC:                 Int,
        targetPC:                 Int,
        isExceptionalControlFlow: Boolean,
        worklist:                 List[Int /*PC*/ ]
    ): Unit = { /*EMPTY*/ }

    override def flow(
        domain: Domain
    )(
        currentPC:                Int,
        targetPC:                 Int,
        isExceptionalControlFlow: Boolean
    ): Unit = { /*EMPTY*/ }

    override def noFlow(domain: Domain)(currentPC: Int, targetPC: Int): Unit = { /*EMPTY*/ }

    override def join(
        domain: Domain
    )(
        pc:            Int,
        thisOperands:  domain.Operands,
        thisLocals:    domain.Locals,
        otherOperands: domain.Operands,
        otherLocals:   domain.Locals,
        result:        Update[(domain.Operands, domain.Locals)]
    ): Unit = { /*EMPTY*/ }

    override def abruptMethodExecution(
        domain: Domain
    )(
        pc:        Int,
        exception: domain.ExceptionValue
    ): Unit = { /*EMPTY*/ }

    override def jumpToSubroutine(
        domain: Domain
    )(
        pc: Int, targetPC: Int, nestingLevel: Int
    ): Unit = {
        println()
        printIndent()
        print(BOLD+"↳\t︎"+RESET)
        indent += 1
    }

    override def returnFromSubroutine(
        domain: Domain
    )(
        pc:              Int,
        returnAddressPC: Int,
        subroutinePCs:   List[Int /*PC*/ ]
    ): Unit = {
        indent -= 1

        println(BOLD+"✓"+"(Resetting: "+subroutinePCs.mkString(", ")+")"+RESET)
        printIndent()
    }

    def abruptSubroutineTermination(
        domain: Domain
    )(
        details:  String,
        sourcePC: Int, targetPC: Int, jumpToSubroutineId: Int,
        terminatedSubroutinesCount: Int,
        forceScheduling:            Boolean,
        oldWorklist:                List[Int /*PC*/ ],
        newWorklist:                List[Int /*PC*/ ]
    ): Unit = { /* EMPTY */ }

    override def ret(
        domain: Domain
    )(
        pc:              Int,
        returnAddressPC: Int,
        oldWorklist:     List[Int /*PC*/ ],
        newWorklist:     List[Int /*PC*/ ]
    ): Unit = { /*EMPTY*/ }

    override def establishedConstraint(
        domain: Domain
    )(
        pc:          Int,
        effectivePC: Int,
        operands:    domain.Operands,
        locals:      domain.Locals,
        newOperands: domain.Operands,
        newLocals:   domain.Locals
    ): Unit = { /*EMPTY*/ }

    override def result(result: AIResult): Unit = { /*EMPTY*/ }

    override def domainMessage(
        domain: Domain,
        source: Class[_], typeID: String,
        pc: Option[Int], message: => String
    ): Unit = { /*EMPTY*/ }

    override def deadLocalVariable(domain: Domain)(pc: Int, lvIndex: Int): Unit = { /*EMPTY*/ }

    override def initialLocals(domain: Domain)(locals: domain.Locals): Unit = { /*EMPTY*/ }
}
