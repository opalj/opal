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

import java.util.NoSuchElementException

import scala.collection.mutable.ArrayBuffer
import org.opalj.control.rerun
import org.opalj.control.iterateUntil
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.WIDE
import org.opalj.br.instructions.LabeledInstruction
import org.opalj.br.instructions.InstructionLabel

/**
 * Factory method to create an initial [[CodeAttributeBuilder]].
 *
 * @author Malte Limmeroth
 */
object CODE {

    /**
     * Creates a new [[CodeAttributeBuilder]] with the given [[CodeElement]]s converted to
     * [[org.opalj.br.instructions.Instruction]]. In case of
     * [[org.opalj.br.instructions.LabeledInstruction]]s the label is already resolved. The
     * annotations are resolved to program counters as well.
     *
     * @see [[CodeElement]] for possible arguments.
     */
    def apply[T](codeElements: CodeElement[T]*): CodeAttributeBuilder[T] = {
        this(codeElements.toIndexedSeq)
    }

    def apply[T](codeElements: IndexedSeq[CodeElement[T]]): CodeAttributeBuilder[T] = {
        val instructionLikes = new ArrayBuffer[LabeledInstruction](codeElements.size)

        var labels = Map.empty[InstructionLabel, br.PC]
        var annotations = Map.empty[br.PC, T]
        val exceptionHandlerBuilder = new ExceptionHandlerGenerator()
        val lineNumberTableBuilder = new LineNumberTableBuilder()
        var hasControlTransferInstructions = false
        val pcMapping = new PCMapping

        var currentPC = 0
        var nextPC = 0
        var modifiedByWide = false
        // fill the instructionLikes array with `null`s for PCs representing instruction arguments
        codeElements foreach {
            case ile @ InstructionLikeElement(i) ⇒
                currentPC = nextPC
                nextPC = i.indexOfNextInstruction(currentPC, modifiedByWide)
                if (ile.isAnnotated) annotations += ((currentPC, ile.annotation))
                instructionLikes.append(i)
                rerun((nextPC - currentPC) - 1) { instructionLikes.append(null) }

                modifiedByWide = i == WIDE
                hasControlTransferInstructions |= i.isControlTransferInstruction

            case LabelElement(label) ⇒
                if (labels.contains(label)) {
                    throw new IllegalArgumentException(s"'$label is already used")
                }
                if (label.isPCLabel) {
                    // let's store the mapping to make it possible to remap the other attributes..
                    pcMapping += (label.pc, nextPC)
                }

                labels += (label → nextPC)

            case e: ExceptionHandlerElement ⇒ exceptionHandlerBuilder.add(e, nextPC)

            case l: LINENUMBER              ⇒ lineNumberTableBuilder.add(l, nextPC)
        }

        // TODO Support if and goto rewriting if required
        // We need to check if we have to adapt ifs and gotos if the branchtarget is not
        // representable using a signed short; in case of gotos we simply use goto_w; in
        // case of ifs, we "negate" the condition and add a goto_w w.r.t. the target and
        // in the other cases jump to the original instruction which follows the if.

        val exceptionHandlers = exceptionHandlerBuilder.result()
        val attributes = lineNumberTableBuilder.result()

        val codeSize = instructionLikes.size
        require(codeSize > 0, "no code found")
        val instructions = new Array[Instruction](codeSize)
        iterateUntil(0, codeSize) { pc ⇒
            val labeledInstruction = instructionLikes(pc)
            if (labeledInstruction != null) {
                try {
                    instructions(pc) = labeledInstruction.resolveJumpTargets(pc, labels)
                } catch {
                    case _: NoSuchElementException ⇒
                        val message = s"$labeledInstruction's label(s) could not be resolved"
                        throw new NoSuchElementException(message)
                }
            }
        }

        new CodeAttributeBuilder(
            instructions,
            hasControlTransferInstructions,
            pcMapping,
            annotations,
            None,
            None,
            exceptionHandlers,
            attributes
        )
    }

}
