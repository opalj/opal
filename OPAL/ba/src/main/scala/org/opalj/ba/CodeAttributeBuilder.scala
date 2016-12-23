/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
import org.opalj.br.instructions.InstructionLike
import org.opalj.br.instructions.LabeledInstruction
import org.opalj.br.instructions.WIDE

/**
 * Builder for the [[org.opalj.br.Code]] attribute with all its properties. Instantiation is only
 * possible with the [[CODE]] factory. The `max_stack` and
 * `max_locals` values will be calculated if not explicitly defined.
 *
 * @author Malte Limmeroth
 */
case class CodeAttributeBuilder private (
        private var codeElements:             IndexedSeq[CodeElement],
        private var maxStack:                 Option[Int],
        private var maxLocals:                Option[Int],
        private var labeledExceptionHandlers: Option[br.ExceptionHandlers] //TODO: create a type and resolve
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

    private[ba] def buildCodeAndAnnotations(
        accessFlags: Int,
        name:        String,
        descriptor:  br.MethodDescriptor
    ): (br.Code, Map[br.PC, AnyRef]) = {
        val instructionsWithPlaceholders = scala.collection.mutable.ArrayBuffer.empty[CodeElement]

        //fill the array with `null` for PCs representing instruction arguments
        var nextPC = 0
        var currentPC = 0
        var modifiedByWide = false
        codeElements foreach { i ⇒
            instructionsWithPlaceholders.append(i)

            i match {
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
                case LabelElement(l) ⇒ //nothing to do here, were just creating blanks for arguments
            }
        }

        //calculate the PCs of all labels
        var labels: Map[Symbol, br.PC] = Map()
        for ((LabelElement(label), index) ← instructionsWithPlaceholders.zipWithIndex) {
            labels += (label → (index - labels.size))
        }

        val annotations = instructionsWithPlaceholders.zipWithIndex.collect {
            case (AnnotatedInstructionElement(_, annotation), pc) ⇒ (pc, annotation)
        }.toMap

        val instructionLikesOnly = instructionsWithPlaceholders.collect {
            case InstructionElement(i) ⇒ i
            case null                  ⇒ null
        }

        for ((instruction: LabeledInstruction, index: Int) ← instructionLikesOnly.zipWithIndex) {
            instructionLikesOnly.update(index, instruction.resolveJumpTargets(index, labels))
        }

        val exceptionHandlers = this.labeledExceptionHandlers.getOrElse(IndexedSeq.empty)

        val finalInstructions = instructionLikesOnly.collect {
            case i: InstructionLike ⇒ i.asInstanceOf[Instruction]
            case null               ⇒ null
        }.toArray

        val _maxLocals = br.Code.computeMaxLocals(
            (ACC_STATIC.mask & accessFlags) == 0,
            descriptor,
            finalInstructions
        )

        val warnMessage = s"you defined %s of method '${descriptor.toJava(name)}' too small;"+
            "explicitly configured value is nevertheless kept"

        if (maxLocals.isDefined) {
            if (maxLocals.get < _maxLocals) {
                println(warnMessage.format("max_locals"))
            }
        }

        val _maxStack = br.Code.computeMaxStack(
            instructions = finalInstructions,
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
            instructions = finalInstructions,
            exceptionHandlers = exceptionHandlers,
            attributes = buildCodeAttributes
        )

        (code, annotations)
    }

    private def buildCodeAttributes: br.Attributes = {
        //TODO: LineNumberTable, LocalVariableTable, LocalVariableTypeTable, StackMapTable
        IndexedSeq.empty
    }
}

/**
 * Defines the factory method to create a [[CodeAttributeBuilder]].
 *
 * @author Malte Limmeroth
 */
object CODE {
    /**
     * Creates a new [[CodeAttributeBuilder]] with the given [[CodeElement]]s which will generate
     * the [[org.opalj.br.Code]]s instructions.
     *
     * @see [[CodeElement]]
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

        CodeAttributeBuilder(codeElements.toIndexedSeq, None, None, None)
    }
}