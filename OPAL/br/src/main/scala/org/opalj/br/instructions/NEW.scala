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

    override final def opcode: Opcode = NEW.opcode

    override final def asNEW: NEW = this

    override final def mnemonic: String = "new"

    override final def jvmExceptions: List[ObjectType] = NEW.jvmExceptions

    override final def mayThrowExceptions: Boolean = true

    override final def length: Int = 3

    override final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    override final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    override final def stackSlotsChange: Int = 1

    override final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        this == code.instructions(otherPC)
    }

    override final def readsLocal: Boolean = false

    override final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    override final def writesLocal: Boolean = false

    override final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    override final def nextInstructions(
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

    override final def expressionResult: Stack.type = Stack

    override def toString: String = "NEW " + objectType.toJava

    override final def toString(currentPC: Int): String = toString()
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
