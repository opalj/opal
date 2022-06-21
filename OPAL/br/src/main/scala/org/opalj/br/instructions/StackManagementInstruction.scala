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

    final override def isStackManagementInstruction: Boolean = true

    final override def length: Int = 1

    final override def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[PC] = {
        List(indexOfNextInstruction(currentPC))
    }

    final override def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || this == other
    }

    final override def readsLocal: Boolean = false

    final override def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final override def writesLocal: Boolean = false

    final override def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    final override def expressionResult: NoExpression.type = NoExpression

    final override def toString(currentPC: Int): String = toString()

    final override def jvmExceptions: List[ObjectType] = Nil

    final override def mayThrowExceptions: Boolean = false

}
