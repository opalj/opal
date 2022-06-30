/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cfg

import scala.reflect.ClassTag

import java.util.Arrays

import scala.collection.{Set => SomeSet}
import scala.collection.AbstractIterator

import org.opalj.log.LogContext
import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger.info
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.IntTrieSet1
import org.opalj.collection.mutable.FixedSizedHashIDMap
import org.opalj.collection.mutable.IntArrayStack
import org.opalj.graphs.DefaultMutableNode
import org.opalj.graphs.DominatorTree
import org.opalj.graphs.Node

/**
 * Represents the control flow graph of a method.
 *
 * To compute a `CFG` use the [[CFGFactory]].
 *
 * ==Thread-Safety==
 * This class is thread-safe; all data is effectively immutable '''after construction''' time.
 *
 * @param   code The code for which the CFG was build.
 * @param   normalReturnNode The unique exit node of the control flow graph if the
 *          method returns normally. If the method always throws an exception, this
 *          node will not have any predecessors.
 * @param   abnormalReturnNode The unique exit node of the control flow graph if the
 *          method returns abnormally (throws an exception). If the method is guaranteed
 *          to never throw an exception, this node will not have any predecessors.
 * @param   catchNodes List of all catch nodes. (Usually, we have one [[CatchNode]] per
 *          [[org.opalj.br.ExceptionHandler]], but if an exception handler does not catch
 *          anything, no [[CatchNode]] is created.)
 * @param   basicBlocks An implicit map between a program counter and its associated
 *          [[BasicBlock]]; it may be a sparse array!
 *
 * @author Erich Wittenbeck
 * @author Michael Eichberg
 */
