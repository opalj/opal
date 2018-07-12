/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An arithmetic instruction as defined by the JVM specification.
 *
 * @author Michael Eichberg
 */
trait ArithmeticInstruction extends Instruction with NoLabels {

    final override def asArithmeticInstruction: this.type = this

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || this == other
    }

    /**
     * A string representation of the operator as
     * used by the Java programming language (if possible).
     * (In case of the comparison operators for long, float and double values the
     * strings `cmp(g|l)?` are used.)
     */
    def operator: String

    /**
     * The computational type of the '''primary value(s)''' processed by the instruction.
     *
     * @note In case of the shift instructions for int and long values the second value
     *      is always an int value but in both cases not all bits are taken into account.
     */
    def computationalType: ComputationalType

    /**
     * Returns `true` if this instruction is a shift (`<<`, `>>`, `>>>`) instruction.
     * [[ShiftInstruction]]s are special since the computational type of the shift
     * value must not be the same as the computational type of the shifted value and
     * not all bits are taken into account.
     */
    def isShiftInstruction: Boolean

    final override def toString(currentPC: Int): String = toString()
}

/**
 * Defines values and methods common to arithmetic instructions.
 *
 * @author Michael Eichberg
 */
object ArithmeticInstruction {

    final val jvmExceptions = List(ObjectType.ArithmeticException)

}
