/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Create new multidimensional array.
 *
 * @param   arrayType The type of the array to be created.
 * @param   dimensions The number of dimensions of the specified array that should be initialized.
 *
 * @author Michael Eichberg
 */
case class MULTIANEWARRAY(
        arrayType:  ArrayType,
        dimensions: Int
) extends CreateNewArrayInstruction
    with InstructionMetaInformation {

    final def opcode: Opcode = MULTIANEWARRAY.opcode

    final def mnemonic: String = "multianewarray"

    final def length: Int = 4

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = dimensions

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def stackSlotsChange: Int = -dimensions + 1

    final def readsLocal: Boolean = false

    final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final def writesLocal: Boolean = false

    final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 */
object MULTIANEWARRAY {

    final val opcode = 197

    /**
     * Factory method to create [[MULTIANEWARRAY]] instructions.
     *
     * @param   arrayTypeName The array's type name; see [[org.opalj.br.FieldType$]] for details.
     * @param   dimensions The number of dimensions that should be initialized; the instruction will
     *          take a corresponding number of values from the stack.
     */
    def apply(arrayTypeName: String, dimensions: Int): MULTIANEWARRAY = {
        val arrayTypeCandidate = FieldType(arrayTypeName)
        require(arrayTypeCandidate.isArrayType, s"given type $arrayTypeName is not an array type")
        val arrayType = arrayTypeCandidate.asArrayType
        val arrayDimensions = arrayType.dimensions
        require(
            dimensions <= arrayDimensions,
            s"$dimensions > $arrayDimensions (the number of dimensions of the given array type)"
        )
        MULTIANEWARRAY(arrayType, dimensions)
    }

}
