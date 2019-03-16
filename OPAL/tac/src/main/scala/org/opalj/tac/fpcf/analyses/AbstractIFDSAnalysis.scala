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
      * Computes the DataFlowFacts valid after statement `stmt` on the CFG edge to statement `succ`
      * if the DataFlowFacts `in` held before `stmt`.
      */
    def normalFlow(stmt: Statement, succ: Statement, in: Set[DataFlowFact]): Set[DataFlowFact]

    /**
      * Computes the DataFlowFacts valid on entry to method `callee` when it is called from statement
      * `stmt` if the DataFlowFacts `in` held before `stmt`.
      */
    def callFlow(stmt: Statement, callee: DeclaredMethod, in: Set[DataFlowFact]): Set[DataFlowFact]

    /**
      * Computes the DataFlowFacts valid on the CFG edge from statement `stmt` to `succ` if `callee`
      * was invoked by `stmt` and DataFlowFacts `in` held before the final statement `exit` of
      * `callee`.
      */
    def returnFlow(
                    stmt:   Statement,
                    callee: DeclaredMethod,
                    exit:   Statement,
                    succ:   Statement,
                    in:     Set[DataFlowFact]
                  ): Set[DataFlowFact]

    /**
      * Computes the DataFlowFacts valid on the CFG edge from statement `stmt` to `succ` irrespective
      * of the call in `stmt` if the DataFlowFacts `in` held before `stmt`.
      */
    def callToReturnFlow(stmt: Statement, succ: Statement, in: Set[DataFlowFact]): Set[DataFlowFact]

    final protected[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    /**
      * The state of the analysis. For each method and source fact, there is a separate state.
      *
      * @param declaredClass The class defining the method that is analyzed.
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
      * @param outgoingFacts Maps each basic block and each exit node of that block
      *                      to the data flow facts valid after the exit node
      */
    class State(
                 val declaredClass:        ObjectType,
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
      */
    def process(
                 worklist: mutable.Queue[(BasicBlock, Set[DataFlowFact], Option[Int], Option[Set[Method]], Option[DataFlowFact])]
               )(
                 implicit
                 state: State
               ): Unit = {
        while (worklist.nonEmpty) {
            val (bb, in, callIndex, callee, dataFlowFact) = worklist.dequeue()
            val oldOut = state.outgoingFacts.getOrElse(bb, Map.empty)
            val nextOut = analyzeBasicBlock(bb, in, callIndex, callee, dataFlowFact)
            val allOut = mergeMaps(oldOut, nextOut)
            state.outgoingFacts = state.outgoingFacts.updated(bb, allOut)

            for (successor ← bb.successors) {
                if (successor.isExitNode) {
                    // Handle self-dependencies: Propagate new information to self calls
                    if ((nextOut.getOrElse(successor, Set.empty) -- oldOut.getOrElse(successor, Set.empty)).nonEmpty)
                        handleCallUpdate(state.source)
                } else {
                    val succ = (if (successor.isBasicBlock) {
                        successor
                    } else { // Skip CatchNodes directly to their handler BasicBlock
                        assert(successor.isCatchNode)
                        assert(successor.successors.size == 1)
                        successor.successors.head
                    }).asBasicBlock

                    val nextIn = nextOut.getOrElse(successor, Set.empty)
                    val oldIn = state.incomingFacts.getOrElse(succ, Set.empty)
                    state.incomingFacts = state.incomingFacts.updated(succ, oldIn ++ nextIn)
                    val newIn = nextIn -- oldIn
                    if (newIn.nonEmpty) {
                        worklist.enqueue((succ, newIn, None, None, None))
                    }
                }
            }
        }
    }

    /**
      * Merges two maps that have sets as values. The resulting map has the keys from both maps with
      * the associated values being the union of the value from both input maps.
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
      */
    def collectResult(node: CFGNode)(implicit state: State): Map[Statement, Set[DataFlowFact]] =
        node.predecessors.collect {
            case bb: BasicBlock if state.outgoingFacts.contains(bb) && state.outgoingFacts(bb).contains(node) ⇒
                val index = bb.endPC
                Statement(
                    state.method,
                    state.code(index),
                    index,
                    state.code,
                    state.cfg
                ) → state.outgoingFacts(bb)(node)
        }.toMap

    /**
      * Creates the analysis result from the current state.
      */
    def createResult()(implicit state: State): ProperPropertyComputationResult = {

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
                c,
                DefaultPropertyComputation
            )
        }
    }

    def c(eps: SomeEPS)(implicit state: State): ProperPropertyComputationResult = {
        (eps: @unchecked) match {
            case FinalE(e: (DeclaredMethod, DataFlowFact) @unchecked)     ⇒ handleCallUpdate(e)

            case InterimEUB(e: (DeclaredMethod, DataFlowFact) @unchecked) ⇒ handleCallUpdate(e)

            case FinalEP(m: Method, _: TACAI) ⇒
                handleCallUpdate(m)
                state.pendingTacCallSites -= m
                state.pendingTacDependees -= m

            case InterimUBP(_: TACAI) ⇒ throw new UnknownError("Can not handle intermediate TAC")
        }

        createResult()
    }

    /**
      *  Computes for one BasicBlock `bb` the DataFlowFacts valid on each CFG edge leaving the
      *  BasicBlock if the DataFlowFacts `sources` held on entry to the BasicBlock.
      *
      *  @param callIndex The index of a call where the information for one callee was updated or
      *                   None if analyzeBasicBlock was not called as the result of such update.
      *  @param callee The callee that got its information updated or None if analyzeBasicBlock
      *                was not called as the result of such update.
      *  @param fact The DataFlowFact that the callee was updated for or None if analyzeBasicBlock
      *              was not called as the result of such update.
      */

    def analyzeBasicBlock(
                           bb:        BasicBlock,
                           sources:   Set[DataFlowFact],
                           callIndex: Option[Int], //TODO IntOption
                           callee:    Option[Set[Method]],
                           fact:      Option[DataFlowFact]
                         )(
                           implicit
                           state: State
                         ): Map[CFGNode, Set[DataFlowFact]] = {

        var flows: Set[DataFlowFact] = sources

        /**
          * Collects information about the TAC Stmt at `index`: The corresponding Statement object,
          * the Set of relevant callees for that statement (None if the statement has no call) and
          * the fact that was updated for the call if analyseBasicBlock was called because of an
          * update.
          */
        def collectInformation(
                                index: Int
                              ): (Statement, Option[SomeSet[Method]], Option[DataFlowFact]) = {
            val stmt = state.code(index)
            val statement = Statement(state.method, stmt, index, state.code, state.cfg)
            val calleesO = if (callIndex.contains(index)) callee else getCallees(stmt)
            val callFact = if (callIndex.contains(index)) fact else None
            (statement, calleesO, callFact)
        }

        // Iterate over all statements but the last one in linear order, only keeping the resulting
        // DataFlowFacts.

        var index = bb.startPC
        val max = bb.endPC
        while (index < max) {
            val (statement, calleesO, callFact) = collectInformation(index)
            flows = if (calleesO.isEmpty) {
                val successor =
                    Statement(state.method, state.code(index + 1), index + 1, state.code, state.cfg)
                normalFlow(statement, successor, flows)
            } else
                handleCall(bb, statement, calleesO.get, flows, callFact).values.head
            index += 1
        }

        // Analyse the last statement for each possible successor statement.

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
        if (sources.contains(null.asInstanceOf[DataFlowFact]))
            result = result.map { result ⇒
                result._1 → (result._2 + null.asInstanceOf[DataFlowFact])
            }
        result
    }

    /** Gets the expression from an assingment/expr statement. */
    def expr(stmt: Stmt[V]): Expr[V] = stmt.astID match {
        case Assignment.ASTID ⇒ stmt.asAssignment.expr
        case ExprStmt.ASTID   ⇒ stmt.asExprStmt.expr
        case _                ⇒ throw new UnknownError("Unexpected statement")
    }

    /**
      * Gets the set of all methods possibly called by `stmt` or None if `stmt` contains no call.
      */
    def getCallees(stmt: Stmt[V])(implicit state: State): Option[SomeSet[Method]] = {
        (stmt.astID: @switch) match {
            case StaticMethodCall.ASTID ⇒
                Some(stmt.asStaticMethodCall.resolveCallTarget.toSet.filter(_.body.isDefined))

            case NonVirtualMethodCall.ASTID ⇒
                Some(
                    stmt.asNonVirtualMethodCall
                      .resolveCallTarget(state.declaredClass)
                      .toSet
                      .filter(_.body.isDefined)
                )

            case VirtualMethodCall.ASTID ⇒
                Some(
                    stmt.asVirtualMethodCall.resolveCallTargets(state.declaredClass).filter(_.body.isDefined)
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
                              .resolveCallTarget(state.declaredClass)
                              .toSet
                              .filter(_.body.isDefined)
                        )

                    case VirtualFunctionCall.ASTID ⇒
                        Some(
                            stmt.asAssignment.expr.asVirtualFunctionCall
                              .resolveCallTargets(state.declaredClass)
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
                              .resolveCallTarget(state.declaredClass)
                              .toSet
                              .filter(_.body.isDefined)
                        )

                    case VirtualFunctionCall.ASTID ⇒
                        Some(
                            stmt.asExprStmt.expr.asVirtualFunctionCall
                              .resolveCallTargets(state.declaredClass)
                              .filter(_.body.isDefined)
                        )
                    case _ ⇒ None
                }

            case _ ⇒ None
        }
    }

    /**
      * Starts processing all BasicBlocks that have to be reevaluated because a callee they may
      * invoke got its TAC computed.
      */
    def handleCallUpdate(m: Method)(implicit state: State): Unit = {
        val blocks = state.pendingTacCallSites(m)
        val queue: mutable.Queue[(BasicBlock, Set[DataFlowFact], Option[Int], Option[Set[Method]], Option[DataFlowFact])] =
            mutable.Queue.empty
        for ((block, callSite) ← blocks)
            queue.enqueue(
                (
                  block,
                  state.incomingFacts(block),
                  Some(callSite),
                  Some(Set(m)),
                  None
                )
            )
        process(queue)
    }

    /**
      * Starts processing all BasicBlocks that have to be reevaluated because a callee they may
      * invoke got updated.
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
      * @param fact A single DataFlowFact valid before the call statement or None if handleCall is
      *             not invoked because of an update to a callee.
      */
    def handleCall(
                    callBB:  BasicBlock,
                    call:    Statement,
                    callees: SomeSet[Method],
                    in:      Set[DataFlowFact],
                    fact:    Option[DataFlowFact]
                  )(
                    implicit
                    state: State
                  ): Map[CFGNode, Set[DataFlowFact]] = {
        // DataFlowFacts valid on the CFG edge to each successor after the call
        var flows: Map[CFGNode, Set[DataFlowFact]] = Map.empty

        // Handle call-to-return flows (only if this is not an update)
        if (fact.isEmpty)
            for (successor ← callBB.successors) {
                flows += successor → callToReturnFlow(call, firstStatement(successor), in)
            }

        for (calledMethod ← callees) {
            val callee = declaredMethods(calledMethod)

            // Facts valid on entry of the callee (a single one if this is an update)
            val toCall = if (fact.isDefined) fact.toSet else callFlow(call, callee, in)

            // Facts valid on each exit statement of the callee
            var fromCall: Map[Statement, Set[DataFlowFact]] = Map.empty
            for (fact ← toCall) {
                if ((calledMethod eq state.method) && fact == state.source._2) {
                    val newDependee =
                        state.pendingIfdsCallSites.getOrElse(state.source, Set.empty) + ((callBB, call.index))
                    state.pendingIfdsCallSites = state.pendingIfdsCallSites.updated(state.source, newDependee)
                    fromCall = mergeMaps(
                        fromCall,
                        mergeMaps(
                            collectResult(state.cfg.normalReturnNode),
                            collectResult(state.cfg.abnormalReturnNode)
                        )
                    )
                } else {
                    val e = (callee, fact)

                    val callFlows = propertyStore(e, propertyKey.key)
                      .asInstanceOf[EOptionP[(DeclaredMethod, DataFlowFact), IFDSProperty[DataFlowFact]]]

                    val oldState = state.pendingIfdsDependees.get(e)

                    fromCall = mergeMaps(
                        fromCall,
                        mapDifference( // Only process new facts, that were not known in `oldState`
                            callFlows match {
                                case ep: FinalEP[_, IFDSProperty[DataFlowFact]] ⇒
                                    state.pendingIfdsDependees -= e
                                    ep.p.flows
                                case ep: InterimEUBP[_, IFDSProperty[DataFlowFact]] ⇒
                                    val newDependee =
                                        state.pendingIfdsCallSites.getOrElse(e, Set.empty) + ((callBB, call.index))
                                    state.pendingIfdsCallSites = state.pendingIfdsCallSites.updated(e, newDependee)
                                    state.pendingIfdsDependees += e → callFlows
                                    ep.ub.flows
                                case _ ⇒
                                    val newDependee =
                                        state.pendingIfdsCallSites.getOrElse(e, Set.empty) + ((callBB, call.index))
                                    state.pendingIfdsCallSites = state.pendingIfdsCallSites.updated(e, newDependee)
                                    state.pendingIfdsDependees += e → callFlows
                                    Map.empty
                            },
                            if (oldState.isDefined) {
                                oldState.get match {
                                    case ep: InterimEUBP[_, IFDSProperty[DataFlowFact]] ⇒ ep.ub.flows
                                    case _                                              ⇒ Map.empty
                                }
                            } else Map.empty
                        )
                    )
                    if (oldState.isDefined && oldState.get != callFlows) {
                        handleCallUpdate(e)
                    }
                }
            }

            // Map data flow facts valid on each exit statement of the callee back to the caller
            val exits = getExits(calledMethod, callBB, call.index)
            for {
                node ← callBB.successors
                exit ← exits
            } {
                val successor = firstStatement(node)
                val oldFlows = flows.getOrElse(node, Set.empty[DataFlowFact])
                val retFlow = returnFlow(
                    call,
                    callee,
                    exit,
                    successor,
                    fromCall.getOrElse(exit, Set.empty)
                )
                flows += node → (oldFlows ++ retFlow)
            }
        }

        flows
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
            null
    }

    /**
      * Memoizes results of getExits.
      */
    val exits: ConcurrentHashMap[Method, Set[Statement]] = new ConcurrentHashMap

    /**
      * Finds all statements that may terminate the given method.
      */
    def getExits(
                  method:    Method,
                  callBB:    BasicBlock,
                  callIndex: Int
                )(
                  implicit
                  state: State
                ): Set[Statement] = {
        val result = exits.get(method)

        if (result == null) {
            val (code, cfg) = propertyStore(method, TACAI.key) match {
                case FinalP(TheTACAI(tac)) ⇒
                    (tac.stmts, tac.cfg)

                case epk: EPK[Method, TACAI] ⇒
                    state.pendingTacDependees += method → epk
                    state.pendingTacCallSites += method →
                      (state.pendingTacCallSites.getOrElse(method, Set.empty) + ((callBB, callIndex)))
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
      * Retrieves and commits the method's result as calculated for its declaring class type for the
      * current DefinedMethod that represents the non-overwritten method in a subtype.
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

    val entryPoints: Map[DeclaredMethod, DataFlowFact]
}

/**
  * Provides information about a statement that may be needed by the concrete analysis.
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
