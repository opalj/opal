/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import scala.annotation.tailrec

import scala.collection.{Set => SomeSet}
import scala.collection.mutable

import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalE
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.value.ValueInformation
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFG
import org.opalj.br.cfg.CFGNode
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.properties.Context
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.analyses.AbstractIFDSAnalysis.V
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.tac.fpcf.properties.IFDSProperty
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI

/**
 * The super type of all IFDS facts.
 */
trait AbstractIFDSFact

/**
 * The super type of all null facts.
 */
trait AbstractIFDSNullFact extends AbstractIFDSFact

/**
 * A framework for IFDS analyses.
 *
 * @tparam IFDSFact The type of flow facts the concrete analysis wants to track
 * @author Dominik Helm
 * @author Mario Trageser
 */
abstract class AbstractIFDSAnalysis[IFDSFact <: AbstractIFDSFact] extends FPCFAnalysis {

    /**
     * Provides the concrete property key that must be unique for every distinct concrete analysis
     * and the lower bound for the IFDSProperty.
     */
    val propertyKey: IFDSPropertyMetaInformation[IFDSFact]

    /**
     * Creates an IFDSProperty containing the result of this analysis.
     *
     * @param result Maps each exit statement to the facts valid after the exit statement.
     * @return An IFDSProperty containing the `result`.
     */
    def createPropertyValue(result: Map[Statement, Set[IFDSFact]]): IFDSProperty[IFDSFact]

    /**
     * Computes the DataFlowFacts valid after statement `statement` on the CFG edge to statement `succ`
     * if the DataFlowFacts `in` held before `statement`.
     */

    /**
     * Computes the data flow for a normal statement.
     *
     * @param statement The analyzed statement.
     * @param successor The successor of the analyzed `statement`, to which the data flow is considered.
     * @param in Some facts valid before the execution of the `statement`.
     * @return The facts valid after the execution of `statement`
     *         under the assumption that `in` held before `statement` and `successor` will be executed next.
     */
    def normalFlow(statement: Statement, successor: Statement, in: Set[IFDSFact]): Set[IFDSFact]

    /**
     * Computes the data flow for a call to start edge.
     *
     * @param call The analyzed call statement.
     * @param calleeContext The called method.
     * @param in Some facts valid before the execution of the `call`.
     * @return The facts valid after the execution of `statement` under the assumption that `in` held before `statement` and `statement` calls `callee`.
     */
    def callFlow(
        call: Statement, calleeContext: Context, in: Set[IFDSFact]
    ): Set[IFDSFact]

    /**
     * Computes the data flow for a exit to return edge.
     *
     * @param call The statement, which called the `callee`.
     * @param calleeContext The method called by `call`.
     * @param exit The statement, which terminated the `calle`.
     * @param successor The statement of the caller, which will be executed after the `callee` returned.
     * @param in Some facts valid before the execution of the `exit`.
     * @return The facts valid after the execution of `exit` in the caller's context
     *         under the assumption that `in` held before the execution of `exit` and that `successor` will be executed next.
     */
    def returnFlow(
        call:          Statement,
        calleeContext: Context,
        exit:          Statement,
        successor:     Statement,
        in:            Set[IFDSFact]
    ): Set[IFDSFact]

    /**
     * Computes the data flow for a call to return edge.
     *
     * @param call The statement, which invoked the call.
     * @param successor The statement, which will be executed after the call.
     * @param in Some facts valid before the `call`.
     * @return The facts valid after the call independently of what happens in the callee under the assumption that `in` held before `call`.
     */
    def callToReturnFlow(
        call: Statement, successor: Statement, in: Set[IFDSFact]
    ): Set[IFDSFact]

    /**
     * Computes the data flow for a summary edge of a native method call.
     *
     * @param call The statement, which invoked the call.
     * @param calleeContext The method, called by `call`.
     * @param successor The statement, which will be executed after the call.
     * @param in Some facts valid before the `call`.
     * @return The facts valid after the call, excluding the call-to-return flow.
     */
    def nativeCall(call: Statement, calleeContext: Context, successor: Statement, in: Set[IFDSFact]): Set[IFDSFact]

    /**
     * All declared methods in the project.
     */
    final protected[this] implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    /**
     * The entry points of this analysis.
     */
    val entryPoints: Map[DeclaredMethod, IFDSFact]

