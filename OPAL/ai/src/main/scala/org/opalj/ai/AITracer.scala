/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.br.Code
import org.opalj.br.instructions.Instruction
import org.opalj.collection.mutable.IntArrayStack

/**
 * Defines the interface between the abstract interpreter and a module for tracing and
 * debugging the interpreter's progress. In general, a tracer is first registered with an
 * abstract interpreter. After that, when a method is analyzed, the [[AI]] calls the
 * tracer's methods at the respective points in time.
 *
 * A tracer is registered with an abstract interpreter by creating a new subclass of
 * [[AI]] and overriding the method [[AI.tracer]].
 *
 * @note '''All data structures passed to the tracer are the original data structures
 *      used by the abstract interpreter.''' Hence, if a value is mutated (e.g., for
 *      debugging purposes) it has to be guaranteed that the state remains meaningful.
 *      Hence, using the [[AITracer]] it is possible to develop a debugger for OPAL and
 *      to enable the user to perform certain mutations.
 *
 * @author Michael Eichberg
 */
trait AITracer {

    /**
     * The set of initial locals computed when the method is interpreted for the first time.
     */
    def initialLocals(domain: Domain)(locals: domain.Locals): Unit

    /**
     * Called immediately before the abstract interpretation of the
     * specified code is performed.
     *
     * If the tracer changes the `operandsArray` and/or `localsArray`, it is
     * the responsibility of the tracer to ensure that the data structures are still
     * valid afterwards.
     */
    def continuingInterpretation(
        code:   Code,
        domain: Domain
    )(
        initialWorkList:                  List[Int /*PC*/ ],
        alreadyEvaluatedPCs:              IntArrayStack,
        operandsArray:                    domain.OperandsArray,
        localsArray:                      domain.LocalsArray,
        memoryLayoutBeforeSubroutineCall: List[(Int /*PC*/ , domain.OperandsArray, domain.LocalsArray)]
    ): Unit

    /**
     * Called before an instruction is evaluated.
     *
     * This enables the tracer to precisely log the behavior of the abstract
     * interpreter, but also enables the tracer to interrupt the evaluation
     * to, e.g., enable stepping through a program.
     *
     * @param operands The operand stack before the execution of the instruction.
     * @param locals The registers before the execution of the instruction.
     */
    def instructionEvalution(
        domain: Domain
    )(
        pc:          Int,
        instruction: Instruction,
        operands:    domain.Operands,
        locals:      domain.Locals
    ): Unit

    /**
     * Called by the interpreter after an instruction (`currentPC`) was evaluated and
     * before the instruction with the program counter `targetPC` may be evaluated.
     *
     * This method is only called if the instruction with the program counter
     * `targetPC` will be evaluated in the future and was not yet scheduled.
     * I.e., when the abstract interpreter
     * determines that the evaluation of an instruction does not change the abstract
     * state (associated with the successor instruction) and, therefore, will not
     * schedule the successor instruction this method is not called.
     *
     * In case of `if` or `switch` instructions `flow` may be
     * called multiple times (even with the same targetPC) before the method
     * `instructionEvaluation` is called again.
     *
     * @note OPAL performs a depth-first exploration. However, subroutines are always
     *      first finished analyzing before an exception handler - that handles abrupt
     *      executions of the subroutine - is evaluated.
     */
    def flow(
        domain: Domain
    )(
        currentPC:                Int,
        targetPC:                 Int,
        isExceptionalControlFlow: Boolean
    ): Unit

    /**
     * Called by the interpret when a local variable with the given index (`lvIndex`)
     * was set to a new value and, therefore, the reference stored in the local variable
     * previously was useless/dead.
     */
    def deadLocalVariable(domain: Domain)(pc: Int, lvIndex: Int): Unit

    /**
     * Called by the interpreter if a successor instruction is NOT scheduled, because
     * the abstract state didn't change.
     */
    def noFlow(
        domain: Domain
    )(
        currentPC: Int,
        targetPC:  Int
    ): Unit

    /**
     * Called if the instruction with the `targetPC` was already scheduled. I.e., the
     * instruction was already scheduled for evaluation, but is now moved to the first
     * position in the list of all instructions to be executed (related to the specific
     * subroutine). '''A rescheduled event is also issued if the instruction was the
     * the first in the list of instructions executed next.'''
     * However, further instructions may be appended to the list before the
     * next `instructionEvaluation` takes place.
     *
     * @note OPAL performs a depth-first exploration.
     */
    def rescheduled(
        domain: Domain
    )(
        sourcePC:                 Int,
        targetPC:                 Int,
        isExceptionalControlFlow: Boolean,
        worklist:                 List[Int /*PC*/ ]
    ): Unit

