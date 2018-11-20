/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Convert int to byte.
 *
 * @author Michael Eichberg
 */
case object I2B extends NumericConversionInstruction {

    final val opcode = 145

    final val mnemonic = "i2b"

    def sourceType: BaseType = IntegerType

    def targetType: BaseType = ByteType
}
