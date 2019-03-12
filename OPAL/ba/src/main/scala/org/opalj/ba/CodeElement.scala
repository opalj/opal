/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import scala.language.implicitConversions
import org.opalj.br.instructions.LabeledInstruction

/**
 * Wrapper for elements that will generate the instructions and attributes of a
 * [[org.opalj.br.Code]] and the annotations of the bytecode.
 *
 * @see [[org.opalj.ba.InstructionElement]]
 * @see [[org.opalj.ba.AnnotatedInstructionElement]]
 * @see [[org.opalj.ba.PseudoInstruction]]
 * @tparam T The type of the annotations of instructions.
 *
 * @author Malte Limmeroth
 */
trait CodeElement[+T] {

    def isPseudoInstruction: Boolean
    def asPseudoInstruction: PseudoInstruction = {
        throw new ClassCastException(s"$this is not a PseudoInstruction")
    }

    def isInstructionLikeElement: Boolean

    def isExceptionHandlerElement: Boolean

    def isTry: Boolean
    def asTry: TRY = throw new ClassCastException(s"$this is not a TRY");

    def isCatch: Boolean

    def isControlTransferInstruction: Boolean

}

/**
 * Implicit conversions to [[CodeElement]].
 */
object CodeElement {

    /**
     * Converts [[org.opalj.br.instructions.LabeledInstruction]]s to
     * [[org.opalj.ba.InstructionElement]]s.
     */
    implicit def instructionToInstructionElement(
        instruction: LabeledInstruction
    ): InstructionElement = {
        new InstructionElement(instruction)
    }

    /**
     * Converts a tuple of [[org.opalj.br.instructions.LabeledInstruction]] and `scala.AnyRef`
     * (an annotated instruction) to [[org.opalj.ba.AnnotatedInstructionElement]].
     */
    implicit def annotatedInstructionToAnnotatedInstructionElement[T](
        ia: (LabeledInstruction, T)
    ): AnnotatedInstructionElement[T] = {
        AnnotatedInstructionElement(ia)
    }

    /**
     * Converts a `Symbol` (label) to [[org.opalj.ba.LabelElement]].
     */
    implicit def symbolToLabelElement(label: Symbol): LabelElement = new LabelElement(label)
}
