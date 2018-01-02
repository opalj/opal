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
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap

import scala.collection.mutable.ArrayBuffer
import org.opalj.control.rerun
import org.opalj.control.iterateUntil
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.WIDE
import org.opalj.br.instructions.LabeledInstruction
import org.opalj.br.instructions.InstructionLabel
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.IntTrieSet1
import org.opalj.br.instructions.LabeledJSR
import org.opalj.br.instructions.LabeledJSR_W
import org.opalj.br.instructions.LabeledSimpleConditionalBranchInstruction
import org.opalj.log.LogContext
import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger.info

/**
 * Factory method to create an initial [[CodeAttributeBuilder]].
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

    def filterDeadCode[T](codeElements: IndexedSeq[CodeElement[T]]): IndexedSeq[CodeElement[T]] = {

        // Basic idea - mark all code elements as live:
        // 1 - We first go linearly over all code elements to find all catch handlers and add them
        //     to the set of all code elements that should be marked as live. (Actually, we add
        //     to the set the pseudo instructions directly following the preceding
        //     instruction.) Additionally, we compute the "label => index" relation.
        // 2 - we follow the cfg to mark the live code elements
        // 3 - we remove the dead code
        //
        // Note:
        // We will later test if we have broken exception handlers; hence, we just assume they
        // are valid for the time being! (E.g., if all instructions in the try block are dead,
        // the try will also be dead, and creating the exception handler will fail..)
        var markedAsLive: IntTrieSet = IntTrieSet1(0)
        val isLive = new Array[Boolean](codeElements.size)
        var isLiveCount = 0
        val labelsToIndexes = new Object2IntOpenHashMap[InstructionLabel]()
        labelsToIndexes.defaultReturnValue(Int.MinValue)

        // Marks the code element with the given index or – if pseudo instructions are
        // preceding it – the earliest directly preceding pseudo instruction as live.
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
        iterateUntil(0, codeElements.size) { index ⇒
            codeElements(index) match {
                case LabelElement(label) ⇒
                    if (labelsToIndexes.containsKey(label)) {
                        throw new IllegalArgumentException(s"'$label is already used")
                    }
                    labelsToIndexes.put(label, index)
                case _: CATCH ⇒ markAsLive(index) // TODO only if we have a try block!
                case _        ⇒ // nothing to do
            }
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
                } while (currentIndex < codeElements.size && !isLive(currentIndex) && {
                    // Currently, we make the assumption that the instruction following the
                    // JSR is live... i.e., a RET exists (which should always be the case for
                    // proper code!)
                    //
                    codeElements(currentIndex - 1) match {
                        case _: PseudoInstruction ⇒ true
                        case InstructionLikeElement(li) ⇒
                            !li.isControlTransferInstruction || {
                                li.branchTargets.foreach(handleBranchTarget)
                                // let's check if we have a "fall-through"
                                li match {
                                    case LabeledJSR(_) | LabeledJSR_W(_) |
                                        _: LabeledSimpleConditionalBranchInstruction ⇒
                                        // let's continue...
                                        true
                                    case _ ⇒
                                        // the next code element is only live if we have
                                        // an explicit jump to it, or if it is the start
                                        // of an exception handler...
                                        false
                                }
                            }
                    }
                })
            }
        }
        // Step 2.2 We now have to test for still required TRY-Block and LINENUMBER markers..

        // Step 3
        if (isLiveCount < codeElements.size) {
            val deadCodeElementsCount = codeElements.size - isLiveCount
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
            iterateUntil(0, codeElements.size) { index ⇒
                if (isLive(index)) {
                    newCodeElements += codeElements(index)
                }
            }
            filterDeadCode(newCodeElements); // tail-recursive call...
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
        val codeElements = filterDeadCode(initialCodeElements)
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
                instructions(pc) = labeledInstruction.resolveJumpTargets(pc, labels)
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
