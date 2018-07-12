/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package ifds

import scala.collection.{Set ⇒ SomeSet}
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
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
import org.opalj.tac.TACode
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.DefaultTACAIKey
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

import scala.annotation.tailrec
import scala.collection.mutable

abstract class AbstractIFDSAnalysis[DataFlowFact] extends FPCFAnalysis {

    val property: IFDSPropertyMetaInformation[DataFlowFact]

    def createProperty(result: Map[Statement, Set[DataFlowFact]]): IFDSProperty[DataFlowFact]

    def normalFlow(stmt: Statement, succ: Statement, in: Set[DataFlowFact]): Set[DataFlowFact]

    def callFlow(stmt: Statement, params: Seq[Expr[V]], callee: DeclaredMethod, in: Set[DataFlowFact]): Set[DataFlowFact]

    def returnFlow(stmt: Statement, callee: DeclaredMethod, exit: Statement, succ: Statement, in: Set[DataFlowFact]): Set[DataFlowFact]

    def callToReturnFlow(stmt: Statement, succ: Statement, in: Set[DataFlowFact]): Set[DataFlowFact]

    protected[this] val tacai: Method ⇒ TACode[TACMethodParameter, V] = project.get(DefaultTACAIKey)
    protected[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    class State(
        val declClass: ObjectType,
        val method:    Method,
        val code:      Array[Stmt[V]],
        val cfg:       CFG[Stmt[V], TACStmts[V]],
        var dependees: Set[SomeEOptionP],
        var data:      Map[(DeclaredMethod, DataFlowFact), Set[(BasicBlock, Int)]],
        var incoming:  Map[BasicBlock, Set[DataFlowFact]],
        var outgoing:  Map[BasicBlock, Map[CFGNode, Set[DataFlowFact]]]
    )

    def performAnalysis(source: (DeclaredMethod, DataFlowFact)): PropertyComputationResult = {
        val (declaredMethod, sourceFact) = source

        if (!declaredMethod.hasSingleDefinedMethod)
            return Result(source, property.noFlowInformation);

        val method = declaredMethod.definedMethod
        val declaringClass: ObjectType = method.classFile.thisType

        // If this is not the method's declaration, but a non-overwritten method in a subtype,
        // don't re-analyze the code
        if (declaringClass ne declaredMethod.declaringClassType)
            return baseMethodResult(declaredMethod.asDefinedMethod, sourceFact);

        val TACode(_, code, _, cfg, _, _) = tacai(method)

        implicit val state: State =
            new State(declaringClass, method, code, cfg, Set.empty, Map.empty, Map.empty, Map.empty)

        val start = cfg.startBlock

        process(mutable.Queue((start, Set(sourceFact), None, None)))

        createResult(source)
    }

    def process(
        initialWorklist: mutable.Queue[(BasicBlock, Set[DataFlowFact], Option[Int], Option[Set[Method]])]
    )(implicit state: State): Unit = {
        val worklist = initialWorklist

        while (worklist.nonEmpty) {
            val (bb, in, callIndex, callee) = worklist.dequeue()
            val oldOut = state.outgoing.getOrElse(bb, Map.empty)
            val nextOut = analyseBasicBlock(bb, in, callIndex, callee)

            val allOut = mergeMaps(oldOut, nextOut)
            state.outgoing = state.outgoing.updated(bb, allOut.toMap)

            for (successor ← bb.successors if !successor.isExitNode) {
                val succ = (if (successor.isBasicBlock) {
                    successor
                } else {
                    assert(successor.isCatchNode)
                    assert(successor.successors.size == 1)
                    successor.successors.head
                }).asBasicBlock

                val nextIn = nextOut(successor)
                val oldIn = state.incoming.getOrElse(succ, Set.empty)
                state.incoming = state.incoming.updated(succ, oldIn ++ nextIn)
                val newIn = nextIn -- oldIn
                if (newIn.nonEmpty) {
                    worklist.enqueue((succ, newIn, None, None))
                }
            }
        }
    }

    // TODO more efficient implementation?
    def mergeMaps[S, T](map1: Map[S, Set[T]], map2: Map[S, Set[T]]): Map[S, Set[T]] = {
        (map1.keysIterator ++ map2.keysIterator).map { key ⇒
            key → (map1.getOrElse(key, Set.empty) ++ map2.getOrElse(key, Set.empty))
        }.toMap
    }

    def createResult(
        source: (DeclaredMethod, DataFlowFact)
    )(implicit state: State): PropertyComputationResult = {
        def collectResult(node: CFGNode): Map[Statement, Set[DataFlowFact]] =
            node.predecessors.collect {
                case bb: BasicBlock if state.outgoing.contains(bb) && state.outgoing(bb).contains(node) ⇒
                    val index = bb.endPC
                    Statement(
                        state.method,
                        state.code(index),
                        index,
                        state.code
                    ) → state.outgoing(bb)(node)
            }.toMap

        val result = mergeMaps(
            collectResult(state.cfg.normalReturnNode),
            collectResult(state.cfg.abnormalReturnNode)
        )

        if (state.dependees.isEmpty) {
            Result(
                source,
                createProperty(result)
            )
        } else {
            IntermediateResult(
                source,
                property.noFlowInformation,
                createProperty(result),
                state.dependees,
                c(source),
                DefaultPropertyComputation
            )
        }
    }

    def c(
        source: (DeclaredMethod, DataFlowFact)
    )(
        eps: SomeEPS
    )(implicit state: State): PropertyComputationResult = {
        state.dependees = state.dependees.filterNot(_.e eq eps.e)

        eps match {
            case EPS(e, _, _) ⇒
                if (eps.isRefinable) state.dependees += eps
                handleCallUpdate(e.asInstanceOf[(DeclaredMethod, DataFlowFact)])
        }

        createResult(source)
    }

    def analyseBasicBlock(
        bb:        BasicBlock,
        sources:   Set[DataFlowFact],
        callIndex: Option[Int], //TODO IntOption
        callee:    Option[Set[Method]]
    )(
        implicit
        state: State
    ): Map[CFGNode, Set[DataFlowFact]] = {

        var flows: Set[DataFlowFact] = sources

        def statementAndCallees(index: Int): (Statement, Option[SomeSet[Method]]) = {
            val stmt = state.code(index)
            val statement = Statement(state.method, stmt, index, state.code)
            val calleesO = if (callIndex.contains(index)) callee else getCallees(stmt)
            (statement, calleesO)
        }

        var index = bb.startPC
        val max = bb.endPC
        while (index < max) {
            val (statement, calleesO) = statementAndCallees(index)
            flows =
                if (calleesO.isEmpty) {
                    val successor =
                        Statement(state.method, state.code(index + 1), index + 1, state.code)
                    normalFlow(statement, successor, flows)
                } else
                    handleCall(bb, statement, bb.successors, calleesO.get, flows).values.head
            index += 1
        }

        val (statement, calleesO) = statementAndCallees(bb.endPC)
        var result =
            if (calleesO.isEmpty) {
                var result: Map[CFGNode, Set[DataFlowFact]] = Map.empty
                for (node ← bb.successors) {
                    result += node → normalFlow(statement, firstStatement(node), flows)
                }
                result
            } else {
                handleCall(bb, statement, bb.successors, calleesO.get, flows)
            }
        if (sources.contains(null.asInstanceOf[DataFlowFact]))
            result = result.map { result ⇒
                result._1 → (result._2 + null.asInstanceOf[DataFlowFact])
            }
        result
    }

    def expr(stmt: Stmt[V]): Expr[V] = {
        stmt.asAssignment.expr
    }

    def getCallees(stmt: Stmt[V])(implicit state: State): Option[SomeSet[Method]] = {
        implicit val project: SomeProject = p
        stmt.astID match {
            case StaticMethodCall.ASTID ⇒
                Some(stmt.asStaticMethodCall.resolveCallTarget.toSet)

            case NonVirtualMethodCall.ASTID ⇒
                Some(stmt.asNonVirtualMethodCall.resolveCallTarget.toSet)

            case VirtualMethodCall.ASTID ⇒
                Some(stmt.asVirtualMethodCall.resolveCallTargets(state.declClass))

            case Assignment.ASTID if expr(stmt).astID == StaticFunctionCall.ASTID ⇒
                Some(stmt.asAssignment.expr.asStaticFunctionCall.resolveCallTarget.toSet)

            case Assignment.ASTID if expr(stmt).astID == NonVirtualFunctionCall.ASTID ⇒
                Some(stmt.asAssignment.expr.asNonVirtualFunctionCall.resolveCallTarget.toSet)

            case Assignment.ASTID if expr(stmt).astID == VirtualFunctionCall.ASTID ⇒
                Some(
                    stmt.asAssignment.expr.asVirtualFunctionCall.resolveCallTargets(state.declClass)
                )

            case _ ⇒ None
        }
    }

    def handleCallUpdate(e: (DeclaredMethod, DataFlowFact))(implicit state: State): Unit = {
        val blocks = state.data(e)
        val queue: mutable.Queue[(BasicBlock, Set[DataFlowFact], Option[Int], Option[Set[Method]])] =
            mutable.Queue.empty
        for ((block, callSite) ← blocks)
            queue.enqueue((block, state.incoming(block), Some(callSite), Some(Set(e._1.definedMethod))))
        process(queue)
    }

    def handleCall(
        block:      BasicBlock,
        call:       Statement,
        successors: Set[CFGNode],
        callees:    SomeSet[Method],
        in:         Set[DataFlowFact]
    )(
        implicit
        state: State
    ): Map[CFGNode, Set[DataFlowFact]] = {
        var flows: Map[CFGNode, Set[DataFlowFact]] = Map.empty
        for (successor ← successors) {
            flows += successor → callToReturnFlow(call, firstStatement(successor), in)
        }
        for (calledMethod ← callees) {
            val callee = declaredMethods(calledMethod)
            val toCall = callFlow(call, asCall(call.stmt).allParams, callee, in)
            var fromCall: Map[Statement, Set[DataFlowFact]] = Map.empty
            for (fact ← toCall) {
                val e = (callee, fact)
                val callFlows = propertyStore(e, property.key) //TODO always use the identical property
                val oldState = state.dependees.find(eop ⇒ eop.e == e && (eop ne callFlows))
                fromCall = mergeMaps(
                    fromCall,
                    callFlows match {
                        case FinalEP(_, p: IFDSProperty[DataFlowFact]) ⇒ p.flows
                        case EPS(_, _, ub: IFDSProperty[DataFlowFact]) ⇒
                            val newDependee = state.data.getOrElse(e, Set.empty) + ((block, call.index))
                            state.data = state.data.updated(e, newDependee)
                            state.dependees += callFlows
                            ub.flows
                        case _ ⇒
                            val newDependee = state.data.getOrElse(e, Set.empty) + ((block, call.index))
                            state.data = state.data.updated(e, newDependee)
                            state.dependees += callFlows
                            Map.empty
                    }
                )
                if (oldState.isDefined) {
                    state.dependees -= oldState.get // FIXME
                    handleCallUpdate(e)
                }
            }
            for {
                node ← successors
                exit ← getExits(calledMethod)
            } {
                val successor = firstStatement(node)
                val c2rFlow = flows.getOrElse(node, Set.empty[DataFlowFact])
                val retFlow =
                    returnFlow(call, callee, exit, successor, fromCall.getOrElse(exit, Set.empty))
                flows += node -> (c2rFlow ++ retFlow)
            }
        }
        flows
    }

    def asCall(stmt: Stmt[V]): Call[V] = stmt.astID match {
        case Assignment.ASTID ⇒ stmt.asAssignment.expr.asFunctionCall
        case _                ⇒ stmt.asMethodCall
    }

    @tailrec
    private def firstStatement(node: CFGNode)(implicit state: State): Statement = {
        if (node.isBasicBlock) {
            val index = node.asBasicBlock.startPC
            Statement(state.method, state.code(index), index, state.code)
        } else if (node.isCatchNode) {
            firstStatement(node.successors.head)
        } else
            null
    }

    def getExits(method: Method): Set[Statement] = {
        val TACode(_, code, _, cfg, _, _) = tacai(method)
        (cfg.abnormalReturnNode.predecessors ++ cfg.normalReturnNode.predecessors).map { block ⇒
            val endPC = block.asBasicBlock.endPC
            Statement(method, code(endPC), endPC, code)
        }
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

case class Statement(
        method: Method,
        stmt:   Stmt[V],
        index:  Int,
        code:   Array[Stmt[V]]
) {
    override def toString: String = s"${method.toJava}"
}

object AbstractIFDSAnalysis {

    /** The type of the TAC domain. */
    type V = DUVar[(Domain with RecordDefUse)#DomainValue]
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