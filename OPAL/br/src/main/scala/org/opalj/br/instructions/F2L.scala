/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Convert float to long.
 *
 * @author Michael Eichberg
 */
case object F2L extends NumericConversionInstruction {

    final val opcode = 140

    final val mnemonic = "f2l"

    def sourceType: BaseType = FloatType

    def targetType: BaseType = LongType

}