    /**
     * The state of the analysis. For each method and source fact, there is a separate state.
     *
     * @param declaringClass The class defining the analyzed `method`.
     * @param method The analyzed method.
     * @param source A fact, that holds at the beginning of `method`.
     * @param code The code of `method`.
     * @param cfg The control glow graph of `method`.
     * @param pendingIfdsCallSites Maps callees of the analyzed `method` together with their input facts
     *                             to the basic block and statement index of the call site(s).
     * @param pendingIfdsDependees Maps callees of the analyzed `method` together with their input facts to the intermediate result of their IFDS analysis.
     *                             Only contains method-fact-pairs, for which this analysis is waiting for a result.
     * @param pendingCgCallSites The basic blocks containing call sites, for which the analysis is still waiting for the call graph result.
     * @param cgDependency If present, the analysis is waiting for the `method`'s call graph.
     * @param incomingFacts Maps each basic block to the data flow facts valid at its first statement.
     * @param outgoingFacts Maps each basic block and successor node to the data flow facts valid at the beginning of the node.
     */
    class State(
            val declaringClass:       ObjectType,
            val context:              Context,
            val source:               (Context, IFDSFact),
            val code:                 Array[Stmt[V]],
            val cfg:                  CFG[Stmt[V], TACStmts[V]],
            var pendingIfdsCallSites: Map[(Context, IFDSFact), Set[(BasicBlock, Int)]],
            var pendingIfdsDependees: Map[(Context, IFDSFact), EOptionP[(Context, IFDSFact), IFDSProperty[IFDSFact]]] = Map.empty,
            var pendingCgCallSites:   Set[BasicBlock]                                                                 = Set.empty,
            var cgDependency:         Option[SomeEOptionP]                                                            = None,
            var incomingFacts:        Map[BasicBlock, Set[IFDSFact]]                                                  = Map.empty,
            var outgoingFacts:        Map[BasicBlock, Map[CFGNode, Set[IFDSFact]]]                                    = Map.empty
    )

    implicit val typeProvider: TypeProvider = project.get(TypeProviderKey)

    /**
     * Performs an IFDS analysis for a method-fact-pair.
     *
     * @param entity The method-fact-pair that will be analyzed.
     * @return An IFDS property mapping from exit statements to the data flow facts valid after these exit statements.
     *         Returns an interim result, if the TAC or call graph of this method or the IFDS analysis for a callee is still pending.
     */
    def performAnalysis(entity: (Context, IFDSFact)): ProperPropertyComputationResult = {
        val (context, sourceFact) = entity

        val declaredMethod = context.method

        // The analysis can only handle single defined methods
        // If a method is not single defined, this analysis assumes that it does not create any facts.
        if (!declaredMethod.hasSingleDefinedMethod)
            return Result(entity, createPropertyValue(Map.empty));

        val method = declaredMethod.definedMethod
        val declaringClass: ObjectType = method.classFile.thisType

        // Fetch the method's three address code. If it is not present, return an empty interim result.
        val (code, cfg) = propertyStore(method, TACAI.key) match {
            case FinalP(TheTACAI(tac)) => (tac.stmts, tac.cfg)

            case epk: EPK[Method, TACAI] =>
                return InterimResult.forUB(
                    entity,
                    createPropertyValue(Map.empty),
                    Set(epk),
                    _ => performAnalysis(entity)
                );

            case tac =>
                throw new UnknownError(s"can't handle intermediate TACs ($tac)");
        }

        // Start processing at the start of the cfg with the given source fact
        implicit val state: State =
            new State(declaringClass, context, entity, code, cfg, Map(entity -> Set.empty))
        val start = cfg.startBlock
        state.incomingFacts += start -> Set(sourceFact)
        process(mutable.Queue((start, Set(sourceFact), None, None, None)))
        createResult()
    }

