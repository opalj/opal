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

    final override def isExceptionHandlerElement: Boolean = false

    final override def isTry: Boolean = false

    final override def isCatch: Boolean = false

    final override def isPCLabel: Boolean = label.isPCLabel

}
