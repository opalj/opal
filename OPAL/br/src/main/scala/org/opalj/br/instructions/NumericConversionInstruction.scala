/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that converts between a numeric value of primitive type A and
 * primitive type B.
 *
 * @author Michael Eichberg
 */
abstract class NumericConversionInstruction
    extends ConstantLengthInstruction
    with InstructionMetaInformation
    with NoLabels {

    final def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    final def length: Int = 1

    def sourceType: BaseType

    def targetType: BaseType

    final def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[PC] = {
        List(indexOfNextInstruction(currentPC))
    }

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def stackSlotsChange: Int = {
        -sourceType.computationalType.operandSize + targetType.computationalType.operandSize
    }

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || (this.opcode == other.opcode)
    }

    final def readsLocal: Boolean = false

    final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final def writesLocal: Boolean = false

    final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    final def expressionResult: Stack.type = Stack

    final override def toString(currentPC: Int): String = toString()
}
