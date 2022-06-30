/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that stores the top-most stack value in a local variable.
 *
 * @author Michael Eichberg
 */
abstract class StoreLocalVariableInstruction extends Instruction with NoLabels {

    final override def isStoreLocalVariableInstruction: Boolean = true

    final override def asStoreLocalVariableInstruction: this.type = this

    def lvIndex: Int

    def computationalType: ComputationalType

    def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

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

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final def readsLocal: Boolean = false

    final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final def writesLocal: Boolean = true

    final def indexOfWrittenLocal: Int = lvIndex

    final def expressionResult: NoExpression.type = NoExpression

    final override def toString(currentPC: Int): String = toString()
}

/**
 * Factory for `StoreLocalVariableInstruction`s.
 *
 * @author Arne Lottmann
 */
object StoreLocalVariableInstruction {

    /**
     * Returns the `xStore` instruction that stores the variable at the top of the stack
     * of the specified type in the local variable at the given index.
     */
    def apply(fieldType: FieldType, lvIndex: Int): StoreLocalVariableInstruction =
        (fieldType.id: @scala.annotation.switch) match {
            case IntegerType.id => ISTORE.canonicalRepresentation(lvIndex)
            case ByteType.id    => ISTORE.canonicalRepresentation(lvIndex)
            case ShortType.id   => ISTORE.canonicalRepresentation(lvIndex)
            case CharType.id    => ISTORE.canonicalRepresentation(lvIndex)
            case BooleanType.id => ISTORE.canonicalRepresentation(lvIndex)
            case LongType.id    => LSTORE.canonicalRepresentation(lvIndex)
            case FloatType.id   => FSTORE.canonicalRepresentation(lvIndex)
            case DoubleType.id  => DSTORE.canonicalRepresentation(lvIndex)
            case _              => ASTORE.canonicalRepresentation(lvIndex)
        }

    /**
     * Returns the `xStore` instruction that stores the variable at the top of the stack
     * of the specified computational type in the local variable at the given index.
     */
    def apply(computationalType: ComputationalType, lvIndex: Int): StoreLocalVariableInstruction = {
        computationalType match {
            case ComputationalTypeInt           => ISTORE.canonicalRepresentation(lvIndex)
            case ComputationalTypeFloat         => FSTORE.canonicalRepresentation(lvIndex)
            case ComputationalTypeLong          => LSTORE.canonicalRepresentation(lvIndex)
            case ComputationalTypeDouble        => DSTORE.canonicalRepresentation(lvIndex)
            case ComputationalTypeReference     => ASTORE.canonicalRepresentation(lvIndex)
            case ComputationalTypeReturnAddress => ASTORE.canonicalRepresentation(lvIndex)
        }
    }

    /**
     * Extracts the index of the accessed local variable.
     */
    def unapply(si: StoreLocalVariableInstruction): Option[(ComputationalType, Int)] =
        Some((si.computationalType, si.lvIndex))
}
