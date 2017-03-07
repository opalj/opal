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

import org.opalj.bi.ACC_STATIC
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.LabeledInstruction
import org.opalj.br.instructions.WIDE

/**
 * Builder for the [[org.opalj.br.Code]] attribute with all its properties. Instantiation is only
 * possible with the [[CODE]] factory. The `max_stack` and
 * `max_locals` values will be calculated if not explicitly defined.
 *
 * @author Malte Limmeroth
 */
class CodeAttributeBuilder private[ba] (
        private val instructions:      Array[Instruction],
        private val annotations:       Map[br.PC, AnyRef],
        private var maxStack:          Option[Int],
        private var maxLocals:         Option[Int],
        private var exceptionHandlers: br.ExceptionHandlers,
        private var attributes:        br.Attributes
) {
    /**
     * Defines the max_stack value.
     */
    def MAXSTACK(value: Int): this.type = {
        maxStack = Some(value)

        this
    }

    /**
     * Defines the max_locals value.
     */
    def MAXLOCALS(value: Int): this.type = {
        maxLocals = Some(value)

        this
    }

    def buildCodeAndAnnotations(
        accessFlags: Int,
        name:        String,
        descriptor:  br.MethodDescriptor
    ): (br.Code, Map[br.PC, AnyRef]) = {
        val _maxLocals = br.Code.computeMaxLocals(
            (ACC_STATIC.mask & accessFlags) == 0,
            descriptor,
            instructions
        )

        val warnMessage = s"you defined %s of method '${descriptor.toJava(name)}' too small;"+
            "explicitly configured value is nevertheless kept"

        if (maxLocals.isDefined) {
            if (maxLocals.get < _maxLocals) {
                println(warnMessage.format("max_locals"))
            }
        }

        val _maxStack = br.Code.computeMaxStack(
            instructions = instructions,
            exceptionHandlers = exceptionHandlers
        )

        if (maxStack.isDefined) {
            if (maxStack.get < _maxStack) {
                println(warnMessage.format("max_stack"))
            }
        }

        val code = br.Code(
            maxStack = maxStack.getOrElse(_maxStack),
            maxLocals = maxLocals.getOrElse(_maxLocals),
            instructions = instructions,
            exceptionHandlers = exceptionHandlers,
            attributes = attributes
        )

        (code, annotations)
    }
}

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
    def apply(codeElements: CodeElement*): CodeAttributeBuilder = {
        require(
            codeElements.exists(_.isInstanceOf[InstructionElement]),
            "a Code attribute has to have at least one instruction"
        )

        val labelSymbols = codeElements.collect { case LabelElement(r) ⇒ r }
        require(
            labelSymbols.distinct.length == labelSymbols.length,
            "each label has to be unique: "+
                labelSymbols.groupBy(identity).collect { case (x, ys) if ys.size > 1 ⇒ x }.mkString
        )

        codeElements.collect { case InstructionElement(inst) ⇒ inst }.collect {
            case bi: LabeledInstruction ⇒ {
                bi.branchTargets foreach { target ⇒
                    require(
                        labelSymbols.contains(target),
                        s"each branch instruction has to reference existing labels."+
                            s"Instruction: $bi, missing label: $target"
                    )
                }
            }
        }
        buildCodeAttributeBuilder(codeElements.toIndexedSeq)
    }

    private def buildCodeAttributeBuilder(codeElements: IndexedSeq[CodeElement]): CodeAttributeBuilder = {
        val instructionsWithPlaceholders = scala.collection.mutable.ArrayBuffer.empty[CodeElement]

        //fill the array with `null` for PCs representing instruction arguments
        var nextPC = 0
        var currentPC = 0
        var modifiedByWide = false
        codeElements foreach { e ⇒
            instructionsWithPlaceholders.append(e)

            e match {
                case InstructionElement(inst) ⇒
                    currentPC = nextPC
                    nextPC = inst.indexOfNextInstruction(currentPC, modifiedByWide)
                    for (j ← 1 until nextPC - currentPC) {
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
        val exceptionHandlerGenerator = new ExceptionHandlerGenerator
        val lineNumberTableGenerator = new LineNumberTableGenerator
        var count: Int = 0
        for ((inst, index) ← instructionsWithPlaceholders.zipWithIndex) {
            if (inst.isInstanceOf[PseudoInstruction]) {
                val pc = index - count
                instructionsWithPlaceholders.remove(pc)
                inst match {
                    case LabelElement(label)        ⇒ labels += (label → (pc))
                    case e: ExceptionHandlerElement ⇒ exceptionHandlerGenerator.add(e, pc)
                    case l: LINENUMBER              ⇒ lineNumberTableGenerator.add(l, pc)
                }
                count += 1
            }
        }

        val exceptionHandlers = exceptionHandlerGenerator.finalizeHandlers

        var attributes = Seq.empty[br.Attribute]
        attributes ++:= lineNumberTableGenerator.finalizeLineNumberTable.to[Seq] //TODO: add more code attributes

        val annotations = instructionsWithPlaceholders.zipWithIndex.collect {
            case (AnnotatedInstructionElement(_, annotation), pc) ⇒ (pc, annotation)
        }.toMap

        val instructionLikesOnly = instructionsWithPlaceholders.collect {
            case InstructionElement(i) ⇒ i
            case null                  ⇒ null
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