/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that loads a local variable and puts it on top of the stack.
 *
 * @author Michael Eichberg
 */
abstract class LoadLocalVariableInstruction extends Instruction with NoLabels {

    final override def isLoadLocalVariableInstruction: Boolean = true

    final override def asLoadLocalVariableInstruction: this.type = this

    /**
     * The index of the local variable(register) that is loaded and put on top
     * of the operand stack.
     */
    def lvIndex: Int

    /**
     * The computational type of the local variable.
     */
    def computationalType: ComputationalType

    final def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

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

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def readsLocal: Boolean = true

    final def indexOfReadLocal: Int = lvIndex

    final def writesLocal: Boolean = false

    final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    final def expressionResult: NoExpression.type = NoExpression

    final override def toString(currentPC: Int): String = toString()
}
/**
 * Defines a factory method for `LoadLocalVariableInstruction`s.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
object LoadLocalVariableInstruction {

    /**
     * Returns the `xLoad` instruction that puts value stored at the given index with
     * the specified type on top of the stack.
     */
    def apply(fieldType: FieldType, lvIndex: Int): LoadLocalVariableInstruction = {
        (fieldType.id: @scala.annotation.switch) match {
            case IntegerType.id => ILOAD.canonicalRepresentation(lvIndex)
            case ByteType.id    => ILOAD.canonicalRepresentation(lvIndex)
            case ShortType.id   => ILOAD.canonicalRepresentation(lvIndex)
            case CharType.id    => ILOAD.canonicalRepresentation(lvIndex)
            case BooleanType.id => ILOAD.canonicalRepresentation(lvIndex)
            case LongType.id    => LLOAD.canonicalRepresentation(lvIndex)
            case FloatType.id   => FLOAD.canonicalRepresentation(lvIndex)
            case DoubleType.id  => DLOAD.canonicalRepresentation(lvIndex)
            case _              => ALOAD.canonicalRepresentation(lvIndex)
        }
    }

    /**
     * Returns the `xLoad` instruction that puts value stored at the given index with
     * the specified computational type on top of the stack.
     */
    def apply(computationalType: ComputationalType, lvIndex: Int): LoadLocalVariableInstruction = {
        computationalType match {
            case ComputationalTypeInt           => ILOAD.canonicalRepresentation(lvIndex)
            case ComputationalTypeFloat         => FLOAD.canonicalRepresentation(lvIndex)
            case ComputationalTypeLong          => LLOAD.canonicalRepresentation(lvIndex)
            case ComputationalTypeDouble        => DLOAD.canonicalRepresentation(lvIndex)
            case ComputationalTypeReference     => ALOAD.canonicalRepresentation(lvIndex)
            case ComputationalTypeReturnAddress => ALOAD.canonicalRepresentation(lvIndex)
        }
    }

    /**
     * Extracts the computational type and index of the accessed local variable.
     */
    def unapply(li: LoadLocalVariableInstruction): Option[(ComputationalType, Int)] = {
        Some((li.computationalType, li.lvIndex))
    }

}