case class CFG[I <: AnyRef, C <: CodeSequence[I]](
        code:                    C,
        normalReturnNode:        ExitNode,
        abnormalReturnNode:      ExitNode,
        catchNodes:              Seq[CatchNode],
        private val basicBlocks: Array[BasicBlock]
) { cfg =>

    if (CFG.Validate) {
        val allBBs = basicBlocks.filter(_ != null)
        val allBBsSet = allBBs.toSet
        // 1. Check that each basic block has a lower start pc than the end pc
        //    i.e., startPC <= endPC.
        check(
            allBBs.forall(bb => bb.startPC <= bb.endPC),
            allBBs.filter(bb => bb.startPC > bb.endPC).mkString
        )

        // 2. Check that each pc belonging to a basic block (bb) actually points to the respective bb
        //    i.e., pc in basicBlock : bb => basicBlock(pc) == bb.
        check(
            allBBsSet.forall { bb =>
                (bb.startPC to bb.endPC).forall { pc =>
                    (basicBlocks(pc) eq null) || (basicBlocks(pc) eq bb)
                }
            },
            basicBlocks.zipWithIndex.filter(_._1 != null).
                map(bb => s"${bb._2}:${bb._1}#${System.identityHashCode(bb._1).toHexString}").
                mkString("basic blocks mapping broken:\n\t", ",\n\t", "\n")
        )

        // 3. Check that the CFG is self-consistent; i.e., that no node references a node
        //    that does not occur in the BB.
        check(
            allBBsSet.forall { bb =>
                bb.successors.forall { successorBB =>
                    (successorBB.isBasicBlock && {
                        val succBB = successorBB.asBasicBlock
                        (basicBlocks(succBB.startPC) eq succBB) && (basicBlocks(succBB.endPC) eq succBB)
                    }) ||
                        (successorBB.isCatchNode && catchNodes.contains(successorBB.asCatchNode)) ||
                        successorBB.isExitNode
                }
            },
            allBBs.
                map(bb => bb.toString+" => "+bb.successors.mkString(", ")).
                mkString("unexpected successors:\n\t", "\n\t", "")
        )
        check(
            allBBsSet.forall { bb =>
                bb.predecessors.forall { predecessorBB =>
                    (
                        predecessorBB.isBasicBlock && {
                            val predBB = predecessorBB.asBasicBlock
                            (basicBlocks(predBB.startPC) eq predBB) && (basicBlocks(predBB.endPC) eq predBB)
                        }
                    ) ||
                        (predecessorBB.isCatchNode && catchNodes.contains(predecessorBB.asCatchNode))
                }
            },
            basicBlocks.zipWithIndex.filter(_._1 != null).map(_.swap).
                map(bb => s"${bb._1}:${bb._2.toString} predecessors: ${bb._2.predecessors.mkString(", ")}").
                mkString("unexpected predecessors:\n\t", "\n\t", s"\ncode:$code")
        )

        // 4.  Check that all catch nodes referred to by the basic blocks are listed in the
        //     sequence of catch nodes
        check(
            allBBs.
                filter(bb => bb.successors.exists { _.isCatchNode }).
                flatMap(bb => bb.successors.collect { case cn: CatchNode => cn }).
                forall(catchBB => catchNodes.contains(catchBB)),
            catchNodes.mkString("the set of catch nodes {", ", ", "} is incomplete:\n") +
                allBBs.collect {
                    case bb if bb.successors.exists(succBB => succBB.isCatchNode && !catchNodes.contains(succBB)) =>
                        s"$bb => ${bb.successors.collect { case cn: CatchNode => cn }.mkString(", ")}"
                }.mkString("\n")
        )

        // 5.   Check that predecessors and successors are consistent.
        check(
            allBBsSet.
                forall(bb => bb.successors.forall { succBB => succBB.predecessors.contains(bb) }),
            "successors and predecessors are inconsistent; e.g., "+
                allBBsSet.
                find(bb => !bb.successors.forall { succBB => succBB.predecessors.contains(bb) }).
                map(bb => bb.successors.find(succBB => !succBB.predecessors.contains(bb)).map(succBB =>
                    s"$succBB is a successor of $bb, but does not list it as a predecessor").get).get
        )
        check(
            allBBsSet.
                forall(bb => bb.predecessors.forall { predBB => predBB.successors.contains(bb) }),
            "predecessors and successors are inconsistent; e.g., "+
                allBBsSet.
                find(bb => !bb.predecessors.forall { predBB => predBB.successors.contains(bb) }).
                map(bb => bb.predecessors.find(predBB => !predBB.successors.contains(bb)).map(predBB =>
                    s"predBB is a predecessor of $bb, but does not list it as a successor").get).get
        )
    }

    /**
     * Computes the maximum fixed point solution for forward data-flow analyses.
     *
     * @param seed The initial facts associated with the first instruction (pc = 0).
     *
     * @param t The transfer function which implements the analysis:
     *        `(Facts, I, PC, CFG.SuccessorId) => Facts`.
     *        The parameters are: 1. the current set of facts, 2. the current instruction,
     *        3. the program counter of the current instruction and 4. the id of the successor.
     *
     * @param join The operation (typically a set intersection or set union) that is
     *        executed to join the results of the predecessors of a specific instruction.
     *        '''It is required that join returns the left (first) set as is if the set of facts
     *        didn't change.'''
     *        I.e., even if the left and right sets contain the same values and are
     *       `equal` (`==`) it is necessary to return the left set.
     */
    final def performForwardDataFlowAnalysis[Facts >: Null <: AnyRef: ClassTag](
        seed: Facts,
        t:    (Facts, I, PC, CFG.SuccessorId) => Facts,
        join: (Facts, Facts) => Facts
    ): (Array[Facts], /*normal return*/ Facts, /*abnormal return*/ Facts) = {

        implicit val logContext: LogContext = GlobalLogContext

        val instructions = code.instructions
        val codeSize = instructions.length

        val entryFacts = new Array[Facts](codeSize) // the facts before instruction evaluation
        entryFacts(0) = seed
        var normalReturnFacts: Facts = null
        var abnormalReturnFacts: Facts = null

        val workList = new IntArrayStack(Math.min(codeSize, 10))
        workList.push(0)

        while (workList.nonEmpty) {
            val pc = workList.pop()
            val instruction = instructions(pc)
            val facts = entryFacts(pc)

            foreachLogicalSuccessor(pc) {
                case CFG.NormalReturnId =>
                    val newFactsNoException = t(facts, instruction, pc, CFG.NormalReturnId)
                    normalReturnFacts =
                        if (normalReturnFacts == null) {
                            newFactsNoException
                        } else {
                            join(normalReturnFacts, newFactsNoException)
                        }

                case CFG.AbnormalReturnId =>
                    val newFactsException = t(facts, instruction, pc, CFG.AbnormalReturnId)
                    abnormalReturnFacts =
                        if (abnormalReturnFacts == null) {
                            newFactsException
                        } else {
                            join(abnormalReturnFacts, newFactsException)
                        }

                case succId =>
                    val newFacts = t(facts, instruction, pc, succId)
                    val effectiveSuccPC = if (succId < 0) -succId else succId
                    val succPCFacts = entryFacts(effectiveSuccPC)
                    if (succPCFacts == null) {
                        if (CFG.TraceDFSolver) {
                            info("progress - df solver", s"[initial] $pc -> $succId: $newFacts")
                        }
                        entryFacts(effectiveSuccPC) = newFacts
                        workList += effectiveSuccPC
                    } else {
                        val newSuccPCFacts = join(succPCFacts, newFacts)
                        if (newSuccPCFacts ne succPCFacts) {
                            if (CFG.TraceDFSolver) {
                                info("progress - df solver", s"[update] $pc -> $succId: $succPCFacts -> $newSuccPCFacts")
                            }
                            entryFacts(effectiveSuccPC) = newSuccPCFacts
                            workList += effectiveSuccPC
                        } else {
                            if (CFG.TraceDFSolver) {
                                info("progress - df solver", s"[no update] $pc -> $succId: $succPCFacts -> $newSuccPCFacts")
                            }
                        }
                    }
            }
        }

        (entryFacts, normalReturnFacts, abnormalReturnFacts)
    }

    /**
     * Computes the maximum fixed point solution for backward data-flow analyses.
     *
     * @param seed The initial facts associated with instructions which lead to (ab)normal
     *        returns.
     *
     * @param t The transfer function which implements the analysis:
     *        `(Facts, I, PC, CFG.PredecessorId) => Facts`.
     *        The parameters are: 1. the current set of facts, 2. the current instruction,
     *        3. the program counter of the current instruction and 4. the id of the predecessor.
     *
     * @param join The operation (typically a set intersection or set union) that is
     *        executed to join the results of the successors of a specific instruction.
     *        '''It is required that join returns the left (first) set as is if the set of facts
     *        didn't change.'''
     *        I.e., even if the left and right sets contain the same values and are
     *       `equal` (`==`) it is necessary to return the left set.
     *
     * @note   No facts will derived for stmts that are not reachable from an
     *         exit node; e.g., due to an infinite loop.
     *         That is, the returned array may contain `null` values and in an
     *         extreme case will only contain null values!
     */
    final def performBackwardDataFlowAnalysis[Facts >: Null <: AnyRef: ClassTag](
        seed: Facts,
        t:    (Facts, I, PC, CFG.PredecessorId) => Facts,
        join: (Facts, Facts) => Facts
    ): (Array[Facts], /*init*/ Facts) = {

        implicit val logContext: LogContext = GlobalLogContext

        val instructions = code.instructions
        val codeSize = instructions.length

        val exitFacts = new Array[Facts](codeSize) // stores the facts after instruction evaluation
        val workList = new IntArrayStack(Math.min(codeSize, 10))
        normalReturnNode.predecessors.foreach { predBB =>
            val returnPC = predBB.asBasicBlock.endPC
            exitFacts(returnPC) = seed
            workList.push(returnPC)
        }
        abnormalReturnNode.predecessors.foreach { predBB =>
            val stmtPC = predBB.asBasicBlock.endPC
            exitFacts(stmtPC) = seed
            workList.push(stmtPC)
        }

        var initFacts: Facts = null

        def handleTransition(pc: PC, predId: Int): Unit = {
            val instruction = instructions(pc)
            val facts = exitFacts(pc)
            val newFacts = t(facts, instruction, pc, predId)

            if (predId >= 0) {
                val predPCFacts = exitFacts(predId)
                if (predPCFacts == null) {
                    if (CFG.TraceDFSolver) {
                        info("progress - df solver", s"[initial] $pc -> $predId: $newFacts")
                    }
                    exitFacts(predId) = newFacts
                    workList += predId
                } else {
                    val newPredPCFacts = join(predPCFacts, newFacts)
                    if (newPredPCFacts ne predPCFacts) {
                        if (CFG.TraceDFSolver) {
                            info(
                                "progress - df solver",
                                s"[update] $pc -> $predId: $predPCFacts -> $newPredPCFacts"
                            )
                        }
                        exitFacts(predId) = newPredPCFacts
                        workList += predId
                    } else {
                        if (CFG.TraceDFSolver) {
                            info(
                                "progress - df solver",
                                s"[no update] $pc -> $predId: $predPCFacts -> $newPredPCFacts"
                            )
                        }
                    }
                }
            } else {
                if (initFacts == null) {
                    if (CFG.TraceDFSolver) {
                        info("progress - df solver", s"[initial] $pc -> -1: $newFacts")
                    }
                    initFacts = newFacts
                } else {
                    val newInitFacts = join(initFacts, newFacts)
                    if (newInitFacts ne initFacts) {
                        if (CFG.TraceDFSolver) {
                            info(
                                "progress - df solver",
                                s"[update] $pc -> -1: $initFacts -> $newInitFacts"
                            )
                        }
                        initFacts = newFacts
                    } else {
                        if (CFG.TraceDFSolver) {
                            info(
                                "progress - df solver",
                                s"[no update] $pc -> -1: $initFacts -> $newFacts"
                            )
                        }
                    }
                }
            }
        }

        while (workList.nonEmpty) {
            val pc = workList.pop()
            foreachPredecessor(pc) { predPC => handleTransition(pc, predPC) }
            if (pc == 0) handleTransition(pc, -1)
        }

        (exitFacts, initFacts)
    }

    /**
     * The basic block associated with the very first instruction.
     */
    final def startBlock: BasicBlock = basicBlocks(0)

    /**
     * Returns the basic block to which the instruction with the given `pc` belongs.
     *
     * @param pc A valid pc.
     * @return The basic block associated with the given `pc`. If the `pc` is not valid,
     *         `null` is returned or an index out of bounds exception is thrown.
     */
    final def bb(pc: Int): BasicBlock = basicBlocks(pc)

    /**
     * Returns the set of all reachable [[CFGNode]]s of the control flow graph.
     */
    lazy val reachableBBs: SomeSet[CFGNode] = basicBlocks(0).reachable(reflexive = true)

    /**
     * Iterates over the set of all [[BasicBlock]]s. (I.e., the exit and catch nodes are
     * not returned.) Always returns the basic block containing the first instruction first.
     */
    def allBBs: Iterator[BasicBlock] = {
        new AbstractIterator[BasicBlock] {

            private[this] var currentBBPC = 0

            def hasNext: Boolean = currentBBPC < basicBlocks.length

            def next(): BasicBlock = {
                val basicBlocks = cfg.basicBlocks
                val current = basicBlocks(currentBBPC)
                currentBBPC = current.endPC + 1
                // jump to the end and check if the instruction directly following this bb
                // actually belongs to a basic block
                val maxPC = basicBlocks.length
                while (currentBBPC < maxPC && (basicBlocks(currentBBPC) eq null)) {
                    currentBBPC += 1
                }
                current
            }
        }
    }

    def allNodes: Iterator[CFGNode] = {
        allBBs ++ catchNodes.iterator ++ Iterator(normalReturnNode, abnormalReturnNode)
    }

    /**
     * Returns all direct runtime successors of the instruction with the given pc.
     *
     * If the returned set is empty, then the instruction is either a return instruction or an
     * instruction that always causes an exception to be thrown that is not handled by
     * a handler of the respective method.
     *
     * @note   If possible, the function `foreachSuccessor` should be used as it does not have
     *         to create comparatively expensive intermediate data structures.
     *
     * @param pc A valid pc of an instruction of the code block from which this cfg was derived.
     */
    def successors(pc: Int): IntTrieSet = {
        val bb = this.bb(pc)
        if (bb.endPC > pc) {
            // it must be - w.r.t. the code array - the next instruction
            IntTrieSet1(code.pcOfNextInstruction(pc))
        } else {
            // the set of successor can be (at the same time) a RegularBB or an ExitNode
            var successorPCs = IntTrieSet.empty
            bb.successors foreach {
                case bb: BasicBlock => successorPCs += bb.startPC
                case cb: CatchNode  => successorPCs += cb.handlerPC
                case _              =>
            }
            successorPCs
        }
    }

    /**
     * Iterates over the direct successors of the instruction with the given pc and calls the given
     * function `f` for each successor. `f` is guaranteed to be called only once for each successor
     * instruction. (E.g., relevant in case of a switch where multiple cases are handled in the
     * same way.)
     */
    def foreachSuccessor(pc: Int)(f: PC => Unit): Unit = {
        val bb = this.bb(pc)
        if (bb.endPC > pc) {
            // it must be - w.r.t. the code array - the next instruction
            f(code.pcOfNextInstruction(pc))
        } else {
            // the set of successors can be (at the same time) a RegularBB or an ExitNode
            var visited = IntTrieSet.empty
            bb.successors foreach { bb =>
                val nextPC =
                    if (bb.isBasicBlock) bb.asBasicBlock.startPC
                    else if (bb.isCatchNode) bb.asCatchNode.handlerPC
                    else -1
                if (nextPC != -1 && !visited.contains(nextPC)) {
                    visited += nextPC
                    f(nextPC)
                }
                // else if (bb.isExitNode)... is not relevant
            }
        }
    }

    /**
     * Iterates over the direct successors of the instruction with the given pc and calls the given
     * function `f` for each successor. `f` is guaranteed to be called only once for each successor
     * instruction. (E.g., relevant in case of a switch where multiple cases are handled in the
     * same way.)
     * The value passed to f will either be:
     *  - the pc of an instruction.
     *  - the value `CFG.AbnormalReturnId` (`Int.MinValue`) in case the evaluation of the
     *    instruction with the given pc throws an exception that leads to an abnormal return.
     *  - the value `CFG.NormalReturnId` (`Int.MaxValue`) in case the evaluation of the
     *    (return) instruction with the given `pc` leads to a normal return.
     *  - `-(successorPC)` if the evaluation leads to an exception that is caught and where the
     *    first instruction of the handler has the given `successorPC`.
     */
    def foreachLogicalSuccessor(pc: Int)(f: Int => Unit): Unit = {
        val bb = this.bb(pc)
        if (bb.endPC > pc) {
            // it must be - w.r.t. the code array - the next instruction
            f(code.pcOfNextInstruction(pc))
        } else {
            // The set of successors can be (at the same time) a RegularBB, a CatchBB or an ExitNode
            var visited = IntTrieSet.empty
            bb.successors foreach {
                case bb: BasicBlock =>
                    val nextPC = bb.startPC
                    if (!visited.contains(nextPC)) {
                        visited += nextPC
                        f(nextPC)
                    }
                case cn: CatchNode =>
                    val nextPC = cn.handlerPC
                    if (!visited.contains(nextPC)) {
                        visited += nextPC
                        f(-nextPC)
                    }
                case en: ExitNode =>
                    f(if (en.normalReturn) CFG.NormalReturnId else CFG.AbnormalReturnId)
            }
        }
    }

    def predecessors(pc: Int): IntTrieSet = {
        if (pc == 0)
            return IntTrieSet.empty;

        val bb = this.bb(pc)
        if (bb.startPC == pc) {
            var predecessorPCs = IntTrieSet.empty
            bb.predecessors foreach {
                case bb: BasicBlock =>
                    predecessorPCs += bb.endPC
                case cn: CatchNode =>
                    cn.predecessors.foreach { bb =>
                        predecessorPCs += bb.asBasicBlock.endPC
                    }
            }
            predecessorPCs
        } else {
            IntTrieSet1(code.pcOfPreviousInstruction(pc))
        }
    }

    def foreachPredecessor(pc: Int)(f: Int => Unit): Unit = {
        if (pc == 0)
            return ;

        val bb = this.bb(pc)
        if (bb.startPC == pc) {
            var visited = IntTrieSet.empty
            bb.predecessors foreach { bb =>
                if (bb.isBasicBlock) {
                    f(bb.asBasicBlock.endPC)
                } else if (bb.isCatchNode) {
                    bb.asCatchNode.predecessors foreach { predBB =>
                        val nextPC = predBB.asBasicBlock.endPC
                        if (!visited.contains(nextPC)) {
                            visited += nextPC
                            f(nextPC)
                        }
                    }
                }
            }
        } else {
            f(code.pcOfPreviousInstruction(pc))
        }
    }

    /**
     * @return Returns the dominator tree of this CFG.
     *
     * @see [[DominatorTree.apply]]
     */
    def dominatorTree: DominatorTree = {
        DominatorTree(
            0,
            basicBlocks.head.predecessors.nonEmpty,
            foreachSuccessor,
            foreachPredecessor,
            basicBlocks.last.endPC
        )
    }

    /**
     * Creates a new CFG where the boundaries of the basic blocks are updated given the `pcToIndex`
     * mapping. The assumption is made that the indexes are continuous.
     * If the first index (i.e., `pcToIndex(0)` is not 0, then a new basic block for the indexes
     * in {0,pcToIndex(0)} is created if necessary.
     *
     * @param  lastIndex The index of the last instruction of the underlying (non-empty) code array.
     *         I.e., if the instruction array contains one instruction then the `lastIndex` has
     *         to be `0`.
     * @param  singletonBBsExpander Function called for each basic block which encompasses a single
     *         instruction to expand the BB to encompass more instructions. This supports the
     *         case where an instruction was transformed in a way that resulted in multiple
     *         instructions/statements, but which all belong to the same basic block.
     *         ''This situation cannot be handled using pcToIndex.''
     *         This information is used to ensure that if a basic block, which currently just
     *         encompasses a single instruction, will encompass the new and the old instruction
     *         afterwards.
     *         The returned value will be used as the `endIndex.`
     *         `endIndex = singletonBBsExpander(pcToIndex(pc of singleton bb))`
     *         Hence, the function is given the mapped index has to return that value if the index
     *         does not belong to the expanded instruction.
     */
    def mapPCsToIndexes[NewI <: AnyRef, NewC <: CodeSequence[NewI]](
        newCode:              NewC,
        pcToIndex:            Array[Int /*PC*/ ],
        singletonBBsExpander: Int /*PC*/ => Int,
        lastIndex:            Int
    ): CFG[NewI, NewC] = {

        /*
        // [USED FOR DEBUGGING PURPOSES] *********************************************************
        println(
            basicBlocks.
                filter(_ != null).
                toSet.
                map((bb: BasicBlock) => bb.toString+" => "+bb.successors.mkString(", ")).
                mkString("Successors:\n", "\n", "\n")
        )
        println(
            basicBlocks.
                filter(_ != null).
                toSet.
                map((bb: BasicBlock) => bb.predecessors.mkString(", ")+" => "+bb.toString).
                mkString("Predecessors:\n", "\n", "\n")
        )
        println(catchNodes.mkString("CatchNodes:", ",", "\n"))
        println(pcToIndex.zipWithIndex.map(_.swap).mkString("Mapping:", ",", "\n"))
        //
        // ********************************************************* [USED FOR DEBUGGING PURPOSES]
        */

        val bbsLength = basicBlocks.length
        // Note, catch node ids have values in the range [-3-number of catch nodes,-3].
        // Furthermore, we may have "dead" exception handlers and therefore the number
        // of catch nodes does not necessarily reflect the smallest catch node id.
        val leastCatchNodeId = if (catchNodes.isEmpty) 0 else catchNodes.iterator.map(_.nodeId).min
        val bbMapping = FixedSizedHashIDMap[CFGNode, CFGNode](
            minValue = Math.min(-2 /*the exit nodes*/ , leastCatchNodeId),
            maxValue = code.instructions.length
        )

        val newBasicBlocks = new Array[BasicBlock](lastIndex + 1)
        val newBasicBlocksArray = newBasicBlocks.asInstanceOf[Array[Object]]
        val requiresNewStartBlock = pcToIndex(0) > 0

        var lastNewBB: BasicBlock = null
        if (requiresNewStartBlock) {
            val endIndex = pcToIndex(0) - 1
            // we have added instructions at the beginning which belong to a new start bb
            lastNewBB = new BasicBlock(startPC = 0, _endPC = endIndex)
            Arrays.fill(newBasicBlocksArray, 0, endIndex + 1, lastNewBB)
        }
        var startPC = 0
        do {
            val oldBB = basicBlocks(startPC)
            val startIndex = pcToIndex(startPC)
            val endIndex = {
                val initialCandidate = pcToIndex(oldBB.endPC)
                val endIndexCandidate =
                    if (initialCandidate == -1) { // There may be dead instructions
                        pcToIndex(
                            ((oldBB.endPC - 1) to oldBB.startPC by -1).
                                find(pcToIndex(_) > -0).getOrElse(0)
                        )
                    } else initialCandidate
                if (startIndex == endIndexCandidate) {
                    singletonBBsExpander(startIndex)
                } else {
                    endIndexCandidate
                }
            }
            lastNewBB = new BasicBlock(startIndex, endIndex)
            bbMapping.put(oldBB, lastNewBB)
            Arrays.fill(newBasicBlocksArray, startIndex, endIndex + 1, lastNewBB)
            // let's advance startPC to the next instruction which is live (which has a BB)
            startPC = oldBB.endPC + 1
            var tempBB: BasicBlock = null
            while (startPC < bbsLength && {
                tempBB = basicBlocks(startPC)
                (tempBB eq null) || pcToIndex(tempBB.startPC) < 0
            }) {
                assert(tempBB ne oldBB)
                // This (index < 0) handles the case where the initial CFG was created using
                // a simple algorithm that actually resulted in a CFG with detached basic blocks;
                // we now kill these basic blocks by jumping over them!
                // NOTE: This is indicative of dead code in the bytecode in the first place!
                if (tempBB ne null) {
                    startPC = tempBB.endPC
                }
                startPC += 1
            }
        } while (startPC < bbsLength)

        if (requiresNewStartBlock) {
            val firstBB = newBasicBlocks(0)
            val secondBB = newBasicBlocks(pcToIndex(0))
            firstBB.addSuccessor(secondBB)
            secondBB.addPredecessor(firstBB)
        }

        // add the catch nodes
        val codeSize = code.instructions.length
        catchNodes foreach { cn =>
            val newCN = cn.copy(
                startPC = pcToIndex(cn.startPC),
                endPC = if (cn.endPC == codeSize) lastIndex + 1 else pcToIndex(cn.endPC),
                handlerPC = pcToIndex(cn.handlerPC)
            )
            bbMapping.put(cn, newCN)
        }

        val newNormalReturnNode = new ExitNode(normalReturn = true)
        bbMapping.put(normalReturnNode, newNormalReturnNode)
        val newAbnormalReturnNode = new ExitNode(normalReturn = false)
        bbMapping.put(abnormalReturnNode, newAbnormalReturnNode)

        // rewire the graph
        bbMapping iterate { (oldBB, newBB) =>
            oldBB.successors foreach { oldSuccBB =>
                val newSuccBB = bbMapping(oldSuccBB)
                assert(newSuccBB ne null, s"no mapping for $oldSuccBB")
                newBB.addSuccessor(newSuccBB)
                // Instead of iterating over the predecessors, we just iterate over
                // the successors; this way, we only include the nodes that are
                // live; nodes that; e.g., are attached to the exit node but for
                // which there is no path to reach them at all are dropped!
                newSuccBB.addPredecessor(newBB)
            }
        }

        val newCatchNodes = catchNodes.map(bbMapping(_).asInstanceOf[CatchNode])
        assert(newCatchNodes.forall { _ ne null })

        // let's see if we can merge the first two basic blocks
        if (requiresNewStartBlock && basicBlocks(0).predecessors.isEmpty) {
            val firstBB = newBasicBlocks(0)
            val secondBB = firstBB.successors.head.asBasicBlock
            val newFirstBB = secondBB.copy(startPC = 0, predecessors = Set.empty)
            newFirstBB.successors.foreach(succBB => succBB.updatePredecessor(secondBB, newFirstBB))
            Arrays.fill(newBasicBlocksArray, 0, secondBB._endPC + 1 /* (exclusive)*/ , newFirstBB)
        }

        CFG[NewI, NewC](
            newCode, newNormalReturnNode, newAbnormalReturnNode, newCatchNodes, newBasicBlocks
        )
    }

    // ---------------------------------------------------------------------------------------------
    //
    // Visualization & Debugging
    //
    // ---------------------------------------------------------------------------------------------

    override def toString: String = {
        //        code:                    Code,
        //        normalReturnNode:        ExitNode,
        //        abnormalReturnNode:      ExitNode,
        //        catchNodes:              Seq[CatchNode],
        //        private val basicBlocks: Array[BasicBlock]

        val cfgNodes: Set[CFGNode] =
            basicBlocks.filter(_ ne null).toSet[CFGNode] ++
                catchNodes +
                normalReturnNode +
                abnormalReturnNode

        val bbIds: Map[CFGNode, Int] = cfgNodes.zipWithIndex.toMap

        bbIds.map { bbId =>
            val (bb, id) = bbId
            if (bb.isExitNode) {
                s"BB_${id.toHexString}: $bb"
            } else {
                bb.successors.
                    map(succBB => "BB_"+bbIds(succBB).toHexString).
                    mkString(s"BB_${id.toHexString}: $bb -> {", ",", "}")
            }
        }.toList.sorted.mkString("CFG(\n\t", "\n\t", "\n)")
    }

    def toDot: String = {
        val rootNodes = Set(startBlock) ++ catchNodes
        org.opalj.graphs.toDot(rootNodes)
    }

    /**
     * @return The pair:
     *         `('''Node for the start BB''', '''all Nodes (incl. the node for the start BB)''')`
     */
    def toDot(
        f:                      BasicBlock => String,
        includeAbnormalReturns: Boolean              = true
    ): (Node, Iterable[Node]) = {
        // 1. create a node foreach cfg node
        val bbsIterator = allBBs
        val startBB = bbsIterator.next()
        val startNodeVisualProperties = Map("fillcolor" -> "green", "style" -> "filled", "shape" -> "box")
        val startBBNode = new DefaultMutableNode(
            f(startBB),
            theVisualProperties = startNodeVisualProperties
        )
        var cfgNodeToGNodes: Map[CFGNode, DefaultMutableNode[String]] = Map(startBB -> startBBNode)
        cfgNodeToGNodes ++= bbsIterator.map(bb => (bb, new DefaultMutableNode(f(bb))))
        cfgNodeToGNodes ++= catchNodes.map(cn => (cn, new DefaultMutableNode(cn.toString)))
        cfgNodeToGNodes += (
            abnormalReturnNode -> {
                if (includeAbnormalReturns)
                    new DefaultMutableNode(
                        "abnormal return", theVisualProperties = abnormalReturnNode.visualProperties
                    )
                else
                    null
            }
        )
        cfgNodeToGNodes += (
            normalReturnNode ->
            new DefaultMutableNode(
                "return", theVisualProperties = normalReturnNode.visualProperties
            )
        )

        // 2. reconnect nodes
        cfgNodeToGNodes foreach { cfgNodeToGNode =>
            val (cfgNode, gNode) = cfgNodeToGNode
            if (gNode != null)
                cfgNode.successors foreach { cfgNode =>
                    val targetGNode = cfgNodeToGNodes(cfgNode)
                    if (targetGNode != null) gNode.addChild(targetGNode)
                }
        }

        val nodes = cfgNodeToGNodes.values
        (startBBNode, nodes)
    }
}

