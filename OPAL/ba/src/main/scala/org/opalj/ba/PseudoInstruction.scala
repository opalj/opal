/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

/**
 * Marker trait for labels (`scala.Symbol`) and pseudo instructions generating `Code` attributes.
 *
 * @author Malte Limmeroth
 */
abstract class PseudoInstruction extends CodeElement[Nothing] {

    final override def isPseudoInstruction: Boolean = true
    final override def asPseudoInstruction: PseudoInstruction = this

    final override def isInstructionLikeElement: Boolean = false

    final override def isControlTransferInstruction: Boolean = false

    def isPCLabel: Boolean = false

}
