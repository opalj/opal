/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import scala.annotation.switch
import scala.annotation.tailrec

import java.util.concurrent.ConcurrentHashMap

import scala.collection.{Set ⇒ SomeSet}
import scala.collection.mutable

import org.opalj.fpcf.CheapPropertyComputation
import org.opalj.fpcf.DefaultPropertyComputation
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalE
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEUB
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.value.ValueInformation
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFG
import org.opalj.br.cfg.CFGNode
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.tac.fpcf.analyses.AbstractIFDSAnalysis.V
import org.opalj.tac.fpcf.properties.IFDSProperty
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI

/**
 * A framework for IFDS analyses.
 *
 * @tparam DataFlowFact The type of flow facts the concrete analysis wants to track
 * @author Dominik Helm
 */
abstract class AbstractIFDSAnalysis[DataFlowFact] extends FPCFAnalysis {

    /**
     * Provides the concrete property key that must be unique for every distinct concrete analysis
     * and the lower bound for the IFDSProperty.
     */
    val propertyKey: IFDSPropertyMetaInformation[DataFlowFact]

    /**
     * Creates an IFDSProperty containing the `result` of the analysis.
     * The result maps from return nodes to the data flow facts valid after these return nodes.
     */
    def createPropertyValue(result: Map[Statement, Set[DataFlowFact]]): IFDSProperty[DataFlowFact]

    /**
     * Computes the DataFlowFacts valid after statement `statement` on the CFG edge to statement `succ`
     * if the DataFlowFacts `in` held before `statement`.
     */
    def normalFlow(statement: Statement, succ: Statement, in: Set[DataFlowFact]): Set[DataFlowFact]

    /**
     * Computes the DataFlowFacts valid on entry to method `callee` when it is called from statement
     * `statement` if the DataFlowFacts `in` held before `statement`.
      * This method must not handle propagating the null fact.
     */
    def callFlow(
        statement: Statement, callee: DeclaredMethod, in: Set[DataFlowFact]
    ): Set[DataFlowFact]

    /**
     * Computes the DataFlowFacts valid on the CFG edge from statement `statement` to `succ` if `callee`
     * was invoked by `statement` and DataFlowFacts `in` held before the final statement `exit` of
     * `callee`.
     */
    def returnFlow(
        statement: Statement,
        callee:    DeclaredMethod,
        exit:      Statement,
        succ:      Statement,
        in:        Set[DataFlowFact]
    ): Set[DataFlowFact]

    /**
     * Computes the DataFlowFacts valid on the CFG edge from statement `statement` to `succ` irrespective
     * of the call in `statement` if the DataFlowFacts `in` held before `statement`.
     */
    def callToReturnFlow(
        statement: Statement, succ: Statement, in: Set[DataFlowFact]
    ): Set[DataFlowFact]

