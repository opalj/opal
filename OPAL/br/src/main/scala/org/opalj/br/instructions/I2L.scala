/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Convert int to long.
 *
 * @author Michael Eichberg
 */
case object I2L extends NumericConversionInstruction {

    final val opcode = 133

    final val mnemonic = "i2l"

    def sourceType: BaseType = IntegerType

    def targetType: BaseType = LongType
}
