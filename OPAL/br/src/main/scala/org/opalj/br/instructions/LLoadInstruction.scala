/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load long from local variable.
 *
 * @author Michael Eichberg
 */
trait LLoadInstruction extends LoadLocalVariableInstruction {

    final def computationalType: ComputationalType = ComputationalTypeLong

    final def stackSlotsChange: Int = 2
}

object LLoadInstruction

trait ConstantIndexLLoadInstruction
    extends LLoadInstruction
    with ImplicitLocalVariableIndex
    with InstructionMetaInformation