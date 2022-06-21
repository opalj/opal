/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import scala.annotation.tailrec
import scala.annotation.switch
import scala.reflect.ClassTag
import java.util.Arrays.fill
import scala.collection.AbstractIterator
import scala.collection.mutable
import scala.collection.immutable.IntMap
import scala.collection.immutable.Queue
import org.opalj.util.AnyToAnyThis
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.collection.IntIterator
import org.opalj.collection.immutable.IntArraySet
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.IntTrieSet1
import org.opalj.collection.immutable.BitArraySet
import org.opalj.collection.immutable.IntIntPair
import org.opalj.collection.immutable.EmptyIntTrieSet
import org.opalj.collection.mutable.IntQueue
import org.opalj.collection.mutable.IntArrayStack
import org.opalj.br.cfg.CFGFactory
import org.opalj.br.cfg.CFG
import org.opalj.br.ClassHierarchy.PreInitializedClassHierarchy
import org.opalj.br.instructions._

/**
 * Representation of a method's code attribute, that is, representation of a method's
 * implementation.
 *
 * @param   maxStack The maximum size of the stack during the execution of the method.
 *          This value is determined by the compiler and is not necessarily the minimum.
 *          However, in the vast majority of cases it is the minimum.
 * @param   maxLocals The number of registers/local variables needed to execute the method.
 *          As in case of `maxStack` this number is expected to be the minimum, but this
 *          is not guaranteed.
 * @param   instructions The instructions of this `Code` array/`Code` block. Since the
 *          code array is not completely filled (it contains `null` values) the
 *          preferred way to iterate over all instructions is to use for-comprehensions
 *          and pattern matching or to use one of the predefined methods [[foreach]],
 *          [[collect]], [[collectPair]], [[collectWithIndex]], etc..
 *          The `instructions` array must not be mutated!
 *
 * @author Michael Eichberg
 */