    /**
     * Analyzes a queue of BasicBlocks.
     *
     * @param worklist A queue of the following elements:
     *        bb The basic block that will be analyzed.
     *        in New data flow facts found to hold at the beginning of the basic block.
     *        calleeWithUpdateIndex If the basic block is analyzed because there is new information for a callee, this is the call site's index.
     *        calleeWithUpdate If the basic block is analyzed because there is new information for a callee, this is the callee.
     *        calleeWithUpdateFact If the basic block is analyzed because there is new information for a callee with a specific input fact,
     *                             this is the input fact.
     */
    def process(
        worklist: mutable.Queue[(BasicBlock, Set[IFDSFact], Option[Int], Option[Context], Option[IFDSFact])]
    )(
        implicit
        state: State
    ): Unit = {
        while (worklist.nonEmpty) {
            val (basicBlock, in, calleeWithUpdateIndex, calleeWithUpdate, calleeWithUpdateFact) =
                worklist.dequeue()
            val oldOut = state.outgoingFacts.getOrElse(basicBlock, Map.empty)
            val nextOut =
                analyzeBasicBlock(basicBlock, in, calleeWithUpdateIndex, calleeWithUpdate, calleeWithUpdateFact)
            val allOut = mergeMaps(oldOut, nextOut)
            state.outgoingFacts = state.outgoingFacts.updated(basicBlock, allOut)

            for (successor <- basicBlock.successors) {
                if (successor.isExitNode) {
                    // Re-analyze recursive call sites with the same input fact.
                    val nextOutSuccessors = nextOut.get(successor)
                    if (nextOutSuccessors.isDefined && nextOutSuccessors.get.nonEmpty) {
                        val oldOutSuccessors = oldOut.get(successor)
                        if (oldOutSuccessors.isEmpty ||
                            nextOutSuccessors.get.exists(nos => !oldOutSuccessors.get.contains(nos))) {
                            val source = state.source
                            reAnalyzeCalls(state.pendingIfdsCallSites(source), source._1, Some(source._2))
                        }
                    }
                    // if ((nextOut.getOrElse(successor, Set.empty) -- oldOut.getOrElse(successor, Set.empty)).nonEmpty) {
                    //    val source = state.source
                    //    reAnalyzeCalls(state.pendingIfdsCallSites(source), source._1.definedMethod, Some(source._2))
                    // }
                } else {
                    val actualSuccessor =
                        (if (successor.isBasicBlock) {
                            successor
                        } else {
                            // skip CatchNodes jump to their handler BasicBlock
                            successor.successors.head
                        }).asBasicBlock

                    val nextIn = nextOut.getOrElse(actualSuccessor, Set.empty)
                    val oldIn = state.incomingFacts.getOrElse(actualSuccessor, Set.empty)
                    val mergedIn = if (nextIn.size > oldIn.size) nextIn ++ oldIn else oldIn ++ nextIn
                    val newIn = nextIn -- oldIn
                    state.incomingFacts = state.incomingFacts.updated(actualSuccessor, mergedIn)
                    /*
                     * Only process the successor with new facts.
                     * It is analyzed at least one time because of the null fact.
                     */
                    if (newIn.nonEmpty) {
                        worklist.enqueue((actualSuccessor, newIn, None, None, None))
                    }
                }
            }
        }
    }

    /**
     * Collects the facts valid at an exit node based on the current results.
     *
     * @param exit The exit node.
     * @return A map, mapping from each predecessor of the `exit` node to the facts valid at the `exit` node
     *         under the assumption that the predecessor was executed before.
     */
    def collectResult(exit: CFGNode)(implicit state: State): Map[Statement, Set[IFDSFact]] = {
        var result = Map.empty[Statement, Set[IFDSFact]]
        exit.predecessors foreach { predecessor =>
            if (predecessor.isBasicBlock) {
                val basicBlock = predecessor.asBasicBlock
                // FIXME ... replace flatMap...isDefined by something that doesn't create intermediate data-structures
                if (state.outgoingFacts.get(basicBlock).flatMap(_.get(exit)).isDefined) {
                    val lastIndex = basicBlock.endPC
                    val stmt = Statement(state.context, basicBlock, state.code(lastIndex), lastIndex, state.code, state.cfg)
                    result += stmt -> state.outgoingFacts(basicBlock)(exit)
                }
            }
        }
        result
    }

    /**
     * Creates the current (intermediate) result for the analysis.
     *
     * @return A result containing a map, which maps each exit statement to the facts valid after the statement, based on the current results.
     *         If the analysis is still waiting for its method's TAC or call graph or the IFDS of another method, an interim result will be returned.
     *
     */
    def createResult()(implicit state: State): ProperPropertyComputationResult = {
        val propertyValue = createPropertyValue(mergeMaps(
            collectResult(state.cfg.normalReturnNode),
            collectResult(state.cfg.abnormalReturnNode)
        ))

        var dependees: Set[SomeEOptionP] = state.pendingIfdsDependees.valuesIterator.toSet
        // In the follwing, we really want to avoid useless copying of dependees:
        if (state.cgDependency.isDefined) {
            if (dependees.isEmpty) {
                dependees = Set(state.cgDependency.get)
            } else {
                // We only implement what is required by the propery store/interface
                new Iterable[SomeEOptionP] {
                    override def iterator: Iterator[SomeEOptionP] = {
                        // This method is actually not called by the property store...
                        Iterator.single(state.cgDependency.get) ++ dependees.iterator
                    }
                    override def foreach[U](f: SomeEOptionP => U): Unit = {
                        f(state.cgDependency.get)
                        dependees.foreach(f)
                    }
                    override def size: Int = dependees.size + 1
                    override def isEmpty = false
                }
            }
        }

        if (dependees.isEmpty) {
            Result(state.source, propertyValue)
        } else {
            InterimResult.forUB(state.source, propertyValue, dependees, propertyUpdate)
        }
    }