    /**
     * All declared methods in the project.
     */
    final protected[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    //TODO Where do we put values into this map?
    val entryPoints: Map[DeclaredMethod, DataFlowFact]

    /**
     * Remembers the results of getExits.
     */
    val exits: ConcurrentHashMap[Method, Set[Statement]] = new ConcurrentHashMap

    /**
     * The state of the analysis. For each method and source fact, there is a separate state.
     *
     * @param declaringClass The class defining the method that is analyzed.
     * @param method The method that is analyzed.
     * @param source The source fact that holds at the beginning of `method`.
     * @param code The code of `method`.
     * @param cfg The Control Flow Graph of `method`.
     * @param pendingIfdsCallSites Maps methods called by the analyzed `method` together with their input facts
     *                             to the basic block and statement index of the call site(s) in the analyzed `method`.
     * @param pendingIfdsDependees Maps methods called by the analyzed `method` together with their input facts
     *                             to the intermediate result of their IFDS analysis.
     *                             Only contains method-fact-pairs, for which this analysis is waiting for a result.
     * @param pendingTacDependees Maps methods called by the analyzed `method` to
     *                            the intermediate result of their three address code analysis.#
     *                            Only contains methods, for which this analysis is waiting for a result.
     * @param pendingTacCallSites Maps methods called by the analyzed `method`
     *                            to the basic block and statement index of the call site(s) of the analyzed method.
     * @param incomingFacts Maps each basic block to the data flow facts valid at its first statement.
     * @param outgoingFacts Maps each basic block to the data flow facts valid at the edges to its successors.
     */
    class State(
            val declaringClass:       ObjectType,
            val method:               Method,
            val source:               (DeclaredMethod, DataFlowFact),
            val code:                 Array[Stmt[V]],
            val cfg:                  CFG[Stmt[V], TACStmts[V]],
            var pendingIfdsCallSites: Map[(DeclaredMethod, DataFlowFact), Set[(BasicBlock, Int)]],
            var pendingIfdsDependees: Map[(DeclaredMethod, DataFlowFact), EOptionP[(DeclaredMethod, DataFlowFact), IFDSProperty[DataFlowFact]]] = Map.empty,
            var pendingTacDependees:  Map[Method, EOptionP[Method, TACAI]]                                                                      = Map.empty,
            var pendingTacCallSites:  Map[Method, Set[(BasicBlock, Int)]]                                                                       = Map.empty,
            var incomingFacts:        Map[BasicBlock, Set[DataFlowFact]]                                                                        = Map.empty,
            var outgoingFacts:        Map[BasicBlock, Map[CFGNode, Set[DataFlowFact]]]                                                          = Map.empty
    )

    /**
     * Performs an IFDS analysis for a method-fact-pair.
     *
     * @param entity The method-fact-pair that will be analyzed.
     * @return An IFDS property mapping from return nodes to the data flow facts valid after these return nodes,
     *         if all dependent TAC and IFDS properties are present.
     *         An interim result if the TAC of this method
     *         or any callee is missing or if a callee has to be analyzed with some input fact.
     */
    def performAnalysis(entity: (DeclaredMethod, DataFlowFact)): ProperPropertyComputationResult = {
        val (declaredMethod, sourceFact) = entity

        // The analysis can only handle single defined methods
        // If a method is not single defined, this analysis assumes that it does not create any facts.
        if (!declaredMethod.hasSingleDefinedMethod)
            return Result(entity, createPropertyValue(Map.empty))

        val method = declaredMethod.definedMethod
        val declaringClass: ObjectType = method.classFile.thisType

        // If this is not the method's declaration, but a non-overwritten method in a subtype, do not re-analyze the code.
        if (declaringClass ne declaredMethod.declaringClassType)
            return baseMethodResult(entity)

        // Fetch the method's three address code. If it is not present, return an empty interim result.
        val (code, cfg) = propertyStore(method, TACAI.key) match {
            case FinalP(TheTACAI(tac)) ⇒ (tac.stmts, tac.cfg)

            case epk: EPK[Method, TACAI] ⇒
                return InterimResult.forUB(
                    entity,
                    createPropertyValue(Map.empty),
                    Seq(epk),
                    _ ⇒ performAnalysis(entity),
                    DefaultPropertyComputation
                );

            case tac ⇒
                throw new UnknownError(s"can't handle intermediate TACs ($tac)")
        }

        // Start processing at the start of the cfg with the given source fact
        implicit val state: State =
            new State(declaringClass, method, entity, code, cfg, Map(entity → Set.empty))
        val start = cfg.startBlock
        state.incomingFacts += start → Set(sourceFact)
        process(mutable.Queue((start, Set(sourceFact), None, None, None)))
        createResult()
    }

    /**
     * Processes a queue of BasicBlocks where new DataFlowFacts are available.
     *
     * @param worklist A queue of the following elements:
     *        bb The basic block that will be processed.
     *        in New data flow facts found for the basic block.
     *        calleeWithUpdateIndex The index of a call site, for which some property was computed.
     *        calleeWithUpdate The method called at `calleeWithUpdateIndex`, for which some property was computed.
     *                         At `callIndexWithUpdate`, only this method will be analyzed.
     *                         Present, iff callIndexWithUpdate is present.
     *        calleeWithUpdateFact If this parameter is present, `calleeWithUpdate` will only be analyzed with this fact
     *                             instead of the facts returned by `callFlow`.
     *                             Can only be present if `calleeWithUpdate` is present.
     *
     * TODO calleeWithUpdate always has one element
     * TODO Make a tuple of the last three queue elements
     */
    def process(
        worklist: mutable.Queue[(BasicBlock, Set[DataFlowFact], Option[Int], Option[Set[Method]], Option[DataFlowFact])]
    )(implicit state: State): Unit = {
        while (worklist.nonEmpty) {
            val (bb, in, calleeWithUpdateIndex, calleeWithUpdate, calleeWithUpdateFact) =
                worklist.dequeue()
            val oldOut = state.outgoingFacts.getOrElse(bb, Map.empty)
            val nextOut =
                analyzeBasicBlock(bb, in, calleeWithUpdateIndex, calleeWithUpdate, calleeWithUpdateFact)
            val allOut = mergeMaps(oldOut, nextOut)
            state.outgoingFacts = state.outgoingFacts.updated(bb, allOut)

            for (successor ← bb.successors) {
                if (successor.isExitNode) {
                    // Handle recursive calls
                    if ((nextOut.getOrElse(successor, Set.empty) -- oldOut.getOrElse(successor, Set.empty)).nonEmpty)
                        //TODO We re-analyze every bb with a call site. Wouldn't the bbs with recursive calls be sufficient?
                        handleCallUpdate(state.source)
                } else {
                    val succ =
                        (if (successor.isBasicBlock) {
                            successor
                        } else {
                            // Skip CatchNodes directly to their handler BasicBlock
                            assert(successor.isCatchNode)
                            assert(successor.successors.size == 1)
                            //TODO Is this correct?
                            successor.successors.head
                        }).asBasicBlock

                    val nextIn = nextOut.getOrElse(successor, Set.empty)
                    val oldIn = state.incomingFacts.getOrElse(succ, Set.empty)
                    val newIn = nextIn -- oldIn
                    state.incomingFacts = state.incomingFacts.updated(succ, oldIn ++ nextIn)
                    // Only process the successors with new facts
                    //TODO What happens, if we have no incoming facts for the successor, but the successor would create new facts?
                    //TODO In this case, the new facts would never be found!
                    if (newIn.nonEmpty) {
                        worklist.enqueue((succ, newIn, None, None, None))
                    }
                }
            }
        }
    }

    /**
     * Merges two maps that have sets as values. The resulting map has the keys from both maps with
     * the associated values being the union of the values from both input maps.
     */
    def mergeMaps[S, T](map1: Map[S, Set[T]], map2: Map[S, Set[T]]): Map[S, Set[T]] = {
        var result = map1
        for ((key, values) ← map2) {
            result = result.updated(key, result.getOrElse(key, Set.empty) ++ values)
        }
        result
    }

    /**
     * Gets, for an ExitNode of the CFG, the DataFlowFacts valid on each CFG edge from a
     * statement to that ExitNode.
     * TODO Why don't we have those facts on the exit node itself?
     */
    def collectResult(node: CFGNode)(implicit state: State): Map[Statement, Set[DataFlowFact]] =
        node.predecessors.collect {
            case bb: BasicBlock if state.outgoingFacts.contains(bb) && state.outgoingFacts(bb).contains(node) ⇒
                val index = bb.endPC
                Statement(state.method, state.code(index), index, state.code, state.cfg) → state
                    .outgoingFacts(bb)(node)
        }.toMap

    /**
     * Creates the analysis result from the current state.
     * If the analysis is waiting for the TAC or IFDS property of another method, an interim result will be returned.
     */
    def createResult()(implicit state: State): ProperPropertyComputationResult = {
        //TODO directly call createPropertyValue
        val result = mergeMaps(
            collectResult(state.cfg.normalReturnNode),
            collectResult(state.cfg.abnormalReturnNode)
        )

        val dependees = state.pendingIfdsDependees.values ++ state.pendingTacDependees.values

        if (dependees.isEmpty) {
            Result(state.source, createPropertyValue(result))
        } else {
            InterimResult.forUB(
                state.source,
                createPropertyValue(result),
                dependees,
                propertyHasBeenComputed,
                DefaultPropertyComputation
            )
        }
    }

    /**
     * Called, when some property, this analysis depends on, has been computed.
     * If there is a new (interim) result for an IFDS property
     * If there is a final TAC result, the method will be removed from `pendingTacCallSites` and `pendingTacDependees`.
     *
     * @param eps The computed property store value.
     * @return The new (interim) result of the analysis.
     */
    def propertyHasBeenComputed(eps: SomeEPS)(
        implicit
        state: State
    ): ProperPropertyComputationResult = {
        (eps: @unchecked) match {
            case FinalE(e: (DeclaredMethod, DataFlowFact) @unchecked)     ⇒ handleCallUpdate(e)

            case InterimEUB(e: (DeclaredMethod, DataFlowFact) @unchecked) ⇒ handleCallUpdate(e)

            //TODO Do we need a FinalEP or is a FinalE sufficient?
            case FinalEP(m: Method, _: TACAI) ⇒
                handleCallUpdate(m)
                state.pendingTacCallSites -= m
                state.pendingTacDependees -= m

            case InterimUBP(_: TACAI) ⇒ throw new UnknownError("Can not handle intermediate TAC")
        }

        createResult()
    }

    /**
     * Computes for one BasicBlock the DataFlowFacts valid on each CFG edge leaving the BasicBlock if the DataFlowFacts
     * `sources` held on entry to the BasicBlock.
     *
     * @param bb The basic block, that will be analyzed.
     * @param sources The source facts, that hold at the beginning of the basic block.
     * @param calleeWithUpdateIndex The index of a call site, for which some property was computed.
     * @param calleeWithUpdate The method called at `calleeWithUpdateIndex`, for which some property was computed.
     *               At `callIndexWithUpdate`, only this method will be analyzed.
     *               Present, iff callIndexWithUpdate is present.
     * @param calleeWithUpdateFact If this parameter is present, `calleeWithUpdate` will only be analyzed with this fact
     *                             instead of the facts returned by `callFlow`.
     *                             Can only be present if `calleeWithUpdate` is present.
     */
    def analyzeBasicBlock(
        bb:                    BasicBlock,
        sources:               Set[DataFlowFact],
        calleeWithUpdateIndex: Option[Int], //TODO IntOption
        calleeWithUpdate:      Option[Set[Method]],
        calleeWithUpdateFact:  Option[DataFlowFact]
    )(
        implicit
        state: State
    ): Map[CFGNode, Set[DataFlowFact]] = {

        /**
         * Collects information about a TAC statement
         *
         * @param index The index of the TAC statement
         * @return A tuple of the following elements:
         *         statement: The statement object at `index`
         *         calees: The methods possibly called at this statement, if it contains a call.
         *                 If `index` equals `calleeWithUpdateIndex`, only `calleeWithUpdate` will be returned.
         *         calleeFact: If `index` equals `calleeWithUpdateIndex`, only `calleeWithUpdateFact` will be returned.
         */
        def collectInformation(
            index: Int
        ): (Statement, Option[SomeSet[Method]], Option[DataFlowFact]) = {
            val stmt = state.code(index)
            val statement = Statement(state.method, stmt, index, state.code, state.cfg)
            val calleesO =
                if (calleeWithUpdateIndex.contains(index)) calleeWithUpdate else getCallees(stmt)
            val calleeFact = if (calleeWithUpdateIndex.contains(index)) calleeWithUpdateFact else None
            (statement, calleesO, calleeFact)
        }

        var flows: Set[DataFlowFact] = sources
        var index = bb.startPC

        // Iterate over all statements but the last one, only keeping the resulting DataFlowFacts.
        while (index < bb.endPC) {
            val (statement, calleesO, calleeFact) = collectInformation(index)
            flows = if (calleesO.isEmpty) {
                val successor =
                    Statement(state.method, state.code(index + 1), index + 1, state.code, state.cfg)
                normalFlow(statement, successor, flows)
            } else
                // Inside a basic block, we only have one successor --> Take the head
                handleCall(bb, statement, calleesO.get, flows, calleeFact).values.head
            index += 1
        }

        // Analyze the last statement for each possible successor statement.
        val (statement, calleesO, callFact) = collectInformation(bb.endPC)
        var result =
            if (calleesO.isEmpty) {
                var result: Map[CFGNode, Set[DataFlowFact]] = Map.empty
                for (node ← bb.successors) {
                    result += node → normalFlow(statement, firstStatement(node), flows)
                }
                result
            } else {
                handleCall(bb, statement, calleesO.get, flows, callFact)
            }

        // Propagate the null fact.
        // TODO At which point do we add the null fact initially?
        if (sources.contains(null.asInstanceOf[DataFlowFact]))
            result = result.map { result ⇒
                result._1 → (result._2 + null.asInstanceOf[DataFlowFact])
            }
        result
    }

    /**
     * Retrieves the expression of an assignment or expression statement.
     *
     * @param stmt The statement. Must be an Assignment or ExprStmt.
     * @return The statement's expression
     */
    def expr(stmt: Stmt[V]): Expr[V] = stmt.astID match {
        case Assignment.ASTID ⇒ stmt.asAssignment.expr
        case ExprStmt.ASTID   ⇒ stmt.asExprStmt.expr
        case _                ⇒ throw new UnknownError("Unexpected statement")
    }

    /**
     * Gets the set of all methods possibly called at some statement.
     *
     * @param stmt The statement
     * @return A set of methods possibly called by `stmt` or None, if `stmt` does not contain a method call.
     */
    def getCallees(stmt: Stmt[V])(implicit state: State): Option[SomeSet[Method]] = {
        (stmt.astID: @switch) match {

            case StaticMethodCall.ASTID ⇒
                Some(stmt.asStaticMethodCall.resolveCallTarget.toSet.filter(_.body.isDefined))

            case NonVirtualMethodCall.ASTID ⇒
                Some(
                    stmt.asNonVirtualMethodCall
                        .resolveCallTarget(state.declaringClass)
                        .toSet
                        .filter(_.body.isDefined)
                )

            case VirtualMethodCall.ASTID ⇒
                Some(
                    stmt.asVirtualMethodCall.resolveCallTargets(state.declaringClass).filter(_.body.isDefined)
                )

            case Assignment.ASTID ⇒
                expr(stmt).astID match {

                    case StaticFunctionCall.ASTID ⇒
                        Some(
                            stmt.asAssignment.expr.asStaticFunctionCall.resolveCallTarget.toSet
                                .filter(_.body.isDefined)
                        )

                    case NonVirtualFunctionCall.ASTID ⇒
                        Some(
                            stmt.asAssignment.expr.asNonVirtualFunctionCall
                                .resolveCallTarget(state.declaringClass)
                                .toSet
                                .filter(_.body.isDefined)
                        )

                    case VirtualFunctionCall.ASTID ⇒
                        Some(
                            stmt.asAssignment.expr.asVirtualFunctionCall
                                .resolveCallTargets(state.declaringClass)
                                .filter(_.body.isDefined)
                        )

                    case _ ⇒ None
                }

            case ExprStmt.ASTID ⇒
                expr(stmt).astID match {

                    case StaticFunctionCall.ASTID ⇒
                        Some(
                            stmt.asExprStmt.expr.asStaticFunctionCall.resolveCallTarget.toSet
                                .filter(_.body.isDefined)
                        )

                    case NonVirtualFunctionCall.ASTID ⇒
                        Some(
                            stmt.asExprStmt.expr.asNonVirtualFunctionCall
                                .resolveCallTarget(state.declaringClass)
                                .toSet
                                .filter(_.body.isDefined)
                        )

                    case VirtualFunctionCall.ASTID ⇒
                        Some(
                            stmt.asExprStmt.expr.asVirtualFunctionCall
                                .resolveCallTargets(state.declaringClass)
                                .filter(_.body.isDefined)
                        )
                    case _ ⇒ None
                }

            case _ ⇒ None
        }
    }

    /**
     * Re-analyzes all basic blocks contained in pendingTacCallSites for some method.
     *
     * @param method The method, for which all pending TAC call sites will be re-analyzed.
     *
     * TODO One handleCallUpdate should be sufficient
     */
    def handleCallUpdate(method: Method)(implicit state: State): Unit = {
        val blocks = state.pendingTacCallSites(method)
        val queue: mutable.Queue[(BasicBlock, Set[DataFlowFact], Option[Int], Option[Set[Method]], Option[DataFlowFact])] =
            mutable.Queue.empty
        for ((block, callSite) ← blocks)
            queue.enqueue((block, state.incomingFacts(block), Some(callSite), Some(Set(method)), None))
        process(queue)
    }

    /**
     * Re-analyzes all basic blocks contained in pendingIfdsCallSites for some method-fact-pair.
     *
     * @param e The method-fact-pair, for which all pending IFDS call sites will be re-analyzed.
     *
     * TODO One handleCallUpdate should be sufficient
     */
    def handleCallUpdate(e: (DeclaredMethod, DataFlowFact))(implicit state: State): Unit = {
        val blocks = state.pendingIfdsCallSites(e)
        val queue: mutable.Queue[(BasicBlock, Set[DataFlowFact], Option[Int], Option[Set[Method]], Option[DataFlowFact])] =
            mutable.Queue.empty
        for ((block, callSite) ← blocks)
            queue.enqueue(
                (
                    block,
                    state.incomingFacts(block),
                    Some(callSite),
                    Some(Set(e._1.definedMethod)),
                    Some(e._2)
                )
            )
        process(queue)
    }

    /**
     * Processes a statement with a call.
     *
     * @param callBB The block that contains the statement
     * @param call The statement with the call
     * @param callees The methods possibly invoked by the call.
     * @param in The DataFlowFacts valid before the call statement.
     * @param calleeWithUpdateFact If present, the callees will only be analyzed with this fact instead of the facts returned by callFlow.
     * @return A map, mapping from each return node to the exit-to-return-facts.
     */
    def handleCall(
        callBB:               BasicBlock,
        call:                 Statement,
        callees:              SomeSet[Method],
        in:                   Set[DataFlowFact],
        calleeWithUpdateFact: Option[DataFlowFact]
    )(implicit state: State): Map[CFGNode, Set[DataFlowFact]] = {
        // DataFlowFacts valid on the CFG edge to each successor after the call
        var summaryEdges: Map[CFGNode, Set[DataFlowFact]] = Map.empty

        // If calleeWithUpdateFact is not present, this means that the basic block already has been analyzed with the `in` facts.
        // TODO Is this correct? Can it happen that there is a new input fact AND an update for a method at the same time?
        // TODO Is it always the case that a call is at the end of a basic block?
        if (calleeWithUpdateFact.isEmpty)
            for (successor ← callBB.successors) {
                summaryEdges += successor → callToReturnFlow(call, firstStatement(successor), in)
            }

        for (calledMethod ← callees) {
            val callee = declaredMethods(calledMethod)
            val callToStart =
                if (calleeWithUpdateFact.isDefined) calleeWithUpdateFact.toSet
                else {
                    var cf = callFlow(call, callee, in)
                    if(in.contains(null.asInstanceOf[DataFlowFact])) cf += null.asInstanceOf[DataFlowFact]
                    cf
                }
            var allNewExitFacts: Map[Statement, Set[DataFlowFact]] = Map.empty
            // Collect exit facts for each start fact separately
            for (fact ← callToStart) {
                /*
                * If this is a recursive call with the same input facts, we assume that the call only produces the facts
                * that are already known. The call site is added to `pendingIfdsCallSites`, so that it will be
                * re-evaluated if new output facts become known for the input fact.
                */
                if ((calledMethod eq state.method) && fact == state.source._2) {
                    val newDependee =
                        state.pendingIfdsCallSites.getOrElse(state.source, Set.empty) + ((callBB, call.index))
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
                        .asInstanceOf[EOptionP[(DeclaredMethod, DataFlowFact), IFDSProperty[DataFlowFact]]]
                    val oldValue = state.pendingIfdsDependees.get(e)
                    val oldExitFacts: Map[Statement, Set[DataFlowFact]] = oldValue match {
                        case Some(ep: InterimEUBP[_, IFDSProperty[DataFlowFact]]) ⇒ ep.ub.flows
                        case _ ⇒ Map.empty
                    }
                    val exitFacts: Map[Statement, Set[DataFlowFact]] = callFlows match {
                        case ep: FinalEP[_, IFDSProperty[DataFlowFact]] ⇒
                            state.pendingIfdsDependees -= e
                            ep.p.flows
                        case ep: InterimEUBP[_, IFDSProperty[DataFlowFact]] ⇒
                            /*
                            * Add the call site to `pendingIfdsCallSites` and `pendingIfdsDependees` and
                            * continue with the facts in the interim result for now. When the analysis for the
                            * callee finishes, the analysis for this call site will be triggered again.
                            */
                            val newDependee =
                                state.pendingIfdsCallSites.getOrElse(e, Set.empty) + ((callBB, call.index))
                            state.pendingIfdsCallSites = state.pendingIfdsCallSites.updated(e, newDependee)
                            state.pendingIfdsDependees += e → callFlows
                            ep.ub.flows
                        // TODO Can we merge both cases to one case?
                        case _ ⇒
                            val newDependee =
                                state.pendingIfdsCallSites.getOrElse(e, Set.empty) + ((callBB, call.index))
                            state.pendingIfdsCallSites = state.pendingIfdsCallSites.updated(e, newDependee)
                            state.pendingIfdsDependees += e → callFlows
                            Map.empty
                    }
                    // Only process new facts that are not in `oldExitFacts`
                    allNewExitFacts = mergeMaps(allNewExitFacts, mapDifference(exitFacts, oldExitFacts))
                    /*
                    * If new exit facts were discovered for the callee-fact-pair, all call sites depending on this pair have to
                    * be re-evaluated. oldValue is undefined if the callee-fact pair has not been queried before or returned a FinalEP.
                    * TODO Can we remove a call site from pendingIfdsCallSites safely when we got a FinalEP?
                    */
                    if (!oldValue.isDefined && oldExitFacts != exitFacts) {
                        handleCallUpdate(e)
                    }
                }
            }

            // Map facts valid on each exit statement of the callee back to the caller
            val calleeExitStatements = getExits(calledMethod, callBB, call.index)
            for {
                successorBlock ← callBB.successors
                exitStatement ← calleeExitStatements
            } {
                val successorStatement = firstStatement(successorBlock)
                val oldSummaryEdges = summaryEdges.getOrElse(successorBlock, Set.empty[DataFlowFact])
                val exitToReturnFacts = returnFlow(
                    call,
                    callee,
                    exitStatement,
                    successorStatement,
                    allNewExitFacts.getOrElse(exitStatement, Set.empty)
                )
                summaryEdges += successorBlock → (oldSummaryEdges ++ exitToReturnFacts)
            }
        }
        summaryEdges
    }

    /**
     * Removes all values that are in the second map from the corresponding keys in the first map.
     */
    def mapDifference[S, T](map1: Map[S, Set[T]], map2: Map[S, Set[T]]): Map[S, Set[T]] = {
        var result = map1
        for ((key, values) ← map2) {
            result = result.updated(key, result(key) -- values)
        }
        result
    }

    /**
     * Gets the Call for a statement that contains a call (MethodCall Stmt or ExprStmt/Assigment
     * with FunctionCall)
     */
    protected[this] def asCall(stmt: Stmt[V]): Call[V] = stmt.astID match {
        case Assignment.ASTID ⇒ stmt.asAssignment.expr.asFunctionCall
        case ExprStmt.ASTID   ⇒ stmt.asExprStmt.expr.asFunctionCall
        case _                ⇒ stmt.asMethodCall
    }

    /**
     * Gets the first statement of a BasicBlock or the first statement of the handler BasicBlock of
     * a CatchNode.
     */
    @tailrec
    private def firstStatement(node: CFGNode)(implicit state: State): Statement = {
        if (node.isBasicBlock) {
            val index = node.asBasicBlock.startPC
            Statement(state.method, state.code(index), index, state.code, state.cfg)
        } else if (node.isCatchNode) {
            firstStatement(node.successors.head)
        } else
            null //TODO Throw an exception?
    }

    /**
     * Retrieves all exit statements of a called method for a specific call site. If the TAC of that method is not
     * present yet, it will be added to `pendingTacDependees` and `pendingTacCallSites`.
     *
     * @param method The method, for which the exit statements will be retrieved.
     * @param callingBlock The basic block, in which the method is called.
     * @param callIndex The index if the `callingBlock`, where the method is called.
     * @return All possible exit nodes.
     */
    def getExits(method: Method, callingBlock: BasicBlock, callIndex: Int)(
        implicit
        state: State
    ): Set[Statement] = {
        // The results of this method are stores in `exits`.
        val result = exits.get(method)
        if (result == null) {
            val (code, cfg) = propertyStore(method, TACAI.key) match {

                case FinalP(TheTACAI(tac)) ⇒
                    (tac.stmts, tac.cfg)

                case epk: EPK[Method, TACAI] ⇒
                    state.pendingTacDependees += method → epk
                    state.pendingTacCallSites += method →
                        (state.pendingTacCallSites.getOrElse(method, Set.empty) + ((callingBlock, callIndex)))
                    return Set.empty;

                case tac ⇒
                    throw new UnknownError(s"can't handle intermediate TACs ($tac)")
            }

            exits.computeIfAbsent(
                method,
                _ ⇒ {
                    (cfg.abnormalReturnNode.predecessors ++ cfg.normalReturnNode.predecessors).map { block ⇒
                        val endPC = block.asBasicBlock.endPC
                        Statement(method, code(endPC), endPC, code, cfg)
                    }
                }
            )
        } else
            result
    }

    /**
     * This method will be called if a non-overwritten declared method in a sub ype shall be analyzed.
     * Analyzes the defined method of the supertype instead.
     *
     * @param source A pair consisting of the declared method of the subtype and an input fact.
     * @return The result of the analysis of the defined method of the supertype.
     */
    def baseMethodResult(source: (DeclaredMethod, DataFlowFact)): ProperPropertyComputationResult = {

        def c(eps: SomeEOptionP): ProperPropertyComputationResult = eps match {
            case FinalP(p) ⇒ Result(source, p)

            case ep @ InterimUBP(ub: Property) ⇒
                InterimResult.forUB(source, ub, Seq(ep), c, CheapPropertyComputation)

            case epk ⇒
                InterimResult.forUB(
                    source,
                    createPropertyValue(Map.empty),
                    Seq(epk),
                    c,
                    CheapPropertyComputation
                )
        }
        c(propertyStore((declaredMethods(source._1.definedMethod), source._2), propertyKey.key))
    }
}

/**
 * A statement that is passed to the concrete analysis.
 *
 * @param method The analyzed method.
 * @param stmt The TAC statement.
 * @param index The index of the Statement in the code. TODO Isn't that redundant?
 * @param code The method's TAC code.
 * @param cfg The method's CFG.
 */
case class Statement(
        method: Method,
        stmt:   Stmt[V],
        index:  Int,
        code:   Array[Stmt[V]],
        cfg:    CFG[Stmt[V], TACStmts[V]]
) {

    override def hashCode(): Int = method.hashCode() * 31 + index

    override def equals(o: Any): Boolean = {
        o match {
            case s: Statement ⇒ s.index == index && s.method == method
            case _            ⇒ false
        }
    }

    override def toString: String = s"${method.toJava}"

}

object AbstractIFDSAnalysis {

    /** The type of the TAC domain. */
    type V = DUVar[ValueInformation]
}

abstract class IFDSAnalysis[DataFlowFact] extends FPCFLazyAnalysisScheduler {

    final override type InitializationData = AbstractIFDSAnalysis[DataFlowFact]

    def property: IFDSPropertyMetaInformation[DataFlowFact]

    final override def derivesLazily: Some[PropertyBounds] = Some(PropertyBounds.ub(property))

    override val uses: Set[PropertyBounds] = Set(PropertyBounds.finalP(TACAI))

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    final override def register(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: AbstractIFDSAnalysis[DataFlowFact]
    ): FPCFAnalysis = {
        ps.registerLazyPropertyComputation(property.key, analysis.performAnalysis)
        for (e ← analysis.entryPoints) { ps.force(e, analysis.propertyKey.key) }
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}

}
