/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Increment local variable by constant.
 *
 * @author Michael Eichberg
 */
case class IINC(lvIndex: Int, constValue: Int) extends ArithmeticInstruction {

    final override def isIINC: Boolean = true

    final override def asIINC: this.type = this

    final def opcode: Opcode = IINC.opcode

    final def mnemonic: String = "iinc"

    final def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    final def computationalType: ComputationalType = ComputationalTypeInt

    final def operator: String = "+ "+constValue

    final def isPrefixOperator: Boolean = false

    final def isShiftInstruction: Boolean = false

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final def stackSlotsChange: Int = 0

    final def readsLocal: Boolean = true

    final def indexOfReadLocal: Int = lvIndex

    final def writesLocal: Boolean = true

    final def indexOfWrittenLocal: Int = lvIndex

    final def indexOfNextInstruction(currentPC: PC)(implicit code: Code): PC = {
        indexOfNextInstruction(currentPC, code.isModifiedByWide(currentPC))
    }

    final def indexOfNextInstruction(currentPC: PC, modifiedByWide: Boolean): PC = {
        if (modifiedByWide) {
            currentPC + 1 + 4
        } else {
            currentPC + 1 + 2
        }
    }
    override def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[PC] = {
        List(indexOfNextInstruction(currentPC))
    }

    final def expressionResult: Register = Register(lvIndex)

    override def toString = "IINC(lvIndex="+lvIndex+", "+constValue+")"

}
object IINC extends InstructionMetaInformation {

    final val opcode = 132

}
