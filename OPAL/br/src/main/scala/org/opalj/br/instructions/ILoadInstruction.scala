/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load int from local variable.
 *
 * @author Michael Eichberg
 */
trait ILoadInstruction extends LoadLocalVariableInstruction {

    final def computationalType: ComputationalType = ComputationalTypeInt

    final def stackSlotsChange: Int = +1
}

trait ConstantIndexILoadInstruction
    extends ILoadInstruction
    with ImplicitLocalVariableIndex
    with InstructionMetaInformation
