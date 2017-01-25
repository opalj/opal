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
package ai
package domain

import scala.collection.BitSet
import scala.collection.mutable

import org.opalj.collection.immutable.{Chain ⇒ List}
import org.opalj.collection.immutable.{Naught ⇒ Nil}
import org.opalj.collection.mutable.UShortSet
import org.opalj.br.PC
import org.opalj.br.Code
import org.opalj.br.instructions.ReturnInstruction
import org.opalj.br.instructions.ATHROW
import org.opalj.graphs.DefaultMutableNode
import org.opalj.graphs.DominatorTree
import org.opalj.graphs.PostDominatorTree
import org.opalj.graphs.DominatorTreeFactory
import org.opalj.graphs.ControlDependencies
import org.opalj.graphs.ControlDependenceGraph
import org.opalj.br.cfg.CFG
import org.opalj.br.cfg.ExitNode
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CatchNode
import org.opalj.br.ExceptionHandler

/**
 * Records the abstract interpretation time control-flow graph (CFG).
 * This CFG is always (still) a sound approximation of the generally incomputable real CFG.
 *
 * ==Usage (Mixin-Composition Order)==
 * This domain overrides the `flow` method and requires that it is mixed in before every
 * other domain that overrides the `flow` method and which may manipulate the `worklist`.
 * E.g., the mixin order should be:
 * {{{ class MyDomain extends Domain with RecordCFG with FlowManipulatingDomain }}}
 * If the mixin order is not correct, the CFG may not be complete/concrete.
 *
 * ==Core Properties==
 *  - Thread-safe: '''No'''; i.e., the domain can only be used by one
 *              abstract interpreter at a time.
 *              However, using the collected results is thread-safe!
 *  - Reusable: '''Yes'''; all state directly associated with the analyzed code block is
 *              reset by the method `initProperties`.
 *  - No Partial Results: If the abstract interpretation was aborted the results have
 *              no meaning and must not be used; however, if the abstract interpretation
 *              is later continued and successfully completed the results are correct.
 *
 * @author Michael Eichberg
 * @author Marc Eichler
 */
