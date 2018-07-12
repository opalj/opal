/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Convert double to long.
 *
 * @author Michael Eichberg
 */
case object D2L extends NumericConversionInstruction {

    final val opcode = 143

    final val mnemonic = "d2l"

    def sourceType: BaseType = DoubleType

    def targetType: BaseType = LongType
}
