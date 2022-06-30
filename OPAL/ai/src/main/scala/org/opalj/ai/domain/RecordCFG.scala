/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import java.lang.ref.{SoftReference => SRef}

import scala.collection.BitSet
import scala.collection.mutable
import scala.jdk.CollectionConverters._

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

import org.opalj.collection.mutable.IntArrayStack
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.IntTrieSet1
import org.opalj.collection.immutable.IntRefPair
import org.opalj.graphs.DefaultMutableNode
import org.opalj.graphs.DominatorTree
import org.opalj.graphs.PostDominatorTree
import org.opalj.graphs.DominanceFrontiers
import org.opalj.br.PC
import org.opalj.br.Code
import org.opalj.br.ExceptionHandler
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.ATHROW
import org.opalj.br.cfg.CFG
import org.opalj.br.cfg.ExitNode
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CatchNode

/**
 * Records the abstract interpretation time control-flow graph (CFG).
 * This CFG is always (still) a sound approximation of the generally incomputable
 * real(runtime) CFG.
 *
 * ==Usage (Mixin-Composition Order)==
 * This domain primarily overrides the `flow` method and requires that it is mixed in before every
 * other domain that overrides the `flow` method and which may manipulate the `worklist`.
 * E.g., the mixin order should be:
 * {{{ class MyDomain extends Domain with RecordCFG with FlowManipulatingDomain }}}
 * If the mixin order is not correct, the computed CFG may not be complete/concrete.
 *
 * ==Core Properties==
 *  - Thread-safe: '''No'''; i.e., the composed domain can only be used by one
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
    cfgDomain: ValuesDomain with TheCode =>

    //
    // DIRECTLY RECORDED INFORMATION
    //

    // ... elements are either null or non-empty
    private[this] var regularSuccessors: Array[IntTrieSet] = _

    // ... elements are either null or non-empty
    private[this] var exceptionHandlerSuccessors: Array[IntTrieSet] = _

    private[this] var theNormalExitPCs: IntTrieSet = _

    private[this] var theAbnormalExitPCs: IntTrieSet = _

    private[this] var theSubroutineStartPCs: IntTrieSet = _

    /**
     * The set of nodes to which a(n un)conditional jump back is executed.
     */
    private[this] var theJumpBackTargetPCs: IntTrieSet = _

    //
    // DERIVED INFORMATION
    //

    /**
     * @note    We use the monitor associated with "this" when computing predecessors;
     *          the monitor associated with "this" is not (to be) used otherwise!
     */
    private[this] var thePredecessors: SRef[Array[IntTrieSet]] = _ // uses regularSuccessors as lock

    /**
     * @note    We use the monitor associated with regularSuccessors when computing the dominator
     *          tree; the monitor associated with regularSuccessors is not (to be) used otherwise!
     */
    private[this] var theDominatorTree: SRef[DominatorTree] = _

    /**
     * @note    We use the monitor associated with exceptionHandlerSuccessors when computing the
     *          bb based cfg; the monitor associated with exceptionHandlerSuccessors is not (to be)
     *          used otherwise!
     */
    private[this] var theBBCFG: SRef[CFG[Instruction, Code]] = _

    //
    // METHODS WHICH RECORD THE AI TIME CFG AND WHICH ARE CALLED BY THE FRAMEWORK
    //

    /**
     * @inheritdoc
     *
     * @note If another domain always overrides this method the invocation of this one has to be
     *       ensured; otherwise the recorded CFG will be incomplete.
     */
    abstract override def initProperties(
        code:          Code,
        cfJoins:       IntTrieSet,
        initialLocals: Locals
    ): Unit = {
        val codeSize = code.instructions.length
        regularSuccessors = new Array[IntTrieSet](codeSize)
        exceptionHandlerSuccessors = new Array[IntTrieSet](codeSize)
        theNormalExitPCs = IntTrieSet.empty
        theAbnormalExitPCs = IntTrieSet.empty
        theSubroutineStartPCs = IntTrieSet.empty
        theJumpBackTargetPCs = IntTrieSet.empty

        // The following values are initialized lazily (when required); after the abstract
        // interpretation was (successfully) performed!
        thePredecessors = null
        theBBCFG = null
        theDominatorTree = null

        super.initProperties(code, cfJoins, initialLocals)
    }

    /**
     * @inheritdoc
     *
     * @note If another domain always overrides this method the invocation of this one has to be
     *       ensured; otherwise the recorded CFG will be incomplete.
     */
    abstract override def flow(
        currentPC:                        Int,
        currentOperands:                  Operands,
        currentLocals:                    Locals,
        successorPC:                      Int,
        isSuccessorScheduled:             Answer,
        isExceptionalControlFlow:         Boolean,
        abruptSubroutineTerminationCount: Int,
        wasJoinPerformed:                 Boolean,
        worklist:                         List[Int /*PC*/ ],
        operandsArray:                    OperandsArray,
        localsArray:                      LocalsArray,
        tracer:                           Option[AITracer]
    ): List[Int /*PC*/ ] = {

        if (successorPC <= currentPC) { // "<=" to handle "x: goto x"
            theJumpBackTargetPCs +!= successorPC
        }

        val successors =
            if (isExceptionalControlFlow)
                cfgDomain.exceptionHandlerSuccessors
            else
                cfgDomain.regularSuccessors

        val successorsOfPC = successors(currentPC)
        if (successorsOfPC eq null)
            successors(currentPC) = IntTrieSet1(successorPC)
        else {
            val newSuccessorsOfPC = successorsOfPC +! successorPC
            if (newSuccessorsOfPC ne successorsOfPC) successors(currentPC) = newSuccessorsOfPC
        }

        super.flow(
            currentPC, currentOperands, currentLocals,
            successorPC, isSuccessorScheduled,
            isExceptionalControlFlow, abruptSubroutineTerminationCount,
            wasJoinPerformed,
            worklist,
            operandsArray, localsArray,
            tracer
        )
    }

    /**
     * @inheritdoc
     *
     * @note If another domain always overrides this method the invocation of this one has to be
     *       ensured; otherwise the recorded CFG will be incomplete.
     */
    abstract override def jumpToSubroutine(
        pc:             Int,
        branchTargetPC: Int,
        returnTargetPC: Int
    ): Unit = {
        theSubroutineStartPCs +!= branchTargetPC
        super.jumpToSubroutine(pc, branchTargetPC, returnTargetPC)
    }

    /**
     * @inheritdoc
     *
     * @note If another domain always overrides this method the invocation of this one has to be
     *       ensured; otherwise the recorded CFG will be incomplete.
     */
    abstract override def returnVoid(pc: Int): Computation[Nothing, ExceptionValue] = {
        val r = super.returnVoid(pc)
        if (r.returnsNormally) theNormalExitPCs +!= pc
        if (r.throwsException) theAbnormalExitPCs +!= pc
        r
    }

    /**
     * @inheritdoc
     *
     * @note If another domain always overrides this method the invocation of this one has to be
     *       ensured; otherwise the recorded CFG will be incomplete.
     */
    abstract override def ireturn(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        val r = super.ireturn(pc, value)
        if (r.returnsNormally) theNormalExitPCs +!= pc
        if (r.throwsException) theAbnormalExitPCs +!= pc
        r
    }

    /**
     * @inheritdoc
     *
     * @note If another domain always overrides this method the invocation of this one has to be
     *       ensured; otherwise the recorded CFG will be incomplete.
     */
    abstract override def lreturn(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        val r = super.lreturn(pc, value)
        if (r.returnsNormally) theNormalExitPCs +!= pc
        if (r.throwsException) theAbnormalExitPCs +!= pc
        r
    }

    /**
     * @inheritdoc
     *
     * @note If another domain always overrides this method the invocation of this one has to be
     *       ensured; otherwise the recorded CFG will be incomplete.
     */
    abstract override def freturn(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        val r = super.freturn(pc, value)
        if (r.returnsNormally) theNormalExitPCs +!= pc
        if (r.throwsException) theAbnormalExitPCs +!= pc
        r
    }

    /**
     * @inheritdoc
     *
     * @note If another domain always overrides this method the invocation of this one has to be
     *       ensured; otherwise the recorded CFG will be incomplete.
     */
    abstract override def dreturn(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        val r = super.dreturn(pc, value)
        if (r.returnsNormally) theNormalExitPCs +!= pc
        if (r.throwsException) theAbnormalExitPCs +!= pc
        r
    }

    /**
     * @inheritdoc
     *
     * @note If another domain always overrides this method the invocation of this one has to be
     *       ensured; otherwise the recorded CFG will be incomplete.
     */
    abstract override def areturn(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        val r = super.areturn(pc, value)
        if (r.returnsNormally) theNormalExitPCs +!= pc
        if (r.throwsException) theAbnormalExitPCs +!= pc
        r
    }

    /**
     * @inheritdoc
     *
     * @note If another domain always overrides this method the invocation of this one has to be
     *       ensured; otherwise the recorded CFG will be incomplete.
     */
    abstract override def abruptMethodExecution(
        pc:             Int,
        exceptionValue: ExceptionValue
    ): Unit = {
        theAbnormalExitPCs +!= pc
        super.abruptMethodExecution(pc, exceptionValue)
    }

    /**
     * @inheritdoc
     *
     * @note If another domain always overrides this method the invocation of this one has to be
     *       ensured; otherwise the recorded CFG will be incomplete.
     */
    abstract override def abstractInterpretationEnded(
        aiResult: AIResult { val domain: cfgDomain.type }
    ): Unit = {
        super.abstractInterpretationEnded(aiResult)

        assert(exceptionHandlerSuccessors.forall(s => (s eq null) || s.nonEmpty))
        assert(regularSuccessors.forall(s => (s eq null) || s.nonEmpty))
    }

    // ==================================== BASIC QUERIES ==========================================
    //
    //

    /**
     * Returns all PCs that may lead to the (ab)normal termination of the method. I.e.,
     * those instructions (in particular method call instructions, but potentially also
     * array access instructions and (I]L)DIV|MOD instructions etc.) that may throw
     * some unhandled exceptions will also be returned; even if the instruction may
     * also have regular and also exception handlers!
     */
    def allExitPCs: IntTrieSet = theNormalExitPCs ++ theAbnormalExitPCs

    def isExitPC(pc: PC): Boolean = theNormalExitPCs.contains(pc) || theAbnormalExitPCs.contains(pc)

    /**
     * Returns the PCs of all return instructions which may have returned normally;
     * which are practically always all return instructions unless the analysis _really_
     * finds an unbalanced return; which is __very__ unlikely.
     */
    def normalExitPCs: IntTrieSet = theNormalExitPCs

    /**
     * Returns the PCs of all instructions whose execution may have led to an exception.
     * This can, e.g., be instance field read/field write statements or method invocations,
     * but _in very rare cases_ also return instructions.
     */
    def abnormalExitPCs: IntTrieSet = theAbnormalExitPCs

    /**
     * Returns the PCs of the first instructions of all subroutines; that is, the instructions
     * a `JSR` instruction jumps to.
     */
    def subroutineStartPCs: PCs = theSubroutineStartPCs

    /**
     * The set of instructions to which a jump back is performed.
     */
    def jumpBackTargetPCs: IntTrieSet = theJumpBackTargetPCs

    /**
     * Returns `true` if the instruction with the given `pc` was executed.
     * The `pc` has to identify a valid instruction.
     */
    private[this] final def unsafeWasExecuted(pc: PC): Boolean = {
        // The following "comparatively expensive" test cannot be replace by a simple
        // operandsArray(pc) eq null test as long as we support containing subroutines.
        // In the latter case, a subroutine's (sub) operands array will contain null
        // values directly after the analysis has finished (when the computeBBCFG is
        // potentially called) but before the subroutine results are merged.
        (regularSuccessors(pc) ne null) || (exceptionHandlerSuccessors(pc) ne null) ||
            normalExitPCs.contains(pc) || abnormalExitPCs.contains(pc)
    }

    /**
     * Returns `true` if the instruction with the given `pc` was executed.
     */
    final def wasExecuted(pc: Int): Boolean = pc < code.instructions.length && unsafeWasExecuted(pc)

    /**
     * Computes the set of all executed instructions.
     */
    final def allExecuted: BitSet = {
        val wasExecuted = new mutable.BitSet(code.codeSize)
        code.foreachProgramCounter { pc => if (unsafeWasExecuted(pc)) wasExecuted += pc }
        wasExecuted
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
    def regularSuccessorsOf(pc: Int): PCs = {
        val s = regularSuccessors(pc)
        if (s ne null) s else NoPCs
    }

    final def hasMultipleSuccessors(pc: Int): Boolean = {
        val regularSuccessorsCount = regularSuccessorsOf(pc).size
        regularSuccessorsCount > 1 ||
            (regularSuccessorsCount + exceptionHandlerSuccessorsOf(pc).size) > 1
    }

    def isDirectRegularPredecessorOf(pc: Int, successorPC: Int): Boolean = {
        regularSuccessorsOf(pc).contains(successorPC)
    }

    /**
     * Returns the set of all instructions executed after the instruction with the
     * given `pc`. If this set is empty, either the instruction belongs to dead code,
     * the instruction is a `return` instruction or the `instruction` throws an exception
     * that is never handled internally.
     *
     * @note The set is recalculated on demand.
     */
    def allSuccessorsOf(pc: Int): PCs = {
        regularSuccessorsOf(pc) ++ exceptionHandlerSuccessorsOf(pc)
    }

    final def successorsOf(pc: Int, regularSuccessorOnly: Boolean): PCs = {
        if (regularSuccessorOnly)
            regularSuccessorsOf(pc)
        else
            allSuccessorsOf(pc)
    }

    def hasNoSuccessor(pc: Int): Boolean = {
        (regularSuccessors(pc) eq null) && (exceptionHandlerSuccessors eq null)
    }

    /**
     * Returns `true` if the execution of the given instruction – identified by its pc –
     * ex-/implicitly throws an exception that is (potentially) handled by the method.
     */
    def throwsException(pc: Int): Boolean = exceptionHandlerSuccessors(pc) ne null

    /**
     * Returns `true` if the execution of the given instruction – identified by its pc –
     * '''always just''' throws an exception that is (potentially) handled by the method.
     */
    def justThrowsException(pc: Int): Boolean = {
        (exceptionHandlerSuccessors(pc) ne null) && (regularSuccessors(pc) eq null)
    }

    def foreachSuccessorOf(pc: Int)(f: PC => Unit): Unit = {
        regularSuccessorsOf(pc).foreach(f)
        exceptionHandlerSuccessorsOf(pc).foreach(f)
    }

    /**
     * Tests if the instruction with the given `pc` has a successor instruction with
     * a `pc'` that satisfies the given predicate `p`.
     */
    def hasSuccessor(
        pc:                    Int,
        regularSuccessorsOnly: Boolean,
        p:                     Int /*PC*/ => Boolean
    ): Boolean = {
        var visitedSuccessors: IntTrieSet = IntTrieSet1(pc)
        var successorsToVisit = successorsOf(pc, regularSuccessorsOnly)
        while (successorsToVisit.nonEmpty) {
            if (successorsToVisit.exists { succPC => p(succPC) })
                return true;

            visitedSuccessors ++!= successorsToVisit
            successorsToVisit =
                successorsToVisit.foldLeft(IntTrieSet.empty) { (l, r) =>
                    var newL = l
                    successorsOf(r, regularSuccessorsOnly) foreach { pc =>
                        if (!visitedSuccessors.contains(pc)) newL +!= pc
                    }
                    newL
                }
        }
        false
    }

    /**
     * Tests if the instruction with the given pc is a direct or
     * indirect predecessor of the given successor instruction.
     *
     * If `pc` equals `successorPC` `true` is returned.
     *
     * @note This method will traverse the entire graph if `successorPC` is '''not''' a regular
     *       predecessor of `pc`. Hence, consider using the `(Post)DominatorTree`.
     */
    def isRegularPredecessorOf(pc: Int, successorPC: Int): Boolean = {
        if (pc == successorPC)
            return true;

        // IMPROVE Use a better data-structure; e.g., an IntTrieSet with efficient head and tail operations to avoid that the successorsToVisit contains the same value multiple times
        var visitedSuccessors = Set(pc)
        val successorsToVisit = IntArrayStack.fromSeq(regularSuccessorsOf(pc).iterator) // REFACTOR fromSeq(Iterator...)
        while (successorsToVisit.nonEmpty) {
            val nextPC = successorsToVisit.pop()
            if (nextPC == successorPC)
                return true;

            visitedSuccessors += nextPC
            regularSuccessorsOf(nextPC).foreach { nextSuccessor =>
                if (!visitedSuccessors.contains(nextSuccessor))
                    successorsToVisit.push(nextSuccessor)
            }
        }
        false
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
    def exceptionHandlerSuccessorsOf(pc: Int): PCs = {
        val s = exceptionHandlerSuccessors(pc)
        if (s ne null) s else NoPCs
    }

    /**
     * Returns `true` if the exception handler may handle at least one exception thrown
     * by an instruction in its try block.
     */
    final def handlesException(exceptionHandler: ExceptionHandler): Boolean = {
        val code = this.code
        var currentPC = exceptionHandler.startPC
        val endPC = exceptionHandler.endPC
        val handlerPC = exceptionHandler.handlerPC
        while (currentPC < endPC) {
            if (exceptionHandlerSuccessorsOf(currentPC).contains(handlerPC))
                return true;
            currentPC = code.pcOfNextInstruction(currentPC)
        }
        false
    }

    /**
     * Computes the transitive hull of all instructions reachable from the given instruction.
     */
    def allReachable(pc: Int): IntTrieSet = {
        var allReachable: IntTrieSet = IntTrieSet1(pc)
        var successorsToVisit = allSuccessorsOf(pc)
        while (successorsToVisit.nonEmpty) {
            val IntRefPair(succPC, newSuccessorsToVisit) = successorsToVisit.headAndTail
            successorsToVisit = newSuccessorsToVisit
            if (!allReachable.contains(succPC)) {
                allReachable +!= succPC
                successorsToVisit ++!= allSuccessorsOf(succPC)
            }
        }
        allReachable
    }

    /**
     * Computes the transitive hull of all instructions reachable from the given set of
     * instructions.
     */
    def allReachable(pcs: IntTrieSet): IntTrieSet = {
        pcs.foldLeft(IntTrieSet.empty) { (c, pc) => c ++! allReachable(pc) }
    }

    // ==================== METHODS WHICH COMPUTE DERIVED DATA-STRUCTURES ==========================
    // ===================== OR WHICH OPERATE ON DERIVED DATA-STRUCTURES ===========================
    //
    //

    private[this] def getOrInitField[T >: Null <: AnyRef](
        getFieldValue: () => SRef[T], // executed concurrently
        setFieldValue: SRef[T] => Unit, // never executed concurrently
        lock:          AnyRef
    )(
        computeFieldValue: => T // never executed concurrently
    ): T = {
        val ref = getFieldValue()
        if (ref eq null) {
            lock.synchronized {
                val ref = getFieldValue()
                var f: T = null
                if ((ref eq null) || { f = ref.get(); f eq null }) {
                    val newValue = computeFieldValue
                    setFieldValue(new SRef(newValue))
                    newValue
                } else {
                    f // initialized by a side-effect of evaluating the if condition
                }
            }
        } else {
            val f = ref.get()
            if (f eq null) {
                lock.synchronized {
                    val ref = getFieldValue()
                    var f: T = null
                    if ((ref eq null) || { f = ref.get(); f eq null }) {
                        val newValue = computeFieldValue
                        setFieldValue(new SRef(newValue))
                        newValue
                    } else {
                        f // initialized by a side-effect of evaluating the if condition
                    }
                }
            } else {
                f // best case... already computed and still available
            }
        }
    }

    private[this] def predecessors: Array[IntTrieSet] = {
        getOrInitField[Array[IntTrieSet]](
            () => this.thePredecessors,
            (predecessors) => this.thePredecessors = predecessors, // to cache the result
            this
        ) {
                val predecessors = new Array[IntTrieSet](regularSuccessors.length)
                code.foreachPC { pc =>
                    foreachSuccessorOf(pc) { successorPC =>
                        val oldPredecessorsOfSuccessor = predecessors(successorPC)
                        predecessors(successorPC) =
                            if (oldPredecessorsOfSuccessor eq null) {
                                IntTrieSet1(pc)
                            } else {
                                oldPredecessorsOfSuccessor + pc
                            }
                    }
                }
                predecessors
            }
    }

    /**
     * Returns the program counter(s) of the instruction(s) that is(are) executed
     * before the instruction with the given pc.
     *
     * If the instruction with the given `pc` was never executed an empty set is returned.
     *
     * @param pc A valid program counter.
     */
    def predecessorsOf(pc: Int): PCs = {
        val s = predecessors(pc)
        if (s ne null) s else NoPCs
    }

    /**
     * Returns `true` if the instruction with the given pc has multiple direct
     * predecessors (more than one).
     */
    final def hasMultiplePredecessors(pc: Int): Boolean = predecessorsOf(pc).hasMultipleElements

    final def foreachPredecessorOf(pc: Int)(f: PC => Unit): Unit = {
        predecessorsOf(pc).foreach(f)
    }

    /**
     * Returns the dominator tree; see
     * [[org.opalj.graphs.DominatorTree$.apply[D<:org\.opalj\.graphs\.AbstractDominatorTree]*]]
     * for details regarding the properties of the dominator tree.
     *
     * @note   To get the list of all evaluated instructions and their dominators.
     *         {{{
     *         val result = AI(...,...,...)
     *         val evaluated = result.evaluatedInstructions
     *         }}}
     */
    def dominatorTree: DominatorTree = {
        getOrInitField[DominatorTree](
            () => this.theDominatorTree,
            dt => this.theDominatorTree = dt,
            regularSuccessors
        ) {
                // We want to keep a non-soft reference and avoid any further useless synchronization.
                val predecessors = this.predecessors
                def foreachPredecessorOf(pc: Int)(f: PC => Unit): Unit = {
                    val s = predecessors(pc)
                    if (s ne null)
                        s.foreach(f)
                }

                DominatorTree(
                    startNode = 0,
                    startNodeHasPredecessors = predecessorsOf(0).nonEmpty,
                    foreachSuccessorOf,
                    foreachPredecessorOf,
                    maxNode = code.instructions.length - 1
                )
            }
    }

    /**
     * Returns the first instructions of the infinite loops of the current method. An infinite loop
     * is a set of instructions that does not have a connection to any instruction outside of
     * the loop (closed strongly connected component).
     * I.e., whatever path is taken, all remaining paths will eventually include the loop header
     * instruction.
     * The very vast majority of methods does not have infinite loops.
     */
    def infiniteLoopHeaders: IntTrieSet = {
        if (theJumpBackTargetPCs.isEmpty)
            return IntTrieSet.empty;
        // Let's test if the set of nodes reachable from a potential loop header is
        // closed; i.e., does not include an exit node and does not refer to a node
        // which is outside of the loop.

        // IDEA traverse the cfg from the exit nodes to the start node and try to determine if
        // every potential loop header can be reached.
        val predecessors = this.predecessors
        var remainingPotentialInfiniteLoopHeaders = theJumpBackTargetPCs
        var nodesToVisit = allExitPCs.toList
        val visitedNodes = new Array[Boolean](code.codeSize)
        while (nodesToVisit.nonEmpty) {
            val nextPC = nodesToVisit.head
            nodesToVisit = nodesToVisit.tail
            visitedNodes(nextPC) = true
            val nextPredecessors = predecessors(nextPC)
            if (nextPredecessors ne null) {
                nextPredecessors.foreach { predPC =>
                    remainingPotentialInfiniteLoopHeaders -= predPC
                    if (remainingPotentialInfiniteLoopHeaders.isEmpty) {
                        return IntTrieSet.empty;
                    }
                    if (!visitedNodes(predPC)) {
                        nodesToVisit ::= predPC
                    }
                }
            }
        }
        if (remainingPotentialInfiniteLoopHeaders.size > 1) {
            // We can use the dominance frontier and dominator tree to check if a loop header
            // is actually an infinite loop (potentially inside another infinite loop).
            val dt = dominatorTree
            val df = DominanceFrontiers(dt, wasExecuted)

            remainingPotentialInfiniteLoopHeaders.foreachPair { (pc1, pc2) =>

                if (df.transitiveDF(pc1).contains(pc2)) {
                    // 1.a) check if a loop with header pc1 belongs to the (forward)
                    //      dominance frontier of the loop with header pc2 -
                    //      in this case the loop pc1 is "just" a regular
                    //      loop nested inside the infinite loop pc2.

                    // pc1 is a regular loop inside an infinite loop
                    remainingPotentialInfiniteLoopHeaders -= pc1
                    if (remainingPotentialInfiniteLoopHeaders.size == 1)
                        return remainingPotentialInfiniteLoopHeaders;
                } else if (df.transitiveDF(pc2).contains(pc1)) {
                    // 1.b) (same as 1.a, but vice versa)

                    remainingPotentialInfiniteLoopHeaders -= pc2
                    if (remainingPotentialInfiniteLoopHeaders.size == 1)
                        return remainingPotentialInfiniteLoopHeaders;
                } else if (dt.strictlyDominates(pc1, pc2)) {
                    // 2. Given (due to the first checkt) that both pcs do not identify
                    //    nested loops (in which case we keep the outer one!), we now have
                    //    to check if one loops calls the other loop; in that case we just
                    //    keep the final loop (the one where the loop header is dominated).

                    remainingPotentialInfiniteLoopHeaders -= pc1
                    if (remainingPotentialInfiniteLoopHeaders.size == 1)
                        return remainingPotentialInfiniteLoopHeaders;
                } else if (dt.strictlyDominates(pc2, pc1)) {
                    remainingPotentialInfiniteLoopHeaders -= pc2
                    if (remainingPotentialInfiniteLoopHeaders.size == 1)
                        return remainingPotentialInfiniteLoopHeaders;
                }

            }
        }

        remainingPotentialInfiniteLoopHeaders
    }

    def bbCFG: CFG[Instruction, Code] = {
        getOrInitField[CFG[Instruction, Code]](
            () => theBBCFG, cfg => theBBCFG = cfg, exceptionHandlerSuccessors
        ) { computeBBCFG }
    }

    /**
     * Returns the basic block based representation of the cfg. This CFG may have less nodes
     * than the CFG computed using the naive bytecode representation because it was possible
     * (a) to detect dead paths or (b) to identify that a method call may never throw an exception
     * (in the given situation).
     */
    private[this] def computeBBCFG: CFG[Instruction, Code] = {

        val instructions = code.instructions
        val codeSize = instructions.length

        val normalReturnNode = new ExitNode(normalReturn = true)
        val abnormalReturnNode = new ExitNode(normalReturn = false)

        // 1. basic initialization
        // BBs is a sparse array; only those fields are used that are related to an instruction
        // that was actually executed!
        val bbs = new Array[BasicBlock](codeSize)

        // OLD val exceptionHandlers = mutable.HashMap.empty[Int, CatchNode]
        val exceptionHandlers = new Int2ObjectOpenHashMap[CatchNode](code.exceptionHandlers.size)
        code.exceptionHandlers.iterator.zipWithIndex.foreach {
            case (exceptionHandler, index) =>
                val handlerPC = exceptionHandler.handlerPC
                if ( // 1.1.    Let's check if the handler was executed at all.
                unsafeWasExecuted(handlerPC) &&
                    // 1.2.    The handler may be shared by multiple try blocks, hence, we have
                    //         to ensure the we have at least one instruction in the try block
                    //         that jumps to the handler.
                    handlesException(exceptionHandler)) {
                    // OLD val catchNodeCandidate = new CatchNode(exceptionHandler, index)
                    // OLD val catchNode = exceptionHandlers.getOrElseUpdate(handlerPC, catchNodeCandidate)
                    var catchNode = exceptionHandlers.get(handlerPC)
                    if (catchNode == null) {
                        catchNode = new CatchNode(exceptionHandler, index)
                        exceptionHandlers.put(handlerPC, catchNode)
                    }
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
        }

        // 2. iterate over the code to determine the basic block boundaries
        var runningBB: BasicBlock = null
        val pcIt = code.programCounters
        while (pcIt.hasNext) {
            val pc = pcIt.next()
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
                        // generated by mature compilers, but some compilers,
                        // e.g., the Groovy compiler, are known to produce some
                        // very bad code!
                    }
                }
            }
            if (runningBB ne null) {
                var endRunningBB: Boolean = false
                var connectedWithNextBBs = false

                if (normalExitPCs.contains(pc)) {
                    // the instruction with the given "pc" has to be a return instruction.
                    runningBB.addSuccessor(normalReturnNode)
                    normalReturnNode.addPredecessor(runningBB)
                    endRunningBB = true
                }
                if (abnormalExitPCs.contains(pc)) {
                    runningBB.addSuccessor(abnormalReturnNode)
                    abnormalReturnNode.addPredecessor(runningBB)
                    endRunningBB = true
                }

                // NOTE THAT WE NEVER HAVE TO SPLIT A BLOCK, BECAUSE WE IMMEDIATELY CONSIDER ALL
                // INCOMING AND OUTGOING DEPENDENCIES!
                def connect(sourceBB: BasicBlock, targetBBStartPC: Int): Unit = {
                    var targetBB = bbs(targetBBStartPC)
                    if (targetBB eq null) {
                        targetBB = new BasicBlock(targetBBStartPC)
                        bbs(targetBBStartPC) = targetBB
                    }
                    targetBB.addPredecessor(sourceBB)
                    sourceBB.addSuccessor(targetBB)
                }

                val nextInstructionPC = code.pcOfNextInstruction(pc)
                val theRegularSuccessors = regularSuccessors(pc)
                if (theRegularSuccessors eq null) {
                    endRunningBB = true
                } else {
                    // ... also handles the case where the last instruction is, e.g., a goto
                    if (endRunningBB || theRegularSuccessors.exists(_ != nextInstructionPC)) {
                        theRegularSuccessors.foreach { targetPC => connect(runningBB, targetPC) }
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
                    theExceptionHandlerSuccessors.foreach { handlerPC =>
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

        if (theSubroutineStartPCs.nonEmpty) {
            theSubroutineStartPCs.foreach { pc => bbs(pc).setIsStartOfSubroutine() }
        }

        // 3. create CFG class
        val exBBs = exceptionHandlers.values.iterator().asScala.toList
        CFG(code, normalReturnNode, abnormalReturnNode, exBBs, bbs)
    }

    /**
     * Returns the [[org.opalj.graphs.PostDominatorTree]] (PDT).
     *
     * @note The construction of `PostDominatorTree`s for methods with multiple exit nodes and
     *       also – potentially - infinite loops has several limitations; in particular, if the
     *       results are used for computing control-dependence information.
     *
     * @note If the method/CFG contains infinite loops (see [[#infiniteLoopHeaders]]) then the
     *       instructions which jump back to the infinite loop headers (from within the loop)
     *       are also used as additional exit nodes.
     */
    def postDominatorTree: PostDominatorTree = {
        val exitPCs = allExitPCs
        val uniqueExitNode =
            if (exitPCs.isSingletonSet && regularSuccessorsOf(exitPCs.head).isEmpty)
                Some(exitPCs.head)
            else
                None
        // We want to keep a non-soft reference and avoid any further useless synchronization.
        val predecessors = this.predecessors
        def foreachPredecessorOf(pc: Int)(f: PC => Unit): Unit = {
            val s = predecessors(pc)
            if (s ne null)
                s.foreach(f)
        }

        val infiniteLoopHeaders = this.infiniteLoopHeaders
        if (infiniteLoopHeaders.nonEmpty) {
            val dominatorTree = this.dominatorTree
            var additionalExitNodes = infiniteLoopHeaders flatMap { loopHeaderPC =>
                predecessors(loopHeaderPC).withFilter { predecessorPC =>
                    // 1. let's ensure that the predecessor actually belongs to the loop...
                    loopHeaderPC == predecessorPC ||
                        dominatorTree.strictlyDominates(loopHeaderPC, predecessorPC)
                }
            }
            // Now we have to ensure to select the outer most exit pcs which are dominated by
            // other additional exit nodes...
            additionalExitNodes foreachPair { (exitPC1, exitPC2) =>
                if (dominatorTree.strictlyDominates(exitPC1, exitPC2))
                    additionalExitNodes -= exitPC1
                else if (dominatorTree.strictlyDominates(exitPC2, exitPC1))
                    additionalExitNodes -= exitPC2
            }
            PostDominatorTree(
                uniqueExitNode,
                exitPCs.contains,
                additionalExitNodes,
                exitPCs.foreach,
                foreachSuccessorOf,
                foreachPredecessorOf,
                maxNode = code.instructions.length - 1
            )
        } else {
            PostDominatorTree(
                uniqueExitNode,
                exitPCs.contains,
                IntTrieSet.empty,
                exitPCs.foreach,
                foreachSuccessorOf,
                foreachPredecessorOf,
                maxNode = code.instructions.length - 1
            )
        }

    }

    /**
     * Computes the control dependencies graph based on the post dominator tree.
     *
     * Internally, a post dominator tree is used for methods without infinite loops; i.e.,
     * we compute non-termination ''in''sensitive control dependencies. Note that – dues to
     * exceptions which may lead to abnormal returns
     */
    def pdtBasedControlDependencies: DominanceFrontiers = {
        DominanceFrontiers(postDominatorTree, wasExecuted)
    }

    // ================================== GENERAL HELPER METHODS ===================================
    //
    //
    //

    /**
     * Creates a graph representation of the CFG.
     *
     * @note The returned graph is recomputed whenever this method is called.
     * @note This implementation is for debugging purposes only. It is NOT performance optimized!
     */
    def cfgAsGraph(): DefaultMutableNode[List[Int /*PC*/ ]] = {
        import scala.collection.immutable.{List => ScalaList}
        val instructions = code.instructions
        val codeSize = instructions.length
        val nodes = new Array[DefaultMutableNode[List[Int /*PC*/ ]]](codeSize)
        val nodePredecessorsCount = new Array[Int](codeSize)
        // 1. create nodes
        val exitNode = new DefaultMutableNode[List[Int /*PC*/ ]](
            Nil,
            (n) => "Exit",
            Map(
                "shape" -> "doubleoctagon",
                "fillcolor" -> "black",
                "color" -> "white",
                "labelloc" -> "l"
            ),
            ScalaList.empty[DefaultMutableNode[List[Int /*PC*/ ]]]
        )
        for (pc <- code.programCounters) {
            nodes(pc) = {
                var visualProperties = Map("shape" -> "box", "labelloc" -> "l")

                if (instructions(pc).isReturnInstruction) {
                    visualProperties += "fillcolor" -> "green"
                    visualProperties += "style" -> "filled"
                } else if (instructions(pc).isInstanceOf[ATHROW.type]) {
                    if (abnormalExitPCs.contains(pc)) {
                        visualProperties += "fillcolor" -> "red"
                        visualProperties += "style" -> "filled"
                    } else {
                        visualProperties += "fillcolor" -> "yellow"
                        visualProperties += "style" -> "filled"
                    }
                } else if (allSuccessorsOf(pc).isEmpty && !isExitPC(pc)) {
                    visualProperties += "fillcolor" -> "red"
                    visualProperties += "style" -> "filled"
                    visualProperties += "shape" -> "octagon"
                }

                if (code.exceptionHandlersFor(pc).nonEmpty) {
                    visualProperties += "color" -> "orange"
                }

                if (code.exceptionHandlers.exists { eh => eh.handlerPC == pc }) {
                    visualProperties += "peripheries" -> "2"
                }

                def pcsToString(pcs: List[Int /*PC*/ ]): String = {
                    def pcToString(pc: Int): String = {
                        val ln = code.lineNumber(pc).map(ln => s"[ln=$ln]").getOrElse("")
                        s"$pc$ln: ${cfgDomain.code.instructions(pc).toString(pc)}"
                    }
                    pcs.map(pcToString).mkString("", "\\l\\l", "\\l")
                }

                new DefaultMutableNode(
                    List(pc),
                    pcsToString,
                    visualProperties,
                    ScalaList.empty[DefaultMutableNode[List[Int /*PC*/ ]]]
                )
            }
        }
        // 2. create edges
        for (pc <- code.programCounters) {
            for (succPC <- allSuccessorsOf(pc)) {
                nodes(pc).addChild(nodes(succPC))
                nodePredecessorsCount(succPC) += 1
            }
            if (isExitPC(pc)) {
                nodes(pc).addChild(exitNode)
            }
        }

        // 3. fold nodes
        // Nodes that have only one successor and where the successor has only one
        // predecessor are merged into one node; basically, we recreate the
        // _effective_ basic blocks; an _effective_ basic block is a block where we do
        // _not observe_ any jumps in and out unless we are at the beginning or end of
        // the block
        for (pc <- code.programCounters) {
            val currentNode = nodes(pc)
            if (currentNode.hasOneChild) {
                val successorNode = currentNode.firstChild
                if (successorNode ne exitNode) {
                    val successorNodePC = successorNode.identifier.head
                    if (nodePredecessorsCount(successorNodePC) == 1) {
                        currentNode.updateIdentifier(
                            currentNode.identifier ::: currentNode.firstChild.identifier
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