trait RecordCFG
        extends CoreDomainFunctionality
        with CustomInitialization
        with ai.ReturnInstructionsDomain {
    domain: ValuesDomain with TheCode ⇒

    private[this] var regularSuccessors: Array[UShortSet] = _
    private[this] var exceptionHandlerSuccessors: Array[UShortSet] = _
    private[this] var predecessors: Array[UShortSet] = _
    private[this] var exitPCs: mutable.BitSet = _
    private[this] var subroutineStartPCs: UShortSet = _
    private[this] var theDominatorTree: DominatorTree = _
    private[this] var thePostDominatorTree: DominatorTreeFactory = _
    private[this] var theControlDependencies: ControlDependencies = _
    private[this] var theBBCFG: CFG = _

    abstract override def initProperties(
        code:          Code,
        cfJoins:       BitSet,
        initialLocals: Locals
    ): Unit = {
        val codeSize = code.instructions.size
        regularSuccessors = new Array[UShortSet](codeSize)
        exceptionHandlerSuccessors = new Array[UShortSet](codeSize)
        exitPCs = new mutable.BitSet(codeSize)
        subroutineStartPCs = UShortSet.empty

        // The following values are initialized lazily (when required); after the abstract
        // interpretation was (successfully) performed!
        predecessors = null
        theDominatorTree = null
        thePostDominatorTree = null
        theControlDependencies = null
        theBBCFG = null

        super.initProperties(code, cfJoins, initialLocals)
    }

    /**
     * Returns all PCs that may lead to the ab(normal) termination of the method. I.e.,
     * those instructions (in particular method call instructions) that may throw
     * some unhandled exceptions will also be returned; even if the instruction may
     * also have regular and also exception handlers!
     *
     * @note This information is lazily computed.
     */
    def allExitPCs: BitSet = exitPCs

    /**
     * Returns the PCs of the first instruction of all subroutines.
     */
    def allSubroutineStartPCs: UShortSet = subroutineStartPCs

    /**
     * Returns the program counter(s) of the instruction(s) that is(are) executed
     * before the instruction with the given pc.
     *
     * If the instruction with the given `pc` was never executed an empty set is
     * returned.
     *
     * @param pc A valid program counter.
     */
    def predecessorsOf(pc: PC): PCs = {

        var predecessors = this.predecessors
        if (predecessors eq null) synchronized {
            predecessors = this.predecessors
            if (predecessors eq null) {
                // => this.regularPredecessors == null
                predecessors = new Array[UShortSet](regularSuccessors.length)
                for {
                    pc ← code.programCounters
                    successorPC ← allSuccessorsOf(pc)
                } {
                    val oldPredecessorsOfSuccessor = predecessors(successorPC)
                    predecessors(successorPC) =
                        if (oldPredecessorsOfSuccessor eq null) {
                            UShortSet(pc)
                        } else {
                            pc +≈: oldPredecessorsOfSuccessor
                        }

                }
                this.predecessors = predecessors
            }
        }
        val s = predecessors(pc)
        if (s ne null) s else NoPCs
    }

    /**
     * Returns the dominator tree.
     *
     * @note
     * To get the list of all evaluated instructions and their dominators.
     * {{{
     *  val result = AI(...,...,...)
     *  val evaluated = result.evaluatedInstructions
     * }}}
     */
    def dominatorTree: DominatorTree = {
        var theDominatorTree = this.theDominatorTree
        if (theDominatorTree eq null) synchronized {
            theDominatorTree = this.theDominatorTree
            if (theDominatorTree eq null) {
                theDominatorTree =
                    DominatorTree(
                        startNode = 0,
                        startNodeHasPredecessors = predecessorsOf(0).nonEmpty,
                        foreachSuccessorOf,
                        foreachPredecessorOf,
                        maxNode = code.instructions.size - 1
                    )
                this.theDominatorTree = theDominatorTree
            }
        }
        theDominatorTree
    }

    def postDominatorTreeFactory: DominatorTreeFactory = {
        var thePostDominatorTree = this.thePostDominatorTree
        if (thePostDominatorTree eq null) synchronized {
            thePostDominatorTree = this.thePostDominatorTree
            if (thePostDominatorTree eq null) {
                thePostDominatorTree =
                    PostDominatorTree(
                        allExitPCs.contains,
                        allExitPCs.foreach,
                        foreachSuccessorOf,
                        foreachPredecessorOf,
                        maxNode = code.instructions.size - 1
                    )
                this.thePostDominatorTree = thePostDominatorTree
            }
        }
        thePostDominatorTree
    }

    def postDominatorTree: DominatorTree = postDominatorTreeFactory.dt

    def controlDependencies: ControlDependencies = {
        var theControlDependencies = this.theControlDependencies
        if (theControlDependencies eq null) synchronized {
            theControlDependencies = this.theControlDependencies
            if (theControlDependencies eq null) {
                val pdtf = postDominatorTreeFactory
                theControlDependencies = ControlDependenceGraph(pdtf, wasExecuted)
                this.theControlDependencies = theControlDependencies
            }
        }
        theControlDependencies
    }

    /**
     * Returns the program counter(s) of the instruction(s) that is(are) executed next if
     * the evaluation of this instruction may succeed without raising an exception.
     *
     * The returned set is always empty for `return` instructions. It is also empty for
     * instructions that always throw an exception (e.g., an integer value that is divided
     * by zero will always result in a NullPointException.)
     *
     * @note The [[org.opalj.br.instructions.ATHROW]] instruction will never have a
     *      `regularSuccessor`. The `return` instructions will never have any successors.
     */
    def regularSuccessorsOf(pc: PC): PCs = {
        val s = regularSuccessors(pc)
        if (s ne null) s else NoPCs
    }

    /**
     * Returns the program counter(s) of the instruction(s) that is(are) executed next if
     * the evaluation of this instruction may raise an exception.
     *
     * The returned set is always empty for instructions that cannot raise exceptions,
     * such as the `StackManagementInstruction`s.
     *
     * @note    The [[org.opalj.br.instructions.ATHROW]] has successors if and only if the
     *          thrown exception is directly handled inside this code block.
     * @note    The successor instructions are necessarily the handlers of catch blocks.
     */
    def exceptionHandlerSuccessorsOf(pc: PC): PCs = {
        val s = exceptionHandlerSuccessors(pc)
        if (s ne null) s else NoPCs
    }

    /**
     * Tests if the instruction with the given `pc` has a successor instruction with
     * a `pc'` that satisfies the given predicate `p`.
     */
    def hasSuccessor(
        pc:                    PC,
        regularSuccessorsOnly: Boolean,
        p:                     PC ⇒ Boolean
    ): Boolean = {
        var visitedSuccessors = UShortSet(pc)
        var successorsToVisit = successorsOf(pc, regularSuccessorsOnly)
        while (successorsToVisit.nonEmpty) {
            if (successorsToVisit.exists { succPC ⇒ p(succPC) })
                return true;

            visitedSuccessors = visitedSuccessors ++ successorsToVisit
            successorsToVisit =
                successorsToVisit.foldLeft(UShortSet.empty) { (l, r) ⇒
                    l ++ (
                        successorsOf(r, regularSuccessorsOnly).filter { pc ⇒
                            !visitedSuccessors.contains(pc)
                        }
                    )
                }
        }
        false
    }

    /**
     * Returns the set of all instructions executed after the instruction with the
     * given `pc`. If this set is empty, either the instruction belongs to dead code,
     * the instruction is a `return` instruction or the `instruction` throws an exception
     * that is never handled internally.
     *
     * @note The set is recalculated on demand.
     */
    def allSuccessorsOf(pc: PC): PCs = {
        regularSuccessorsOf(pc) ++ exceptionHandlerSuccessorsOf(pc)
    }

    final def successorsOf(pc: PC, regularSuccessorOnly: Boolean): PCs = {
        if (regularSuccessorOnly)
            regularSuccessorsOf(pc)
        else
            allSuccessorsOf(pc)
    }

    final def hasMultipleSuccessors(pc: PC): Boolean = {
        val regularSuccessorsCount = regularSuccessorsOf(pc).size
        regularSuccessorsCount > 1 ||
            (regularSuccessorsCount + exceptionHandlerSuccessorsOf(pc).size) > 1
    }

    final def foreachPredecessorOf(pc: PC)(f: PC ⇒ Unit): Unit = {
        predecessorsOf(pc).foreach { f }
    }

    final def foreachSuccessorOf(pc: PC)(f: PC ⇒ Unit): Unit = {
        regularSuccessorsOf(pc).foreach { f }
        exceptionHandlerSuccessorsOf(pc).foreach { f }
    }

    /**
     * Returns `true` if the instruction with the given pc has multiple direct
     * predecessors (more than one).
     */
    final def hasMultiplePredecessors(pc: PC): Boolean = predecessorsOf(pc).size > 1

    private[this] final def unsafeWasExecuted(pc: PC): Boolean = {
        (regularSuccessors(pc) ne null) || (exceptionHandlerSuccessors(pc) ne null) ||
            exitPCs.contains(pc)
    }

    final def wasExecuted(pc: PC): Boolean = pc < code.instructions.size && unsafeWasExecuted(pc)

    /**
     * Returns true if the exception handler may handle at least one exception thrown
     * by an instruction in the try block.
     */
    final def handlesException(exceptionHandler: ExceptionHandler): Boolean = {
        val endPC = exceptionHandler.endPC
        val handlerPC = exceptionHandler.handlerPC
        var currentPC = exceptionHandler.startPC
        while (currentPC <= endPC) {
            if (exceptionHandlerSuccessorsOf(currentPC).exists(_ == handlerPC))
                return true;
            currentPC = code.pcOfNextInstruction(currentPC)
        }
        false
    }

    /**
     * Tests if the instruction with the given pc is a direct or
     * indirect predecessor of the given successor instruction.
     */
    def isRegularPredecessorOf(pc: PC, successorPC: PC): Boolean = {
        var visitedSuccessors = UShortSet(pc)
        var successorsToVisit = regularSuccessorsOf(pc)
        while (successorsToVisit.nonEmpty) {
            if (successorsToVisit.contains(successorPC))
                return true;

            visitedSuccessors = visitedSuccessors ++ successorsToVisit
            successorsToVisit =
                successorsToVisit.foldLeft(UShortSet.empty) { (l, r) ⇒
                    l ++ (regularSuccessorsOf(r).filter { pc ⇒ !visitedSuccessors.contains(pc) })
                }
        }
        false
    }

    def bbCFG: CFG = {
        var theBBCFG = this.theBBCFG
        if (theBBCFG eq null) synchronized {
            theBBCFG = this.theBBCFG
            if (theBBCFG eq null) {
                theBBCFG = computeBBCFG
                this.theBBCFG = theBBCFG
            }
        }
        theBBCFG
    }

    /**
     * Returns the basic block based representation of the cfg. This CFG may have less nodes
     * than the CFG computed using the naive bytecode representation because it was possible
     * (a) to detect dead paths or (b) to identify that a method call may never throw an exception
     * (in the given situation).
     */
    private[this] def computeBBCFG: CFG = {

        val instructions = code.instructions
        val codeSize = instructions.length

        val normalReturnNode = new ExitNode(normalReturn = true)
        val abnormalReturnNode = new ExitNode(normalReturn = false)

        // 1. basic initialization
        // BBs is a sparse array; only those fields are used that are related to an instruction
        // that was actually executed!
        val bbs = new Array[BasicBlock](codeSize)

        val exceptionHandlers = mutable.HashMap.empty[PC, CatchNode]
        for {
            exceptionHandler ← code.exceptionHandlers
            // 1.1.    Let's check if the handler was executed at all.
            if unsafeWasExecuted(exceptionHandler.handlerPC)
            // 1.2.    The handler may be shared by multiple try blocks, hence, we have
            //         to ensure the we have at least one instruction in the try block
            //         that jumps to the handler.
            if handlesException(exceptionHandler)
        } {
            val handlerPC = exceptionHandler.handlerPC
            val catchNodeCandiate = new CatchNode(exceptionHandler)
            val catchNode = exceptionHandlers.getOrElseUpdate(handlerPC, catchNodeCandiate)
            var handlerBB = bbs(handlerPC)
            if (handlerBB eq null) {
                handlerBB = new BasicBlock(handlerPC)
                handlerBB.addPredecessor(catchNode)
                bbs(handlerPC) = handlerBB
            } else {
                handlerBB.addPredecessor(catchNode)
            }
            catchNode.addSuccessor(handlerBB)
        }

        // 2. iterate over the code to determine the basic block boundaries
        var runningBB: BasicBlock = null
        val pcIt = code.programCounters
        while (pcIt.hasNext) {
            val pc = pcIt.next
            if (runningBB eq null) {
                runningBB = bbs(pc)
                if (runningBB eq null) {
                    if (unsafeWasExecuted(pc)) {
                        runningBB = new BasicBlock(pc)
                        bbs(pc) = runningBB
                    } else {
                        // When we reach this point, we have found code that is
                        // dead in the sense that it is not reachable on any
                        // possible control-flow. Such code is typically not
                        // generated by mature compilers, but some compilers
                        // e.g., the Groovy compiler are known to produce some
                        // very bad code!
                    }
                }
            }
            if (runningBB ne null) {
                var endRunningBB: Boolean = false
                var connectedWithNextBBs = false

                if (exitPCs.contains(pc)) {
                    val successorNode = code.instructions(pc) match {
                        case r: ReturnInstruction ⇒ normalReturnNode
                        case _                    ⇒ abnormalReturnNode
                    }
                    runningBB.addSuccessor(successorNode)
                    successorNode.addPredecessor(runningBB)
                    endRunningBB = true
                    // connection is done later, when we handle the (regular) successors
                }

                // NOTE THAT WE NEVER HAVE TO SPLIT A BLOCK, BECAUSE WE IMMEDIATELY CONSIDER ALL
                // INCOMING AND OUTGOING DEPENDENCIES!
                def connect(sourceBB: BasicBlock, targetBBStartPC: PC): Unit = {
                    var targetBB = bbs(targetBBStartPC)
                    if (targetBB eq null) {
                        targetBB = new BasicBlock(targetBBStartPC)
                        bbs(targetBBStartPC) = targetBB
                    }
                    targetBB.addPredecessor(sourceBB)
                    sourceBB.addSuccessor(targetBB)
                }

                val nextInstructionPC = code.pcOfNextInstruction(pc)
                val theRegularSuccessors = regularSuccessorsOf(pc)
                if (theRegularSuccessors.isEmpty) {
                    endRunningBB = true
                } else {
                    // the following also handles the case where the last instruction is, e.g., a goto
                    if (endRunningBB || theRegularSuccessors.exists(_ != nextInstructionPC)) {
                        theRegularSuccessors.foreach { targetPC ⇒ connect(runningBB, targetPC) }
                        endRunningBB = true
                        connectedWithNextBBs = true
                    }
                }

                val theExceptionHandlerSuccessors = exceptionHandlerSuccessorsOf(pc)
                if (theExceptionHandlerSuccessors.nonEmpty) {
                    if (!endRunningBB && !connectedWithNextBBs) {
                        connect(runningBB, nextInstructionPC)
                        connectedWithNextBBs = true
                    }
                    endRunningBB = true
                    theExceptionHandlerSuccessors.foreach { handlerPC ⇒
                        val catchNode: CatchNode = exceptionHandlers(handlerPC)
                        catchNode.addPredecessor(runningBB)
                        runningBB.addSuccessor(catchNode)
                    }
                }
                if (!endRunningBB &&
                    !connectedWithNextBBs &&
                    hasMultiplePredecessors(nextInstructionPC)) {
                    endRunningBB = true
                    connect(runningBB, nextInstructionPC)
                }

                if (endRunningBB) {
                    runningBB.endPC = pc
                    runningBB = null
                } else {
                    bbs(nextInstructionPC) = runningBB
                }
            }
        }

        if (subroutineStartPCs.nonEmpty) {
            subroutineStartPCs.foreach { pc ⇒ bbs(pc).setIsStartOfSubroutine() }
        }

        // 3. create CFG class
        CFG(code, normalReturnNode, abnormalReturnNode, exceptionHandlers.values.toList, bbs)
    }

    //
    // METHODS CALLED BY THE ABSTRACT INTERPRETATION FRAMEWORK WHICH RECORD THE AI TIME
    // CFG
    //

    /**
     * @inheritdoc
     *
     * @note This method is called by the abstract interpretation framework.
     */
    abstract override def flow(
        currentPC:                        PC,
        currentOperands:                  Operands,
        currentLocals:                    Locals,
        successorPC:                      PC,
        isSuccessorSchedulued:            Answer,
        isExceptionalControlFlow:         Boolean,
        abruptSubroutineTerminationCount: Int,
        wasJoinPerformed:                 Boolean,
        worklist:                         List[PC],
        operandsArray:                    OperandsArray,
        localsArray:                      LocalsArray,
        tracer:                           Option[AITracer]
    ): List[PC] = {

        val successors =
            if (isExceptionalControlFlow)
                domain.exceptionHandlerSuccessors
            else
                domain.regularSuccessors

        val successorsOfPC = successors(currentPC)
        if (successorsOfPC eq null)
            successors(currentPC) = UShortSet(successorPC)
        else {
            val newSuccessorsOfPC = successorPC +≈: successorsOfPC
            if (newSuccessorsOfPC ne successorsOfPC)
                successors(currentPC) = newSuccessorsOfPC
        }

        super.flow(
            currentPC, currentOperands, currentLocals,
            successorPC, isSuccessorSchedulued,
            isExceptionalControlFlow, abruptSubroutineTerminationCount,
            wasJoinPerformed,
            worklist,
            operandsArray, localsArray,
            tracer
        )
    }

    abstract override def jumpToSubroutine(pc: PC, branchTarget: PC, returnTarget: PC): Unit = {
        subroutineStartPCs = branchTarget +≈: subroutineStartPCs
        super.jumpToSubroutine(pc, branchTarget, returnTarget)
    }

    /**
     * @inheritdoc
     *
     * @note This method is only intended to be called by the AI framework.
     */
    abstract override def returnVoid(
        pc: PC
    ): Computation[Nothing, ExceptionValue] = {
        exitPCs += pc
        super.returnVoid(pc)
    }

    /**
     * @inheritdoc
     *
     * @note This method is only intended to be called by the AI framework.
     */
    abstract override def ireturn(
        pc:    PC,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        exitPCs += pc
        super.ireturn(pc, value)
    }

    /**
     * @inheritdoc
     *
     * @note This method is only intended to be called by the AI framework.
     */
    abstract override def lreturn(
        pc:    PC,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        exitPCs += pc
        super.lreturn(pc, value)
    }

    /**
     * @inheritdoc
     *
     * @note This method is only intended to be called by the AI framework.
     */
    abstract override def freturn(
        pc:    PC,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        exitPCs += pc
        super.freturn(pc, value)
    }

    /**
     * @inheritdoc
     *
     * @note This method is only intended to be called by the AI framework.
     */
    abstract override def dreturn(
        pc:    PC,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        exitPCs += pc
        super.dreturn(pc, value)
    }

    /**
     * @inheritdoc
     *
     * @note This method is only intended to be called by the AI framework.
     */
    abstract override def areturn(
        pc:    PC,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        exitPCs += pc
        super.areturn(pc, value)
    }

    /**
     * @inheritdoc
     *
     * @note This method is only intended to be called by the AI framework.
     */
    abstract override def abruptMethodExecution(
        pc:             PC,
        exceptionValue: ExceptionValue
    ): Unit = {
        exitPCs += pc
        super.abruptMethodExecution(pc, exceptionValue)
    }

    // GENERAL HELPER METHODS

    /**
     * Creates a graph representation of the CFG.
     *
     * @note The returned graph is recomputed whenever this method is called.
     * @note This implementation is for debugging purposes only. It is NOT performance optimized!
     */
    def cfgAsGraph(): DefaultMutableNode[List[PC]] = {
        import scala.collection.immutable.{List ⇒ ScalaList}
        val instructions = code.instructions
        val codeSize = instructions.size
        val nodes = new Array[DefaultMutableNode[List[PC]]](codeSize)
        val nodePredecessorsCount = new Array[Int](codeSize)
        // 1. create nodes
        val exitNode = new DefaultMutableNode[List[PC]](
            Nil,
            (n) ⇒ "Exit",
            Map("shape" → "doubleoctagon", "fillcolor" → "black", "color" → "white", "labelloc" → "l"),
            ScalaList.empty[DefaultMutableNode[List[PC]]]
        )
        for (pc ← code.programCounters) {
            nodes(pc) = {
                var visualProperties = Map("shape" → "box", "labelloc" → "l")

                if (instructions(pc).isInstanceOf[ReturnInstruction]) {
                    visualProperties += "fillcolor" → "green"
                    visualProperties += "style" → "filled"
                } else if (instructions(pc).isInstanceOf[ATHROW.type]) {
                    if (allExitPCs.contains(pc)) {
                        visualProperties += "fillcolor" → "red"
                        visualProperties += "style" → "filled"
                    } else {
                        visualProperties += "fillcolor" → "yellow"
                        visualProperties += "style" → "filled"
                    }
                } else if (allSuccessorsOf(pc).isEmpty && !allExitPCs.contains(pc)) {
                    visualProperties += "fillcolor" → "red"
                    visualProperties += "style" → "filled"
                    visualProperties += "shape" → "octagon"
                }

                if (code.exceptionHandlersFor(pc).nonEmpty) {
                    visualProperties += "color" → "orange"
                }

                if (code.exceptionHandlers.exists { eh ⇒ eh.handlerPC == pc }) {
                    visualProperties += "peripheries" → "2"
                }

                def pcsToString(pcs: List[PC]): String = {
                    def pcToString(pc: PC): String = {
                        val ln = code.lineNumber(pc).map(ln ⇒ s"[ln=$ln]").getOrElse("")
                        pc + ln+": "+domain.code.instructions(pc).toString(pc)
                    }
                    pcs.map(pcToString(_)).mkString("", "\\l\\l", "\\l")
                }

                new DefaultMutableNode(
                    List(pc),
                    pcsToString,
                    visualProperties,
                    ScalaList.empty[DefaultMutableNode[List[PC]]]
                )
            }
        }
        // 2. create edges
        for (pc ← code.programCounters) {
            for (succPC ← allSuccessorsOf(pc)) {
                nodes(pc).addChild(nodes(succPC))
                nodePredecessorsCount(succPC) += 1
            }
            if (allExitPCs.contains(pc)) {
                nodes(pc).addChild(exitNode)
            }
        }

        // 3. fold nodes
        // Nodes that have only one successor and where the successor has only one
        // predecessor are merged into one node; basically, we recreate the
        // _effective_ basic blocks; an _effective_ basic block is a block where we do
        // _not observe_ any jumps in and out unless we are at the beginning or end of
        // the block
        for (pc ← code.programCounters) {
            val currentNode = nodes(pc)
            if (currentNode.hasOneChild) {
                val successorNode = currentNode.firstChild
                if (successorNode ne exitNode) {
                    val successorNodePC = successorNode.identifier.head
                    if (nodePredecessorsCount(successorNodePC) == 1) {
                        currentNode.updateIdentifier(
                            currentNode.identifier :&:: currentNode.firstChild.identifier
                        )
                        currentNode.mergeVisualProperties(successorNode.visualProperties)
                        currentNode.removeLastAddedChild() // the only child...
                        currentNode.addChildren(successorNode.children)
                        nodes(successorNodePC) = currentNode
                    }
                }
            }
        }

        nodes(0)
    }
}
