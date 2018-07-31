/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Convert int to short.
 *
 * @author Michael Eichberg
 */
case object I2S extends NumericConversionInstruction {

    final val opcode = 147

    final val mnemonic = "i2s"

    def sourceType: BaseType = IntegerType

    def targetType: BaseType = ShortType
}