    /**
     * Called, when the call graph for this method or an IFDSProperty for another method was computed.
     * Re-analyzes the relevant parts of this method and returns the new analysis result.
     *
     * @param eps The new property value.
     * @return The new (interim) result of this analysis.
     */
    def propertyUpdate(eps: SomeEPS)(
        implicit
        state: State
    ): ProperPropertyComputationResult = {
        (eps: @unchecked) match {
            case FinalE(e: (Context, IFDSFact) @unchecked) => reAnalyzeCalls(state.pendingIfdsCallSites(e), e._1, Some(e._2))

            case interimEUBP @ InterimEUBP(e: (Context, IFDSFact) @unchecked, ub: IFDSProperty[IFDSFact @unchecked]) =>
                if (ub.flows.values.forall(_.isInstanceOf[AbstractIFDSNullFact])) {
                    // Do not re-analyze the caller if we only get the null fact.
                    // Update the pendingIfdsDependee entry to the new interim result.
                    state.pendingIfdsDependees += e -> interimEUBP.asInstanceOf[EOptionP[(Context, IFDSFact), IFDSProperty[IFDSFact]]]
                } else reAnalyzeCalls(state.pendingIfdsCallSites(e), e._1, Some(e._2))

            case FinalEP(_: DefinedMethod, _: Callees) =>
                reAnalyzebasicBlocks(state.pendingCgCallSites)

            case InterimEUBP(_: DefinedMethod, _: Callees) =>
                reAnalyzebasicBlocks(state.pendingCgCallSites)
        }

        createResult()
    }

    /**
     * Computes for one basic block the facts valid on each CFG edge leaving the block if `sources` hold before the block.
     *
     * @param basicBlock The basic block, that will be analyzed.
     * @param in The facts, that hold before the block.
     * @param calleeWithUpdateIndex If the basic block is analyzed because there is new information for a callee, this is the call site's index.
     * @param calleeWithUpdate If the basic block is analyzed because there is new information for a callee, this is the callee.
     * @param calleeWithUpdateFact If the basic block is analyzed because there is new information for a callee with a specific input fact,
     *                             this is the input fact.
     * @return A map, mapping each successor node to its input facts. Instead of catch nodes, this map contains their handler nodes.
     */
    def analyzeBasicBlock(
        basicBlock:            BasicBlock,
        in:                    Set[IFDSFact],
        calleeWithUpdateIndex: Option[Int],
        calleeWithUpdate:      Option[Context],
        calleeWithUpdateFact:  Option[IFDSFact]
    )(
        implicit
        state: State
    ): Map[CFGNode, Set[IFDSFact]] = {

        /*
         * Collects information about a statement.
         *
         * @param index The statement's index.
         * @return A tuple of the following elements:
         *         statement: The statement at `index`.
         *         calees: The methods possibly called at this statement, if it contains a call.
         *                 If `index` equals `calleeWithUpdateIndex`, only `calleeWithUpdate` will be returned.
         *         calleeFact: If `index` equals `calleeWithUpdateIndex`, only `calleeWithUpdateFact` will be returned, None otherwise.
         */
        def collectInformation(
            index: Int
        ): (Statement, Option[SomeSet[Context]], Option[IFDSFact]) = {
            val stmt = state.code(index)
            val statement = Statement(state.context, basicBlock, stmt, index, state.code, state.cfg)
            val calleesO =
                if (calleeWithUpdateIndex.contains(index)) calleeWithUpdate.map(Set(_)) else getCalleesIfCallStatement(basicBlock, index)
            val calleeFact = if (calleeWithUpdateIndex.contains(index)) calleeWithUpdateFact else None
            (statement, calleesO, calleeFact)
        }

        var flows: Set[IFDSFact] = in
        var index = basicBlock.startPC

        // Iterate over all statements but the last one, only keeping the resulting DataFlowFacts.
        while (index < basicBlock.endPC) {
            val (statement, calleesO, calleeFact) = collectInformation(index)
            flows = if (calleesO.isEmpty) {
                val successor =
                    Statement(state.context, basicBlock, state.code(index + 1), index + 1, state.code, state.cfg)
                normalFlow(statement, successor, flows)
            } else
                // Inside a basic block, we only have one successor --> Take the head
                handleCall(basicBlock, statement, calleesO.get, flows, calleeFact).values.head
            index += 1
        }

        // Analyze the last statement for each possible successor statement.
        val (statement, calleesO, callFact) = collectInformation(basicBlock.endPC)
        var result: Map[CFGNode, Set[IFDSFact]] =
            if (calleesO.isEmpty) {
                var result: Map[CFGNode, Set[IFDSFact]] = Map.empty
                for (node <- basicBlock.successors) {
                    result += node -> normalFlow(statement, firstStatement(node), flows)
                }
                result
            } else {
                handleCall(basicBlock, statement, calleesO.get, flows, callFact).map(entry => entry._1.node -> entry._2)
            }

        // Propagate the null fact.
        result = result.map(result => result._1 -> (propagateNullFact(in, result._2)))
        result
    }

