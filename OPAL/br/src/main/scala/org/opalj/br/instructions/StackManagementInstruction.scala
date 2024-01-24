/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that directly manipulates the operand stack by popping, swapping or
 * duplicating values.
 *
 * @author Michael Eichberg
 */
abstract class StackManagementInstruction
    extends ConstantLengthInstruction
    with NoLabels
    with InstructionMetaInformation {

    override final def isStackManagementInstruction: Boolean = true

    override final def length: Int = 1

    override final def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[PC] = {
        List(indexOfNextInstruction(currentPC))
    }

    override final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || this == other
    }

    override final def readsLocal: Boolean = false

    override final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    override final def writesLocal: Boolean = false

    override final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    override final def expressionResult: NoExpression.type = NoExpression

    override final def toString(currentPC: Int): String = toString()

    override final def jvmExceptions: List[ObjectType] = Nil

    override final def mayThrowExceptions: Boolean = false

}