final class Code private (
        val maxStack:          Int,
        val maxLocals:         Int,
        val instructions:      Array[Instruction],
        val exceptionHandlers: ExceptionHandlers,
        val attributes:        Attributes
) extends Attribute
    with CommonAttributes
    with InstructionsContainer
    with CodeSequence[Instruction]
    with Iterable[PCAndInstruction] {
    code =>

    def copy(
        maxStack:          Int                = this.maxStack,
        maxLocals:         Int                = this.maxLocals,
        instructions:      Array[Instruction] = this.instructions,
        exceptionHandlers: ExceptionHandlers  = this.exceptionHandlers,
        attributes:        Attributes         = this.attributes
    ): Code = {
        new Code(maxStack, maxLocals, instructions, exceptionHandlers, attributes)
    }

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        other match {
            case that: Code => this.similar(that, config)
            case _          => false
        }
    }

    def similar(other: Code, config: SimilarityTestConfiguration): Boolean = {

        if (!(this.maxStack == other.maxStack && this.maxLocals == other.maxLocals)) {
            return false;
        }
        if (this.exceptionHandlers != other.exceptionHandlers) {
            return false;
        }

        if (!(
            this.instructions.length == other.instructions.length && {
                var areEqual = true
                val max = instructions.length
                var i = 0
                while (i < max && areEqual) {
                    val thisI = this.instructions(i)
                    val otherI = other.instructions(i)
                    areEqual = (thisI == null && otherI == null) ||
                        (thisI != null && otherI != null && thisI.similar(otherI))
                    i += 1
                }
                areEqual
            }
        )) {
            return false;
        }

        compareAttributes(other.attributes, config).isEmpty
    }

    @inline final def codeSize: Int = instructions.length

    override def iterator: Iterator[PCAndInstruction] = {
        new AbstractIterator[PCAndInstruction] {
            private[this] var pc = 0

            def hasNext: Boolean = pc < instructions.length

            def next(): PCAndInstruction = {
                val inst = PCAndInstruction(pc, instructions(pc))
                pc = inst.instruction.indexOfNextInstruction(pc)(code)
                inst
            }
        }
    }

    def instructionIterator: Iterator[Instruction] = this.iterator.map(_.instruction)

    override def instructionsOption: Some[Array[Instruction]] = Some(instructions)

    /**
     * Returns an iterator to iterate over the program counters (`pcs`) of the instructions
     * of this `Code` block.
     *
     * @see See the method [[foreach]] for an alternative.
     */
    def programCounters: IntIterator = new IntIterator {
        private[this] var nextPC = 0 // there is always at least one instruction
        def hasNext: Boolean = nextPC < instructions.length
        def next(): Int = { val pc = nextPC; nextPC = pcOfNextInstruction(nextPC); pc }
    }

    def foreachProgramCounter[U](f: Int => U): Unit = {
        var nextPC = 0 // there is always at least one instruction
        val maxPC = instructions.length
        while (nextPC < maxPC) {
            f(nextPC)
            nextPC = pcOfNextInstruction(nextPC)
        }
    }

    /**
     * Counts the number of instructions.
     *
     * @note The number of instructions is always smaller or equal to the size of the code array.
     * @note This operation has complexity O(n).
     */
    def instructionsCount: Int = {
        var c = 0
        var pc = 0
        val max = instructions.length
        while (pc < max) {
            c += 1
            pc = pcOfNextInstruction(pc)
        }
        c
    }

    /**
     * Calculates for each instruction the subroutine to which it belongs to – if any.
     * This information is required to, e.g., identify the subroutine
     * contexts that need to be reset in case of an exception in a subroutine.
     *
     * @note   Calling this method only makes sense for Java bytecode that actually contains
     *         [[org.opalj.br.instructions.JSR]] and [[org.opalj.br.instructions.RET]]
     *         instructions.
     *
     * @return Basically a map that maps the `pc` of each instruction to the id of the
     *         subroutine.
     *         For each instruction (with a specific `pc`) the `pc` of the first instruction
     *         of the subroutine it belongs to is returned. The pc `0` identifies the instruction
     *         as belonging to the core method. The pc `-1` identifies the instruction as
     *         dead by compilation.
     */
    def belongsToSubroutine(): Array[Int] = {
        val subroutineIds = new Array[Int](instructions.length)
        fill(subroutineIds, -1) // <= initially all instructions belong to "no routine"

        val nextSubroutines = new IntQueue(0)

        def propagate(subroutineId: Int, subroutinePC: Int /*PC*/ ): Unit = {

            val nextPCs = new IntQueue(subroutinePC)
            while (nextPCs.nonEmpty) {
                val pc = nextPCs.dequeue
                if (subroutineIds(pc) == -1) {
                    subroutineIds(pc) = subroutineId
                    val instruction = instructions(pc)

                    (instruction.opcode: @switch) match {
                        case ATHROW.opcode                                    =>
                        /* Nothing do to; will be handled when we deal with exceptions. */

                        case /* xReturn: */ 176 | 175 | 174 | 172 | 173 | 177 =>
                        /* Nothing to do; there are no successor! */

                        case RET.opcode                                       =>
                        /*Nothing to do; handled by JSR*/
                        case JSR.opcode | JSR_W.opcode =>
                            val UnconditionalBranchInstruction(branchoffset) = instruction
                            nextSubroutines.enqueue(pc + branchoffset)
                            nextPCs.enqueue(pcOfNextInstruction(pc))

                        case GOTO.opcode | GOTO_W.opcode =>
                            val UnconditionalBranchInstruction(branchoffset) = instruction
                            nextPCs.enqueue(pc + branchoffset)

                        case /*IFs:*/ 165 | 166 | 198 | 199 |
                            159 | 160 | 161 | 162 | 163 | 164 |
                            153 | 154 | 155 | 156 | 157 | 158 =>
                            val SimpleConditionalBranchInstruction(branchoffset) = instruction
                            nextPCs.enqueue(pc + branchoffset)
                            nextPCs.enqueue(pcOfNextInstruction(pc))

                        case TABLESWITCH.opcode | LOOKUPSWITCH.opcode =>
                            val SwitchInstruction(defaultOffset, jumpOffsets) = instruction
                            nextPCs.enqueue(pc + defaultOffset)
                            jumpOffsets foreach { jumpOffset => nextPCs.enqueue(pc + jumpOffset) }

                        case _ =>
                            nextPCs.enqueue(pcOfNextInstruction(pc))
                    }
                }
            }
        }

        var remainingExceptionHandlers = exceptionHandlers

        while (nextSubroutines.nonEmpty) {
            val subroutineId = nextSubroutines.dequeue
            propagate(subroutineId, subroutineId)

            // all handlers that handle exceptions related to one of the instructions
            // belonging to this subroutine belong to this subroutine (unless the handler
            // is already associated with a previous subroutine!)
            def belongsToCurrentSubroutine(startPC: Int, endPC: Int, handlerPC: Int): Boolean = {
                var currentPC = startPC
                while (currentPC < endPC) {
                    if (subroutineIds(currentPC) != -1) {
                        propagate(subroutineId, handlerPC)
                        // we are done
                        return true;
                    } else {
                        currentPC = pcOfNextInstruction(currentPC)
                    }
                }
                false
            }

            remainingExceptionHandlers = remainingExceptionHandlers filter { eh =>
                subroutineIds(eh.handlerPC) == -1 && // we did not already analyze the handler
                    !belongsToCurrentSubroutine(eh.startPC, eh.endPC, eh.handlerPC)
            }
        }

        subroutineIds
    }

    /**
     * Returns the set of all program counters where two or more control flow
     * paths may join.
     *
     * ==Example==
     * {{{
     *     0: iload_1
     *     1: ifgt    6
     *     2: iconst_1
     *     5: goto 10
     *     6: ...
     *     9: iload_1
     *    10: return // <= PATH JOIN: the predecessors are the instructions 5 and 9.
     * }}}
     *
     * In case of exception handlers the sound overapproximation is made that
     * all exception handlers with a fitting type may be reached on multiple paths.
     */
    def cfJoins(
        implicit
        classHierarchy: ClassHierarchy = PreInitializedClassHierarchy
    ): IntTrieSet = {
        /* OLD - DOESN'T USE THE CLASS HIERARCHY!
        val instructions = this.instructions
        val instructionsLength = instructions.length
        val cfJoins = new mutable.BitSet(instructionsLength)
        exceptionHandlers.foreach { eh =>
            // [REFINE] For non-finally handlers, test if multiple paths
            // can lead to the respective exception
            cfJoins += eh.handlerPC
        }
        // The algorithm determines for each instruction the successor instruction
        // that is reached and then marks it. If an instruction was already reached in the
        // past, it will then mark the instruction as a "join" instruction.
        val isReached = new mutable.BitSet(instructionsLength)
        isReached += 0 // the first instruction is always reached!
        var pc = 0
        while (pc < instructionsLength) {
            val instruction = instructions(pc)
            val nextPC = pcOfNextInstruction(pc)
            @inline def runtimeSuccessor(pc: PC): Unit = {
                if (isReached.contains(pc))
                    cfJoins += pc
                else
                    isReached += pc
            }
            (instruction.opcode: @scala.annotation.switch) match {
                case ATHROW.opcode => /*already handled*/

                case RET.opcode    => /*Nothing to do; handled by JSR*/
                case JSR.opcode | JSR_W.opcode =>
                    val jsrInstr = instruction.asInstanceOf[JSRInstruction]
                    runtimeSuccessor(pc + jsrInstr.branchoffset)
                    runtimeSuccessor(nextPC)

                case GOTO.opcode | GOTO_W.opcode =>
                    val bInstr = instruction.asInstanceOf[UnconditionalBranchInstruction]
                    runtimeSuccessor(pc + bInstr.branchoffset)

                case /*IFs:*/ 165 | 166 | 198 | 199 |
                    159 | 160 | 161 | 162 | 163 | 164 |
                    153 | 154 | 155 | 156 | 157 | 158 =>
                    val bInstr = instruction.asInstanceOf[SimpleConditionalBranchInstruction]
                    val jumpTargetPC = pc + bInstr.branchoffset
                    if (jumpTargetPC != nextPC) {
                        // we have an "if" that always immediately continues with the next
                        // instruction; hence, this "if" is useless
                        runtimeSuccessor(jumpTargetPC)
                    }
                    runtimeSuccessor(nextPC)

                case TABLESWITCH.opcode | LOOKUPSWITCH.opcode =>
                    instruction.nextInstructions(pc)(code, null /*not required!*/ ) foreach { pc =>
                        runtimeSuccessor(pc)
                    }

                case /*xReturn:*/ 176 | 175 | 174 | 172 | 173 | 177 =>
                /*Nothing to do. (no successor!)*/

                case _ =>
                    runtimeSuccessor(nextPC)
            }
            pc = nextPC
        }
        cfJoins
        */
        val instructions = this.instructions
        val instructionsLength = instructions.length

        var cfJoins: IntTrieSet = EmptyIntTrieSet

        val isReached = new Array[Boolean](instructionsLength)
        isReached(0) = true // the first instruction is always reached!

        var pc = 0
        while (pc < instructionsLength) {
            val instruction = instructions(pc)
            val nextPC = pcOfNextInstruction(pc)

            @inline def runtimeSuccessor(pc: Int): Unit = {
                if (isReached(pc))
                    cfJoins +!= pc
                else
                    isReached(pc) = true
            }

            (instruction.opcode: @switch) match {
                case RET.opcode => // potential path joins are determined when we process JSRs

                case JSR.opcode | JSR_W.opcode =>
                    val UnconditionalBranchInstruction(branchoffset) = instruction
                    runtimeSuccessor(pc + branchoffset)
                    runtimeSuccessor(nextPC)

                case _ =>
                    val nextPCs = instruction.nextInstructions(pc)(this, classHierarchy)
                    nextPCs.foreach(runtimeSuccessor)
            }

            pc = nextPC
        }
        cfJoins
    }

    /**
     * Computes for each instruction the set of predecessor instructions as well as all
     * instructions without predecessors. Those instructions with multiple predecessors
     * are also returned.
     *
     * @return  (1) An array which contains for each instruction the set of all predecessors,
     *          (2) the set of all instructions which have only predecessors; i.e., no successors
     *          and (3) the set of all instructions where multiple paths join.
     *          ´(Array[PCs]/*PREDECESSOR_PCs*/, PCs/*FINAL_PCs*/, PCs/*CF_JOINS*/)´.
     *
     * Note, that in case of completely broken code, set 2 may contain other
     * instructions than `return` and `athrow` instructions.
     * If the code contains jsr/ret instructions, the full blown CFG is computed.
     */
    def predecessorPCs(implicit classHierarchy: ClassHierarchy): (Array[PCs], PCs, PCs) = {
        implicit val code = this

        val instructions = this.instructions
        val instructionsLength = instructions.length

        val allPredecessorPCs = new Array[PCs](instructionsLength)
        allPredecessorPCs(0) = EmptyIntTrieSet // initialization for the start node
        var exitPCs: IntTrieSet = EmptyIntTrieSet

        var cfJoins: IntTrieSet = EmptyIntTrieSet
        val isReached = new Array[Boolean](instructionsLength)
        isReached(0) = true // the first instruction is always reached!
        @inline def runtimeSuccessor(successorPC: Int): Unit = {
            if (isReached(successorPC))
                cfJoins +!= successorPC
            else
                isReached(successorPC) = true
        }

        lazy val cfg = CFGFactory(code, classHierarchy) // fallback if we analyze pre Java 5 code...
        var pc = 0
        while (pc < instructionsLength) {
            val i = instructions(pc)
            val nextPCs =
                if (i.opcode == RET.opcode)
                    i.asInstanceOf[RET].nextInstructions(pc, () => cfg)
                else
                    i.nextInstructions(pc, regularSuccessorsOnly = false)
            if (nextPCs.isEmpty) {
                exitPCs += pc
            } else {
                nextPCs foreach { nextPC =>
                    if (nextPC < instructionsLength) {
                        // compute cfJoins
                        runtimeSuccessor(nextPC)
                        // compute predecessors
                        val predecessorPCs = allPredecessorPCs(nextPC)
                        if (predecessorPCs eq null) {
                            allPredecessorPCs(nextPC) = IntTrieSet1(pc)
                        } else {
                            allPredecessorPCs(nextPC) = predecessorPCs +! pc
                        }
                    } else {
                        // This handles cases where we have totally broken code; e.g.,
                        // compile-time dead code at the end of the method where the
                        // very last instruction is not even a ret/jsr/goto/return/atrow
                        // instruction (e.g., a NOP instruction as in case of jPython
                        // related classes.)
                        exitPCs +!= pc
                    }
                }
            }
            pc = i.indexOfNextInstruction(pc)
        }

        (allPredecessorPCs, exitPCs, cfJoins)
    }

    /**
     * Computes for each instruction which variables are live; see
     * `liveVariables(predecessorPCs: Array[PCs], finalPCs: PCs, cfJoins: BitSet)` for further
     * details.
     */
    def liveVariables(implicit classHierarchy: ClassHierarchy): LiveVariables = {
        val (predecessorPCs, finalPCs, cfJoins) = this.predecessorPCs(classHierarchy)
        liveVariables(predecessorPCs, finalPCs, cfJoins)
    }

    /**
     * Performs a live variable analysis restricted to a method's locals.
     *
     * @return  For each instruction (identified by its pc) the set of variables (register values)
     *          which are live (identified by their index) is determined.
     *          I.e., if you need to know if the variable with the index 5 is
     *          (still) live at instruction j with pc 37 it is sufficient to test if the bit
     *          set stored at index 37 contains the value 5.
     */
    def liveVariables(
        predecessorPCs: Array[PCs],
        finalPCs:       PCs,
        cfJoins:        PCs
    ): LiveVariables = {
        // IMPROVE Use StackMapTable (if available) to preinitialize the live variable information
        val instructions = this.instructions
        val instructionsLength = instructions.length
        val liveVariables = new Array[BitArraySet](instructionsLength)
        val workqueue = IntQueue.empty
        val AllDead = BitArraySet.empty
        finalPCs foreach { pc => liveVariables(pc) = AllDead; workqueue.enqueue(pc) }
        // required to handle endless loops!
        cfJoins foreach { pc =>
            val instruction = instructions(pc)
            var liveVariableInfo = AllDead
            if (instruction.readsLocal) {
                // This instruction is by construction "not a final instruction"
                // because this instruction never throws any(!) exceptions and it
                // also never "returns".
                liveVariableInfo += instruction.indexOfReadLocal
            }
            liveVariables(pc) = liveVariableInfo
            workqueue.enqueue(pc)
        }
        while (!workqueue.isEmpty) {
            val pc = workqueue.dequeue
            val instruction = instructions(pc)
            var liveVariableInfo = liveVariables(pc)
            if (instruction.readsLocal) {
                val lvIndex = instruction.indexOfReadLocal
                if (!liveVariableInfo.contains(lvIndex)) {
                    liveVariableInfo += lvIndex
                    liveVariables(pc) = liveVariableInfo
                }
            } else if (instruction.writesLocal) {
                val lvIndex = instruction.indexOfWrittenLocal
                if (liveVariableInfo.contains(lvIndex)) {
                    liveVariableInfo -= lvIndex
                    liveVariables(pc) = liveVariableInfo
                }
            }
            val thePCPredecessorPCs = predecessorPCs(pc)
            // if the code contains some "trivially" dead code as, e.g., shown next:
            //      com.sun.org.apache.xalan.internal.xsltc.runtime.BasisLibrary (JDK8u92)
            //      PC   Line  Instruction
            //      0    1363  aload_0
            //      1    |     checkcast com.sun.org.apache.xalan.internal.xsltc.DOM
            //      4    |     areturn
            //      5    1365  astore_1  // ... exception handler for irrelevant exception...
            //      ...
            //      23   |     areturn
            // predecessorPCs(pc) will be null!
            if (thePCPredecessorPCs ne null) {
                thePCPredecessorPCs foreach { predecessorPC =>
                    val predecessorLiveVariableInfo = liveVariables(predecessorPC)
                    if (predecessorLiveVariableInfo eq null) {
                        liveVariables(predecessorPC) = liveVariableInfo
                        workqueue.enqueue(predecessorPC)
                    } else {
                        val newLiveVariableInfo = predecessorLiveVariableInfo | liveVariableInfo
                        if (newLiveVariableInfo != predecessorLiveVariableInfo) {
                            liveVariables(predecessorPC) = newLiveVariableInfo
                            workqueue.enqueue(predecessorPC)
                        }
                    }
                }
            }
        }
        liveVariables
    }

    /**
     * Returns the set of all program counters where two or more control flow paths join or fork.
     *
     * ==Example==
     * {{{
     *  0: iload_1
     *  1: ifgt    6 // <= PATH FORK
     *  2: iconst_1
     *  5: goto 10
     *  6: ...
     *  9: iload_1
     * 10: return // <= PATH JOIN: the predecessors are the instructions 5 and 9.
     * }}}
     *
     * In case of exception handlers the sound overapproximation is made that
     * all exception handlers may be reached on multiple paths.
     *
     * @return A triple which contains (1) the set of pcs of those instructions where multiple
     *         control-flow paths join; (2) the pcs of the instructions which may result in
     *         multiple different control-flow paths and (3) for each of the later instructions
     *         the set of all potential targets.
     */
    def cfPCs(
        implicit
        classHierarchy: ClassHierarchy = PreInitializedClassHierarchy
    ): (PCs /*cfJoins*/ , PCs /*forks*/ , IntMap[PCs] /*forkTargetPCs*/ ) = {
        val instructions = this.instructions
        val instructionsLength = instructions.length

        var cfJoins: IntTrieSet = EmptyIntTrieSet
        var cfForks: IntTrieSet = EmptyIntTrieSet
        var cfForkTargets = IntMap.empty[IntTrieSet]

        val isReached = new Array[Boolean](instructionsLength)
        isReached(0) = true // the first instruction is always reached!

        lazy val cfg = CFGFactory(this, classHierarchy)

        var pc = 0
        while (pc < instructionsLength) {
            val instruction = instructions(pc)
            val nextPC = pcOfNextInstruction(pc)

            @inline def runtimeSuccessor(pc: Int): Unit = {
                if (isReached(pc))
                    cfJoins += pc
                else
                    isReached(pc) = true
            }

            (instruction.opcode: @switch) match {
                case RET.opcode =>
                    // The ret may return to different sites;
                    // the potential path joins are determined when we process the JSR.
                    cfForks +!= pc
                    cfForkTargets += ((pc, cfg.successors(pc)))

                case JSR.opcode | JSR_W.opcode =>
                    val jsrInstr = instruction.asInstanceOf[JSRInstruction]
                    runtimeSuccessor(pc + jsrInstr.branchoffset)
                    runtimeSuccessor(nextPC)

                case _ =>
                    val nextInstructions = instruction.nextInstructions(pc)(this, classHierarchy)
                    nextInstructions.foreach(runtimeSuccessor)
                    if (nextInstructions.length > 1) {
                        cfForks +!= pc
                        cfForkTargets += ((pc, nextInstructions.foldLeft(IntTrieSet.empty)(_ +! _)))
                    }
            }

            pc = nextPC
        }
        (cfJoins, cfForks, cfForkTargets)
    }

    /**
     * Iterates over all instructions and calls the given function `f` for every instruction.
     */
    @inline final def iterate[U](f: ( /*pc:*/ Int, Instruction) => U): Unit = {
        val instructionsLength = instructions.length
        var pc = 0
        while (pc < instructionsLength) {
            val instruction = instructions(pc)
            f(pc, instruction)
            pc = pcOfNextInstruction(pc)
        }
    }

    /**
     * Iterates over all instructions with the given opcode and calls the given function `f` for every instruction.
     */
    @inline final def iterate[U](
        instructionType: InstructionMetaInformation
    )(
        f: ( /*pc:*/ Int, Instruction) => U
    ): Unit = {
        val opcode = instructionType.opcode
        val instructionsLength = instructions.length
        var pc = 0
        while (pc < instructionsLength) {
            val instruction = instructions(pc)
            if (instruction.opcode == opcode) f(pc, instruction)
            pc = pcOfNextInstruction(pc)
        }
    }

    @inline final def forall(f: ( /*pc:*/ Int, Instruction) => Boolean): Boolean = {
        val instructionsLength = instructions.length
        var pc = 0
        while (pc < instructionsLength) {
            val instruction = instructions(pc)
            if (!f(pc, instruction))
                return false;

            pc = pcOfNextInstruction(pc)
        }
        true
    }

    /**
     * Iterates over all instructions and calls the given function `f`
     * for every instruction.
     */
    @inline final def foreachInstruction[U](f: Instruction => U): Unit = {
        val instructionsLength = instructions.length
        var pc = 0
        while (pc < instructionsLength) {
            val instruction = instructions(pc)
            f(instruction)
            pc = pcOfNextInstruction(pc)
        }
    }

    /**
     * Iterates over all instructions and calls the given function `f`
     * for every instruction.
     */
    @inline final def foreachPC[U](f: PC => U): Unit = {
        val instructionsLength = instructions.length
        var pc = 0
        while (pc < instructionsLength) {
            f(pc)
            pc = pcOfNextInstruction(pc)
        }
    }

    /**
     * Returns a view of all handlers (exception and finally handlers) for the
     * instruction with the given program counter (`pc`) that may catch an exception; as soon
     * as a finally handler is found no further handlers will be returned!
     *
     * In case of multiple exception handlers that are identical (in particular
     * in case of the finally handlers) only the first one is returned as that
     * one is the one that will be used by the JVM at runtime.
     * No further checks (w.r.t. the type hierarchy) are done.
     *
     * @param pc The program counter of an instruction of this `Code` array.
     */
    def handlersFor(pc: Int, justExceptions: Boolean = false): List[ExceptionHandler] = {
        var handledExceptions = Set.empty[ObjectType]
        val ehs = List.newBuilder[ExceptionHandler]
        exceptionHandlers forall { eh =>
            if (eh.startPC <= pc && eh.endPC > pc) {
                val catchTypeOption = eh.catchType
                if (catchTypeOption.isDefined) {
                    val catchType = catchTypeOption.get
                    if (!handledExceptions.contains(catchType)) {
                        handledExceptions += catchType
                        ehs += eh
                    }
                    true
                } else {
                    if (!justExceptions) {
                        ehs += eh
                    }
                    false
                }

            } else {
                // the handler is not relevant
                true
            }
        }
        ehs.result()
    }

    /**
     * Returns a view of all potential exception handlers (if any) for the
     * instruction with the given program counter (`pc`). `Finally` handlers
     * (`catchType == None`) are not returned but will stop the evaluation (as all further
     * exception handlers have no further meaning w.r.t. the runtime)!
     * In case of identical caught exceptions only the
     * first of them will be returned. No further checks (w.r.t. the typehierarchy) are done.
     *
     * @param pc The program counter of an instruction of this `Code` array.
     */
    def exceptionHandlersFor(pc: PC): List[ExceptionHandler] = {
        handlersFor(pc, justExceptions = true)
    }

    /**
     * Returns the handlers that may handle the given exception.
     *
     * The (known/given) type hierarchy is taken into account as well as
     * the order between the exception handlers.
     */
    def handlersForException(
        pc:        Int,
        exception: ObjectType
    )(
        implicit
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[ExceptionHandler] = {
        import classHierarchy.isASubtypeOf

        var handledExceptions = Set.empty[ObjectType]

        val ehs = List.newBuilder[ExceptionHandler]
        exceptionHandlers forall { eh =>
            if (eh.startPC <= pc && eh.endPC > pc) {
                val catchTypeOption = eh.catchType
                if (catchTypeOption.isDefined) {
                    val catchType = catchTypeOption.get
                    val isSubtype = isASubtypeOf(exception, catchType)
                    if (isSubtype.isYes) {
                        ehs += eh
                        /* we found a definitiv matching handler*/ false
                    } else if (isSubtype.isUnknown) {
                        if (!handledExceptions.contains(catchType)) {
                            handledExceptions += catchType
                            ehs += eh
                        }
                        /* we may have a better fit */ true
                    } else {
                        /* the exception type is not relevant*/ true
                    }
                } else {
                    ehs += eh
                    /* we are done; we found a finally handler... */ false
                }
            } else {
                /* the handler is not relevant */ true
            }
        }
        ehs.result()
    }

    /**
     * The list of pcs of those instructions that may handle an exception if the evaluation
     * of the instruction with the given `pc` throws an exception.
     *
     * In case of multiple finally handlers only the first one will be returned and no further
     * exception handlers will be returned. In case of identical caught exceptions only the
     * first of them will be returned. No further checks (w.r.t. the type hierarchy) are done.
     *
     * If different exceptions are handled by the same handler, the corresponding pc is returned
     * multiple times.
     */
    def handlerInstructionsFor(pc: Int): List[Int] /*Chain[PC]*/ = {
        var handledExceptions = Set.empty[ObjectType]

        val pcs = List.newBuilder[Int] /*PC*/
        exceptionHandlers forall { eh =>
            if (eh.startPC <= pc && eh.endPC > pc) {
                val catchTypeOption = eh.catchType
                if (catchTypeOption.isDefined) {
                    val catchType = catchTypeOption.get
                    if (!handledExceptions.contains(catchType)) {
                        handledExceptions += catchType
                        pcs += eh.handlerPC
                    }
                    true
                } else {
                    pcs += eh.handlerPC
                    false // we effectively abort after the first finally handler
                }
            } else {
                // the handler is not relevant
                true
            }
        }
        pcs.result()
    }

    /**
     * Returns the program counter of the next instruction after the instruction with
     * the given counter (`currentPC`).
     *
     * @param  currentPC The program counter of an instruction. If `currentPC` is the
     *                   program counter of the last instruction of the code block then the returned
     *                   program counter will be equivalent to the length of the Code/Instructions
     *                   array.
     */
    @inline final def pcOfNextInstruction(currentPC: Int): /*PC*/ Int = {
        instructions(currentPC).indexOfNextInstruction(currentPC)(this)
        // OLD: ITERATING OVER THE ARRAY AND CHECKING FOR NON-NULL IS NO LONGER SUPPORTED!
        //    @inline final def pcOfNextInstruction(currentPC: PC): PC = {
        //        val max_pc = instructions.size
        //        var nextPC = currentPC + 1
        //        while (nextPC < max_pc && (instructions(nextPC) eq null))
        //            nextPC += 1
        //
        //        nextPC
        //    }
    }

    /**
     * Returns the program counter of the previous instruction in the code array.
     * `currentPC` must be the program counter of an instruction.
     *
     * This function is only defined if currentPC is larger than 0; i.e., if there
     * is a previous instruction! If currentPC is larger than `instructions.size` the
     * behavior is undefined.
     */
    @inline final def pcOfPreviousInstruction(currentPC: Int): /*PC*/ Int = {
        var previousPC = currentPC - 1
        val instructions = this.instructions
        while (previousPC > 0 && !instructions(previousPC).isInstanceOf[Instruction]) {
            previousPC -= 1
        }
        previousPC
    }

    /**
     * Returns the line number table - if any.
     *
     * @note    A code attribute is allowed to have multiple line number tables. However, all
     *          tables are merged into one by OPAL at class loading time.
     *
     * @note    Depending on the configuration of the reader for `ClassFile`s this
     *          attribute may not be reified.
     */
    def lineNumberTable: Option[LineNumberTable] = {
        attributes collectFirst { case lnt: LineNumberTable => lnt }
    }

    /**
     * Returns the line number associated with the instruction with the given pc if
     * it is available.
     *
     * @param pc Index of the instruction for which we want to get the line number.
     * @return `Some` line number or `None` if no line-number information is available.
     */
    def lineNumber(pc: Int): Option[Int] = lineNumberTable.flatMap(_.lookupLineNumber(pc))

    /**
     * Returns `Some(true)` if both pcs have the same line number. If line number information
     * is not available `None` is returned.
     */
    def haveSameLineNumber(firstPC: Int, secondPC: Int): Option[Boolean] = {
        lineNumber(firstPC).flatMap(firstLN => lineNumber(secondPC).map(_ == firstLN))
    }

    /**
     * Returns the smallest line number (if any).
     *
     * @note   The line number associated with the first instruction (pc === 0) is
     *         not necessarily the smallest one.
     *         {{{
     *                  public void foo(int i) {
     *                    super.foo( // The call has the smallest line number.
     *                      i+=1; // THIS IS THE FIRST OPERATION...
     *                    )
     *                  }
     *         }}}
     */
    def firstLineNumber: Option[Int] = lineNumberTable.flatMap(_.firstLineNumber())

    /**
     * Collects (the merged if necessary) local variable table.
     *
     * @note   A code attribute is allowed to have multiple local variable tables. However, all
     *         tables are merged into one by OPAL at class loading time.
     *
     * @note   Depending on the configuration of the reader for `ClassFile`s this
     *         attribute may not be reified.
     */
    def localVariableTable: Option[LocalVariables] = {
        attributes collectFirst { case LocalVariableTable(lvt) => lvt }
    }

    /**
     * Returns the set of local variables defined at the given pc base on debug information.
     *
     * @return A mapping of the index to the name of the local variable. The map is
     *         empty if no debug information is available.
     */
    def localVariablesAt(pc: Int): Map[Int, LocalVariable] = { // IMRPOVE Use IntMap for the return value.
        localVariableTable match {
            case Some(lvt) =>
                lvt.collect {
                    case lv @ LocalVariable(
                        startPC,
                        length,
                        _ /*name*/ ,
                        _ /*fieldType*/ ,
                        index
                        ) if startPC <= pc && startPC + length > pc =>
                        (index, lv)
                }.toMap
            case _ =>
                Map.empty
        }
    }

    /**
     * Returns the local variable stored at the given local variable index that is live at
     * the given instruction (pc).
     */
    def localVariable(pc: Int, index: Int): Option[LocalVariable] = {
        localVariableTable flatMap { lvs =>
            lvs find { lv =>
                val result = lv.index == index &&
                    lv.startPC <= pc &&
                    (lv.startPC + lv.length) > pc
                result
            }
        }
    }

    /**
     * Collects all local variable type tables.
     *
     * @note Depending on the configuration of the reader for `ClassFile`s this
     *       attribute may not be reified.
     */
    def localVariableTypeTable: Iterable[LocalVariableTypes] = {
        attributes collect { case LocalVariableTypeTable(lvtt) => lvtt }
    }

    /**
     * The JVM specification mandates that a Code attribute has at most one
     * StackMapTable attribute.
     *
     * @note   Depending on the configuration of the reader for `ClassFile`s this
     *         attribute may not be reified.
     */
    def stackMapTable: Option[StackMapTable] = {
        attributes collectFirst { case smt: StackMapTable => smt }
    }

    /**
     * Computes the set of PCs for which a stack map frame is required. Calling this method
     * (i.e., the generation of stack map tables in general) is only defined for Java > 5 code;
     * i.e., cocde which does not use JSR/RET; therefore the behavior for Java 5 or earlier code
     * is deliberately undefined.
     *
     * @param classHierarchy The computation of the stack map table generally requires the
     *                       presence of a complete type hierarchy.
     *
     * @return The sorted set of PCs for which a stack map frame is required.
     */
    def stackMapTablePCs(implicit classHierarchy: ClassHierarchy): IntArraySet = {

        var stackMapTablePCs: IntArraySet = IntArraySet.empty
        iterate { (pc, instruction) =>
            if (instruction.isControlTransferInstruction) {
                instruction.opcode match {
                    case JSR.opcode | JSR_W.opcode | RET.opcode =>
                        throw BytecodeProcessingFailedException(
                            "computation of stack map tables containing JSR/RET is not supported; "+
                                "the attribute is neither required nor helpful in this case"
                        )
                    case GOTO.opcode | GOTO_W.opcode =>
                        stackMapTablePCs += pc + instruction.asGotoInstruction.branchoffset
                        val nextPC = code.pcOfNextInstruction(pc)
                        if (nextPC < codeSize) {
                            // test for a goto at the end...
                            stackMapTablePCs += nextPC
                        }
                    case _ =>
                        stackMapTablePCs ++=
                            instruction.asControlTransferInstruction.jumpTargets(pc)(
                                code = this,
                                classHierarchy = classHierarchy
                            )
                }
            }
        }

        code.exceptionHandlers.foreach(ex => stackMapTablePCs += ex.handlerPC)

        stackMapTablePCs
    }

    /**
     * True if the instruction with the given program counter is modified by wide.
     *
     * @param pc A valid index in the code array.
     */
    @inline def isModifiedByWide(pc: Int): Boolean = pc > 0 && instructions(pc - 1) == WIDE

    def foldLeft[T <: Any](start: T)(f: (T, Int /*PC*/ , Instruction) => T): T = {
        val max_pc = instructions.length
        var pc = 0
        var vs = start
        while (pc < max_pc) {
            vs = f(vs, pc, instructions(pc))
            pc = pcOfNextInstruction(pc)
        }
        vs
    }

    /**
     * Collects all instructions for which the given function is defined. The order in
     * which the instructions are collected is reversed when compared to the order in the
     * instructions array.
     */
    def collectInstructions[B <: AnyRef](f: PartialFunction[Instruction, B]): List[B] = {
        val max_pc = instructions.length
        var result: List[B] = List.empty
        var pc = 0
        while (pc < max_pc) {
            val instruction = instructions(pc)
            val r: Any = f.applyOrElse(instruction, AnyToAnyThis)
            if (r.asInstanceOf[AnyRef] ne AnyToAnyThis) {
                result ::= r.asInstanceOf[B]
            }
            pc = pcOfNextInstruction(pc)
        }
        result
    }

    /**
     * Collects all instructions for which the given function is defined.
     */
    def collectInstructionsWithPC[B <: AnyRef](
        f: PartialFunction[PCAndInstruction, B]
    ): List[PCAndAnyRef[B]] = {
        val max_pc = instructions.length
        var result: List[PCAndAnyRef[B]] = List.empty
        var pc = 0
        while (pc < max_pc) {
            val instruction = instructions(pc)
            val r: Any = f.applyOrElse(PCAndInstruction(pc, instruction), AnyToAnyThis)
            if (r.asInstanceOf[AnyRef] ne AnyToAnyThis) {
                result ::= PCAndAnyRef(pc, r.asInstanceOf[B])
            }
            pc = pcOfNextInstruction(pc)
        }
        result
    }

    /**
     * Collects all instructions for which the given function is defined.
     *
     * ==Usage scenario==
     * Use this function if you want to search for and collect specific instructions and
     * when you do not immediately require the program counter/index of the instruction
     * in the instruction array to make the decision whether you want to collect the
     * instruction.
     *
     * ==Examples==
     * Example usage to collect the declaring class of all get field accesses where the
     * field name is "last".
     * {{{
     * collect({
     *  case GETFIELD(declaringClass, "last", _) => declaringClass
     * })
     * }}}
     *
     * Example usage to collect all instances of a "DUP" instruction.
     * {{{
     * code.collect({ case dup @ DUP => dup })
     * }}}
     *
     * @return The result of applying the function f to all instructions for which f is
     *         defined combined with the index (program counter) of the instruction in the
     *         code array.
     */
    def collect[B <: AnyRef](f: PartialFunction[Instruction, B]): List[PCAndAnyRef[B]] = {
        val max_pc = instructions.length
        var pc = 0
        var result: List[PCAndAnyRef[B]] = List.empty
        while (pc < max_pc) {
            val instruction = instructions(pc)
            val r: Any = f.applyOrElse(instruction, AnyToAnyThis)
            if (r.asInstanceOf[AnyRef] ne AnyToAnyThis) {
                result ::= PCAndAnyRef(pc, r.asInstanceOf[B])
            }
            pc = pcOfNextInstruction(pc)
        }
        result.reverse
    }

    def filter[B](f: (PC, Instruction) => Boolean): IntArraySet = {
        val max_pc = instructions.length

        val pcs = IntArrayStack.empty
        var pc = 0
        while (pc < max_pc) {
            if (f(pc, instructions(pc))) pcs += pc
            pc = pcOfNextInstruction(pc)
        }
        IntArraySet._UNSAFE_fromSorted(pcs.toArray)
    }

    /**
     * Finds a pair of consecutive instructions that are matched by the given partial
     * function.
     *
     * ==Example Usage==
     * {{{
     * (pc, _) <- body.findPair {
     *      case (
     *          INVOKESPECIAL(receiver1, _, SingleArgumentMethodDescriptor((paramType: BaseType, _))),
     *          INVOKEVIRTUAL(receiver2, name, NoArgumentMethodDescriptor(returnType: BaseType))
     *      ) if (...) => (...)
     *      } yield ...
     * }}}
     */
    def collectPair[B <: AnyRef](
        f: PartialFunction[(Instruction, Instruction), B]
    ): List[PCAndAnyRef[B]] = {
        val max_pc = instructions.length

        var firstPC = 0
        var firstInstruction = instructions(firstPC)
        var secondPC = pcOfNextInstruction(0)
        var secondInstruction: Instruction = null

        var result: List[PCAndAnyRef[B]] = Nil
        while (secondPC < max_pc) {
            secondInstruction = instructions(secondPC)

            val instrs = (firstInstruction, secondInstruction)
            val r: Any = f.applyOrElse(instrs, AnyToAnyThis)
            if (r.asInstanceOf[AnyRef] ne AnyToAnyThis) {
                result ::= PCAndAnyRef(firstPC, r.asInstanceOf[B])
            }

            firstInstruction = secondInstruction
            firstPC = secondPC
            secondPC = pcOfNextInstruction(secondPC)
        }
        result
    }

    /**
     * Finds a sequence of instructions that are matched by the given partial function.
     *
     * @note If possible, use one of the more specialized methods, such as, [[collectPair]].
     *       The pure iteration overhead caused by this method is roughly 10-20 times higher
     *       than this one.
     *
     * @return List of pairs where the first element is the pc of the first instruction
     *         of a matched sequence and the second value is the result of the evaluation
     *         of the partial function.
     */
    def findSequence[B <: AnyRef](
        windowSize: Int
    )(
        f: PartialFunction[Queue[Instruction], B]
    ): List[PCAndAnyRef[B]] = {
        require(windowSize > 0)

        val max_pc = instructions.length
        var instrs: Queue[Instruction] = Queue.empty
        var firstPC, lastPC = 0
        var elementsInQueue = 0

        //
        // INITIALIZATION
        //
        while (elementsInQueue < windowSize - 1 && lastPC < max_pc) {
            instrs = instrs.enqueue(instructions(lastPC))
            lastPC = pcOfNextInstruction(lastPC)
            elementsInQueue += 1
        }

        //
        // SLIDING OVER THE CODE
        //
        var result: List[PCAndAnyRef[B]] = Nil
        while (lastPC < max_pc) {
            instrs = instrs.enqueue(instructions(lastPC))

            val r: Any = f.applyOrElse(instrs, AnyToAnyThis)
            if (r.asInstanceOf[AnyRef] ne AnyToAnyThis) {
                result ::= PCAndAnyRef(firstPC, r.asInstanceOf[B])
            }

            firstPC = pcOfNextInstruction(firstPC)
            lastPC = pcOfNextInstruction(lastPC)
            instrs = instrs.tail
        }

        result.reverse
    }

    /**
     * Matches pairs of two consecutive instructions. For each matched pair,
     * the program counter of the first instruction is returned.
     *
     * ==Example Usage==
     * {{{
     * for {
     *  classFile <- project.view.map(_._1).par
     *  method @ MethodWithBody(body) <- classFile.methods
     *  pc <- body.matchPair({
     *      case (
     *          INVOKESPECIAL(receiver1, _, TheArgument(parameterType: BaseType)),
     *          INVOKEVIRTUAL(receiver2, name, NoArgumentMethodDescriptor(returnType: BaseType))
     *      ) => { (receiver1 eq receiver2) && (returnType ne parameterType) }
     *      case _ => false
     *      })
     *  } yield (classFile, method, pc)
     * }}}
     */
    def matchPair(f: (Instruction, Instruction) => Boolean): List[Int /*PC*/ ] = {
        val max_pc = instructions.length
        var pc1 = 0
        var pc2 = pcOfNextInstruction(pc1)

        var result: List[Int /*PC*/ ] = List.empty
        while (pc2 < max_pc) {
            if (f(instructions(pc1), instructions(pc2))) {
                result = pc1 :: result
            }

            pc1 = pc2
            pc2 = pcOfNextInstruction(pc2)
        }
        result
    }

    /**
     * Finds all sequences of three consecutive instructions that are matched by `f`.
     */
    def matchTriple(f: (Instruction, Instruction, Instruction) => Boolean): List[Int /*PC*/ ] = {
        matchTriple(Int.MaxValue, f)
    }

    /**
     * Finds a sequence of 3 consecutive instructions for which the given function returns
     * `true`, and returns the `PC` of the first instruction in each found sequence.
     *
     * @param matchMaxTriples Is the maximum number of triples that is passed to `f`.
     *      E.g., if `matchMaxTriples` is "1" only the first three instructions are
     *                        passed to `f`.
     */
    def matchTriple(
        matchMaxTriples: Int                                                = Int.MaxValue,
        f:               (Instruction, Instruction, Instruction) => Boolean
    ): List[Int /*PC*/ ] = {
        val max_pc = instructions.length
        var matchedTriplesCount = 0
        var pc1 = 0
        var pc2 = pcOfNextInstruction(pc1)
        if (pc2 >= max_pc)
            return List.empty;

        var pc3 = pcOfNextInstruction(pc2)

        var result: List[Int /*PC*/ ] = List.empty
        while (pc3 < max_pc && matchedTriplesCount < matchMaxTriples) {
            if (f(instructions(pc1), instructions(pc2), instructions(pc3))) {
                result = pc1 :: result
            }

            matchedTriplesCount += 1

            // Move forward by 1 instruction at a time. Even though (..., 1, 2, 3, _, ...)
            // didn't match, it's possible that (..., _, 1, 2, 3, ...) matches.
            pc1 = pc2
            pc2 = pc3
            pc3 = pcOfNextInstruction(pc3)
        }
        result
    }

    /**
     * Returns the next instruction that will be executed at runtime that is not a
     * [[org.opalj.br.instructions.GotoInstruction]].
     * If the given instruction is not a [[org.opalj.br.instructions.GotoInstruction]],
     * the given instruction is returned.
     */
    @tailrec def nextNonGotoInstruction(pc: Int): /*PC*/ Int = {
        instructions(pc) match {
            case GotoInstruction(branchoffset) => nextNonGotoInstruction(pc + branchoffset)
            case _                             => pc
        }
    }

    /**
     * Tests if the straight-line sequence of instructions that starts with the given `pc`
     * always ends with an `ATHROW` instruction or a method call that always throws an
     * exception. The call sequence furthermore has to contain no complex logic.
     * Here, complex means that evaluating the instruction may result in multiple control flows.
     * If the sequence contains complex logic, `false` will be returned.
     *
     * One use case of this method is, e.g., to check if the code
     * of the default case of a switch instruction always throws some error
     * (e.g., an `UnknownError` or `AssertionError`).
     * {{{
     * switch(...) {
     *  case X : ....
     *  default :
     *      throw new AssertionError();
     * }
     * }}}
     * This is a typical idiom used in Java programs and which may be relevant for
     * certain analyses to detect.
     *
     * @note   If complex control flows should also be considered it is possible to compute
     *         a methods [[org.opalj.br.cfg.CFG]] and use that one.
     *
     * @param  pc           The program counter of an instruction that strictly dominates all
     *                      succeeding instructions up until the next instruction (as determined
     *                      by [[#cfJoins]] where two or more paths join. If the pc belongs to an instruction
     *                      where multiple paths join, `false` will be returned.
     *
     * @param  anInvocation When the analysis finds a method call, it calls this method
     *                      to let the caller decide whether the called method is an (indirect) way
     *                      of always throwing an exception.
     *                      If `true` is returned the analysis terminates and returns `true`; otherwise
     *                      the analysis continues.
     *
     * @param  aThrow       If all (non-exception) paths will always end in one specific
     *                      `ATHROW` instruction then this function is called (callback) to let the
     *                      caller decide if the "expected" exception is thrown. This analysis will
     *                      return with the result of this call.
     *
     * @return `true` if the bytecode sequence starting with the instruction with the
     *         given `pc` always ends with an [[org.opalj.br.instructions.ATHROW]] instruction.
     *         `false` in all other cases (i.e., the sequence does not end with an `athrow`
     *         instruction or the control flow is more complex.)
     */
    @inline def alwaysResultsInException(
        pc:           Int,
        cfJoins:      IntTrieSet,
        anInvocation: ( /*PC*/ Int) => Boolean,
        aThrow:       ( /*PC*/ Int) => Boolean
    ): Boolean = {

        var currentPC = pc
        while (!cfJoins.contains(currentPC)) {
            val instruction = instructions(currentPC)

            (instruction.opcode: @scala.annotation.switch) match {
                case ATHROW.opcode =>
                    val result = aThrow(currentPC)
                    return result;

                case RET.opcode | JSR.opcode | JSR_W.opcode =>
                    return false;

                case GOTO.opcode | GOTO_W.opcode =>
                    currentPC += instruction.asInstanceOf[GotoInstruction].branchoffset

                case /*IFs:*/ 165 | 166 | 198 | 199 |
                    159 | 160 | 161 | 162 | 163 | 164 |
                    153 | 154 | 155 | 156 | 157 | 158 =>
                    return false;

                case TABLESWITCH.opcode | LOOKUPSWITCH.opcode =>
                    return false;

                case /*xReturn:*/ 176 | 175 | 174 | 172 | 173 | 177 =>
                    return false;

                case INVOKEINTERFACE.opcode
                    | INVOKESPECIAL.opcode
                    | INVOKESTATIC.opcode
                    | INVOKEVIRTUAL.opcode =>
                    if (anInvocation(currentPC))
                        return true;

                    currentPC = pcOfNextInstruction(currentPC)

                case _ =>
                    currentPC = pcOfNextInstruction(currentPC)
            }
        }

        false
    }

    @throws[ClassFormatError]("if it is impossible to compute the maximum height of the stack")
    def stackDepthAt(
        atPC:           Int,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Int = {
        stackDepthAt(atPC, CFGFactory(this, classHierarchy))
    }

    /**
     * Computes the stack depth for the instruction with the given pc (`atPC`).
     * I.e, computes the stack depth before executing the instruction! This function is intended
     * to be used if and only if the stack depth is only required for a single instruction; it
     * recomputes the stack depth for all instructions whenever the function is called.
     *
     * @note If the CFG is already available, it should be passed as the computation is
     *       potentially the most expensive part.
     *
     * @return the stack depth or -1 if the instruction is invalid/dead.
     */
    @throws[ClassFormatError]("if it is impossible to compute the maximum height of the stack")
    def stackDepthAt(atPC: Int, cfg: CFG[Instruction, Code]): Int = {
        var paths: List[( /*PC*/ Int, Int /*stackdepth before executing the instruction*/ )] = List.empty
        val visitedPCs = new mutable.BitSet(instructions.length)

        // We start with the first instruction and an empty stack.
        paths ::= ((0, 0))
        visitedPCs += 0

        // We have to make sure, that all exception handlers are evaluated for
        // max_stack, if an exception is caught, the stack size is always 1 -
        // containing the exception itself.
        for (exceptionHandler <- exceptionHandlers) {
            val handlerPC = exceptionHandler.handlerPC
            if (visitedPCs.add(handlerPC)) paths ::= ((handlerPC, 1))
        }

        while (paths.nonEmpty) {
            val (pc, initialStackDepth) = paths.head
            if (pc == atPC) {
                return initialStackDepth;
            }
            paths = paths.tail
            val newStackDepth = initialStackDepth + instructions(pc).stackSlotsChange
            cfg.foreachSuccessor(pc) { succPC =>
                if (visitedPCs.add(succPC)) {
                    paths ::= ((succPC, newStackDepth))
                }
            }
        }

        -1
    }

    /**
     * This attribute's kind id.
     */
    override def kindId: Int = Code.KindId

    /**
     * A complete representation of this code attribute (including instructions,
     * attributes, etc.).
     */
    override def toString: String = {
        s"Code_attribute(maxStack=$maxStack, maxLocals=$maxLocals, "+
            instructions.zipWithIndex.filter(_._1 ne null).map(_.swap).toString +
            exceptionHandlers.toString+","+
            attributes.toString+
            ")"
    }

    /**
     * Collects the results of the evaluation of the partial function until the partial function
     * is not defined.
     *
     * @return The program counter of the instruction for which the given partial function was
     *         not defined along with the list of previous results. '''The results are sorted in
     *         descending order w.r.t. the PC'''.
     */
    def collectUntil[B <: AnyRef](f: PartialFunction[PCAndInstruction, B]): PCAndAnyRef[List[B]] = {
        val max_pc = instructions.length
        var pc = 0
        var result: List[B] = List.empty
        while (pc < max_pc) {
            val r: Any = f.applyOrElse(PCAndInstruction(pc, instructions(pc)), AnyToAnyThis)
            if (r.asInstanceOf[AnyRef] ne AnyToAnyThis) {
                result = r.asInstanceOf[B] :: result
            } else {
                return PCAndAnyRef(pc, result);
            }
            pc = pcOfNextInstruction(pc)
        }
        PCAndAnyRef(pc, result)
    }

    /**
     * Applies the given function to the first instruction for which the given function
     * is defined.
     */
    def collectFirstWithIndex[B](f: PartialFunction[PCAndInstruction, B]): Option[B] = {
        val max_pc = instructions.length
        var pc = 0
        while (pc < max_pc) {
            val r: Any = f.applyOrElse(PCAndInstruction(pc, instructions(pc)), AnyToAnyThis)
            if (r.asInstanceOf[AnyRef] ne AnyToAnyThis) {
                return Some(r.asInstanceOf[B]);
            }
            pc = pcOfNextInstruction(pc)
        }

        None
    }

    /**
     * Applies the given function `f` to all instruction objects for which the function is
     * defined. The function is passed a tuple consisting of the current program
     * counter/index in the code array and the corresponding instruction.
     *
     * ==Example==
     * Example usage to collect the program counters (indexes) of all instructions that
     * are the target of a conditional branch instruction:
     * {{{
     * code.collectWithIndex({
     *  case (pc, cbi: ConditionalBranchInstruction) =>
     *      Seq(cbi.indexOfNextInstruction(pc, code), pc + cbi.branchoffset)
     *  }) // .flatten should equal (Seq(...))
     * }}}
     */
    def collectWithIndex[B: ClassTag](f: PartialFunction[PCAndInstruction, B]): List[B] = {
        val max_pc = instructions.length
        var pc = 0
        val vs = List.newBuilder[B]
        while (pc < max_pc) {
            val r: Any = f.applyOrElse(PCAndInstruction(pc, instructions(pc)), AnyToAnyThis)
            if (r.asInstanceOf[AnyRef] ne AnyToAnyThis) {
                vs += r.asInstanceOf[B]
            }
            pc = pcOfNextInstruction(pc)
        }
        vs.result()
    }

    /**
     * Slides over the code array and tries to apply the given function to each sequence
     * of instructions consisting of `windowSize` elements.
     *
     * ==Scenario==
     * If you want to search for specific patterns of bytecode instructions. Some "bug
     * patterns" are directly related to specific bytecode sequences and these patterns
     * can easily be identified using this method.
     *
     * ==Example==
     * Search for sequences of the bytecode instructions `PUTFIELD` and `ALOAD_O` in the
     * method's body and return the list of program counters of the start of the
     * identified sequences.
     * {{{
     * code.slidingCollect(2)({
     *      case (pc, Seq(PUTFIELD(_, _, _), ALOAD_0)) => (pc)
     * }) should be(Seq(...))
     * }}}
     *
     * @note If possible, use one of the more specialized methods, such as, [[collectPair]].
     *       The pure iteration overhead caused by this method is roughly 10-20 times higher
     *       than this one.
     *
     * @param windowSize The size of the sequence of instructions that is passed to the
     *                   partial function.
     *                   It must be larger than 0. **Do not use this method with windowSize "1"**;
     *                   it is more efficient to use the `collect` or `collectWithIndex` methods
     *                   instead.
     *
     * @return The list of results of applying the function f for each matching sequence.
     */
    def slidingCollect[B <: AnyRef](
        windowSize: Int
    )(
        f: PartialFunction[PCAndAnyRef[Queue[Instruction]], B]
    ): List[B] = {
        require(windowSize > 0)

        val max_pc = instructions.length
        var instrs: Queue[Instruction] = Queue.empty
        var firstPC, lastPC = 0
        var elementsInQueue = 0

        //
        // INITIALIZATION
        //
        while (elementsInQueue < windowSize - 1 && lastPC < max_pc) {
            instrs = instrs.enqueue(instructions(lastPC))
            lastPC = pcOfNextInstruction(lastPC)
            elementsInQueue += 1
        }

        //
        // SLIDING OVER THE CODE
        //
        var result: List[B] = List.empty
        while (lastPC < max_pc) {
            instrs = instrs.enqueue(instructions(lastPC))

            val r: Any = f.applyOrElse(PCAndAnyRef(firstPC, instrs), AnyToAnyThis)
            if (r.asInstanceOf[AnyRef] ne AnyToAnyThis) {
                result ::= r.asInstanceOf[B]
            }

            firstPC = pcOfNextInstruction(firstPC)
            lastPC = pcOfNextInstruction(lastPC)
            instrs = instrs.tail
        }

        result.reverse
    }

}

/**
 * Defines constants useful when analyzing a method's code.
 *
 * @author Michael Eichberg
 */
object Code {

    def apply(
        maxStack:          Int,
        maxLocals:         Int,
        instructions:      Array[Instruction],
        exceptionHandlers: ExceptionHandlers  = NoExceptionHandlers,
        attributes:        Attributes         = NoAttributes
    ): Code = {

        var localVariableTablesCount = 0
        var lineNumberTablesCount = 0
        attributes foreach { a =>
            if (a.isInstanceOf[LocalVariableTable]) {
                localVariableTablesCount += 1
            } else if (a.isInstanceOf[UnpackedLineNumberTable]) {
                lineNumberTablesCount += 1
            }
        }

        if (localVariableTablesCount <= 1 && lineNumberTablesCount <= 1) {
            new Code(maxStack, maxLocals, instructions, exceptionHandlers, attributes)
        } else {
            val (localVariableTables, otherAttributes1) =
                partitionByType(attributes, classOf[LocalVariableTable])
            val newAttributes1 =
                if (localVariableTables.nonEmpty && localVariableTables.tail.nonEmpty) {
                    val theLVT = localVariableTables.flatMap[LocalVariable](_.localVariables)
                    otherAttributes1 :+ new LocalVariableTable(theLVT)
                } else {
                    attributes
                }

            val (lineNumberTables, otherAttributes2) =
                partitionByType(newAttributes1, classOf[UnpackedLineNumberTable])
            val newAttributes2 =
                if (lineNumberTables.nonEmpty && lineNumberTables.size > 1) {
                    val mergedTables = lineNumberTables.flatMap[LineNumber](_.lineNumbers)
                    val sortedTable = mergedTables.sortWith((ltA, ltB) => ltA.startPC < ltB.startPC)
                    otherAttributes2 :+ UnpackedLineNumberTable(sortedTable)
                } else {
                    newAttributes1
                }

            new Code(maxStack, maxLocals, instructions, exceptionHandlers, newAttributes2)
        }
    }

    def unapply(
        code: Code
    ): Option[(Int, Int, Array[Instruction], ExceptionHandlers, Attributes)] = {
        import code._
        Some((maxStack, maxLocals, instructions, exceptionHandlers, attributes))
    }

    /**
     * The unique id associated with attributes of kind: [[Code]].
     *
     * `KindId`s can be used for efficient branching on attributes.
     */
    final val KindId = 6

    /**
     * The maximum number of registers required to execute the code - independent
     * of the number of parameters.
     *
     * @note    The method's descriptor may actually require
     */
    def computeMaxLocalsRequiredByCode(instructions: Array[Instruction]): Int = {
        val instructionsLength = instructions.length
        var pc = 0
        var maxRegisterIndex = -1
        var modifiedByWide = false
        do {
            val i: Instruction = instructions(pc)
            if (i == WIDE) {
                modifiedByWide = true
                pc += 1
            } else {
                if (i.writesLocal) {
                    var lastRegisterIndex = i.indexOfWrittenLocal
                    if (i.isStoreLocalVariableInstruction &&
                        i.asStoreLocalVariableInstruction.computationalType.operandSize == 2) {
                        // i.e., not IINC...
                        lastRegisterIndex += 1
                    }
                    if (lastRegisterIndex > maxRegisterIndex) {
                        maxRegisterIndex = lastRegisterIndex
                    }
                }
                pc = i.indexOfNextInstruction(pc, modifiedByWide)
                modifiedByWide = false
            }
        } while (pc < instructionsLength)

        maxRegisterIndex + 1 /* the first register has index 0 */
    }

    def computeMaxLocals(
        isInstanceMethod: Boolean,
        descriptor:       MethodDescriptor,
        instructions:     Array[Instruction]
    ): Int = {
        Math.max(
            computeMaxLocalsRequiredByCode(instructions),
            descriptor.parameterTypes.foldLeft(if (isInstanceMethod) 1 else 0) { (c, n) =>
                c + n.computationalType.operandSize
            }
        )
    }

    def computeCFG(
        instructions:      Array[Instruction],
        exceptionHandlers: ExceptionHandlers  = NoExceptionHandlers,
        classHierarchy:    ClassHierarchy     = ClassHierarchy.PreInitializedClassHierarchy
    ): CFG[Instruction, Code] = {
        CFGFactory(
            Code(Int.MaxValue, Int.MaxValue, instructions, exceptionHandlers),
            classHierarchy
        )
    }

    /**
     * Computes the maximum stack size required when executing this code block.
     *
     * @note If the cfg is available, call the respective `computeMaxStack` method.
     *
     * @throws java.lang.ClassFormatError If the stack size differs between execution paths.
     */
    @throws[ClassFormatError]("if it is impossible to compute the maximum height of the stack")
    def computeMaxStack(
        instructions:      Array[Instruction],
        classHierarchy:    ClassHierarchy     = ClassHierarchy.PreInitializedClassHierarchy,
        exceptionHandlers: ExceptionHandlers  = NoExceptionHandlers
    ): Int = {
        computeMaxStack(
            instructions,
            exceptionHandlers,
            computeCFG(instructions, exceptionHandlers, classHierarchy)
        )
    }

    /**
     * Computes the maximum stack size required when executing this code block.
     *
     * @throws java.lang.ClassFormatError If the stack size differs between execution paths.
     */
    @throws[ClassFormatError]("if it is impossible to compute the maximum height of the stack")
    def computeMaxStack(
        instructions:      Array[Instruction],
        exceptionHandlers: ExceptionHandlers,
        cfg:               CFG[Instruction, Code]
    ): Int = {
        // Basic idea: follow all paths
        var maxStackDepth: Int = 0

        // IntIntPair:  /*PC*/ Int, Int /*stackdepth before executing the instruction*/
        var paths: List[IntIntPair] = List()
        val visitedPCs = new mutable.BitSet(instructions.length)

        // We start with the first instruction and an empty stack.
        paths ::= IntIntPair(0, 0)
        visitedPCs += 0

        // We have to make sure, that all exception handlers are evaluated for
        // max_stack, if an exception is caught, the stack size is always 1 -
        // containing the exception itself.
        for (exceptionHandler <- exceptionHandlers) {
            val handlerPC = exceptionHandler.handlerPC
            if (visitedPCs.add(handlerPC)) paths ::= IntIntPair(handlerPC, 1)
        }

        while (paths.nonEmpty) {
            val stackInfo = paths.head
            val pc = stackInfo._1
            val initialStackDepth = stackInfo._2
            paths = paths.tail
            val stackDepth = initialStackDepth + instructions(pc).stackSlotsChange
            maxStackDepth = Math.max(maxStackDepth, stackDepth)
            cfg.foreachSuccessor(pc) { succPC =>
                if (visitedPCs.add(succPC)) {
                    paths ::= IntIntPair(succPC, stackDepth)
                }
            }
        }

        maxStackDepth
    }

    /**
     * Creates a method body which throws a `java.lang.Error` with the given message or
     * that states that the underlying bytecode is invalid if the message is empty.
     */
    def invalidBytecode(
        descriptor:       MethodDescriptor,
        isInstanceMethod: Boolean,
        message:          Option[String]   = None
    ): Code = {
        new Code(
            maxStack = 3 /* 3 for the message! */ ,
            maxLocals = descriptor.requiredRegisters + (if (isInstanceMethod) 1 else 0),
            instructions =
                Array(
                    NEW(ObjectType.Error), null, null,
                    DUP,
                    message.
                        map(LoadString.apply).
                        getOrElse(LoadString("OPAL: the underlying bytecode is invalid")), null,
                    INVOKESPECIAL(
                        ObjectType.Error,
                        isInterface = false,
                        "<init>",
                        MethodDescriptor.JustTakes(ObjectType.String)
                    ), null, null,
                    ATHROW
                ),
            exceptionHandlers = NoExceptionHandlers,
            attributes = NoAttributes
        )
    }

}