    /**
     * Retrieves the expression of an assignment or expression statement.
     *
     * @param statement The statement. Must be an Assignment or ExprStmt.
     * @return The statement's expression.
     */
    def getExpression(statement: Stmt[V]): Expr[V] = statement.astID match {
        case Assignment.ASTID => statement.asAssignment.expr
        case ExprStmt.ASTID   => statement.asExprStmt.expr
        case _                => throw new UnknownError("Unexpected statement")
    }

    /**
     * Gets the set of all methods possibly called at some statement.
     *
     * @param basicBlock The basic block containing the statement.
     * @param index The statement's index.
     * @return All methods possibly called at the statement index or None, if the statement does not contain a call.
     */
    def getCalleesIfCallStatement(basicBlock: BasicBlock, index: Int)(implicit state: State): Option[SomeSet[Context]] = {
        val statement = state.code(index)
        val pc = statement.pc
        statement.astID match {
            case StaticMethodCall.ASTID | NonVirtualMethodCall.ASTID | VirtualMethodCall.ASTID => Some(getCallees(basicBlock, pc))
            case Assignment.ASTID | ExprStmt.ASTID => getExpression(statement).astID match {
                case StaticFunctionCall.ASTID | NonVirtualFunctionCall.ASTID | VirtualFunctionCall.ASTID => Some(getCallees(basicBlock, pc))
                case _ => None
            }
            case _ => None
        }
    }

    /**
     * Gets the set of all methods possibly called at some call statement.
     *
     * @param basicBlock The basic block containing the call.
     * @param pc The call's program counter.
     * @return All methods possibly called at the statement index.
     */
    def getCallees(
        basicBlock: BasicBlock, pc: Int
    )(implicit state: State): SomeSet[Context] = {
        val ep = propertyStore(state.context.method, Callees.key)
        ep match {
            case FinalEP(_, p) =>
                state.cgDependency = None
                state.pendingCgCallSites -= basicBlock
                p.callees(state.source._1, pc).toSet
            case InterimEUBP(_, p) =>
                addCgDependency(basicBlock, ep)
                p.callees(state.source._1, pc).toSet
            case _ =>
                addCgDependency(basicBlock, ep)
                Set.empty
        }
    }

    /**
     * Maps some declared methods to their defined methods.
     *
     * @param declaredMethods Some declared methods.
     * @return All defined methods of `declaredMethods`.
     */
    def definedMethods(declaredMethods: Iterator[Context]): SomeSet[Method] = {
        val result = scala.collection.mutable.Set.empty[Method]
        declaredMethods.map(_.method).filter { declaredMethod =>
            declaredMethod.hasSingleDefinedMethod || declaredMethod.hasMultipleDefinedMethods
        }.foreach { declaredMethod =>
            declaredMethod.foreachDefinedMethod(defineMethod => result.add(defineMethod))
        }
        result
    }

    /**
     * Sets the cgDependency to `ep` and adds the `basicBlock` to the pending cg call sites.
     *
     * @param basicBlock The basic block, which will be added to the pending cg call sites.
     * @param ep The result of the call graph analysis.
     */
    def addCgDependency(basicBlock: BasicBlock, ep: EOptionP[DeclaredMethod, Callees])(implicit state: State): Unit = {
        state.cgDependency = Some(ep)
        state.pendingCgCallSites += basicBlock
    }

    /**
     * Re-analyzes some basic blocks.
     *
     * @param basicBlocks The basic blocks, that will be re-analyzed.
     */
    def reAnalyzebasicBlocks(basicBlocks: Set[BasicBlock])(implicit state: State): Unit = {
        val queue: mutable.Queue[(BasicBlock, Set[IFDSFact], Option[Int], Option[Context], Option[IFDSFact])] = mutable.Queue.empty
        for (bb <- basicBlocks)
            queue.enqueue((bb, state.incomingFacts(bb), None, None, None))
        process(queue)
    }

