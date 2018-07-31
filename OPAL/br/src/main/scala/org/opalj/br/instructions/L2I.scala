/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Convert long to int.
 *
 * @author Michael Eichberg
 */
case object L2I extends NumericConversionInstruction {

    final val opcode = 136

    final val mnemonic = "l2i"

    def sourceType: BaseType = LongType

    def targetType: BaseType = IntegerType
}
