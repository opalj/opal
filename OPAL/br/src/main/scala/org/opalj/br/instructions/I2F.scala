/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Convert int to float.
 *
 * @author Michael Eichberg
 */
case object I2F extends NumericConversionInstruction {

    final val opcode = 134

    final val mnemonic = "i2f"

    def sourceType: BaseType = IntegerType

    def targetType: BaseType = FloatType
}
