/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import org.opalj.br.ObjectType.OutOfMemoryError

/**
 * Create new object.
 *
 * @author Michael Eichberg
 */
case class NEW(objectType: ObjectType) extends ConstantLengthInstruction with NoLabels {

    final override def opcode: Opcode = NEW.opcode

    final override def asNEW: NEW = this

    final override def mnemonic: String = "new"

    final override def jvmExceptions: List[ObjectType] = NEW.jvmExceptions

    final override def mayThrowExceptions: Boolean = true

    final override def length: Int = 3

    final override def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final override def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final override def stackSlotsChange: Int = 1

    final override def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        this == code.instructions(otherPC)
    }

    final override def readsLocal: Boolean = false

    final override def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final override def writesLocal: Boolean = false

    final override def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    final override def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[PC] = {
        if (regularSuccessorsOnly)
            List(indexOfNextInstruction(currentPC))
        else
            Instruction.nextInstructionOrExceptionHandler(this, currentPC, OutOfMemoryError)
    }

    final override def expressionResult: Stack.type = Stack

    override def toString: String = "NEW "+objectType.toJava

    final override def toString(currentPC: Int): String = toString()
}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
object NEW extends InstructionMetaInformation {

    final val opcode = 187

    final val jvmExceptions = List(ObjectType.OutOfMemoryError)

    /**
     * Creates a new [[NEW]] instruction given the fully qualified name in binary notation.
     * @see     [[org.opalj.br.ObjectType$]] for details.
     */
    def apply(fqn: String): NEW = NEW(ObjectType(fqn))

}