    /**
     * Re-analyzes some call sites with respect to one specific callee.
     *
     * @param callSites The call sites, which are analyzed.
     * @param callee The callee, which will be considered at the `callSites`.
     * @param fact If defined, the `callee` will only be analyzed for this fact.
     */
    def reAnalyzeCalls(
        callSites:     Set[(BasicBlock, Int)],
        calleeContext: Context,
        fact:          Option[IFDSFact]
    )(implicit state: State): Unit = {
        val queue: mutable.Queue[(BasicBlock, Set[IFDSFact], Option[Int], Option[Context], Option[IFDSFact])] =
            mutable.Queue.empty
        for ((block, index) <- callSites)
            queue.enqueue(
                (
                    block,
                    state.incomingFacts(block),
                    Some(index),
                    Some(calleeContext),
                    fact
                )
            )
        process(queue)
    }

    /**
     * Processes a statement with a call.
     *
     * @param basicBlock The basic block that contains the statement
     * @param call The call statement.
     * @param callees All possible callees of the call.
     * @param in The facts valid before the call statement.
     * @param calleeWithUpdateFact If present, the `callees` will only be analyzed with this fact instead of the facts returned by callFlow.
     * @return A map, mapping from each successor statement of the `call` to the facts valid at their start.
     */
    def handleCall(
        basicBlock:           BasicBlock,
        call:                 Statement,
        callees:              SomeSet[Context],
        in:                   Set[IFDSFact],
        calleeWithUpdateFact: Option[IFDSFact]
    )(
        implicit
        state: State
    ): Map[Statement, Set[IFDSFact]] = {
        val successors = successorStatements(call, basicBlock)
        // Facts valid at the start of each successor
        var summaryEdges: Map[Statement, Set[IFDSFact]] = Map.empty

        // If calleeWithUpdateFact is present, this means that the basic block already has been analyzed with the `in` facts.
        if (calleeWithUpdateFact.isEmpty)
            for (successor <- successors) {
                summaryEdges += successor -> propagateNullFact(in, callToReturnFlow(call, successor, in))
            }

        for (callee <- callees) {
            if (callee.method.definedMethod.isNative) {
                // We cannot analyze native methods. Let the concrete analysis decide what to do.
                for {
                    successor <- successors
                } {
                    summaryEdges += successor -> (summaryEdges(successor) ++ nativeCall(call, callee, successor, in))
                }
            } else {
                val callToStart =
                    if (calleeWithUpdateFact.isDefined) calleeWithUpdateFact.toSet
                    else propagateNullFact(in, callFlow(call, callee, in))
                var allNewExitFacts: Map[Statement, Set[IFDSFact]] = Map.empty
                // Collect exit facts for each input fact separately
                for (fact <- callToStart) {
                    /*
                    * If this is a recursive call with the same input facts, we assume that the call only produces the facts that are already known.
                    * The call site is added to `pendingIfdsCallSites`, so that it will be re-evaluated if new output facts become known for the input fact.
                    */
                    if ((callee eq state.context) && fact == state.source._2) {
                        // FIXME Get rid of "getOrElse(...,Set.empty)" due to its potentially very BAD performance
                        val newDependee =
                            state.pendingIfdsCallSites.getOrElse(state.source, Set.empty) + ((basicBlock, call.index))
                        state.pendingIfdsCallSites = state.pendingIfdsCallSites.updated(state.source, newDependee)
                        allNewExitFacts = mergeMaps(
                            allNewExitFacts,
                            mergeMaps(
                                collectResult(state.cfg.normalReturnNode),
                                collectResult(state.cfg.abnormalReturnNode)
                            )
                        )
                    } else {
                        val e = (callee, fact)
                        val callFlows = propertyStore(e, propertyKey.key)
                            .asInstanceOf[EOptionP[(Context, IFDSFact), IFDSProperty[IFDSFact]]]
                        val oldValue = state.pendingIfdsDependees.get(e)
                        val oldExitFacts: Map[Statement, Set[IFDSFact]] = oldValue match {
                            case Some(ep: InterimEUBP[_, IFDSProperty[IFDSFact]]) => ep.ub.flows
                            case _                                                => Map.empty
                        }
                        val exitFacts: Map[Statement, Set[IFDSFact]] = callFlows match {
                            case ep: FinalEP[_, IFDSProperty[IFDSFact]] =>
                                // FIXME Get rid of "getOrElse(...,Set.empty)" due to its potentially very BAD performance
                                val newDependee =
                                    state.pendingIfdsCallSites.getOrElse(e, Set.empty) - ((basicBlock, call.index))
                                state.pendingIfdsCallSites = state.pendingIfdsCallSites.updated(e, newDependee)
                                state.pendingIfdsDependees -= e
                                ep.p.flows
                            case ep: InterimEUBP[_, IFDSProperty[IFDSFact]] =>
                                /*
                              * Add the call site to `pendingIfdsCallSites` and `pendingIfdsDependees` and
                              * continue with the facts in the interim result for now. When the analysis for the
                              * callee finishes, the analysis for this call site will be triggered again.
                              */
                                addIfdsDependee(e, callFlows, basicBlock, call.index)
                                ep.ub.flows
                            case _ =>
                                addIfdsDependee(e, callFlows, basicBlock, call.index)
                                Map.empty
                        }
                        // Only process new facts that are not in `oldExitFacts`
                        allNewExitFacts = mergeMaps(allNewExitFacts, mapDifference(exitFacts, oldExitFacts))
                        /*
                     * If new exit facts were discovered for the callee-fact-pair, all call sites depending on this pair have to be re-evaluated.
                     * oldValue is undefined if the callee-fact pair has not been queried before or returned a FinalEP.
                     */
                        if (oldValue.isDefined && oldExitFacts != exitFacts) {
                            reAnalyzeCalls(state.pendingIfdsCallSites(e), e._1, Some(e._2))
                        }
                    }
                }

                //Create exit to return facts. At first for normal returns, then for abnormal returns.
                for {
                    successor <- successors
                    if successor.node.isBasicBlock || successor.node.isNormalReturnExitNode
                    exitStatement <- allNewExitFacts.keys
                    if exitStatement.stmt.astID == Return.ASTID || exitStatement.stmt.astID == ReturnValue.ASTID
                } {
                    // FIXME Get rid of "getOrElse(...,Set.empty)" due to its potentially very BAD performance
                    summaryEdges += successor -> (summaryEdges.getOrElse(successor, Set.empty[IFDSFact]) ++
                        returnFlow(call, callee, exitStatement, successor, allNewExitFacts.getOrElse(exitStatement, Set.empty)))
                }
                for {
                    successor <- successors
                    if successor.node.isCatchNode || successor.node.isAbnormalReturnExitNode
                    exitStatement <- allNewExitFacts.keys
                    if exitStatement.stmt.astID != Return.ASTID && exitStatement.stmt.astID != ReturnValue.ASTID
                } {
                    // FIXME Get rid of "getOrElse(...,Set.empty)" due to its potentially very BAD performance
                    summaryEdges += successor -> (summaryEdges.getOrElse(successor, Set.empty[IFDSFact]) ++
                        returnFlow(call, callee, exitStatement, successor, allNewExitFacts.getOrElse(exitStatement, Set.empty)))
                }
            }
        }
        summaryEdges
    }

