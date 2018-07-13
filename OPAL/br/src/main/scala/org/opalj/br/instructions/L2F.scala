/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Convert long to float.
 *
 * @author Michael Eichberg
 */
case object L2F extends NumericConversionInstruction {

    final val opcode = 137

    final val mnemonic = "l2f"

    def sourceType: BaseType = LongType

    def targetType: BaseType = FloatType

}
