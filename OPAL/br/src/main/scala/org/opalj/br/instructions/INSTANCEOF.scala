/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Determine if object is of given type.
 *
 * @author Michael Eichberg
 */
case class INSTANCEOF(
        referenceType: ReferenceType
) extends ConstantLengthInstruction with NoLabels {

    final def opcode: Opcode = INSTANCEOF.opcode

    final def mnemonic: String = "instanceof"

    final def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    final def length: Int = INSTANCEOF.length

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def stackSlotsChange: Int = 0

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || this == other
    }

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
        List(indexOfNextInstruction(currentPC))
    }

    final def expressionResult: Stack.type = Stack

    override def toString: String = s"INSTANCEOF(${referenceType.toJava})"

    final override def toString(currentPC: Int): String = toString()
}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 */
object INSTANCEOF extends InstructionMetaInformation {

    final val opcode = 193

    final val length = 3

    /**
     * Factory method to create [[INSTANCEOF]] instructions.
     *
     * @param   referenceTypeName The `referenceType` against which the type test is done; see
     * +            [[org.opalj.br.ReferenceType$]]'s `apply` method for the correct syntax.
     */
    def apply(referenceTypeName: String): INSTANCEOF = INSTANCEOF(ReferenceType(referenceTypeName))
}
