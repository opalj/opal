/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load float from local variable.
 *
 * @author Michael Eichberg
 */
trait FLoadInstruction extends LoadLocalVariableInstruction {

    final def computationalType: ComputationalType = ComputationalTypeFloat

    final def stackSlotsChange: Int = +1
}

trait ConstantIndexFLoadInstruction
    extends FLoadInstruction
    with ImplicitLocalVariableIndex
    with InstructionMetaInformation