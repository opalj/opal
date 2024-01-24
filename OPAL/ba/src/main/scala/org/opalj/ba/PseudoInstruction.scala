/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

/**
 * Marker trait for labels (`scala.Symbol`) and pseudo instructions generating `Code` attributes.
 *
 * @author Malte Limmeroth
 */
abstract class PseudoInstruction extends CodeElement[Nothing] {

    override final def isPseudoInstruction: Boolean = true
    override final def asPseudoInstruction: PseudoInstruction = this

    override final def isInstructionLikeElement: Boolean = false

    override final def isControlTransferInstruction: Boolean = false

    def isPCLabel: Boolean = false

}
