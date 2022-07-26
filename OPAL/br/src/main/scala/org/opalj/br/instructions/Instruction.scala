/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Common superclass of all instructions which are in their final form.
 *
 * @author Michael Eichberg
 */
trait Instruction extends InstructionLike {

    /**
     * The index of the next instruction in the (sparse) code array.
     *
     * @note    This is primarily a convenience method that delegates to the method
     *          `indexOfNextInstrution(PC,Boolean)`. However, given that this is also the
     *          standard method called by clients, it is often meaningful to directly implement
     *          this. In particular since most instructions cannot be modified by wide.
     */
    def indexOfNextInstruction(currentPC: Int)(implicit code: Code): Int

    /**
     * Returns the pcs of the instructions that may be executed next at runtime. This
     * method takes potentially thrown exceptions into account. I.e., every instruction
     * that may throw an exception checks if it is handled locally and
     * – if so – checks if an appropriate handler exists and – if so – also returns
     * the first instruction of the handler. The chain may contain duplicates, iff the state
     * is potentially different when the target instruction is reached.
     *
     * @param   regularSuccessorsOnly If `true`, only those instructions are returned
     *          which are not related to an exception thrown by this instruction.
     * @return  The absolute addresses of '''all instructions''' that may be executed next
     *          at runtime.
     */
    def nextInstructions(
        currentPC:             Int,
        regularSuccessorsOnly: Boolean = false
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[Int /*PC*/ ]

    /**
     * Checks for structural equality of two instructions.
     *
     * @note   Implemted by using the underlying (compiler generated) equals methods.
     */
    def similar(other: Instruction): Boolean = this == other

    /**
     * Converts this instruction to a [[LabeledInstruction]], where relative jump targets are
     * replaced by symbols using the program counters of the target instructions as
     * Symbols (i.e., absolute targets).
     *
     * @param currentPC The pc of the current instruction.
     */
    def toLabeledInstruction(currentPC: Int): LabeledInstruction

    // ---------------------------------------------------------------------------------------------
    //
    // TYPE TEST AND TYPE CAST RELATED INSTRUCTIONS
    //
    // ---------------------------------------------------------------------------------------------

    def isSimpleBranchInstruction: Boolean = false
    def isSimpleConditionalBranchInstruction: Boolean = false
    def isCompoundConditionalBranchInstruction: Boolean = false
    def isGotoInstruction: Boolean = false
    def isStackManagementInstruction: Boolean = false
    def isLoadLocalVariableInstruction: Boolean = false
    def isStoreLocalVariableInstruction: Boolean = false
    def isCheckcast: Boolean = false
    def isInvocationInstruction: Boolean = false
    def isMethodInvocationInstruction: Boolean = false
    def isInvokeStatic: Boolean = false
    def isIINC: Boolean = false

    def asReturnInstruction: ReturnInstruction = throw new ClassCastException();

    def asATHROW: ATHROW.type = throw new ClassCastException();
    def asIINC: IINC = throw new ClassCastException();

    def asNEW: NEW = throw new ClassCastException();
    def asCreateNewArrayInstruction: CreateNewArrayInstruction = throw new ClassCastException();

    def asLoadLocalVariableInstruction: LoadLocalVariableInstruction = {
        throw new ClassCastException();
    }
    def asStoreLocalVariableInstruction: StoreLocalVariableInstruction = {
        throw new ClassCastException();
    }

    def asControlTransferInstruction: ControlTransferInstruction = throw new ClassCastException();
    def asGotoInstruction: GotoInstruction = throw new ClassCastException();

    def asSimpleBranchInstruction: SimpleBranchInstruction = throw new ClassCastException();
    def asSimpleConditionalBranchInstruction: SimpleConditionalBranchInstruction[_] = {
        throw new ClassCastException();
    }
    def asCompoundConditionalBranchInstruction: CompoundConditionalBranchInstruction = {
        throw new ClassCastException();
    }
    def asIFICMPInstruction: IFICMPInstruction[_] = throw new ClassCastException();
    def asIF0Instruction: IF0Instruction[_] = throw new ClassCastException();
    def asIFACMPInstruction: IFACMPInstruction[_] = throw new ClassCastException();
    def asIFXNullInstruction: IFXNullInstruction[_] = throw new ClassCastException();

    def asInvocationInstruction: InvocationInstruction = throw new ClassCastException();
    def asMethodInvocationInstruction: MethodInvocationInstruction = {
        throw new ClassCastException();
    }

    def asArithmeticInstruction: ArithmeticInstruction = throw new ClassCastException();

    def asTABLESWITCH: TABLESWITCH = throw new ClassCastException();
    def asLOOKUPSWITCH: LOOKUPSWITCH = throw new ClassCastException();
}

/**
 * Functionality common to instructions.
 *
 * @author Michael Eichberg
 */
object Instruction {

    final val IllegalIndex: Int = 1

    /**
     * Facilitates the matching of [[Instruction]] objects.
     *
     * @return Returns the triple `Some((opcode,mnemonic,list of jvm exceptions))`.
     */
    def unapply(instruction: Instruction): Some[(Int, String, List[ObjectType])] = {
        Some((instruction.opcode, instruction.mnemonic, instruction.jvmExceptions))
    }

    /**
     * Determines if the instructions with the pcs `aPC` and `bPC` are isomorphic.
     *
     * @see [[Instruction.isIsomorphic]] for further details.
     */
    def areIsomorphic(aPC: Int, bPC: Int)(implicit code: Code): Boolean = {
        assert(aPC != bPC)

        code.instructions(aPC).isIsomorphic(aPC, bPC)
    }

    private[instructions] def nextInstructionOrExceptionHandlers(
        instruction: Instruction,
        currentPC:   Int,
        exceptions:  List[ObjectType]
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[Int /*PC*/ ] = {
        var pcs = List(instruction.indexOfNextInstruction(currentPC))
        exceptions foreach { exception =>
            pcs = (code.handlersForException(currentPC, exception).map(_.handlerPC)) ++: pcs
        }
        pcs
    }

    private[instructions] def nextInstructionOrExceptionHandler(
        instruction: Instruction,
        currentPC:   Int,
        exception:   ObjectType
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[Int /*PC*/ ] = {
        val nextInstruction = instruction.indexOfNextInstruction(currentPC)
        nextInstruction :: (code.handlersForException(currentPC, exception).map(_.handlerPC))
    }

    final val justNullPointerException: List[org.opalj.br.ObjectType] = {
        List(ObjectType.NullPointerException)
    }
}
