/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.br.instructions.Instruction
import org.opalj.ai.util.containsInPrefix
import org.opalj.collection.mutable.IntArrayStack

/**
 * Defines the core functionality that is shared across all [[Domain]]s that implement
 * the operations related to different kinds of values and instructions. It primarily
 * defines the abstraction for DomainValues.
 *
 * @note    This trait defines concrete methods that facilitate unit testing of
 *          partial domains that build on top of this `CoreDomain` such as the
 *          [[IntegerValuesDomain]].
 *
 * @see     [[Domain]] For an explanation of the underlying concepts and ideas.
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
trait CoreDomainFunctionality extends ValuesDomain with SubroutinesDomain { coreDomain =>

    /**
     * Replaces all occurrences of `oldValue` (using reference-quality) with `newValue`. If no
     * occurrences are found, the original operands and locals data structures
     * are returned.
     */
    def updateMemoryLayout(
        oldValue: DomainValue,
        newValue: DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {
        (
            operands.mapConserve(o => if (o eq oldValue) newValue else o),
            locals.mapConserve(l => if (l eq oldValue) newValue else l)
        )
    }

    /**
     * This methods is called after the evaluation of the instruction with
     * the given `pc` with respect to `targetPC`, but before the values are propagated
     * (joined) and before it is checked whether the interpretation needs to be continued.
     * I.e., if the operands (`newOperands`) or locals (`newLocals`) are further refined
     * then the refined operands and locals are joined (if necessary).
     *
     * @note During the evaluation of the instruction it is possible that this method
     *      is called multiple times with different `targetPC`s. The latter is not only
     *      true for control flow instructions, but also for those instructions
     *      that may raise an exception.
     *
     * This method can and is intended to be overridden to further refine the operand
     * stack/the locals. However, the overriding method should always forward the (possibly
     * refined) operands and locals to the `super` method (`stackable traits`).
     */
    def afterEvaluation(
        pc:                       Int,
        instruction:              Instruction,
        oldOperands:              Operands,
        oldLocals:                Locals,
        targetPC:                 Int,
        isExceptionalControlFlow: Boolean,
        forceJoin:                Boolean,
        newOperands:              Operands,
        newLocals:                Locals
    ): (Operands, Locals) = (newOperands, newLocals)

    /**
     * In case of a mutable value, we need to distinguish several cases:
     *  1.   the current instruction has (at most) one successor (including
     *       potential exception handlers!). In this case, the current domain
     *       value which represents the mutable reference value can just be
     *       used as is; operations that mutate it can directly performed.
     *  1.   the current instruction has multiple successors, none of the
     *       successors is already evaluated. In this case, it is sufficient
     *       to clone the domain value to ensure that the value can be
     *       independently mutated on each branch.
     *  1.   the current instruction has multiple successors, some of the
     *       successors were already evaluated and some were not; all
     *       values refer to the same instance.
     *  1.   the current instruction has multiple successors; some of the
     *       successors were already evaluated and are related to a
     *       different instance. In this case - to ensure termination -
     *       we create an abstract representation of the values that
     *       abstract over the common properties.
     * .... TODO describe
     */

    /**
     * Joins the given operand stacks and local variables.
     *
     * ''In general there should be no need to refine this method.'' Overriding this
     * method should only be done for analysis purposes.
     *
     * ==Performance==
     * This method heavily relies on reference comparisons to speed up the overall
     * process of performing an abstract interpretation of a method. Hence,
     * a computation should – whenever possible – return (one of) the original object(s) if
     * that value has the same abstract state as the result. Furthermore, if all original
     * values capture the same abstract state as the result of the computation, the "left"
     * value/the value that was already used in the past should be returned.
     *
     * @return  The joined operand stack and registers.
     *          Returns `NoUpdate` if ''this'' memory layout already subsumes the
     *          ''other'' memory layout.
     * @note    The size of the operands stacks that are to be joined and the number of
     *          registers/locals that are to be joined can be expected to be identical
     *          under the assumption that the bytecode is valid and the framework contains no
     *          bugs.
     * @note    The operand stacks are guaranteed to contain compatible values w.r.t. the
     *          computational type (unless the bytecode is not valid or OPAL contains
     *          an error). I.e., if the result of joining two operand stack values is an
     *           `IllegalValue` we assume that the domain implementation is incorrect.
     *          However, the joining of two register values can result in an illegal value -
     *          which identifies the value as being dead.
     */
    def join(
        pc:            Int,
        thisOperands:  Operands,
        thisLocals:    Locals,
        otherOperands: Operands,
        otherLocals:   Locals
    ): Update[(Operands, Locals)] = {
        beforeBaseJoin(pc)

        var operandsUpdated: UpdateType = NoUpdateType
        val newOperands: Operands =
            if (thisOperands eq otherOperands) {
                thisOperands
            } else {
                def fuseOperands(thisValue: DomainValue, otherValue: DomainValue): DomainValue = {
                    if (thisValue eq otherValue)
                        thisValue
                    else {
                        val updatedOperand = joinValues(pc, thisValue, otherValue)
                        if (updatedOperand eq NoUpdate) {
                            thisValue
                        } else {
                            operandsUpdated &:= updatedOperand
                            updatedOperand.value
                        }
                    }
                }
                thisOperands.zip(otherOperands).map { case (o1, o2) => fuseOperands(o1, o2) }
            }

        var localsUpdated: UpdateType = NoUpdateType
        val newLocals: Locals =
            if (thisLocals eq otherLocals) {
                thisLocals
            } else {
                def fuseLocals(thisValue: DomainValue, otherValue: DomainValue) = {
                    if ((thisValue eq null) || (otherValue eq null)) {
                        localsUpdated &:= MetaInformationUpdateType
                        TheIllegalValue
                    } else {
                        val updatedLocal = joinValues(pc, thisValue, otherValue)
                        if (updatedLocal eq NoUpdate) {
                            thisValue
                        } else {
                            localsUpdated &:= updatedLocal
                            val value = updatedLocal.value
                            value
                        }
                    }
                }
                val newLocals = thisLocals.fuse(otherLocals, fuseLocals)
                if (localsUpdated.noUpdate)
                    thisLocals
                else
                    newLocals
            }
        val updateType = operandsUpdated &: localsUpdated

        afterBaseJoin(pc)

        joinPostProcessing(updateType, pc, thisOperands, thisLocals, newOperands, newLocals)
    }

    /**
     * This method is called immediately before a join operation with regard
     * to the specified `pc` is performed.
     *
     * @note This method is intended to be overwritten by clients to perform custom
     *      operations.
     */
    protected[this] def beforeBaseJoin(pc: Int): Unit = { /*empty*/ }

    protected[this] def joinValues(
        pc:   Int,
        left: DomainValue, right: DomainValue
    ): Update[DomainValue] = {
        left.join(pc, right)
    }

    /**
     * This method is called after all values which differ have been joined, but before
     * `joinPostProcessing` will be called.
     * @param pc
     */
    protected[this] def afterBaseJoin(pc: Int): Unit = { /*empty*/ }

    /**
     * Enables the customization of the behavior of the base [[join]] method.
     *
     * This method in particular enables, in case of a [[MetaInformationUpdate]],
     * to raise the update type to force the continuation of the abstract interpretation process.
     *
     * Methods should always `override` this method and should call the super method.
     *
     * @param   updateType The current update type. The level can be raised. It is
     *          an error to lower the update level.
     * @param   oldOperands The old operands, before the join. Should not be changed.
     * @param   oldLocals The old locals, before the join. Should not be changed.
     * @param   newOperands The new operands; may be updated.
     * @param   newLocals The new locals; may be updated.
     */
    protected[this] def joinPostProcessing(
        updateType:  UpdateType,
        pc:          Int,
        oldOperands: Operands,
        oldLocals:   Locals,
        newOperands: Operands,
        newLocals:   Locals
    ): Update[(Operands, Locals)] = {
        updateType((newOperands, newLocals))
    }

    /**
     * Called by the framework after performing a computation to inform the domain about the result.
     * That is, after evaluating the effect of the instruction with `currentPC` on the current
     * stack and register and (if necessary) joining the updated stack and registers with the stack
     * and registers associated with the instruction `successorPC`. (Hence, this method
     * is ONLY called for `return` instructions if the return instruction throws an
     * `IllegalMonitorStateException`.)
     * This function basically informs the domain about the instruction that
     * ''may be'' evaluated next. The flow function is called for ''every possible
     * successor'' of the instruction with `currentPC`. This includes all branch
     * targets as well as those instructions that handle exceptions.
     *
     * In some cases it will even be the case that `flow` is called multiple times with
     * the same pair of program counters: (`currentPC`, `successorPC`). This may happen,
     * e.g., in case of a switch instruction where multiple values have the same
     * body/target instruction and we do not have precise information about the switch value.
     * E.g., as in the following snippet:
     * {{{
     * switch (i) {  // pc: X => Y (for "1"), Y (for "2"), Y (for "3")
     * case 1:
     * case 2:
     * case 3: System.out.println("Great.");            // pc: Y
     * default: System.out.println("Not So Great.");    // pc: Z
     * }
     * }}}
     * The flow function is also called after instructions that are domain independent
     * such as `dup` and `load` instructions which ''just'' manipulate the registers
     * and stack in a generic way.
     * This enables the domain to precisely follow the evaluation
     * progress and in particular to perform control-flow dependent analyses.
     *
     * @param  currentPC The program counter of the instruction that is currently evaluated
     *         by the abstract interpreter.
     *
     * @param  currentOperands The current operands. I.e., the operand stack before the
     *         instruction is evaluated.
     *
     * @param  currentLocals The current locals. I.e., the locals before the instruction is
     *         evaluated.
     *
     * @param  successorPC The program counter of an instruction that is a potential
     *         successor of the instruction with `currentPC`. In general the AI framework
     *         adds the pc of the successor instruction to the beginning of the worklist
     *         unless it is a join instruction. In this case the pc is added to the end – in
     *         the context of the current (sub)routine. Hence, the AI framework first evaluates
     *         all paths leading to a join instruction before the join instruction will
     *         be evaluated.
     *
     * @param  isSuccessorScheduled `Yes` if the successor instruction is or was scheduled.
     *         I.e., `Yes` is returned if the worklist contains `successorPC`, `No` if the
     *         worklist does not contain `successorPC`. `Unknown` is returned if the AI
     *         framework did not process the worklist and doesn't know anything about
     *         the scheduled successors. Note that this value is independent of the
     *         subroutine in which the value may be scheduled. If an implementation schedules
     *         `successorPC` the the super call has to set `isSuccessorScheduled` to `Yes`.
     *
     * @param  isExceptionalControlFlow `true` if and only if the evaluation of
     *         the instruction with the program counter `currentPC` threw an exception;
     *         `false` otherwise. Hence, if this parameter is `true` the instruction
     *         with `successorPC` is the first instruction of the handler.
     *
     * @param  abruptSubroutineTerminationCount `> 0` if and only if we have an exceptional
     *         control flow that terminates one or more subroutines.
     *         In this case the successor instruction is scheduled (if at all) after all
     *         subroutines that will be terminated by the exception.
     *
     * @param  wasJoinPerformed `true` if a join was performed. I.e., the successor
     *         instruction is an instruction (`Code.cfJoins`) that was already
     *         previously evaluated and where multiple paths potentially join.
     *
     * @param  operandsArray The array that associates '''every instruction''' with its
     *         operand stack that is in effect.  Note, that only those elements of the
     *         array contain values that are related to instructions that were
     *         evaluated in the past; the other elements are `null`. Furthermore,
     *         it identifies the `operandsArray` of the subroutine that will execute the
     *         instruction with `successorPC`.
     *         '''The operandsArray may be `null` for the ''current'' instruction (not the successor
     *         instruction) if the execution of the current instruction leads to the termination
     *         of the current subroutine. In this case the information about the operands
     *         and locals associated with all instructions belonging to the subroutine is
     *         reset.'''
     *
     * @param  localsArray The array that associates every instruction with its current
     *         register values. Note, that only those elements of the
     *         array contain values that are related to instructions that were evaluated in
     *         the past. The other elements are `null`. Furthermore,
     *         it identifies the `localsArray` of the subroutine that will execute the
     *         instruction with `successorPC`.
     *         '''The localsArray may be `null` for the ''current'' instruction (not the successor
     *         instruction) if the execution of the current instruction leads to the termination
     *         of the current subroutine. In this case the information about the operands
     *         and locals associated with all instructions belonging to the subroutine is
     *         reset.'''
     *
     * @param  worklist The current list of instructions that will be evaluated next.
     *
     *         ==If subroutines are not used (i.e., Java >= 5)==
     *         If you want to force the evaluation of the instruction
     *         with the program counter `successorPC` it is sufficient to test whether
     *         the list already contains `successorPC` and – if not – to prepend it.
     *         If the worklist already contains `successorPC` then the domain is allowed
     *         to move the PC to the beginning of the worklist.
     *
     *         ==If the code contains subroutines (JSR/RET)==
     *         If the PC does not belong to the same (current) (sub)routine, it is not
     *         allowed to be moved to the beginning of the worklist.
     *         (Subroutines can only be found in code generated by old Java compilers;
     *         before Java 6. Subroutines are identified by jsr/ret instructions.
     *         A subroutine can be identified by going back in the worklist
     *         and by looking for specific "program counters" (e.g., [[SUBROUTINE_START]],
     *         [[SUBROUTINE_END]]).
     *         These program counters mark the beginning of a subroutine. In other
     *         words, an instruction can be freely moved around unless a special program
     *         counter value is found. All special program counters use negative values.
     *         Additionally, neither the negative values nor the positive values between
     *         two negative values should be changed. Furthermore, no value (PC) should be put
     *         between negative values that capture subroutine information.
     *         If the domain updates the worklist, it is the responsibility of the domain
     *         to call the tracer and to inform it about the changes.
     *         Note that the worklist is not allowed to contain duplicates related to the
     *         evaluation of the current (sub-)routine.
     *
     * @return The updated worklist. In most cases this is simply the given `worklist`.
     *         The default case is also to return the given `worklist`.
     *
     * @note   The domain is allowed to modify the `worklist`, `operandsArray` and
     *         `localsArray`. However, the AI will not perform any checks. '''In case of
     *         updates of the `operandsArray` or `localsArray` it is necessary to first
     *         create a shallow copy before updating it'''.
     *         If this is not done, it may happen that the locals associated
     *         with other instructions are also updated.
     *
     * @note   '''A method that overrides this method must always call the super method
     *         to ensure that every domain that uses this hook gets informed about a flow.'''
     */
    def flow(
        currentPC:                        Int,
        currentOperands:                  Operands,
        currentLocals:                    Locals,
        successorPC:                      Int,
        isSuccessorScheduled:             Answer,
        isExceptionalControlFlow:         Boolean,
        abruptSubroutineTerminationCount: Int,
        wasJoinPerformed:                 Boolean,
        worklist:                         List[Int /*PC*/ ],
        operandsArray:                    OperandsArray,
        localsArray:                      LocalsArray,
        tracer:                           Option[AITracer]
    ): List[Int /*PC*/ ] = worklist

    /**
     * Called by the framework after evaluating the instruction with the given pc. I.e.,
     * the state of all potential successor instructions was updated and the
     * flow method was called – potentially multiple times – accordingly.
     *
     * By default this method does nothing.
     */
    def evaluationCompleted(
        pc:            Int,
        worklist:      List[Int /*PC*/ ],
        evaluatedPCs:  IntArrayStack,
        operandsArray: OperandsArray,
        localsArray:   LocalsArray,
        tracer:        Option[AITracer]
    ): Unit = { /* does nothing by default */ }

    /**
     * Called by the abstract interpreter when the abstract interpretation of a method
     * has ended. The abstract interpretation of a method ends if either the fixpoint
     * is reached or the interpretation was aborted.
     *
     * By default this method does nothing.
     *
     * Domains that override this method are expected to also call
     * `super.abstractInterpretationEnded(aiResult)`.
     */
    def abstractInterpretationEnded(aiResult: AIResult { val domain: coreDomain.type }): Unit = {
        /* does nothing by default */
    }

    /**
     * This function can be called when the instruction `successorPC` needs to be
     * scheduled. The function will test if the instruction is already scheduled and
     * – if so – returns the given worklist. Otherwise the instruction
     * is scheduled in the correct (subroutine-)context.
     */
    protected[this] def schedule(
        successorPC:                      Int,
        abruptSubroutineTerminationCount: Int,
        worklist:                         List[Int /*PC*/ ]
    ): List[Int /*PC*/ ] = {
        if (abruptSubroutineTerminationCount > 0) {
            var header: List[Int /*PC*/ ] = Nil
            val relevantWorklist = {
                var subroutinesToTerminate = abruptSubroutineTerminationCount
                worklist.dropWhile { pc =>
                    if (pc == SUBROUTINE) {
                        subroutinesToTerminate -= 1
                        if (subroutinesToTerminate > 0) {
                            header ::= pc
                            true
                        } else {
                            false
                        }
                    } else {
                        header ::= pc
                        true
                    }
                }.tail /* drop SUBROUTINE MARKER */
            }
            if (containsInPrefix(relevantWorklist, successorPC, SUBROUTINE_START)) {
                worklist
            } else {
                header.reverse ::: (SUBROUTINE :: successorPC :: relevantWorklist)
            }
        } else {
            if (containsInPrefix(worklist, successorPC, SUBROUTINE_START)) {
                worklist
            } else {
                successorPC :: worklist
            }
        }
    }
}
