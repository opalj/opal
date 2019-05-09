/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Create new array of reference.
 *
 * @author Michael Eichberg
 */
case class ANEWARRAY(componentType: ReferenceType) extends CreateNewOneDimensionalArrayInstruction {

    final def opcode: Opcode = ANEWARRAY.opcode

    final def mnemonic: String = "anewarray"

    final def length: Int = 3

    final def arrayType: ArrayType = ArrayType(componentType)

}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 */
object ANEWARRAY extends InstructionMetaInformation {

    final val opcode = 189

    /**
     * Factory method to create [[ANEWARRAY]] instructions.
     *
     * @param   componentTypeName The name of the array's '''component type''';
     *          see [[org.opalj.br.ReferenceType$]] for details regarding the syntax!
     */
    def apply(componentTypeName: String): ANEWARRAY = ANEWARRAY(ReferenceType(componentTypeName))

}