    /**
     * Determines the successor statements for one source statement.
     *
     * @param statement The source statement.
     * @param basicBlock The basic block containing the source statement.
     * @return All successors of `statement`.
     */
    def successorStatements(
        statement:  Statement,
        basicBlock: BasicBlock
    )(
        implicit
        state: State
    ): Set[Statement] = {
        val index = statement.index
        if (index == basicBlock.endPC) {
            for (successorBlock <- basicBlock.successors) yield firstStatement(successorBlock)
        } else {
            val nextIndex = index + 1
            Set(Statement(statement.context, basicBlock, statement.code(nextIndex), nextIndex, statement.code, statement.cfg))
        }
    }

    /**
     * Adds a method-fact-pair as to the IFDS call sites and dependees.
     *
     * @param entity The method-fact-pair.
     * @param calleeProperty The property, that was returned for `entity`.
     * @param callBB The basic block of the call site.
     * @param callIndex The index of the call site.
     */
    def addIfdsDependee(
        entity:         (Context, IFDSFact),
        calleeProperty: EOptionP[(Context, IFDSFact), IFDSProperty[IFDSFact]],
        callBB:         BasicBlock,
        callIndex:      Int
    )(
        implicit
        state: State
    ): Unit = {
        val callSites = state.pendingIfdsCallSites
        state.pendingIfdsCallSites = callSites.updated(entity, callSites.getOrElse(entity, Set.empty) + ((callBB, callIndex)))
        state.pendingIfdsDependees += entity -> calleeProperty
    }

    /**
     * If `from` contains a null fact, it will be added to `to`.
     *
     * @param from The set, which may contain the null fact initially.
     * @param to The set, to which the null fact may be added.
     * @return `to` with the null fact added, if it is contained in `from`.
     */
    def propagateNullFact(from: Set[IFDSFact], to: Set[IFDSFact]): Set[IFDSFact] = {
        val nullFact = from.find(_.isInstanceOf[AbstractIFDSNullFact])
        if (nullFact.isDefined) to + nullFact.get
        else to
    }

