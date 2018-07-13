/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Convert double to float.
 *
 * @author Michael Eichberg
 */
case object D2F extends NumericConversionInstruction {

    final val opcode = 144

    final val mnemonic = "d2f"

    def sourceType: BaseType = DoubleType

    def targetType: BaseType = FloatType

}
