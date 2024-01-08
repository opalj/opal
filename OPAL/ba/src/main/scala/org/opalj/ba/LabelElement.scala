/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import org.opalj.br.instructions.InstructionLabel

/**
 * Wrapper for `InstructionLabel`s representing branch targets.
 *
 * @author Malte Limmeroth
 */
case class LabelElement(label: InstructionLabel) extends PseudoInstruction {

    override final def isExceptionHandlerElement: Boolean = false

    override final def isTry: Boolean = false

    override final def isCatch: Boolean = false

    override final def isPCLabel: Boolean = label.isPCLabel

}
