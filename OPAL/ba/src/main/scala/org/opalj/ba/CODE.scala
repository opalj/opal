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

import scala.collection.mutable.ArrayBuffer

import org.opalj.control.rerun
import org.opalj.control.iterateUntil
import org.opalj.br.PC
import org.opalj.br.Code
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.WIDE
import org.opalj.br.instructions.LabeledInstruction

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

        /*  val labelSymbols = codeElements.collect { case LabelElement(r) ⇒ r }

        require(codeElements.exists(_.isInstanceOf[InstructionElement]), "no code found")

        require(
            labelSymbols.distinct.length == labelSymbols.length,
            labelSymbols.
                groupBy(identity).
                collect { case (x, ys) if ys.size > 1 ⇒ x }.
                mkString("each label has to be unique:\n\t", "\n\t", "")
        )

        codeElements foreach {
            case InstructionElement(i) ⇒
                i.branchTargets.foreach { target ⇒
                    require(labelSymbols.contains(target), s"$i: undefined branch target $target")
                }
            case _ ⇒ /*NOT RELEVANT*/
        }

        val formattedInstructions = ArrayBuffer.empty[CodeElement[T]]

        // fill the array with `null`s for PCs representing instruction arguments
        var nextPC = 0
        var currentPC = 0
        var modifiedByWide = false
        codeElements foreach { e ⇒
            formattedInstructions.append(e)
            e match {
                case InstructionLikeElement(i) ⇒
                    currentPC = nextPC
                    nextPC = i.indexOfNextInstruction(currentPC, modifiedByWide)

                    rerun((nextPC - currentPC)-1){                        formattedInstructions.append(null)                    }

                    modifiedByWide = false
                    if (i == WIDE) {
                        modifiedByWide = true
                    }

                case _ ⇒ // we are not further interested in EXCEPTION HANDLERS, LABELS...
            }
        }
        // calculate the PCs of all PseudoInstructions // IMPROVE merge with previous loop!
        var labels: Map[Symbol, br.PC] = Map.empty
        val exceptionHandlerBuilder = new ExceptionHandlerGenerator
        val lineNumberTableBuilder = new LineNumberTableBuilder()
        var count: Int = 0
        for {
            (inst, index) ← formattedInstructions.iterator.zipWithIndex
            if inst != null && inst.isPseudoInstruction
        }{
                           val pc = index - count
                formattedInstructions.remove(pc)
                inst match {
                    case LabelElement(label)        ⇒ labels += (label → pc)
                    case e: ExceptionHandlerElement ⇒ exceptionHandlerBuilder.add(e, pc)
                    case l: LINENUMBER              ⇒ lineNumberTableBuilder.add(l, pc)
                }
                count += 1
                    }

        // We need to check if we have to adapt ifs and gotos if the branchtarget is not
        // representable using a signed short; in case of gotos we simply use goto_w; in
        // case of ifs, we "negate" the condition and add a goto_w w.r.t. the target and
        // in the other cases jump to the original instruction which follows the if.

        val exceptionHandlers = exceptionHandlerBuilder.result()

        val attributes: IndexedSeq[br.Attribute] = lineNumberTableBuilder.result()

        val annotations = formattedInstructions.zipWithIndex.collect { // IMPROVE use iterator?
            case (AnnotatedInstructionElement(_, annotation), pc) ⇒ (pc, annotation)
        }.toMap

        val instructionLikesOnly = formattedInstructions.collect {
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

        */

        val instructionLikes = new ArrayBuffer[LabeledInstruction](codeElements.size)

        var labels = Map.empty[Symbol, br.PC]
        var annotations = Map.empty[br.PC, T]
        val exceptionHandlerBuilder = new ExceptionHandlerGenerator()
        val lineNumberTableBuilder = new LineNumberTableBuilder()
        var hasControlTransferInstructions = false

        var currentPC = 0
        var nextPC = 0
        var modifiedByWide = false
        // fill the instructionLikes array with `null`s for PCs representing instruction arguments
        codeElements foreach {
            case ile @ InstructionLikeElement(i) ⇒
                if (ile.isAnnotated) {
                    annotations += ((currentPC, ile.annotation))
                }
                currentPC = nextPC
                nextPC = i.indexOfNextInstruction(currentPC, modifiedByWide)
                instructionLikes.append(i)
                rerun((nextPC - currentPC) - 1) { instructionLikes.append(null) }

                modifiedByWide = i == WIDE
                hasControlTransferInstructions |= i.isControlTransferInstruction

            case LabelElement(label)        ⇒ labels += (label → nextPC)

            case e: ExceptionHandlerElement ⇒ exceptionHandlerBuilder.add(e, nextPC)

            case l: LINENUMBER              ⇒ lineNumberTableBuilder.add(l, nextPC)
        }

        val exceptionHandlers = exceptionHandlerBuilder.result()
        val attributes = lineNumberTableBuilder.result()

        // TODO We need to check if we have to adapt ifs and gotos if the branchtarget is not
        // representable using a signed short; in case of gotos we simply use goto_w; in
        // case of ifs, we "negate" the condition and add a goto_w w.r.t. the target and
        // in the other cases jump to the original instruction which follows the if.

        val codeSize = instructionLikes.size
        val instructions = new Array[Instruction](codeSize)
        iterateUntil(0, codeSize) { pc ⇒
            val labeledInstruction = instructionLikes(pc)
            if (labeledInstruction != null) {
                instructions(pc) = labeledInstruction.resolveJumpTargets(pc, labels)
            }
        }

        new CodeAttributeBuilder(
            instructions.toArray,
            hasControlTransferInstructions,
            annotations,
            None,
            None,
            exceptionHandlers,
            attributes
        )
    }

    def toLabeledCode(code: Code): LabeledCode = {
        val estimatedSize = code.codeSize
        val labeledInstructions = new ArrayBuffer[CodeElement[AnyRef]](estimatedSize)

        // Transform the current code to use labels; this approach handles cases such as
        // switches which now require more/less bytes very elegantly.
        code.iterate { (pc, i) ⇒
            // IMPROVE use while loop
            code.exceptionHandlers.iterator.zipWithIndex.foreach { (ehIndex) ⇒
                val (eh, index) = ehIndex
                // Recall that endPC is exclusive while TRYEND is inclusive... Hence,
                // we have to add it before the next instruction...
                if (eh.endPC == pc) labeledInstructions += TRYEND(Symbol(s"eh$index"))

                if (eh.startPC == pc)
                    labeledInstructions += TRY(Symbol(s"eh$index"))
                if (eh.handlerPC == pc)
                    labeledInstructions += CATCH(Symbol(s"eh$index"), eh.catchType)

            }
            labeledInstructions += LabelElement(Symbol(pc.toString))
            labeledInstructions += i.toLabeledInstruction(pc)
        }

        new LabeledCode(code, labeledInstructions)
    }

    /**
     * Inserts the given sequence of instructions before, at or after the instruction with the
     * given pc.
     * Here, '''before''' means that those instruction which currently jump to the instruction with
     * the given pc, will jump to the first instruction of the given sequence of instructions.
     *
     * @note   The instructions are only considered to be prototypes and are adapted (in case of
     *         jump instructions) if necessary.
     * @note   This method does not provide support for methods that will - if too many instructions
     *         are added - exceed the maximum allowed length of methods.
     *
     * @param insertionPC The pc of an instruction.
     * @param insertionPosition Given an instruction I which is a jump target and which has the pc
     *         `insertionPC`. In this case, the effect of the (insertion) position is:
     *
     *         ''Before''
     *                  `insertionPC:` // the jump target will be the newly inserted instructions
     *                  `&lt;new instructions&gt;`
     *                  `&lt;remaining original instructions&gt;`
     *
     *         ''After''
     *                  `insertionPC:`
     *                  `&lt;original instruction with program counter insertionPC&gt;`
     *                  `newJumpTarget(insertionPC+1):`
     *                      // i.e., an instruction which jumps to the original
     *                      // instruction which follows the instruction with
     *                      // insertionPC will still jump to that instruction
     *                      // and not the new one.
     *                      // Additionally, existing exception handlers which
     *                      // included the specified instruction will also
     *                      // include this instruction.
     *                  `&lt;new instructions&gt;`
     *                  `pcOfNextInstruction(insertionPC):`
     *                  `&lt;remaining original instructions&gt;`
     *
     *         '''At'''
     *                  `newJumpTarget(insertionPC):`
     *                  `&lt;new instructions&gt;`
     *                  `insertionPC:`
     *                  `&lt;remaining original instructions&gt;`
     *
     *         (W.r.t. labeled code the effect can also be described as shown next:
     *         Let's assume that:
     *         EH ... code elements modelling exception handlers (TRY|TRYEND|CATCH)
     *         I ... the (implicitly referenced) instruction
     *         L ... the (implicit) label of the instruction
     *         CE ... the new CodeElements
     *
     *         '''Given''':
     *         EH | L | I // EH can be empty, L is (for original instructions) always existing!
     *
     *         '''Before''':
     *         EH | L | CE | I
     *
     *         '''At''':
     *         EH | CE | L | I // existing exception handlers w.r.t. L are effective
     *
     *         '''After''':
     *         EH | L | I | CE | EH | L+1 // i.e., the insertion position depends on L+1(!)
     *         )
     *
     *         Hence, `At` and `After` can be used interchangeably except when an
     *         instruction should be added at the very beginning or after the end.
     *
     * @param newInstructions The sequence of instructions that will be added at the specified
     *         position relative to the instruction with the given pc.
     *         If this list of instructions contains instructions which have jump
     *         targets then these jump targets have to use `Symbol`s which are not used
     *         by the code (which are the program counters of the code's instructions).
     *         E.g., by appending something like `new` to every Symbol we will get
     *         unique jump targets for instructions.
     */
    def insert(
        insertionPC:       PC,
        insertionPosition: InsertionPosition.Value,
        newInstructions:   Seq[CodeElement[AnyRef]],
        labeledCode:       LabeledCode
    ): Unit = {
        val instructions = labeledCode.instructions

        // In the array we can have (after the label) all other code elements... (and if
        // we already inserted other code, we could have multiple labels...)
        insertionPosition match {

            case InsertionPosition.Before ⇒
                val insertionPCLabel = LabelElement(Symbol(insertionPC.toString))
                val insertionPCLabelIndex = instructions.indexOf(insertionPCLabel)
                instructions.insert(insertionPCLabelIndex + 1, newInstructions: _*)

            case InsertionPosition.At ⇒
                val insertionPCLabel = LabelElement(Symbol(insertionPC.toString))
                val insertionPCLabelIndex = instructions.indexOf(insertionPCLabel)
                instructions.insert(insertionPCLabelIndex, newInstructions: _*)

            case InsertionPosition.After ⇒
                val originalCode = labeledCode.originalCode
                val effectivePC = originalCode.pcOfNextInstruction(insertionPC)
                var insertionPCLabelIndex =
                    if (effectivePC >= originalCode.codeSize)
                        instructions.size
                    else {
                        val insertionPCLabel = LabelElement(Symbol(effectivePC.toString))
                        instructions.indexOf(insertionPCLabel)
                    }
                // Let's find the index where we want to actually insert the new instructions...
                // which is before all exception related code elements!
                while (instructions(insertionPCLabelIndex - 1).isExceptionHandlerElement) {
                    insertionPCLabelIndex -= 1
                }
                instructions.insert(insertionPCLabelIndex, newInstructions: _*)

        }
    }
}

object InsertionPosition extends Enumeration {
    final val Before = Value("before")
    final val At = Value("at")
    final val After = Value("after")
}

/**
 * Container for some labeled code.
 *
 * @note Using LabeledCode is NOT thread safe.
 *
 * @param originalCode The original code.
 */
class LabeledCode(
        val originalCode:             Code,
        private[ba] val instructions: ArrayBuffer[CodeElement[AnyRef]]
) {

    def toCodeAttributeBuilder: CodeAttributeBuilder[AnyRef] = {
        val initialCodeAttributeBuilder = CODE(instructions)
        // let's check if we have to compute a new StackMapTable attribute
        // originalCode.cfJoins
        // initialCodeAttributeBuilder.instructions

        // TODO We have to adapt the exiting attributes and we have to merge the line number tables
        // if necessary.

        initialCodeAttributeBuilder
    }

    def insert(
        insertionPC: PC, insertionPosition: InsertionPosition.Value,
        newInstructions: Seq[CodeElement[AnyRef]]
    ): Unit = {
        CODE.insert(insertionPC, insertionPosition, newInstructions, this)
    }
}
