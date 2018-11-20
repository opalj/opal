/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Convert long to double.
 *
 * @author Michael Eichberg
 */
case object L2D extends NumericConversionInstruction {

    final val opcode = 138

    final val mnemonic = "l2d"

    def sourceType: BaseType = LongType

    def targetType: BaseType = DoubleType
}
