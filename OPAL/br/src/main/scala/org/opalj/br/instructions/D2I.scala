/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Convert double to int.
 *
 * @author Michael Eichberg
 */
case object D2I extends NumericConversionInstruction {

    final val opcode = 142

    final val mnemonic = "d2i"

    def sourceType: BaseType = DoubleType

    def targetType: BaseType = IntegerType

}
