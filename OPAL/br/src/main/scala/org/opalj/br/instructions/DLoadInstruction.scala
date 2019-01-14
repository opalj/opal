/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load double from local variable.
 *
 * @author Michael Eichberg
 */
trait DLoadInstruction extends LoadLocalVariableInstruction {

    final def computationalType: ComputationalType = ComputationalTypeDouble

    final def stackSlotsChange: Int = +2

}

trait ConstantIndexDLoadInstruction
    extends DLoadInstruction
    with ImplicitLocalVariableIndex
    with InstructionMetaInformation