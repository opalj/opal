/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import java.util.NoSuchElementException

import scala.collection.mutable.ArrayBuffer

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap

import org.opalj.control.repeat
import org.opalj.control.iterateUntil
import org.opalj.log.LogContext
import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger.info
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
import org.opalj.collection.immutable.IntRefPair

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

    def setBaseConfig(config: Config): Unit = {
        logDeadCodeRemoval = config.getBoolean(LogDeadCodeRemovalConfigKey)
        info("code generation", s"compile-time dead code removal is logged: $logDeadCodeRemoval")
        logDeadCode = config.getBoolean(LogDeadCodeConfigKey)
        info("code generation", s"compile-time dead code is logged: $logDeadCode")
        logCodeRewriting = config.getBoolean(LogCodeRewritingConfigKey)
        info("code generation", s"code rewritings are logged: $logCodeRewriting")
    }

    setBaseConfig(ConfigFactory.load(this.getClass.getClassLoader))

    /**
     * Removes (compile-time) dead (pseudo) instructions from the given code by
     * performing a most conservative control-flow analysis. The goal is to remove
     * just those instructions which would hinder the computation of a stack-map
     * table.
     * Data-flow information (in particular the potential types of concrete exceptions)
     * are not tracked; i.e., if we have a try block with some instructions the related
     * catch block is considered to be live, even if the declared exception is never thrown).
     * TODO If we have a try block with instructions that NEVER throw any exception, we should remove it; requires (control-flow dependent!) tests when we set a TRY to live ...)
     * We never remove "PCLabels" to ensure the completeness of pc mappings.
     *
     * @note The code element has to be valid bytecode; i.e., a verification of the code using
     *       the old, pre Java 7 (type-inference based) bytecode verified would succeed!
     */
    def removeDeadCode[T](codeElements: scala.collection.IndexedSeq[CodeElement[T]]): scala.collection.IndexedSeq[CodeElement[T]] = {
        val codeElementsSize = codeElements.size
        if (codeElementsSize == 0)
            return codeElements;

        // Basic idea - mark all code elements as live that are potentially executed. Throw away
        // the rest!
        // 1 -  We collect all labels of all jump targets and try/catch elements
        //      (We eagerly check the uniqueness of the labels and that a TRYEND always
        //      has a corresponding TRY; we do not perform any other checks eagerly.)
        // 2 -  we follow the cfg to mark the reachable code elements as live; as soon as
        //      we mark a TRY as live, we will mark the corresponding CATCH as live; i.e.,
        //      schedule the marking of the first instruction of the catch block as live.
        // 3 -  we remove the dead code
        // 4 -  if we have found dead code rerun the algorithm to detect further dead code;
        //      otherwise return the given codeElements as is

        // The core data-structure of the worklist algorithm which stores the elements
        // that need to be processed.
        var markedAsLive: IntTrieSet = IntTrieSet1(0)

        // The special handling is required due to the tracking of this information by the
        // TypeLevelDomain which is used to compute the stack map table.
        var monitorInstructionIsUsed = false

        // A boolean array containing the information which elements are live.
        val isLive = new Array[Boolean](codeElementsSize)
        var isLiveCount = 0

        val labelsToIndexes = new Object2IntOpenHashMap[InstructionLabel]()
        labelsToIndexes.defaultReturnValue(Int.MinValue)
        val tryLabelsToIndexes = new Object2IntOpenHashMap[Symbol]()
        tryLabelsToIndexes.defaultReturnValue(Int.MinValue)
        val tryEndLabelsToIndexes = new Object2IntOpenHashMap[Symbol]()
        tryEndLabelsToIndexes.defaultReturnValue(Int.MinValue)
        val catchLabelsToIndexes = new Object2IntOpenHashMap[Symbol]()
        catchLabelsToIndexes.defaultReturnValue(Int.MinValue)

        // Step 1
        iterateUntil(0, codeElementsSize) { index =>
            codeElements(index) match {

                case LabelElement(label) =>
                    if (labelsToIndexes.containsKey(label)) {
                        throw new IllegalArgumentException(s"jump '$label is already used")
                    }
                    labelsToIndexes.put(label, index)

                case TRY(label) =>
                    if (tryLabelsToIndexes.containsKey(label)) {
                        throw new IllegalArgumentException(s"try '${label.name} is already used")
                    }
                    tryLabelsToIndexes.put(label, index)
                case TRYEND(label) =>
                    if (tryEndLabelsToIndexes.containsKey(label)) {
                        throw new IllegalArgumentException(s"tryend '${label.name} is already used")
                    }
                    if (!tryLabelsToIndexes.containsKey(label)) {
                        throw new IllegalArgumentException(
                            s"tryend '${label.name} without or before try"
                        )
                    }
                    tryEndLabelsToIndexes.put(label, index)

                case CATCH(label, _, _) =>
                    if (catchLabelsToIndexes.containsKey(label)) {
                        throw new IllegalArgumentException(s"catch '${label.name} is already used")
                    }
                    catchLabelsToIndexes.put(label, index)

                case _ => // nothing to do
            }
        }

        // Step 2.1
        // Helper function:

        // Marks the CATCH/handler corresponding to the given try as live and schedules
        // the first (real) instruction.
        def markHandlerAsLive(liveTryStart: TRY): Unit = {
            // We have to check the CATCH
            val TRY(label) = liveTryStart
            val catchIndex = catchLabelsToIndexes.getInt(label)
            if (catchIndex == Int.MinValue) {
                throw new IllegalArgumentException(s"try '${label.name} without catch")
            }

            if (!isLive(catchIndex)) {
                // DEBUG: println(s"[markHandlerAsLive] setting catch $label ($catchIndex) to live")
                isLive(catchIndex) = true
                isLiveCount += 1

                var indexOfFirstInstruction = (catchIndex + 1)
                while (!codeElements(indexOfFirstInstruction).isInstructionLikeElement) {
                    indexOfFirstInstruction += 1
                }
                markedAsLive += indexOfFirstInstruction // we schedule the instruction following the catch
            } else {
                // DEBUG: println(s"[markHandlerAsLive] catch $label ($catchIndex) is already live")
            }
        }

        // Marks the meta-information related to the (pseudo) instruction with the given index
        // as live.
        // A try is only marked live when we do the liveness propagation; this is necessary
        // to detect completely useless try blocks consisting only of instructions which
        // never throw an exception.
        def markMetaInformationAsLive(index: Int): Unit = {
            var currentIndex = index
            // the code element "0" is already marked as live..
            while (currentIndex > 0) {
                if (isLive(currentIndex) || markedAsLive.contains(currentIndex)) {
                    return ; // nothing to do
                }

                val currentInstruction = codeElements(currentIndex)
                if (currentInstruction.isInstructionLikeElement) {
                    // We basically only want to mark TRYs and Jump Labels belonging to
                    // the code element with the given `index` as live.
                    return ;
                } else if (!currentInstruction.isExceptionHandlerElement) {
                    // DEBUG: println(s"[markMetaInformationAsLive] scheduling $index")
                    markedAsLive += currentIndex
                }
                currentIndex -= 1
            }
        }

        def handleBranchTarget(branchTarget: InstructionLabel): Unit = {
            var targetIndex = labelsToIndexes.getInt(branchTarget)
            if (targetIndex == Int.MinValue) {
                val message = s"the $branchTarget label could not be resolved"
                throw new NoSuchElementException(message)
            }

            // Recall that we always eagerly mark all PCLabels as live to enable remappings;
            // hence, it may happen that in (nested) catch blocks, which are only processed
            // in a second step, the target (pseudo) instruction is actually already marked as
            // live though the real instruction is not (yet) live.
            val targetInstruction = codeElements(targetIndex)
            if (targetInstruction.isPseudoInstruction &&
                isLive(targetIndex) &&
                targetInstruction.asPseudoInstruction.isPCLabel) {
                targetIndex += 1
                while (!codeElements(targetIndex).isInstructionLikeElement) {
                    targetIndex += 1
                }
                markedAsLive += targetIndex
                // we schedule the instruction after the label
            }

            markMetaInformationAsLive(targetIndex)
        }

        /* Returns `true` if any instruction was actually marked as live. */
        def processMarkedAsLive(): Unit = {
            while (markedAsLive.nonEmpty) {
                // mark all code elements which can be executed subsequently as live
                val IntRefPair(nextIndex, newMarkedAsLive) = markedAsLive.headAndTail
                markedAsLive = newMarkedAsLive

                var currentIndex = nextIndex
                if (!isLive(currentIndex)) {

                    var currentInstruction: CodeElement[T] = codeElements(currentIndex)
                    var continueIteration = true
                    do {
                        val isNotYetLive = !isLive(currentIndex)
                        if (isNotYetLive && !currentInstruction.isExceptionHandlerElement) {
                            // This check is primarily required due to the eager marking
                            // of PCLabels as live.
                            isLive(currentIndex) = true
                            isLiveCount += 1
                        }

                        // Currently, we make the assumption that the instruction following the
                        // JSR is live... i.e., a RET exists (which should always be the case for
                        // proper code!)
                        continueIteration = currentInstruction match {
                            case pi: PseudoInstruction =>
                                true

                            case InstructionLikeElement(li) =>
                                if (li.isControlTransferInstruction) {
                                    li.branchTargets.foreach(handleBranchTarget)
                                    // let's check if we have a "fall-through"
                                    li match {
                                        case _: LabeledJSR | _: LabeledJSR_W |
                                            _: LabeledSimpleConditionalBranchInstruction =>
                                            // let's continue...
                                            true
                                        case _ =>
                                            // ... we have a goto(_w) instruction, hence
                                            // the next instruction like element is only live
                                            // if we have an explicit jump to it, or if it is
                                            // the start of an exception handler...
                                            false
                                    }
                                } else if (li.isReturnInstruction || li.isAthrow) {
                                    false
                                } else {
                                    if (li.isMonitorInstruction) {
                                        monitorInstructionIsUsed = true
                                    }
                                    true
                                }
                        }
                        currentIndex += 1
                    } while (continueIteration
                        && currentIndex < codeElementsSize
                        && {
                            currentInstruction = codeElements(currentIndex)
                            // In the following we ignore pseudo instructions
                            // (in particular PCLabels)
                            // because they may have been set to live already!
                            currentInstruction.isPseudoInstruction || !isLive(currentIndex)
                        })
                }
            }
        }

        /**
         * Propagates liveness information in particular w.r.t. LINENUMBER and
         * TRY(END) pseudo instructions; updates isLiveCount if necessary.
         */
        def propagateLiveInformation(): Unit = {
            // Step 2.2 We now have to test for still required TRY-Block and LINENUMBER markers..
            //          (Basically, we just set them to "isLive".)
            //          A TRY/TRYEND marker is to be live if we have one or more live instructions
            //          between two corresponding markers.
            //          A LINENUMBER marker is set to live if we have no live LINENUMBER marker
            //          before the next live instruction.
            //          PC based `InstructionLabels` are ALWAYS set to live to facilitate
            //          remappings!

            // If we just had some dead PC based labels, we continue using the old code elements...
            iterateUntil(0, codeElementsSize) { index =>
                if (!isLive(index)) {
                    codeElements(index) match {
                        case LabelElement(_: PCLabel) =>
                            isLiveCount += 1
                            isLive(index) = true

                        case _: LINENUMBER =>
                            var nextIndex = index + 1
                            while (nextIndex < codeElementsSize) {
                                codeElements(nextIndex) match {

                                    case _: InstructionLikeElement[T] if isLive(nextIndex) =>
                                        isLive(index) = true
                                        isLiveCount += 1
                                        nextIndex = Int.MaxValue // <=> abort loop

                                    case _: LINENUMBER =>
                                        nextIndex = Int.MaxValue // <=> abort loop

                                    case _ =>
                                        nextIndex += 1
                                }
                            }

                        case tryStart @ TRY(label) =>
                            // We have to check if some relevant instruction belonging to the
                            // try block is live.
                            var nextIndex = index + 1
                            while (nextIndex < codeElementsSize) {
                                codeElements(nextIndex) match {

                                    case i: InstructionLikeElement[T] =>
                                        val instruction = i.instruction
                                        if (isLive(nextIndex) &&
                                            instruction.mayThrowExceptions &&
                                            (
                                                monitorInstructionIsUsed
                                                || !instruction.isReturnInstruction
                                            )) {
                                            isLive(index) = true
                                            isLiveCount += 1
                                            markHandlerAsLive(tryStart)
                                            nextIndex = Int.MaxValue // <=> abort loop (successful)
                                        } else {
                                            nextIndex += 1
                                        }

                                    case TRYEND(`label`) =>
                                        nextIndex = Int.MaxValue // <=> abort loop (successful)

                                    case _ =>
                                        nextIndex += 1
                                }
                            }
                            if (nextIndex == codeElementsSize) {
                                throw new IllegalArgumentException(s"'try $label without try end")
                            }

                        case TRYEND(label) =>
                            // We have found a "still dead" try end; if the TRY is (now) live,
                            // we simply set TRYEND to live... otherwise it remains dead;
                            // we check the intermediate range when we see the TRY (if required).
                            if (isLive(tryLabelsToIndexes(label))) {
                                isLiveCount += 1
                                isLive(index) = true
                            }

                        case _ => // nothing to do
                    }
                }
            }
        }

        // The main loop processing the worklist data-structure.
        var continueProcessingCode = false
        do {
            continueProcessingCode = false
            processMarkedAsLive()
            val oldIsLiveCount = isLiveCount
            if (oldIsLiveCount < codeElementsSize) {
                propagateLiveInformation()
                if (oldIsLiveCount != isLiveCount && markedAsLive.nonEmpty) {
                    continueProcessingCode = true
                }
            }
        } while (continueProcessingCode)

        // Post-processing
        if (isLiveCount < codeElementsSize) {
            // Step 3 - create new code elements
            val deadCodeElementsCount = codeElementsSize - isLiveCount
            if (logDeadCodeRemoval) {
                info("code generation", s"found $deadCodeElementsCount dead (pseudo)instructions")
            }
            if (logDeadCode) {
                val deadCode = new ArrayBuffer[String](deadCodeElementsCount)
                iterateUntil(0, codeElements.size) { index =>
                    if (!isLive(index)) {
                        deadCode += s"$index: ${codeElements(index)}"
                    }
                }
                info(
                    "code generation",
                    deadCode.mkString("compile-time dead (pseudo)instructions:\n\t", "\n\t", "\n")
                )
            }

            val newCodeElements = new ArrayBuffer[CodeElement[T]](isLiveCount)
            iterateUntil(0, codeElementsSize) { index =>
                if (isLive(index)) {
                    newCodeElements += codeElements(index)
                }
            }
            // if we have removed a try block we now have to remove the handler's code...
            // DEBUG: println(codeElements.zipWithIndex.map(_.swap).mkString("old\n\t", "\n\t", "\n"))
            // DEBUG: println(newCodeElements.zipWithIndex.map(_.swap).mkString("new:\n\t", "\n\t", "\n\n"))
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

    def apply[T](initialCodeElements: scala.collection.IndexedSeq[CodeElement[T]]): CodeAttributeBuilder[T] = {
        val codeElements = removeDeadCode(initialCodeElements)
        val codeElementsSize = codeElements.size
        val instructionLikes = new ArrayBuffer[LabeledInstruction](codeElementsSize)
        val pcToCodeElementIndex = new Int2IntArrayMap(codeElementsSize)

        var labels = Map.empty[InstructionLabel, br.PC]
        var annotations = Map.empty[br.PC, T]
        val exceptionHandlerTableBuilder = new ExceptionHandlerTableBuilder()
        val lineNumberTableBuilder = new LineNumberTableBuilder()
        var hasControlTransferInstructions = false
        val pcMapping = new PCMapping(initialSize = codeElements.length) // created based on `PCLabel`s

        var currentPC = 0
        var nextPC = 0
        var modifiedByWide = false
        // fill the instructionLikes array with `null`s for PCs representing instruction arguments
        iterateUntil(0, codeElementsSize) { index =>
            codeElements(index) match {
                case ile @ InstructionLikeElement(i) =>
                    currentPC = nextPC
                    nextPC = i.indexOfNextInstruction(currentPC, modifiedByWide)
                    if (ile.isAnnotated) annotations += ((currentPC, ile.annotation))
                    instructionLikes.append(i)
                    pcToCodeElementIndex.put(currentPC, index)
                    repeat((nextPC - currentPC) - 1) {
                        instructionLikes.append(null)
                    }

                    modifiedByWide = i == WIDE
                    hasControlTransferInstructions |= i.isControlTransferInstruction

                case LabelElement(label) =>
                    if (label.isPCLabel) {
                        // let's store the mapping to make it possible to remap the other attributes..
                        pcMapping += (label.pc, nextPC)
                    }
                    labels += (label -> nextPC)

                case e: ExceptionHandlerElement => exceptionHandlerTableBuilder.add(e, nextPC)

                case l: LINENUMBER              => lineNumberTableBuilder.add(l, nextPC)
            }
        }

        val codeSize = instructionLikes.size
        require(codeSize > 0, "no code found")
        val exceptionHandlers = exceptionHandlerTableBuilder.result()
        val attributes = lineNumberTableBuilder.result()

        val instructions = new Array[Instruction](codeSize)
        var codeElementsToRewrite = IntArraySet.empty
        iterateUntil(0, codeSize) { pc =>
            // Idea: first collect all instructions that definitively need to be rewritten;
            // then do the rewriting and then start the code generation again. Due to the
            // rewriting – which will cause the code to become even longer - it might be
            // necessary to rewrite even more ifs, gotos and switches.
            val labeledInstruction = instructionLikes(pc)
            if (labeledInstruction != null) {
                // We implicitly (there will be an exception) check if we have to adapt ifs,
                // gotos and switches. (In general, this should happen, very, very, very
                // infrequently and hence will hardly ever be a performance problem!)
                try {
                    instructions(pc) = labeledInstruction.resolveJumpTargets(pc, labels)
                } catch {
                    case _: BranchoffsetOutOfBoundsException =>
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

            codeElementsToRewrite.reverseIntIterator.foreach { index =>
                val InstructionElement(i) = codeElements(index)
                i match {
                    case LabeledGOTO(label) => newCodeElements(index) = LabeledGOTO_W(label)

                    case LabeledJSR(label)  => newCodeElements(index) = LabeledJSR_W(label)

                    case scbi: LabeledSimpleConditionalBranchInstruction =>
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

                    case _: LabeledTABLESWITCH  => ??? // TODO implement rewriting table switches if the branch offsets are out of bounds
                    case _: LabeledLOOKUPSWITCH => ??? // TODO implement rewriting lookup switches if the branch offsets are out of bounds

                }
            }
            // We had to rewrite the code; hence, we have to start all over again!
            return this(newCodeElements.toIndexedSeq);
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
