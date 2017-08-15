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

import scala.collection.mutable

import org.opalj.br.instructions.LabeledInstruction
import org.opalj.br.instructions.WIDE

/**
 * Factory method for creating a [[CodeAttributeBuilder]].
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

        require(
            codeElements.exists(_.isInstanceOf[InstructionElement]),
            "a code attribute has to have at least one instruction"
        )

        val labelSymbols = codeElements.collect { case LabelElement(r) ⇒ r }
        require(
            labelSymbols.distinct.length == labelSymbols.length,
            "each label has to be unique: "+
                labelSymbols.groupBy(identity).collect { case (x, ys) if ys.size > 1 ⇒ x }.mkString
        )

        codeElements.collect { case InstructionElement(i) ⇒ i }.foreach {
            case bi: LabeledInstruction ⇒
                bi.branchTargets foreach { target ⇒
                    require(
                        labelSymbols.contains(target),
                        s"branch target label $target of $bi undefined"
                    )
                }
            case _ ⇒ // we don't care
        }

        val instructionsWithPlaceholders = mutable.ArrayBuffer.empty[CodeElement[T]]

        //fill the array with `null`s for PCs representing instruction arguments
        var nextPC = 0
        var currentPC = 0
        var modifiedByWide = false
        codeElements foreach { e ⇒
            instructionsWithPlaceholders.append(e)

            e match {
                case InstructionLikeElement(inst) ⇒
                    currentPC = nextPC
                    nextPC = inst.indexOfNextInstruction(currentPC, modifiedByWide)
                    for (j ← 1 until nextPC - currentPC) { // IMPROVE use while loop
                        instructionsWithPlaceholders.append(null)
                    }
                    modifiedByWide = false
                    if (inst == WIDE) {
                        modifiedByWide = true
                    }
                case _ ⇒
            }
        }

        //calculate the PCs of all PseudoInstructions
        var labels: Map[Symbol, br.PC] = Map.empty
        val exceptionHandlerBuilder = new ExceptionHandlerGenerator
        val lineNumberTableBuilder = new LineNumberTableBuilder()
        var count: Int = 0
        for ((inst, index) ← instructionsWithPlaceholders.zipWithIndex) { // IMPROVE use while loop
            if (inst.isInstanceOf[PseudoInstruction]) {
                val pc = index - count
                instructionsWithPlaceholders.remove(pc)
                inst match {
                    case LabelElement(label)        ⇒ labels += (label → (pc))
                    case e: ExceptionHandlerElement ⇒ exceptionHandlerBuilder.add(e, pc)
                    case l: LINENUMBER              ⇒ lineNumberTableBuilder.add(l, pc)
                }
                count += 1
            }
        }

        val exceptionHandlers = exceptionHandlerBuilder.result()

        val attributes: IndexedSeq[br.Attribute] = lineNumberTableBuilder.result()

        val annotations = instructionsWithPlaceholders.zipWithIndex.collect { // IMPROVE use iterator?
            case (AnnotatedInstructionElement(_, annotation), pc) ⇒ (pc, annotation)
        }.toMap

        val instructionLikesOnly = instructionsWithPlaceholders.collect {
            case InstructionLikeElement(i) ⇒ i
            case null                      ⇒ null
            // ... filter pseudo instructions
        }

        val instructions = instructionLikesOnly.zipWithIndex.map { tuple ⇒
            val (instruction, index) = tuple
            if (instruction != null) {
                instruction.resolveJumpTargets(index, labels)
            } else {
                null
            }
        }

        new CodeAttributeBuilder(
            instructions.toArray,
            annotations,
            None,
            None,
            exceptionHandlers,
            attributes
        )
    }
}
