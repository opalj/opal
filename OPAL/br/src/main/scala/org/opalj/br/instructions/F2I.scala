/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Convert float to int.
 *
 * @author Michael Eichberg
 */
case object F2I extends NumericConversionInstruction {

    final val opcode = 139

    final val mnemonic = "f2i"
    def sourceType: BaseType = FloatType

    def targetType: BaseType = IntegerType
}
