/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Common superclass of all instructions.
 *
 * In general, we distinguish between a finally assembled [[Instruction]] where all
 * jump targets are resolved to concrete branchoffsets and [[LabeledInstruction]]s where the
 * jump target(s) are identified using symbols which need to replaced by concrete branchoffsets
 * before a class file object can be generated.
 *
 * @author Michael Eichberg
 */
trait InstructionLike {

    /**
     *  The opcode of the instruction as defined by the JVM specification. The
     *  opcode is a value in the range [0..255].
     */
    def opcode: Int

    /**
     *  The mnemonic of the instruction as defined by the JVM specification.
     */
    def mnemonic: String

    def isControlTransferInstruction: Boolean = false
    def isLoadConstantInstruction: Boolean = false
    def isReturnInstruction: Boolean = false
    def isMonitorInstruction: Boolean = false
    def isAthrow: Boolean = false
    def isRET: Boolean = false

    /**
     * The exceptions that may be thrown by the JVM at runtime if the execution of
     * this instruction fails.
     * I.e., these are neither exceptions that are explicitly created and then thrown
     * by user code nor errors that may arise due to an invalid code base (in particular
     * `LinkageError`s). However, `OutOfMemoryError`s are possible.
     *
     * @note    The returned types always precisely describe the thrown exception;
     *          they are not upper bounds.
     *
     * All instructions – except of the [[InvocationInstruction]]s and the [[ATHROW$]] instruction –
     * will always either succeed, throw a linkage time related exception or throw one of the
     * specified exceptions.
     */
    def jvmExceptions: List[ObjectType]

    /**
     * Returns `true` if the evaluation of the instruction may lead to some runtime exception.
     * For example, in case of [[INVOKESTATIC]] `jvmExceptions` will return an empty list. However,
     * in general the called method may throw an arbitrary exception.
     *
     * Errors (such as LinkageError) related to invalid projects are not considered.
     */
    def mayThrowExceptions: Boolean

    /**
     * The index of the next instruction in the code array.
     */
    def indexOfNextInstruction(currentPC: Int, modifiedByWide: Boolean): Int

    /**
     * Determines if this instruction is isomorphic to the given instruction.
     *
     * Two instructions are isomporphic if they access the same operand and register
     * values and if the instructions have the same bytecode representation, except
     * of (A) (potential) padding bytes and (B) the branch offset of JSR(_W) instructions.
     * In the first case the branch offsets are corrected by the number of padding bytes and
     * in the second case the absolute addresses are compared (i.e., whether both
     * instructions call the same subroutine).
     *
     * For example, an `aload_0` instruction is only
     * isomorphic to another `aload_0` instruction and is not isomorphic to an `aload(0)`
     * instruction – though the runtime effect is the same. However, a [[LOOKUPSWITCH]]
     * ([[TABLESWITCH]]) instruction is considered isomorphic to another respective
     * instruction if the only difference is the number of padding bytes. Furthermore,
     * two JSR(_W) instructions are isomorphic if and only if they jump to the same
     * subroutine.
     *
     * @note The number of padding bytes is generally calculated by `(otherPC % 4) -
     *      (thisPC %4)` (=== `"padding other" - "padding this"`)
     *      and should be added to the branch offsets of this `(XYZ)switch` instruction
     *      when the branch targets are compared to the other instructions branchoffsets.
     *      {{{
     *      // "padding b" - "padding a"
     *      // === (3 - (bPC % 4)) - (3 - (aPC % 4))
     *      // === (aPC % 4) - (bPC %4)
     *      }}}
     * @note this.isIsomorphic(`thisPC`,`thisPC`) is always `true`
     */
    def isIsomorphic(thisPC: Int, otherPC: Int)(implicit code: Code): Boolean

    /**
     * The number of values that are popped from the operand stack. Here, long and
     * double values are also counted as one value though they use two stack slots. E.g.,
     * [[IADD]] (integer add) and [[LADD]] (long add) both pop two values and push
     * one value.
     *
     * @note In case of some of the [[StackManagementInstruction]] the number of popped values is
     *      not fixed. In that case the number depends on the concrete layout of the
     *      operand stack. E.g., the [[POP2]] instruction may just pop one
     *      ''categeory 2'' value (of type `long` or `double`) or two ''category 1''
     *      values.
     *
     * @param   ctg A function that returns the computational type category of
     *          the value on the operand stack with a given value index. E.g., The top value on
     *          the operand stack has index '0' and may occupy one (for category 1 values)
     *          or two stack slots (for category 2 values.)
     */
    def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int

    /**
     * The number of values that are put onto the operand stack. Here, long and
     * double values are also counted as one value though they use two stack slots. E.g.,
     * [[IADD]] (integer add) and [[LADD]] (long add) both pop two values and push
     * one value.
     *
     * @note In case of some of the [[StackManagementInstruction]] this number is
     *      not fixed. In that case the number depends on the concrete layout of the
     *      operand stack. E.g., the [[DUP2]]
     *      instruction may just duplicate one ''categeory 2'' value (result is 1)
     *      (of type long or double) or two ''category 1'' values (result is 2).
     *
     * @param   ctg A function that returns the computational type category of
     *          the value on the operand stack with a given value index. The top value on
     *          the operand stack has index '0' and may occupy one (for category 1 values)
     *          or two stack slots (for category 2 values.)
     */
    def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int

    /**
     * The number of stack slots pushed or popped by this instruction.
     *
     * @note    Overall, each [[DUP]] instruction always pushes the same number of stack slots.
     *          Only the number of values that are processed may depend on the stack layout.
     */
    def stackSlotsChange: Int

    /**
     * Returns `true` if this instruction reads/uses a local variable.
     */
    def readsLocal: Boolean

    /**
     * The index of the local (variable)/register that is read is returned. This
     * method is only defined if [[readsLocal]] returns `true`.
     */
    @throws[UnsupportedOperationException]("thrown if no local variable is read")
    def indexOfReadLocal: Int

    /**
     * Returns `true` if this instruction writes/updates a local variable.
     */
    def writesLocal: Boolean

    /**
     * The index of the local (variable)/register that is written. This
     * method is only defined if [[writesLocal]] returns `true`.
     */
    @throws[UnsupportedOperationException]("thrown if no local variable is written")
    def indexOfWrittenLocal: Int

    /**
     * Returns the location – [[Stack]], [[Register]] or [[NoExpression]] – where the value
     * computed by this instruction is stored. In this case an instruction is only considered
     * to be an expression if it puts a value on the stack or in a register that is the result of
     * some kind of computation; i.e., just copying, duplicating or moving a value between the
     * stack and the registers is not considered to be an expression.
     *
     * @note    The CHECKCAST instruction is special in the sense that it just inspects the
     *          top-most value.
     */
    def expressionResult: ExpressionResultLocation

    /**
     * Returns a string representation of this instruction. If this instruction is a
     * (conditional) jump instruction, then the PCs of the target instructions are
     * given absolute address.
     *
     * @param   currentPC The program counter of this instruction. Used to resolve relative
     *          jump targets.
     */
    def toString(currentPC: Int): String

}
