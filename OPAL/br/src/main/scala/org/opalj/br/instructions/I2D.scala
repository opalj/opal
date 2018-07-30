/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Convert int to double.
 *
 * @author Michael Eichberg
 */
case object I2D extends NumericConversionInstruction {

    final val opcode = 135

    final val mnemonic = "i2d"

    def sourceType: BaseType = IntegerType

    def targetType: BaseType = DoubleType
}