object CFG {

    final val NormalReturnId = Int.MaxValue
    final val AbnormalReturnId = Int.MinValue

    /**
     * Identifies the successor of an instruction.
     * The id is either:
     *  - `CFG.NormalReturnId` (`== Int.MaxValue`) if the successor is the unique exit
     *           node representing normal returns.
     *  - `CFG.AbnormalReturnId` (`== Int.MinValue`) if the successor is the unique exit
     *           node representing abnormal returns (i.e., an uncaught exception will be thrown
     *           by the instruction).
     *  - [-65535,-1] to identify the catch handler that handles the exception thrown
     *           by the current instruction. The pc of the first instruction of the catch handler
     *           is (`- successorId`).
     *  - the (valid) pc of the next instruction (the normal case.)
     */
    final type SuccessorId = Int

    /**
     * Identifies the predecessor of an instruction.
     *  - -1 if the current instruction is the first instruction (`pc`/`index` = `0`)
     *  - a regular pc.
     */
    final type PredecessorId = Int

    final val ValidateKey = "org.opalj.br.cfg.CFG.Validate"

    private[this] var validate: Boolean = {
        val initialValidate = BaseConfig.getBoolean(ValidateKey)
        updateValidate(initialValidate)
        initialValidate
    }

    // We think of it as a runtime constant (which can be changed for testing purposes).
    def Validate: Boolean = validate

    def updateValidate(newValidate: Boolean): Unit = {
        implicit val logContext: GlobalLogContext.type = GlobalLogContext
        validate =
            if (newValidate) {
                info("OPAL", s"$ValidateKey: validation on")
                true
            } else {
                info("OPAL", s"$ValidateKey: validation off")
                false
            }
    }

    final val TraceDFSolverKey = "org.opalj.br.cfg.CFG.DF.Solver.Trace"

    final val TraceDFSolver: Boolean = {
        val traceDFSolver = BaseConfig.getBoolean(TraceDFSolverKey)
        info("OPAL", s"$TraceDFSolverKey: $traceDFSolver")(GlobalLogContext)
        traceDFSolver
    }
}
