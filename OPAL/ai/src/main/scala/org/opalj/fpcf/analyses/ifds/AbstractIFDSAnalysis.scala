/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package ifds

import java.util.concurrent.ConcurrentHashMap

import scala.collection.{Set ⇒ SomeSet}
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFG
import org.opalj.br.cfg.CFGNode
import org.opalj.fpcf.analyses.ifds.AbstractIFDSAnalysis.V
import org.opalj.fpcf.properties.IFDSProperty
import org.opalj.fpcf.properties.IFDSPropertyMetaInformation
import org.opalj.tac.DUVar
import org.opalj.tac.Stmt
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.Assignment
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.TACStmts
import org.opalj.tac.Expr
import org.opalj.tac.Call
import org.opalj.tac.ExprStmt
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.value.KnownTypedValue

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * A framework for IFDS analyses.
 *
 * @tparam DataFlowFact The type of flow facts the concrete analysis wants to track
 *
 * @author Dominik Helm
 */
abstract class AbstractIFDSAnalysis[DataFlowFact] extends FPCFAnalysis {

    /**
     * Provides the concrete property key (that must be unique for every distinct concrete analysis
     * and the lower bound for the IFDSProperty.
     */
    val property: IFDSPropertyMetaInformation[DataFlowFact]

    /** Creates the concrete IFDSProperty. */
    def createProperty(result: Map[Statement, Set[DataFlowFact]]): IFDSProperty[DataFlowFact]

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
    def returnFlow(stmt: Statement, callee: DeclaredMethod, exit: Statement, succ: Statement, in: Set[DataFlowFact]): Set[DataFlowFact]

    /**
     * Computes the DataFlowFacts valid on the CFG edge from statement `stmt` to `succ` irrespective
     * of the call in `stmt` if the DataFlowFacts `in` held before `stmt`.
     */
    def callToReturnFlow(stmt: Statement, succ: Statement, in: Set[DataFlowFact]): Set[DataFlowFact]

