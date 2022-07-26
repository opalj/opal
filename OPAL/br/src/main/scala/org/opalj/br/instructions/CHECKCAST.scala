/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Check whether object is of given type.
 *
 * @author Michael Eichberg
 */
case class CHECKCAST(
        referenceType: ReferenceType
) extends Instruction with ConstantLengthInstruction with NoLabels {

    final override def isCheckcast: Boolean = true

    final def opcode: Opcode = CHECKCAST.opcode

    final def mnemonic: String = "checkcast"

    final def jvmExceptions: List[ObjectType] = CHECKCAST.jvmExceptions

    final def mayThrowExceptions: Boolean = true

    final def length: Int = 3

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        this == code.instructions(otherPC)
    }

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def stackSlotsChange: Int = 0

    final def readsLocal: Boolean = false

    final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final def writesLocal: Boolean = false

    final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    final def nextInstructions(
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
            Instruction.nextInstructionOrExceptionHandler(
                this, currentPC, ObjectType.ClassCastException
            )
    }

    final def expressionResult: Stack.type = Stack

    override def toString: String = "CHECKCAST("+referenceType.toJava+")"

    final override def toString(currentPC: Int): String = toString()

}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 */
object CHECKCAST extends InstructionMetaInformation {

    final val opcode = 192

    val jvmExceptions = List(ObjectType.ClassCastException)

    /**
     * Factory method to create [[CHECKCAST]] instructions.
     *
     * @param   referenceType The name of the [[org.opalj.br.ReferenceType]]. See the corresponding
     *          factory method for further details.
     */
    def apply(referenceType: String): CHECKCAST = CHECKCAST(ReferenceType(referenceType))

}
