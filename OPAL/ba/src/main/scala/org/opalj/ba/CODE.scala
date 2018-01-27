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

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap

import scala.collection.mutable.ArrayBuffer

import org.opalj.control.rerun
import org.opalj.control.iterateUntil
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.WIDE
import org.opalj.br.instructions.LabeledInstruction
import org.opalj.br.instructions.InstructionLabel
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.IntArraySet
import org.opalj.collection.immutable.IntTrieSet1
import org.opalj.br.instructions.LabeledJSR
import org.opalj.br.instructions.LabeledJSR_W
import org.opalj.br.instructions.LabeledSimpleConditionalBranchInstruction
import org.opalj.br.instructions.PCLabel
import org.opalj.br.instructions.BranchoffsetOutOfBoundsException
import org.opalj.br.instructions.LabeledGOTO
import org.opalj.br.instructions.LabeledGOTO_W
import org.opalj.br.instructions.RewriteLabel
import org.opalj.br.instructions.LabeledTABLESWITCH
import org.opalj.br.instructions.LabeledLOOKUPSWITCH
import org.opalj.log.LogContext
import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger.info

/**
 * Factory to create an initial [[CodeAttributeBuilder]].
 *
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
object CODE {

    implicit def logContext: LogContext = GlobalLogContext

    final val CodeConfigKeyPrefix = "org.opalj.ba.CODE."
    final val LogDeadCodeRemovalConfigKey = CodeConfigKeyPrefix+"logDeadCodeRemoval"
    final val LogDeadCodeConfigKey = CodeConfigKeyPrefix+"logDeadCode"
    final val LogCodeRewritingConfigKey = CodeConfigKeyPrefix+"logCodeRewriting"

    @volatile private[this] var logDeadCodeRemoval: Boolean = true
    @volatile private[this] var logDeadCode: Boolean = true
    @volatile private[this] var logCodeRewriting: Boolean = true

    def setBaseConfig(config: Config) = {
        logDeadCodeRemoval = config.getBoolean(LogDeadCodeRemovalConfigKey)
        info("code generation", s"compile-time dead code removal is logged: $logDeadCodeRemoval")
        logDeadCode = config.getBoolean(LogDeadCodeConfigKey)
        info("code generation", s"compile-time dead code is logged: $logDeadCode")
        logCodeRewriting = config.getBoolean(LogCodeRewritingConfigKey)
        info("code generation", s"code rewritings are logged: $logCodeRewriting")
    }

    setBaseConfig(ConfigFactory.load(this.getClass.getClassLoader()))

    /**
     * Removes (compile-time) dead code from the given code; never removes "PCLabels" to
     * ensure the completeness of pc mappings.
     */
    def removeDeadCode[T](codeElements: IndexedSeq[CodeElement[T]]): IndexedSeq[CodeElement[T]] = {
        val codeElementsSize = codeElements.size
        if (codeElementsSize == 0)
            return codeElements;

        // Basic idea - mark all code elements as live that are potentially executed and throw away
        // the rest!
        // 1 - We first go linearly over all code elements to find all catch handlers and add them
        //     to the set of all code elements that should be marked as live. (Actually, we add
        //     to the set the pseudo instructions directly following the preceding
        //     instruction.) Additionally, we compute the "label => index" relation.
        // 2 - we follow the cfg to mark the reachable code elements as live
        // 3 - we remove the dead code
        //
        // Note:
        // We will later test if we have broken exception handlers; hence, we just assume they
        // are valid w.r.t. the given code!

        var markedAsLive: IntTrieSet = IntTrieSet1(0)
        val isLive = new Array[Boolean](codeElementsSize)
        var isLiveCount = 0
        val labelsToIndexes = new Object2IntOpenHashMap[InstructionLabel]()
        val catchLabelsToIndexes = new Object2IntOpenHashMap[Symbol]()
        val tryLabelsToIndexes = new Object2IntOpenHashMap[Symbol]()
        labelsToIndexes.defaultReturnValue(Int.MinValue)

        // Helper function:
        // Marks the code element with the given index or – if pseudo instructions are
        // preceding it the earliest directly preceding pseudo instruction - as live.
        def markAsLive(index: Int): Unit = {
            var currentIndex = index
            while (currentIndex > 0) {
                if (isLive(currentIndex) || markedAsLive.contains(currentIndex)) {
                    return ; // nothing to do
                }
                currentIndex -= 1
                if (!codeElements(currentIndex).isPseudoInstruction) {
                    markedAsLive += (currentIndex + 1)
                    return ;
                }
            }
            // the code element "0" is already marked as live..
        }

        // Step 1
        iterateUntil(0, codeElementsSize) { index ⇒
            codeElements(index) match {

                case LabelElement(label) ⇒
                    if (labelsToIndexes.containsKey(label)) {
                        throw new IllegalArgumentException(s"jump '$label is already used")
                    }
                    labelsToIndexes.put(label, index)

                case TRY(label) ⇒
                    if (tryLabelsToIndexes.containsKey(label)) {
                        throw new IllegalArgumentException(s"try '${label.name} is already used")
                    }
                    tryLabelsToIndexes.put(label, index)
                case CATCH(label, _) ⇒
                    if (catchLabelsToIndexes.containsKey(label)) {
                        throw new IllegalArgumentException(s"catch '${label.name} is already used")
                    }
                    catchLabelsToIndexes.put(label, index)

                case _ ⇒ // nothing to do
            }
        }
        // we do not want to mark "dead" catch blocks as live
        tryLabelsToIndexes.keySet().iterator().forEachRemaining { label ⇒
            if (!catchLabelsToIndexes.containsKey(label)) {
                throw new IllegalArgumentException(s"'try block $label without catch")
            }
            markAsLive(catchLabelsToIndexes.getInt(label))
        }

        // Step 2.1
        def handleBranchTarget(branchTarget: InstructionLabel): Unit = {
            val targetIndex = labelsToIndexes.getInt(branchTarget)
            if (targetIndex == Int.MinValue) {
                val message = s"the $branchTarget label could not be resolved"
                throw new NoSuchElementException(message)
            }
            markAsLive(targetIndex)
        }
        while (markedAsLive.nonEmpty) {
            // mark all code elements which can be executed subsequently as live
            val (nextIndex, newMarkedAsLive) = markedAsLive.getAndRemove
            var currentIndex = nextIndex
            markedAsLive = newMarkedAsLive
            if (!isLive(currentIndex)) {
                do {
                    isLive(currentIndex) = true
                    isLiveCount += 1
                    currentIndex += 1
                } while (currentIndex < codeElementsSize && !isLive(currentIndex) && {
                    // Currently, we make the assumption that the instruction following the
                    // JSR is live... i.e., a RET exists (which should always be the case for
                    // proper code!)
                    codeElements(currentIndex - 1) match {
                        case _: PseudoInstruction ⇒ true
                        case InstructionLikeElement(li) ⇒
                            if (li.isControlTransferInstruction) {
                                li.branchTargets.foreach(handleBranchTarget)
                                // let's check if we have a "fall-through"
                                li match {
                                    case LabeledJSR(_) | LabeledJSR_W(_) |
                                        _: LabeledSimpleConditionalBranchInstruction ⇒
                                        // let's continue...
                                        true
                                    case _ ⇒
                                        // ... we have a goto(_w) instruction, hence
                                        // the next instruction like element is only live if we have
                                        // an explicit jump to it, or if it is the start
                                        // of an exception handler...
                                        false
                                }
                            } else if (li.isReturnInstruction || li.isAthrow) {
                                false
                            } else {
                                true
                            }
                    }
                })
            }
        }

        if (isLiveCount < codeElementsSize) {
            // Step 2.2 We now have to test for still required TRY-Block and LINENUMBER markers..
            //          (Basically, we just set them to "isLive".)
            //          A TRY/TRYEND marker is to be live if we have one or more live instructions
            //          between two corresponding markers.
            //          A LINENUMBER marker is set to live if we have no live LINENUMBER marker
            //          before the next live instruction.
            //          PC based `InstructionLabels` are ALWAYS set to live to facilitate
            //          remappings!

            // If we just had some dead PC based labels, we continue using the old code elements...
            iterateUntil(0, codeElementsSize) { index ⇒
                if (!isLive(index)) {
                    codeElements(index) match {
                        case LabelElement(_: PCLabel) ⇒
                            isLiveCount += 1
                            isLive(index) = true

                        case _: LINENUMBER ⇒
                            var nextIndex = index + 1
                            while (nextIndex < codeElementsSize) {
                                codeElements(nextIndex) match {

                                    case _: InstructionLikeElement[T] if isLive(nextIndex) ⇒
                                        isLive(index) = true
                                        isLiveCount += 1
                                        nextIndex = codeElementsSize // <=> abort loop

                                    case _: LINENUMBER ⇒
                                        nextIndex = codeElementsSize // <=> abort loop

                                    case _ ⇒
                                        nextIndex += 1
                                }
                            }

                        case TRY(label) ⇒
                            var nextIndex = index + 1
                            while (nextIndex < codeElementsSize) {
                                codeElements(nextIndex) match {

                                    case _: InstructionLikeElement[T] if isLive(nextIndex) ⇒
                                        isLive(index) = true
                                        isLiveCount += 1
                                        nextIndex = Int.MaxValue // <=> abort loop (successful)

                                    case TRYEND(`label`) ⇒
                                        nextIndex = Int.MaxValue // <=> abort loop (successful)

                                    case _ ⇒
                                        nextIndex += 1
                                }
                            }
                            if (nextIndex == codeElementsSize) {
                                throw new IllegalArgumentException(s"'try $label without try end")
                            }

                        case TRYEND(label) ⇒
                            // We have found a "still dead" try end; if the TRY is (now) live,
                            // we simply set TRYEND to live... otherwise it remains dead;
                            // we check the intermediate range when we see the TRY (if required).
                            if (!tryLabelsToIndexes.containsKey(label)) {
                                throw new IllegalArgumentException(s"'try end $label without try")
                            }
                            if (isLive(tryLabelsToIndexes(label))) {
                                isLiveCount += 1
                                isLive(index) = true
                            }

                        case _ ⇒ // nothing to do
                    }
                }
            }
            if (isLiveCount == codeElementsSize) {
                // eventually nothing is dead...
                return codeElements;
            }

            // Step 3 - create new code elements
            val deadCodeElementsCount = codeElementsSize - isLiveCount
            if (logDeadCodeRemoval) {
                info("code generation", s"found $deadCodeElementsCount dead code elements")
            }
            if (logDeadCode) {
                val deadCode = new Array[String](deadCodeElementsCount)
                var deadCodeIndex = 0
                iterateUntil(0, codeElements.size) { index ⇒
                    if (!isLive(index)) {
                        deadCode(deadCodeIndex) = s"$index: ${codeElements(index)}"
                        deadCodeIndex += 1
                    }
                }
                info(
                    "code generation",
                    deadCode.mkString("compile-time dead code elements:\n\t", "\n\t", "\n")
                )
            }

            val newCodeElements = new ArrayBuffer[CodeElement[T]](isLiveCount)
            iterateUntil(0, codeElementsSize) { index ⇒
                if (isLive(index)) newCodeElements += codeElements(index)
            }
            // if we have removed a try block we now have to remove the handler's code...
            // [debug] println(codeElements.mkString("old\n\t", "\n\t", "\n"))
            // [debug] println(newCodeElements.mkString("new:\n\t", "\n\t", "\n\n"))
            removeDeadCode(newCodeElements) // tail-recursive call...
        } else {
            codeElements
        }
    }

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

    def apply[T](initialCodeElements: IndexedSeq[CodeElement[T]]): CodeAttributeBuilder[T] = {
        val codeElements = removeDeadCode(initialCodeElements)
        val codeElementsSize = codeElements.size
        val instructionLikes = new ArrayBuffer[LabeledInstruction](codeElementsSize)
        val pcToCodeElementIndex = new Int2IntArrayMap(codeElementsSize)

        var labels = Map.empty[InstructionLabel, br.PC]
        var annotations = Map.empty[br.PC, T]
        val exceptionHandlerBuilder = new ExceptionHandlerGenerator()
        val lineNumberTableBuilder = new LineNumberTableBuilder()
        var hasControlTransferInstructions = false
        val pcMapping = new PCMapping(initialSize = codeElements.length) // created based on `PCLabel`s

        var currentPC = 0
        var nextPC = 0
        var modifiedByWide = false
        // fill the instructionLikes array with `null`s for PCs representing instruction arguments
        iterateUntil(0, codeElementsSize) { index ⇒
            codeElements(index) match {
                case ile @ InstructionLikeElement(i) ⇒
                    currentPC = nextPC
                    nextPC = i.indexOfNextInstruction(currentPC, modifiedByWide)
                    if (ile.isAnnotated) annotations += ((currentPC, ile.annotation))
                    instructionLikes.append(i)
                    pcToCodeElementIndex.put(currentPC, index)
                    rerun((nextPC - currentPC) - 1) {
                        instructionLikes.append(null)
                    }

                    modifiedByWide = i == WIDE
                    hasControlTransferInstructions |= i.isControlTransferInstruction

                case LabelElement(label) ⇒
                    if (label.isPCLabel) {
                        // let's store the mapping to make it possible to remap the other attributes..
                        pcMapping += (label.pc, nextPC)
                    }
                    labels += (label → nextPC)

                case e: ExceptionHandlerElement ⇒ exceptionHandlerBuilder.add(e, nextPC)

                case l: LINENUMBER              ⇒ lineNumberTableBuilder.add(l, nextPC)
            }
        }

        val codeSize = instructionLikes.size
        require(codeSize > 0, "no code found")
        val exceptionHandlers = exceptionHandlerBuilder.result()
        val attributes = lineNumberTableBuilder.result()

        val instructions = new Array[Instruction](codeSize)
        var codeElementsToRewrite = IntArraySet.empty
        iterateUntil(0, codeSize) { pc ⇒
            // Idea: first collect all instructions that definitively need to be rewritten;
            // then do the rewriting and then start the code generation again. Due to the
            // rewriting – which will cause the code to become even longer - it might be
            // necessary to rewrite even more ifs, gotos and switches.
            val labeledInstruction = instructionLikes(pc)
            if (labeledInstruction != null) {
                // We implicitly (there will be an exception) check if we have to adapt ifs,
                // gotos and switches. (In general, this should happen, very, very, very
                // infrequently and hence will hardly every be a performance problem!)
                try {
                    instructions(pc) = labeledInstruction.resolveJumpTargets(pc, labels)
                } catch {
                    case _: BranchoffsetOutOfBoundsException ⇒
                        val codeIndex = pcToCodeElementIndex.get(pc)
                        if (logCodeRewriting) {
                            info(
                                "code generation",
                                s"rewriting ${codeElements(codeIndex)} - branchoffset out of bounds"
                            )
                        }
                        codeElementsToRewrite += codeIndex
                }
            }
        }
        if (codeElementsToRewrite.nonEmpty) {
            val newCodeElements = new ArrayBuffer[CodeElement[T]](codeElementsSize)
            newCodeElements ++= codeElements

            codeElementsToRewrite.reverseIntIterator.foreach { index ⇒
                val InstructionElement(i) = codeElements(index)
                i match {
                    case LabeledGOTO(label) ⇒ newCodeElements(index) = LabeledGOTO_W(label)

                    case LabeledJSR(label)  ⇒ newCodeElements(index) = LabeledJSR_W(label)

                    case scbi: LabeledSimpleConditionalBranchInstruction ⇒
                        //          if_<cond> => y
                        //   x:     ...
                        //   y:     ...
                        // is rewritten to:
                        //          if_!<cond> => r     // in place update
                        //          goto_w Y            // added
                        //   r:x:  ...                  // added label "r"
                        val r = RewriteLabel()
                        val y = scbi.branchTarget
                        newCodeElements(index) = scbi.negate(r)
                        newCodeElements.insert(index + 1, LabeledGOTO_W(y))
                        newCodeElements.insert(index + 2, LabelElement(r))

                    case _: LabeledTABLESWITCH  ⇒ ??? // TODO implement rewriting table switches if the branch offsets are out of bounds
                    case _: LabeledLOOKUPSWITCH ⇒ ??? // TODO implement rewriting lookup switches if the branch offsets are out of bounds

                }
            }
            // We had to rewrite the code; hence, we have to start all over again!
            return this(newCodeElements);
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
