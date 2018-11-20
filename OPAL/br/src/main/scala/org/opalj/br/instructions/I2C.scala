/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Convert int to char.
 *
 * @author Michael Eichberg
 */
case object I2C extends NumericConversionInstruction {

    final val opcode = 146

    final val mnemonic = "i2c"

    def sourceType: BaseType = IntegerType

    def targetType: BaseType = CharType
}
