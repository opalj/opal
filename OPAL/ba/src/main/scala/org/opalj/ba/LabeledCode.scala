/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ba

import scala.collection.mutable.ArrayBuffer
import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap
import org.opalj.br
import org.opalj.br.PC
import org.opalj.br.Code
import org.opalj.br.StackMapTable
import org.opalj.br.LineNumber
import org.opalj.br.LineNumberTable
import org.opalj.br.CodeAttribute
import org.opalj.br.UnpackedLineNumberTable
import org.opalj.br.instructions.InstructionLabel
import org.opalj.br.instructions.PCLabel

import scala.collection.immutable.ArraySeq

/**
 * Mutable container for some labeled code.
 * We will use [[org.opalj.br.instructions.PCLabel]] labels for those labels which were
 * created based on the original code. This enables the computation ofa mapping from
 * old pcs to new pcs.
 *
 * @note Using `LabeledCode` is NOT thread safe.
 *
 * @param originalCode The original code.
 */
class LabeledCode(
        val originalCode:         Code,
        private var instructions: ArrayBuffer[CodeElement[AnyRef]]
) {

    /**
     * Returns a view of the current code elements.
     *
     * This iterator is not fail-fast and the result is undetermined if – while the
     * iteration is not completed – a change of the code is performed.
     */
    def codeElements: Iterator[CodeElement[AnyRef]] = instructions.iterator

    def removedDeadCode(): Unit = {
        CODE.removeDeadCode(instructions) match {
            case is: ArrayBuffer[CodeElement[AnyRef]] =>
                instructions = is
            case is: IndexedSeq[CodeElement[AnyRef]] =>
                instructions = new ArrayBuffer[CodeElement[AnyRef]](is.size) ++ is
        }
    }

    /**
     * Inserts the given sequence of instructions before, at or after the instruction - identified
     * by a [[org.opalj.br.instructions.PCLabel]] - with the given pc.
     * CODE objects created by `Code.toLabeldCode` generally creates
     * [[org.opalj.br.instructions.PCLabel]].
     *
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
     *         targets then these jump targets have to use `InstructionLabel`s which are not used
     *         by the code (which are the program counters of the code's instructions).
     *         E.g., by using name based labels we will get unique jump targets for instructions.
     */
    def insert(
        insertionPC:       PC,
        insertionPosition: InsertionPosition.Value,
        newInstructions:   Seq[CodeElement[AnyRef]]
    ): Unit = {
        val instructions = this.instructions

        // In the array we can have (after the label) all other code elements... (and if
        // we already inserted other code, we could have multiple labels...)
        insertionPosition match {

            case InsertionPosition.Before =>
                val insertionPCLabel = LabelElement(InstructionLabel(insertionPC))
                val insertionPCLabelIndex = instructions.indexOf(insertionPCLabel)
                instructions.insertAll(insertionPCLabelIndex + 1, newInstructions)

            case InsertionPosition.At =>
                val insertionPCLabel = LabelElement(InstructionLabel(insertionPC))
                val insertionPCLabelIndex = instructions.indexOf(insertionPCLabel)
                instructions.insertAll(insertionPCLabelIndex, newInstructions)

            case InsertionPosition.After =>
                val originalCode = this.originalCode
                val effectivePC = originalCode.pcOfNextInstruction(insertionPC)
                var insertionPCLabelIndex =
                    if (effectivePC >= originalCode.codeSize)
                        instructions.size
                    else {
                        val insertionPCLabel = LabelElement(InstructionLabel(effectivePC))
                        instructions.indexOf(insertionPCLabel)
                    }
                // Let's find the index where we want to actually insert the new instructions...
                // which is before all exception related code elements!
                while (instructions(insertionPCLabelIndex - 1).isExceptionHandlerElement) {
                    insertionPCLabelIndex -= 1
                }
                instructions.insertAll(insertionPCLabelIndex, newInstructions)

        }
    }

    /**
     * Replaces the [[InstructionLikeElement]] associate with the given pc by the given
     * instruction sequence. I.e., only the [[InstructionLikeElement]] is replaced; all
     * other information associated with the respective program counter (e.g., line number
     * or exception handling related markers) is kept.
     *
     * The instruction sequence has to process the values on the stack that would have been
     * processed. Overall the sequence has to be stack-neutral.
     */
    def replace(
        pc:              PC,
        newInstructions: Seq[CodeElement[AnyRef]]
    ): Unit = {
        val pcLabel = LabelElement(InstructionLabel(pc))
        var pcInstructionLikeIndex = instructions.indexOf(pcLabel) + 1
        while (!instructions(pcInstructionLikeIndex).isInstructionLikeElement) {
            pcInstructionLikeIndex += 1
        }
        instructions.remove(pcInstructionLikeIndex)
        instructions.insertAll(pcInstructionLikeIndex, newInstructions)
    }

    /**
     * Creates a new [[CodeAttributeBuilder]] based on this `LabeledCode`; that builder can then
     * be used to construct a valid [[org.opalj.br.Code]] attribute.
     */
    def result: CodeAttributeBuilder[AnyRef] = {
        val initialCodeAttributeBuilder = CODE(instructions)
        val codeSize = initialCodeAttributeBuilder.instructions.length
        var explicitAttributes = initialCodeAttributeBuilder.attributes
        // We filter the (old) stack map table - it is most likely no longer valid!
        val oldAttributes = originalCode.attributes.filter { a => a.kindId != StackMapTable.KindId }

        initialCodeAttributeBuilder.copy(
            oldAttributes.map[br.Attribute] {

                case lnt: LineNumberTable =>
                    val oldRemappedLNT =
                        lnt.remapPCs(codeSize, initialCodeAttributeBuilder.pcMapping)
                    val explicitLNT =
                        initialCodeAttributeBuilder.attributes.collectFirst {
                            case lnt: LineNumberTable => lnt
                        }
                    explicitLNT match {
                        case None => oldRemappedLNT
                        case Some(explicitLNT @ LineNumberTable(explicitLNs)) =>
                            explicitAttributes = explicitAttributes.filter(a => a != explicitLNT)
                            // explicit line number have precedence
                            val newLNs = new Int2IntAVLTreeMap()
                            oldRemappedLNT.lineNumbers.foreach { ln =>
                                newLNs.put(ln.startPC, ln.lineNumber)
                            }
                            explicitLNs.foreach(ln => newLNs.put(ln.startPC, ln.lineNumber))
                            val finalLNs = new Array[LineNumber](newLNs.size)
                            var index = 0
                            newLNs.int2IntEntrySet().iterator().forEachRemaining { e =>
                                finalLNs(index) = LineNumber(e.getIntKey, e.getIntValue)
                                index += 1
                            }
                            UnpackedLineNumberTable(ArraySeq.unsafeWrapArray[LineNumber](finalLNs))
                        case Some(_) => throw new MatchError(explicitLNT)
                    }

                case ca: CodeAttribute => ca.remapPCs(codeSize, initialCodeAttributeBuilder.pcMapping)

                case a                 => a
            } ++ explicitAttributes
        )
    }

    override def toString: String = instructions.mkString("LabeledCode(\n\t", "\n\t", "\n)")

}

