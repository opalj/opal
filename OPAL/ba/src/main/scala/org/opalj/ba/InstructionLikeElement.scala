/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import org.opalj.br.instructions.LabeledInstruction

/**
 * Represents an (annotated) labeled instruction.
 *
 * @author Malte Limmeroth
 */
sealed abstract class InstructionLikeElement[+T] extends CodeElement[T] {

    final override def isInstructionLikeElement: Boolean = true

    final override def isPseudoInstruction: Boolean = false

    final override def isExceptionHandlerElement: Boolean = false

    final override def isTry: Boolean = false

    final override def isCatch: Boolean = false

    final override def isControlTransferInstruction: Boolean = {
        instruction.isControlTransferInstruction
    }

    def isAnnotated: Boolean

    def annotation: T

    def instruction: LabeledInstruction
}

object InstructionLikeElement {

    def unapply(ile: InstructionLikeElement[_]): Some[LabeledInstruction] = {
        Some(ile.instruction)
    }
}

/**
 * Wrapper for [[org.opalj.br.instructions.LabeledInstruction]]s.
 */
case class InstructionElement(
        instruction: LabeledInstruction
) extends InstructionLikeElement[Nothing] {
    final override def isAnnotated: Boolean = false
    final override def annotation: Nothing = throw new UnsupportedOperationException
}

/**
 * Wrapper for annotated [[org.opalj.br.instructions.LabeledInstruction]]s.
 */
case class AnnotatedInstructionElement[+T](
        instruction: LabeledInstruction,
        annotation:  T
) extends InstructionLikeElement[T] {
    final override def isAnnotated: Boolean = true
}

object AnnotatedInstructionElement {

    def apply[T](ia: (LabeledInstruction, T)): AnnotatedInstructionElement[T] = {
        val (i, a) = ia
        new AnnotatedInstructionElement(i, a)
    }
}
