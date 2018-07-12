/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Convert float to double.
 *
 * @author Michael Eichberg
 */
case object F2D extends NumericConversionInstruction {

    final val opcode = 141

    final val mnemonic = "f2d"

    def sourceType: BaseType = FloatType

    def targetType: BaseType = DoubleType
}