    /**
     * Called by the abstract interpreter whenever two paths converge and the values
     * on the operand stack and the registers are joined.
     *
     * @param thisOperands The operand stack as it was used the last time when the
     *      instruction with the given program counter was evaluated.
     * @param thisLocals The registers as they were used the last time when the
     *      instruction with the given program counter was evaluated.
     * @param otherOperands The current operand stack when we re-reach the instruction
     * @param otherLocals The current registers.
     * @param result The result of joining the operand stacks and register
     *      assignment.
     */
    def join(
        domain: Domain
    )(
        pc:            Int,
        thisOperands:  domain.Operands,
        thisLocals:    domain.Locals,
        otherOperands: domain.Operands,
        otherLocals:   domain.Locals,
        result:        Update[(domain.Operands, domain.Locals)]
    ): Unit

    /**
     * Called before a jump to a subroutine.
     */
    def jumpToSubroutine(domain: Domain)(pc: Int, target: Int, nestingLevel: Int): Unit

    /**
     * Called when a `RET` instruction is encountered. (That does not necessary imply
     * that the evaluation of the subroutine as such has finished. It is possible
     * that other paths still need to be pursued.)
     */
    def ret(
        domain: Domain
    )(
        pc:              Int,
        returnAddressPC: Int,
        oldWorklist:     List[Int /*PC*/ ],
        newWorklist:     List[Int /*PC*/ ]
    ): Unit

    /**
     * Called when the evaluation of a subroutine (JSR/RET) as a whole is completed.
     * I.e., all possible paths are analyzed and the fixpoint is reached.
     */
    def returnFromSubroutine(
        domain: Domain
    )(
        pc:              Int,
        returnAddressPC: Int,
        subroutinePCs:   List[Int /*PC*/ ]
    ): Unit

    /**
     * Called when the evaluation of a subroutine terminated abruptly due to an unhandled
     * exception.
     *
     * @param jumpToSubroutineId The subroutine that will be continued. The id is the pc
     *      of the first instruction of the subroutine. It is 0 if it is the method
     *      as such.
     * @param terminatedSubroutinesCount The number of subroutines that are terminated.
     */
    def abruptSubroutineTermination(
        domain: Domain
    )(
        details:  String,
        sourcePC: Int, targetPC: Int,
        jumpToSubroutineId:         Int,
        terminatedSubroutinesCount: Int,
        forceScheduling:            Boolean,
        oldWorklist:                List[Int /*PC*/ ],
        newWorklist:                List[Int /*PC*/ ]
    ): Unit

    /**
     * Called when the analyzed method throws an exception that is not caught within
     * the method. I.e., the interpreter evaluates an `athrow` instruction or some
     * other instruction that throws an exception.
     */
    def abruptMethodExecution(
        domain: Domain
    )(
        pc:        Int,
        exception: domain.ExceptionValue
    ): Unit

    /**
     * Called when the abstract interpretation of a method has completed/was
     * interrupted.
     */
    def result(result: AIResult): Unit

    /**
     * Called by the framework if a constraint is established. Constraints are generally
     * established whenever a conditional jump is performed and the
     * evaluation of the condition wasn't definitive. In this case a constraint will
     * be established for each branch. In general the constraint will be applied
     * before the join of the stack and locals with the successor instruction is done.
     */
    def establishedConstraint(
        domain: Domain
    )(
        pc:          Int,
        effectivePC: Int,
        operands:    domain.Operands,
        locals:      domain.Locals,
        newOperands: domain.Operands,
        newLocals:   domain.Locals
    ): Unit

    /**
     * Called by the domain if something noteworthy was determined.
     *
     * @param  domain The domain.
     * @param  source The class (typically the (partial) domain) that generated the message.
     * @param  typeID A `String` that identifies the message. This value must not be `null`,
     *         but it can be the empty string.
     * @param  message The message; a non-null `String` that is formatted for the console.
     */
    def domainMessage(
        domain: Domain,
        source: Class[_], typeID: String,
        pc: Option[Int /*PC*/ ], message: => String // IMPROVE Use IntOption
    ): Unit

}