    protected[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    class State(
            val declClass:     ObjectType,
            val method:        Method,
            val source:        (DeclaredMethod, DataFlowFact),
            val code:          Array[Stmt[V]],
            val cfg:           CFG[Stmt[V], TACStmts[V]],
            var ifdsData:      Map[(DeclaredMethod, DataFlowFact), Set[(BasicBlock, Int)]],
            var ifdsDependees: Map[(DeclaredMethod, DataFlowFact), EOptionP[(DeclaredMethod, DataFlowFact), IFDSProperty[DataFlowFact]]] = Map.empty,
            var tacDependees:  Map[Method, EOptionP[Method, TACAI]]                                                                      = Map.empty,
            var tacData:       Map[Method, Set[(BasicBlock, Int)]]                                                                       = Map.empty,
            // DataFlowFacts known to be valid on entry to a basic block
            var incoming: Map[BasicBlock, Set[DataFlowFact]] = Map.empty,
            // DataFlowFacts known to be valid on exit from a basic block on the cfg edge to a specific successor
            var outgoing: Map[BasicBlock, Map[CFGNode, Set[DataFlowFact]]] = Map.empty
    )

    /**
     * Performs IFDS aAnalysis for one specific entity, i.e. one DeclaredMethod/DataFlowFact pair.
     */
    def performAnalysis(source: (DeclaredMethod, DataFlowFact)): PropertyComputationResult = {
        val (declaredMethod, sourceFact) = source

        // Deal only with single defined methods for now
        if (!declaredMethod.hasSingleDefinedMethod)
            return Result(source, property.noFlowInformation);

        val method = declaredMethod.definedMethod
        val declaringClass: ObjectType = method.classFile.thisType

        // If this is not the method's declaration, but a non-overwritten method in a subtype,
        // don't re-analyze the code
        if (declaringClass ne declaredMethod.declaringClassType)
            return baseMethodResult(declaredMethod.asDefinedMethod, sourceFact);

        val (code, cfg) = propertyStore(method, TACAI.key) match {
            case finalEP: FinalEP[Method, TACAI] ⇒
                val tac = finalEP.ub.tac.get
                (tac.stmts, tac.cfg)
            case _: IntermediateEP[Method, TACAI] ⇒
                throw new UnknownError("Can not handle intermediate TAC")
            case epk ⇒ return IntermediateResult(
                source,
                property.noFlowInformation,
                createProperty(Map.empty),
                Seq(epk),
                _ ⇒ performAnalysis(source),
                DefaultPropertyComputation
            );
        }

        implicit val state: State =
            new State(declaringClass, method, source, code, cfg, Map(source → Set.empty))

        // Start processing at the start of the cfg with the given source fact
        val start = cfg.startBlock
        state.incoming += start → Set(sourceFact)
        process(mutable.Queue((start, Set(sourceFact), None, None, None)))

        createResult()
    }

    /**
     * Processes a queue of BasicBlocks where new DataFlowFacts are available.
     */
    def process(
        initialWorklist: mutable.Queue[(BasicBlock, Set[DataFlowFact], Option[Int], Option[Set[Method]], Option[DataFlowFact])]
    )(implicit state: State): Unit = {
        val worklist = initialWorklist

        while (worklist.nonEmpty) {
            val (bb, in, callIndex, callee, dataFlowFact) = worklist.dequeue()
            val oldOut = state.outgoing.getOrElse(bb, Map.empty)
            val nextOut = analyseBasicBlock(bb, in, callIndex, callee, dataFlowFact)
            val allOut = mergeMaps(oldOut, nextOut)
            state.outgoing = state.outgoing.updated(bb, allOut)

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
                    val oldIn = state.incoming.getOrElse(succ, Set.empty)
                    state.incoming = state.incoming.updated(succ, oldIn ++ nextIn)
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
            case bb: BasicBlock if state.outgoing.contains(bb) && state.outgoing(bb).contains(node) ⇒
                val index = bb.endPC
                Statement(
                    state.method,
                    state.code(index),
                    index,
                    state.code,
                    state.cfg
                ) → state.outgoing(bb)(node)
        }.toMap

    /**
     * Creates the analysis result from the current state.
     */
    def createResult()(implicit state: State): PropertyComputationResult = {

        val result = mergeMaps(
            collectResult(state.cfg.normalReturnNode),
            collectResult(state.cfg.abnormalReturnNode)
        )

        val dependees = state.ifdsDependees.values ++ state.tacDependees.values

        if (dependees.isEmpty) {
            Result(
                state.source,
                createProperty(result)
            )
        } else {
            IntermediateResult(
                state.source,
                property.noFlowInformation,
                createProperty(result),
                dependees,
                c,
                DefaultPropertyComputation
            )
        }
    }

    def c(eps: SomeEPS)(implicit state: State): PropertyComputationResult = {

        eps match {
            case EPS(e, _, _: IFDSProperty[DataFlowFact]) ⇒
                state.ifdsDependees -= eps.e.asInstanceOf[(DeclaredMethod, DataFlowFact)]
                if (eps.isRefinable)
                    state.ifdsDependees +=
                        e.asInstanceOf[(DeclaredMethod, DataFlowFact)] →
                        eps.asInstanceOf[EOptionP[(DeclaredMethod, DataFlowFact), IFDSProperty[DataFlowFact]]]
                handleCallUpdate(e.asInstanceOf[(DeclaredMethod, DataFlowFact)])
            case FinalEP(m: Method, _: TACAI) ⇒
                handleCallUpdate(m)
                state.tacData -= m
                state.tacDependees -= m
            case IntermediateEP(_, _, _: TACAI) ⇒
                throw new UnknownError("Can not handle intermediate TAC")
        }

        createResult()
    }

    /**
     *  Computes for one BasicBlock `bb` the DataFlowFacts valid on each CFG edge leaving the
     *  BasicBlock if the DataFlowFacts `sources` held on entry to the BasicBlock.
     *  @param callIndex The index of a call where the information for one callee was updated or
     *                   None if analyseBasicBlock was not called as the result of such update.
     *  @param callee The callee that got its information updated or None if analyseBasicBlock
     *                was not called as the result of such update.
     *  @param fact The DataFlowFact that the callee was updated for or None if analyseBasicBlock
     *              was not called as the result of such update.
     */

    def analyseBasicBlock(
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
        def collectInformation(index: Int): (Statement, Option[SomeSet[Method]], Option[DataFlowFact]) = {
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
            flows =
                if (calleesO.isEmpty) {
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
        implicit val project: SomeProject = p
        stmt.astID match {
            case StaticMethodCall.ASTID ⇒
                Some(stmt.asStaticMethodCall.resolveCallTarget.toSet.filter(_.body.isDefined))

            case NonVirtualMethodCall.ASTID ⇒
                Some(stmt.asNonVirtualMethodCall.resolveCallTarget.toSet.filter(_.body.isDefined))

            case VirtualMethodCall.ASTID ⇒
                Some(stmt.asVirtualMethodCall.resolveCallTargets(state.declClass).filter(_.body.isDefined))

            case Assignment.ASTID if expr(stmt).astID == StaticFunctionCall.ASTID ⇒
                Some(stmt.asAssignment.expr.asStaticFunctionCall.resolveCallTarget.toSet.filter(_.body.isDefined))

            case Assignment.ASTID if expr(stmt).astID == NonVirtualFunctionCall.ASTID ⇒
                Some(stmt.asAssignment.expr.asNonVirtualFunctionCall.resolveCallTarget.toSet.filter(_.body.isDefined))

            case Assignment.ASTID if expr(stmt).astID == VirtualFunctionCall.ASTID ⇒
                Some(
                    stmt.asAssignment.expr.asVirtualFunctionCall.resolveCallTargets(state.declClass).filter(_.body.isDefined)
                )

            case ExprStmt.ASTID if expr(stmt).astID == StaticFunctionCall.ASTID ⇒
                Some(stmt.asExprStmt.expr.asStaticFunctionCall.resolveCallTarget.toSet.filter(_.body.isDefined))

            case ExprStmt.ASTID if expr(stmt).astID == NonVirtualFunctionCall.ASTID ⇒
                Some(stmt.asExprStmt.expr.asNonVirtualFunctionCall.resolveCallTarget.toSet.filter(_.body.isDefined))

            case ExprStmt.ASTID if expr(stmt).astID == VirtualFunctionCall.ASTID ⇒
                Some(
                    stmt.asExprStmt.expr.asVirtualFunctionCall.resolveCallTargets(state.declClass).filter(_.body.isDefined)
                )

            case _ ⇒ None
        }
    }

    /**
     * Starts processing all BasicBlocks that have to be reevaluated because a callee they may
     * invoke got its TAC computed.
     */
    def handleCallUpdate(m: Method)(implicit state: State): Unit = {
        val blocks = state.tacData(m)
        val queue: mutable.Queue[(BasicBlock, Set[DataFlowFact], Option[Int], Option[Set[Method]], Option[DataFlowFact])] =
            mutable.Queue.empty
        for ((block, callSite) ← blocks)
            queue.enqueue(
                (
                    block,
                    state.incoming(block),
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
        val blocks = state.ifdsData(e)
        val queue: mutable.Queue[(BasicBlock, Set[DataFlowFact], Option[Int], Option[Set[Method]], Option[DataFlowFact])] =
            mutable.Queue.empty
        for ((block, callSite) ← blocks)
            queue.enqueue(
                (
                    block,
                    state.incoming(block),
                    Some(callSite),
                    Some(Set(e._1.definedMethod)),
                    Some(e._2)
                )
            )
        process(queue)
    }

    /**
     * Processes a statement with a call.
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
                        state.ifdsData.getOrElse(state.source, Set.empty) + ((callBB, call.index))
                    state.ifdsData = state.ifdsData.updated(state.source, newDependee)
                    fromCall = mergeMaps(
                        fromCall,
                        mergeMaps(
                            collectResult(state.cfg.normalReturnNode),
                            collectResult(state.cfg.abnormalReturnNode)
                        )
                    )
                } else {
                    val e = (callee, fact)

                    val callFlows = propertyStore(e, property.key).asInstanceOf[EOptionP[(DeclaredMethod, DataFlowFact), IFDSProperty[DataFlowFact]]]

                    val oldState = state.ifdsDependees.get(e)

                    fromCall = mergeMaps(
                        fromCall,
                        mapDifference( // Only process new facts, that were not known in `oldState`
                            callFlows match {
                                case FinalEP(_, p: IFDSProperty[DataFlowFact]) ⇒
                                    state.ifdsDependees -= e
                                    p.flows
                                case EPS(_, _, ub: IFDSProperty[DataFlowFact]) ⇒
                                    val newDependee =
                                        state.ifdsData.getOrElse(e, Set.empty) + ((callBB, call.index))
                                    state.ifdsData = state.ifdsData.updated(e, newDependee)
                                    state.ifdsDependees += e → callFlows
                                    ub.flows
                                case _ ⇒
                                    val newDependee =
                                        state.ifdsData.getOrElse(e, Set.empty) + ((callBB, call.index))
                                    state.ifdsData = state.ifdsData.updated(e, newDependee)
                                    state.ifdsDependees += e → callFlows
                                    Map.empty
                            },
                            if (oldState.isDefined)
                                oldState.get match {
                                case EPS(_, _, ub) ⇒ ub.flows
                                case _             ⇒ Map.empty
                            }
                            else Map.empty
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
    def asCall(stmt: Stmt[V]): Call[V] = stmt.astID match {
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
    )(implicit state: State): Set[Statement] = {
        val result = exits.get(method)

        if (result == null) {
            val (code, cfg) = propertyStore(method, TACAI.key) match {
                case finalEP: FinalEP[Method, TACAI] ⇒
                    val tac = finalEP.ub.tac.get
                    (tac.stmts, tac.cfg)
                case _: IntermediateEP[Method, TACAI] ⇒
                    throw new UnknownError("Can not handle intermediate TAC")
                case epk ⇒
                    state.tacDependees += method → epk
                    state.tacData += method →
                        (state.tacData.getOrElse(method, Set.empty) + ((callBB, callIndex)))
                    return Set.empty;
            }
            exits.computeIfAbsent(method, _ ⇒ {
                (cfg.abnormalReturnNode.predecessors ++ cfg.normalReturnNode.predecessors).map {
                    block ⇒
                        val endPC = block.asBasicBlock.endPC
                        Statement(method, code(endPC), endPC, code, cfg)
                }
            })
        } else
            result
    }

    /**
     * Retrieves and commits the methods result as calculated for its declaring class type for the
     * current DefinedMethod that represents the non-overwritten method in a subtype.
     */
    def baseMethodResult(dm: DefinedMethod, sourceFact: DataFlowFact): PropertyComputationResult = {
        def c(eps: SomeEOptionP): PropertyComputationResult = eps match {
            case FinalEP(_, p) ⇒ Result(dm, p)
            case ep @ IntermediateEP(_, lb, ub) ⇒
                IntermediateResult(
                    (dm, sourceFact), lb, ub,
                    Seq(ep), c, CheapPropertyComputation
                )
            case epk ⇒
                IntermediateResult(
                    (dm, sourceFact), property.noFlowInformation, createProperty(Map.empty),
                    Seq(epk), c, CheapPropertyComputation
                )
        }
        c(propertyStore((declaredMethods(dm.definedMethod), sourceFact), property.key))
    }
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
    override def toString: String = s"${method.toJava}"

    override def hashCode(): Int = {
        method.hashCode() * 31 + index
    }

    override def equals(o: Any): Boolean = {
        o match {
            case s: Statement ⇒ s.index == index && s.method == method
            case _            ⇒ false
        }
    }
}

object AbstractIFDSAnalysis {

    /** The type of the TAC domain. */
    type V = DUVar[KnownTypedValue]
}

sealed trait IFDSAnalysisScheduler[DataFlowFact] extends ComputationSpecification {
    final override type InitializationData = AbstractIFDSAnalysis[DataFlowFact]

    def property: IFDSPropertyMetaInformation[DataFlowFact]
}

abstract class LazyIFDSAnalysis[DataFlowFact] extends IFDSAnalysisScheduler[DataFlowFact]
    with FPCFLazyAnalysisScheduler {

    final override def derives: Set[PropertyKind] = Set(property)

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    final override def startLazily(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: AbstractIFDSAnalysis[DataFlowFact]
    ): FPCFAnalysis = {
        ps.registerLazyPropertyComputation(
            property.key, analysis.performAnalysis
        )
        analysis
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}