object LabeledCode {

    /**
     * Factory method to create some labeled code; using labeled code makes it particularly
     * simple to weave.
     *
     * @param code The code which will be transformed.
     * @param filterInstruction Tests if the instruction with a specific PC should be added to the
     *                          output; this is particularly useful to filter dead code.
     * @return The labeled code.
     */
    def apply(code: Code, filterInstruction: PC => Boolean = (_) => true): LabeledCode = {
        val codeSize = code.codeSize
        val estimatedSize = codeSize
        val labeledInstructions = new ArrayBuffer[CodeElement[AnyRef]](estimatedSize)

        // Transform the current code to use labels; this approach handles cases such as
        // switches which now require more/less bytes very elegantly.
        code.iterate { (pc, i) =>
            // IMPROVE [L1] use while loop
            code.exceptionHandlers.iterator.zipWithIndex.foreach { ehIndex =>
                val (eh, index) = ehIndex
                // Recall that endPC is exclusive while TRYEND is inclusive... Hence,
                // we have to add it before the next instruction...
                if (eh.endPC == pc) labeledInstructions += TRYEND(Symbol(s"eh$index"))

                if (eh.startPC == pc)
                    labeledInstructions += TRY(Symbol(s"eh$index"))
                if (eh.handlerPC == pc)
                    labeledInstructions += CATCH(Symbol(s"eh$index"), index, eh.catchType)

            }
            labeledInstructions += LabelElement(PCLabel(pc))
            if (filterInstruction(pc)) {
                labeledInstructions += i.toLabeledInstruction(pc)
            }
        }
        // The pc of a handler that handles "all" instructions is equal to "codeSize" and
        // therefore code.iterate will not reach it.
        code.exceptionHandlers.iterator.zipWithIndex.foreach { ehIndex =>
            val (eh, index) = ehIndex
            if (eh.endPC == codeSize) {
                labeledInstructions += TRYEND(Symbol(s"eh$index"))
            }
        }
        // We have to add the pc that would be used by the instruction which would follow the last
        // instruction in the code array. This is required when we remap, e.g.,  LocalVariableTables
        // where the range extends across ALL instructions.
        labeledInstructions += LabelElement(PCLabel(code.instructions.length))

        new LabeledCode(code, labeledInstructions)
    }

}

object InsertionPosition extends Enumeration {
    final val Before = Value("before")
    final val At = Value("at")
    final val After = Value("after")
}