    /**
     * Merges two maps that have sets as values.
     *
     * @param map1 The first map.
     * @param map2 The second map.
     * @return A map containing the keys of both maps. Each key is mapped to the union of both maps' values.
     */
    def mergeMaps[S, T](map1: Map[S, Set[T]], map2: Map[S, Set[T]]): Map[S, Set[T]] = {
        var result = map1
        for ((key, values) <- map2) {
            result.get(key) match {
                case Some(resultValues) =>
                    if (resultValues.size > values.size)
                        result = result.updated(key, resultValues ++ values)
                    else
                        result = result.updated(key, values ++ resultValues)
                case None =>
                    result = result.updated(key, values)
            }
        }
        result
    }

    /**
     * Computes the difference of two maps that have sets as their values.
     *
     * @param minuend The map, from which elements will be removed.
     * @param subtrahend The map, whose elements will be removed from `minuend`.
     * @return A map, containing the keys and values of `minuend`.
     *         The values of the result only contain those elements not present in `subtrahend` for the same key.
     */
    def mapDifference[S, T](minuend: Map[S, Set[T]], subtrahend: Map[S, Set[T]]): Map[S, Set[T]] = {
        var result = minuend
        for ((key, values) <- subtrahend) {
            result = result.updated(key, result(key) -- values)
        }
        result
    }

    /**
     * Gets the call for a statement that contains a call.
     *
     * @param statement The statement.
     * @return The call contained in `statement`.
     */
    protected[this] def asCall(statement: Stmt[V]): Call[V] = statement.astID match {
        case Assignment.ASTID => statement.asAssignment.expr.asFunctionCall
        case ExprStmt.ASTID   => statement.asExprStmt.expr.asFunctionCall
        case _                => statement.asMethodCall
    }

    /**
     * Gets the first statement of a cfg node.
     *
     * @param node The node, for which the first statement will be retrieved.
     * @return If the `node` is a basic block, its first statement will be returned.
     *         If it is a catch node, the first statement of its handler will be returned.
     *         If it is an exit node an artificial statement without code will be returned.
     */
    @tailrec
    private def firstStatement(node: CFGNode)(implicit state: State): Statement = {
        if (node.isBasicBlock) {
            val index = node.asBasicBlock.startPC
            Statement(state.context, node, state.code(index), index, state.code, state.cfg)
        } else if (node.isCatchNode) {
            firstStatement(node.successors.head)
        } else if (node.isExitNode) {
            Statement(state.context, node, null, 0, state.code, state.cfg)
        } else throw new IllegalArgumentException(s"Unknown node type: $node")
    }
}

/**
 * A statement that is passed to the concrete analysis.
 *
 * @param context The method containing the statement.
 * @param node The basic block containing the statement.
 * @param stmt The TAC statement.
 * @param index The index of the Statement in the code.
 * @param code The method's TAC code.
 * @param cfg The method's CFG.
 */
case class Statement(
        context: Context,
        node:    CFGNode,
        stmt:    Stmt[V],
        index:   Int,
        code:    Array[Stmt[V]],
        cfg:     CFG[Stmt[V], TACStmts[V]]
) {

    override def hashCode(): Int = context.hashCode() * 31 + index

    override def equals(o: Any): Boolean = {
        o match {
            case s: Statement => s.index == index && s.context == context
            case _            => false
        }
    }

    override def toString: String = s"${context.method.definedMethod.toJava}"

}

object AbstractIFDSAnalysis {

    /**
     * The type of the TAC domain.
     */
    type V = DUVar[ValueInformation]
}

abstract class IFDSAnalysis[IFDSFact <: AbstractIFDSFact] extends FPCFLazyAnalysisScheduler {

    final override type InitializationData = AbstractIFDSAnalysis[IFDSFact]

    def property: IFDSPropertyMetaInformation[IFDSFact]

    final override def derivesLazily: Some[PropertyBounds] = Some(PropertyBounds.ub(property))

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, TypeProviderKey)

    override val uses: Set[PropertyBounds] = Set(PropertyBounds.finalP(TACAI))

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    final override def register(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: AbstractIFDSAnalysis[IFDSFact]
    ): FPCFAnalysis = {
        ps.registerLazyPropertyComputation(property.key, analysis.performAnalysis)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {
        val ifdsAnalysis = analysis.asInstanceOf[AbstractIFDSAnalysis[IFDSFact]]
        for (e <- ifdsAnalysis.entryPoints) { ps.force(e, ifdsAnalysis.propertyKey.key) }
    }

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}

}

