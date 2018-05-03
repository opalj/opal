/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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

    def isInstructionLikeElement: Boolean

    def isPseudoInstruction: Boolean

    def isExceptionHandlerElement: Boolean

    def isTry: Boolean

    def asTry: TRY = throw new ClassCastException(s"cannot cast $this to TRY")

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
