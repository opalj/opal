/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ifds

import java.io.File
import java.io.PrintWriter

import scala.collection.{Set ⇒ SomeSet}
import scala.collection.mutable

import com.typesafe.config.ConfigValueFactory
import javax.swing.JOptionPane

import org.opalj.util.Milliseconds
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalE
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
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
import org.opalj.fpcf.seq.PKESequentialPropertyStore
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
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.ai.domain.l0.PrimitiveTACAIDomain
import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.fpcf.properties.IFDSProperty
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis.V

/**
 * The supertype of all IFDS facts.
 */
trait AbstractIFDSFact

/**
 * The super type of all null facts.
 */
trait AbstractIFDSNullFact extends AbstractIFDSFact

/**
 * A framework for IFDS analyses.
 *
 * @tparam IFDSFact The type of flow facts, which are tracked by the concrete analysis.
 * @author Dominik Helm
 * @author Mario Trageser
 */
abstract class AbstractIFDSAnalysis[IFDSFact <: AbstractIFDSFact] extends FPCFAnalysis with Subsumable[IFDSFact] {

    /**
     * Provides the concrete property key that must be unique for every distinct concrete analysis
     * and the lower bound for the IFDSProperty.
     */
    val propertyKey: IFDSPropertyMetaInformation[IFDSFact]

    /**
     * All declared methods in the project.
     */
    final protected implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    /**
     * Counts, how many times the abstract methods were called.
     */
    var numberOfCalls = new NumberOfCalls()

    /**
     * Counts, how many input facts are passed to callbacks.
     */
    var sumOfInputfactsForCallbacks = 0L

    /**
     * The entry points of this analysis.
     */
    def entryPoints: Seq[(DeclaredMethod, IFDSFact)]

    /**
     * The state of the analysis. For each method and source fact, there is a separate state.
     *
     * @param declaringClass The class defining the analyzed `method`.
     * @param method The analyzed method.
     * @param source The input fact, for which the `method` is analyzed.
     * @param code The code of the `method`.
     * @param cfg The control flow graph of the `method`.
     * @param pendingIfdsCallSites Maps callees of the analyzed `method` together with their input
     *                             facts to the basic block and statement index, at which they may
     *                             be called.
     * @param pendingIfdsDependees Maps callees of the analyzed `method` together with their input
     *                             facts to the intermediate result of their IFDS analysis.
     *                             Only contains method-fact-pairs, for which this analysis is
     *                             waiting for a final result.
     * @param pendingCgCallSites The basic blocks containing call sites, for which the analysis is
     *                           waiting for the final call graph result.
     * @param incomingFacts Maps each basic block to the data flow facts valid at its first
     *                      statement.
     * @param outgoingFacts Maps each basic block and successor node to the data flow facts valid at
     *                      the beginning of the successor.
     */
    protected class State(
            val declaringClass:       ObjectType,
            val method:               Method,
            val source:               (DeclaredMethod, IFDSFact),
            val code:                 Array[Stmt[V]],
            val cfg:                  CFG[Stmt[V], TACStmts[V]],
            var pendingIfdsCallSites: Map[(DeclaredMethod, IFDSFact), Set[(BasicBlock, Int)]],
            var pendingTacCallSites:  Map[DeclaredMethod, Set[BasicBlock]]                                                          = Map.empty,
            var pendingIfdsDependees: Map[(DeclaredMethod, IFDSFact), EOptionP[(DeclaredMethod, IFDSFact), IFDSProperty[IFDSFact]]] = Map.empty,
            var pendingTacDependees:  Map[Method, EOptionP[Method, TACAI]]                                                          = Map.empty,
            var pendingCgCallSites:   Set[BasicBlock]                                                                               = Set.empty,
            var incomingFacts:        Map[BasicBlock, Set[IFDSFact]]                                                                = Map.empty,
            var outgoingFacts:        Map[BasicBlock, Map[CFGNode, Set[IFDSFact]]]                                                  = Map.empty
    )

    /**
     * The null fact of this analysis.
     */
    protected def nullFact: IFDSFact

    /**
     * Computes the data flow for a normal statement.
     *
     * @param statement The analyzed statement.
     * @param successor The successor of the analyzed `statement`, for which the data flow shall be
     *                  computed.
     * @param in The facts, which hold before the execution of the `statement`.
     * @return The facts, which hold after the execution of `statement` under the assumption
     *         that the facts in `in` held before `statement` and `successor` will be
     *         executed next.
     */
    protected def normalFlow(statement: Statement, successor: Statement, in: Set[IFDSFact]): Set[IFDSFact]

    /**
     * Computes the data flow for a call to start edge.
     *
     * @param call The analyzed call statement.
     * @param callee The called method, for which the data flow shall be computed.
     * @param in The facts, which hold before the execution of the `call`.
     * @param source The entity, which is analyzed.
     * @return The facts, which hold after the execution of `statement` under the assumption that
     *         the facts in `in` held before `statement` and `statement` calls `callee`.
     */
    protected def callFlow(call: Statement, callee: DeclaredMethod, in: Set[IFDSFact],
                           source: (DeclaredMethod, IFDSFact)): Set[IFDSFact]

    /**
     * Computes the data flow for an exit to return edge.
     *
     * @param call The statement, which called the `callee`.
     * @param callee The method called by `call`, for which the data flow shall be computed.
     * @param exit The statement, which terminated the `calle`.
     * @param successor The statement of the caller, which will be executed after the `callee`
     *                  returned.
     * @param in The facts, which hold before the execution of the `exit`.
     * @return The facts, which hold after the execution of `exit` in the caller's context
     *         under the assumption that `in` held before the execution of `exit` and that
     *         `successor` will be executed next.
     */
    protected def returnFlow(call: Statement, callee: DeclaredMethod, exit: Statement,
                             successor: Statement, in: Set[IFDSFact]): Set[IFDSFact]

    /**
     * Computes the data flow for a call to return edge.
     *
     * @param call The statement, which invoked the call.
     * @param successor The statement, which will be executed after the call.
     * @param in The facts, which hold before the `call`.
     * @param source The entity, which is analyzed.
     * @return The facts, which hold after the call independently of what happens in the callee
     *         under the assumption that `in` held before `call`.
     */
    protected def callToReturnFlow(call: Statement, successor: Statement, in: Set[IFDSFact],
                                   source: (DeclaredMethod, IFDSFact)): Set[IFDSFact]

    /**
     * When a callee outside of this analysis' context is called, this method computes the summary
     * edge for the call.
     *
     * @param call The statement, which invoked the call.
     * @param callee The method called by `call`.
     * @param successor The statement, which will be executed after the call.
     * @param in The facts facts, which hold before the `call`.
     * @return The facts, which hold after the call, excluding the call to return flow.
     */
    protected def callOutsideOfAnalysisContext(call: Statement, callee: DeclaredMethod,
                                               successor: Statement, in: Set[IFDSFact]): Set[IFDSFact]

    /**
     * Determines the basic blocks, at which the analysis starts.
     *
     * @param sourceFact The source fact of the analysis.
     * @param cfg The control flow graph of the analyzed method.
     * @return The basic blocks, at which the analysis starts.
     */
    protected def startBlocks(sourceFact: IFDSFact, cfg: CFG[Stmt[V], TACStmts[V]]): Set[BasicBlock]

    /**
     * Collects the facts valid at all exit nodes based on the current results.
     *
     * @return A map, mapping from each predecessor of all exit nodes to the facts, which hold at
     *         the exit node under the assumption that the predecessor was executed before.
     */
    protected def collectResult(implicit state: State): Map[Statement, Set[IFDSFact]]

    /**
     * Creates an IFDSProperty containing the result of this analysis.
     *
     * @param result Maps each exit statement to the facts, which hold after the exit statement.
     * @return An IFDSProperty containing the `result`.
     */
    protected def createPropertyValue(result: Map[Statement, Set[IFDSFact]]): IFDSProperty[IFDSFact]

    /**
     * Determines the nodes, that will be analyzed after some `basicBlock`.
     *
     * @param basicBlock The basic block, that was analyzed before.
     * @return The nodes, that will be analyzed after `basicBlock`.
     */
    protected def nextNodes(basicBlock: BasicBlock): Set[CFGNode]

    /**
     * Checks, if some `node` is the last node.
     *
     * @return True, if `node` is the last node, i.e. there is no next node.
     */
    protected def isLastNode(node: CFGNode): Boolean

    /**
     * If the passed `node` is a catch node, all successors of this catch node are determined.
     *
     * @param node The node.
     * @return If the node is a catch node, all its successors will be returned.
     *         Otherwise, the node itself will be returned.
     */
    protected def skipCatchNode(node: CFGNode): Set[BasicBlock]

    /**
     * Determines the first index of some `basic block`, that will be analyzed.
     *
     * @param basicBlock The basic block.
     * @return The first index of some `basic block`, that will be analyzed.
     */
    protected def firstIndex(basicBlock: BasicBlock): Int

    /**
     * Determines the last index of some `basic block`, that will be analzyed.
     *
     * @param basicBlock The basic block.
     * @return The last index of some `basic block`, that will be analzyed.
     */
    protected def lastIndex(basicBlock: BasicBlock): Int

    /**
     * Determines the index, that will be analyzed after some other `index`.
     *
     * @param index The source index.
     * @return The index, that will be analyzed after `index`.
     */
    protected def nextIndex(index: Int): Int

    /**
     * Gets the first statement of a node, that will be analyzed.
     *
     * @param node The node.
     * @return The first statement of a node, that will be analyzed.
     */
    protected def firstStatement(node: CFGNode)(implicit state: State): Statement

    /**
     * Determines the statement, that will be analyzed after some other `statement`.
     *
     * @param statement The source statement.
     * @return The successor statements
     */
    protected def nextStatements(statement: Statement)(implicit state: State): Set[Statement]

    /**
     * Determines the facts, for which a `callee` is analyzed.
     *
     * @param call The call, which calls `callee`.
     * @param callee The method, which is called by `call`.
     * @param in The facts, which hold before the `call`.
     * @return The facts, for which `callee` will be analyzed.
     */
    protected def callToStartFacts(call: Statement, callee: DeclaredMethod,
                                   in: Set[IFDSFact])(implicit state: State): Set[IFDSFact]

    /**
     * Collects the exit facts of a `callee` and adds them to the `summaryEdges`.
     *
     * @param summaryEdges The current summary edges. They map successor statements of the `call`
     *                     to facts, which hold before they are executed.
     * @param successors The successor of `call`, which is considered.
     * @param call The statement, which calls `callee`.
     * @param callee The method, called by `call`.
     * @param exitFacts Maps exit statements of the `callee` to the facts, which hold after them.
     * @return The summary edges plus the exit to return facts for `callee` and `successor`.
     */
    protected def addExitToReturnFacts(
        summaryEdges: Map[Statement, Set[IFDSFact]],
        successors:   Set[Statement], call: Statement,
        callee:    DeclaredMethod,
        exitFacts: Map[Statement, Set[IFDSFact]]
    )(implicit state: State): Map[Statement, Set[IFDSFact]]

    /**
     * Performs an IFDS analysis for a method-fact-pair.
     *
     * @param entity The method-fact-pair, that will be analyzed.
     * @return An IFDS property mapping from exit statements to the facts valid after these exit
     *         statements. Returns an interim result, if the TAC or call graph of this method or the
     *         IFDS analysis for a callee is still pending.
     */
    def performAnalysis(entity: (DeclaredMethod, IFDSFact)): ProperPropertyComputationResult = {
        val (declaredMethod, sourceFact) = entity

        /*
         * The analysis can only handle single defined methods.
         * If a method is not single defined, this analysis assumes that it does not create any
         * facts.
         */
        if (!declaredMethod.hasSingleDefinedMethod)
            return Result(entity, createPropertyValue(Map.empty));

        val method = declaredMethod.definedMethod
        val declaringClass: ObjectType = method.classFile.thisType

        /*
         * If this is not the method's declaration, but a non-overwritten method in a subtype, do
         * not re-analyze the code.
         */
        if (declaringClass ne declaredMethod.declaringClassType) return baseMethodResult(entity);

        /*
         * Fetch the method's three address code. If it is not present, return an empty interim
         * result.
         */
        val (code, cfg) = propertyStore(method, TACAI.key) match {
            case FinalP(TheTACAI(tac)) ⇒ (tac.stmts, tac.cfg)

            case epk: EPK[Method, TACAI] ⇒
                return InterimResult.forUB(
                    entity,
                    createPropertyValue(Map.empty),
                    Set(epk),
                    _ ⇒ performAnalysis(entity)
                );

            case tac ⇒
                throw new UnknownError(s"can't handle intermediate TACs ($tac)");
        }

        // Start processing at the start of the cfg with the given source fact
        implicit val state: State =
            new State(declaringClass, method, entity, code, cfg, Map(entity → Set.empty), Map())
        val queue = mutable.Queue.empty[(BasicBlock, Set[IFDSFact], Option[Int], Option[Method], Option[IFDSFact])]
        startBlocks(sourceFact, cfg).foreach { start ⇒
            state.incomingFacts += start → Set(sourceFact)
            queue.enqueue((start, Set(sourceFact), None, None, None))
        }
        process(queue)
        createResult()
    }

    /**
     * Creates the current (intermediate) result for the analysis.
     *
     * @return A result containing a map, which maps each exit statement to the facts valid after
     *         the statement, based on the current results. If the analysis is still waiting for its
     *         method's TAC or call graph or the result of another method-fact-pair, an interim
     *         result will be returned.
     *
     */
    protected def createResult()(implicit state: State): ProperPropertyComputationResult = {
        val propertyValue = createPropertyValue(collectResult)
        val dependees = state.pendingIfdsDependees.values ++ state.pendingTacDependees.values
        if (dependees.isEmpty) Result(state.source, propertyValue)
        else InterimResult.forUB(state.source, propertyValue, dependees.toSet, propertyUpdate)
    }

    /**
     * Called, when there is an updated result for a tac, call graph or another method-fact-pair,
     * for which the current analysis is waiting. Re-analyzes the relevant parts of this method and
     * returns the new analysis result.
     *
     * @param eps The new property value.
     * @return The new (interim) result of this analysis.
     */
    protected def propertyUpdate(eps: SomeEPS)(implicit state: State): ProperPropertyComputationResult = {
        (eps: @unchecked) match {
            case FinalE(e: (DeclaredMethod, IFDSFact) @unchecked) ⇒
                reAnalyzeCalls(state.pendingIfdsCallSites(e), e._1.definedMethod, Some(e._2))

            case interimEUBP @ InterimEUBP(e: (DeclaredMethod, IFDSFact) @unchecked,
                ub: IFDSProperty[IFDSFact]) ⇒
                if (ub.flows.values
                    .forall(facts ⇒ facts.size == 1 && facts.forall(_ == nullFact))) {
                    // Do not re-analyze the caller if we only get the null fact.
                    // Update the pendingIfdsDependee entry to the new interim result.
                    state.pendingIfdsDependees +=
                        e → interimEUBP.asInstanceOf[EOptionP[(DeclaredMethod, IFDSFact), IFDSProperty[IFDSFact]]]
                } else
                    reAnalyzeCalls(state.pendingIfdsCallSites(e), e._1.definedMethod, Some(e._2))

            case FinalEP(_: DefinedMethod, _: Callees) ⇒
                reAnalyzebasicBlocks(state.pendingCgCallSites)

            case InterimEUBP(_: DefinedMethod, _: Callees) ⇒
                reAnalyzebasicBlocks(state.pendingCgCallSites)

            case FinalEP(method: Method, _: TACAI) ⇒
                state.pendingTacDependees -= method
                reAnalyzebasicBlocks(state.pendingTacCallSites(declaredMethods(method)))
        }

        createResult()
    }

    /**
     * Checks, if a callee is inside this analysis' context.
     * If not, `callOutsideOfAnalysisContext` is called instead of analyzing the callee.
     * By default, native methods are not inside the analysis context.
     *
     * @param callee The callee.
     * @return True, if the callee is inside the analysis context.
     */
    protected def insideAnalysisContext(callee: DeclaredMethod): Boolean =
        callee.definedMethod.body.isDefined

    /**
     * Called, when some new information is found at the last node of the method.
     * This method can be overwritten by a subclass to perform additional actions.
     *
     * @param nextIn The input facts, which were found.
     * @param oldIn The input facts, which were already known, if present.
     */
    protected def foundNewInformationForLastNode(
        nextIn: Set[IFDSFact],
        oldIn:  Option[Set[IFDSFact]],
        state:  State
    ): Unit = {}

    /**
     * Processes a statement with a call.
     *
     * @param basicBlock The basic block, which contains the `call`.
     * @param call The call statement.
     * @param callees All possible callees.
     * @param in The facts, which hold before the call statement.
     * @param calleeWithUpdateFact If present, the `callees` will only be analyzed with this fact
     *                             instead of the facts returned by callToStartFacts.
     * @return A map, mapping from each successor statement of the `call` to the facts, which hold
     *         at their start.
     */
    protected def handleCall(basicBlock: BasicBlock, call: Statement, callees: SomeSet[Method],
                             in: Set[IFDSFact], calleeWithUpdateFact: Option[IFDSFact])(
        implicit
        state: State
    ): Map[Statement, Set[IFDSFact]] = {
        val successors = nextStatements(call)
        val inputFacts = beforeHandleCall(call, in)
        // Facts valid at the start of each successor
        var summaryEdges: Map[Statement, Set[IFDSFact]] = Map.empty

        /*
         * If calleeWithUpdateFact is present, this means that the basic block already has been
         * analyzed with the `inputFacts`.
         */
        if (calleeWithUpdateFact.isEmpty)
            for (successor ← successors) {
                numberOfCalls.callToReturnFlow += 1
                sumOfInputfactsForCallbacks += in.size
                summaryEdges += successor →
                    propagateNullFact(
                        inputFacts,
                        callToReturnFlow(call, successor, inputFacts, state.source)
                    )
            }

        for (calledMethod ← callees) {
            val callee = declaredMethods(calledMethod)
            if (!insideAnalysisContext(callee)) {
                // Let the concrete analysis decide what to do.
                for {
                    successor ← successors
                } summaryEdges +=
                    successor → (summaryEdges(successor) ++
                        callOutsideOfAnalysisContext(call, callee, successor, in))
            } else {
                val callToStart =
                    if (calleeWithUpdateFact.isDefined) Set(calleeWithUpdateFact.get)
                    else {
                        propagateNullFact(inputFacts, callToStartFacts(call, callee, inputFacts))
                    }
                var allNewExitFacts: Map[Statement, Set[IFDSFact]] = Map.empty
                // Collect exit facts for each input fact separately
                for (fact ← callToStart) {
                    /*
                    * If this is a recursive call with the same input facts, we assume that the
                    * call only produces the facts that are already known. The call site is added to
                    * `pendingIfdsCallSites`, so that it will be re-evaluated if new output facts
                    * become known for the input fact.
                    */
                    if ((calledMethod eq state.method) && fact == state.source._2) {
                        val newDependee =
                            if (state.pendingIfdsCallSites.contains(state.source))
                                state.pendingIfdsCallSites(state.source) +
                                    ((basicBlock, call.index))
                            else Set((basicBlock, call.index))
                        state.pendingIfdsCallSites =
                            state.pendingIfdsCallSites.updated(state.source, newDependee)
                        allNewExitFacts = mergeMaps(allNewExitFacts, collectResult)
                    } else {
                        val e = (callee, fact)
                        val callFlows = propertyStore(e, propertyKey.key)
                            .asInstanceOf[EOptionP[(DeclaredMethod, IFDSFact), IFDSProperty[IFDSFact]]]
                        val oldValue = state.pendingIfdsDependees.get(e)
                        val oldExitFacts: Map[Statement, Set[IFDSFact]] = oldValue match {
                            case Some(ep: InterimEUBP[_, IFDSProperty[IFDSFact]]) ⇒ ep.ub.flows
                            case _                                                ⇒ Map.empty
                        }
                        val exitFacts: Map[Statement, Set[IFDSFact]] = callFlows match {
                            case ep: FinalEP[_, IFDSProperty[IFDSFact]] ⇒
                                if (state.pendingIfdsCallSites.contains(e)
                                    && state.pendingIfdsCallSites(e).nonEmpty) {
                                    val newDependee =
                                        state.pendingIfdsCallSites(e) - ((basicBlock, call.index))
                                    state.pendingIfdsCallSites =
                                        state.pendingIfdsCallSites.updated(e, newDependee)
                                }
                                state.pendingIfdsDependees -= e
                                ep.p.flows
                            case ep: InterimEUBP[_, IFDSProperty[IFDSFact]] ⇒
                                /*
                              * Add the call site to `pendingIfdsCallSites` and
                              * `pendingIfdsDependees` and continue with the facts in the interim
                              * result for now. When the analysis for the callee finishes, the
                              * analysis for this call site will be triggered again.
                              */
                                addIfdsDependee(e, callFlows, basicBlock, call.index)
                                ep.ub.flows
                            case _ ⇒
                                addIfdsDependee(e, callFlows, basicBlock, call.index)
                                Map.empty
                        }
                        // Only process new facts that are not in `oldExitFacts`
                        allNewExitFacts =
                            mergeMaps(
                                allNewExitFacts,
                                filterNewInformation(exitFacts, oldExitFacts, project)
                            )
                        /*
                         * If new exit facts were discovered for the callee-fact-pair, all call
                         * sites depending on this pair have to be re-evaluated. oldValue is
                         * undefined if the callee-fact pair has not been queried before or returned
                         *  a FinalEP.
                         */
                        if (oldValue.isDefined && oldExitFacts != exitFacts) {
                            reAnalyzeCalls(
                                state.pendingIfdsCallSites(e),
                                e._1.definedMethod, Some(e._2)
                            )
                        }
                    }
                }
                summaryEdges = addExitToReturnFacts(summaryEdges, successors, call, callee,
                    allNewExitFacts)
            }
        }
        summaryEdges
    }

    /**
     * This method is called at the beginning of handleCall.
     * A subclass can overwrite this method, to change the input facts of the call.
     *
     * @param call The call statement.
     * @param in The input facts, which hold before the `call`.
     * @return The changed set of input facts.
     */
    protected def beforeHandleCall(call: Statement, in: Set[IFDSFact]): Set[IFDSFact] = in

    /**
     * Gets the call object for a statement that contains a call.
     *
     * @param call The call statement.
     * @return The call object for `call`.
     */
    protected def asCall(call: Stmt[V]): Call[V] = call.astID match {
        case Assignment.ASTID ⇒ call.asAssignment.expr.asFunctionCall
        case ExprStmt.ASTID   ⇒ call.asExprStmt.expr.asFunctionCall
        case _                ⇒ call.asMethodCall
    }

    /**
     * Gets the set of all methods directly callable at some call statement.
     *
     * @param basicBlock The basic block containing the call.
     * @param pc The call's program counter.
     * @param caller The caller, performing the call.
     * @return All methods directly callable at the statement index.
     */
    protected def getCallees(basicBlock: BasicBlock, pc: Int,
                             caller: DeclaredMethod): SomeSet[Method] = {
        val ep = propertyStore(caller, Callees.key)
        ep match {
            case FinalEP(_, p) ⇒ definedMethods(p.directCallees(pc))
            case _ ⇒
                throw new IllegalStateException(
                    "call graph mut be computed before the analysis starts"
                )
        }
    }

    /**
     * Merges two maps that have sets as values.
     *
     * @param map1 The first map.
     * @param map2 The second map.
     * @return A map containing the keys of both maps. Each key is mapped to the union of both maps'
     *         values.
     */
    protected def mergeMaps[S, T](map1: Map[S, Set[T]], map2: Map[S, Set[T]]): Map[S, Set[T]] = {
        var result = map1
        for ((key, values) ← map2) {
            result.get(key) match {
                case Some(resultValues) ⇒
                    if (resultValues.size > values.size)
                        result = result.updated(key, resultValues ++ values)
                    else
                        result = result.updated(key, values ++ resultValues)
                case None ⇒
                    result = result.updated(key, values)
            }
        }
        result
    }

    /**
     * Returns all methods, that can be called from outside the library.
     * The call graph must be computed, before this method may be invoked.
     *
     * @return All methods, that can be called from outside the library.
     */
    protected def methodsCallableFromOutside: Set[DeclaredMethod] =
        declaredMethods.declaredMethods.filter(canBeCalledFromOutside).toSet

    /**
     * Checks, if some `method` can be called from outside the library.
     * The call graph must be computed, before this method may be invoked.
     *
     * @param method The method, which may be callable from outside.
     * @return True, if `method` can be called from outside the library.
     */
    protected def canBeCalledFromOutside(method: DeclaredMethod): Boolean = {
        val FinalEP(_, callers) = propertyStore(method, Callers.key)
        callers.hasCallersWithUnknownContext
    }

    /**
     * Analyzes a queue of BasicBlocks.
     *
     * @param worklist A queue of the following elements:
     *        bb The basic block that will be analyzed.
     *        in New data flow facts found to hold at the beginning of the basic block.
     *        calleeWithUpdateIndex If the basic block is analyzed because there is new information
     *        for a callee, this is the call site's index.
     *        calleeWithUpdate If the basic block is analyzed because there is new information for a
     *        callee, this is the callee.
     *        calleeWithUpdateFact If the basic block is analyzed because there is new information
     *        for a callee with a specific input fact, this is the input fact.
     */
    private def process(worklist: mutable.Queue[(BasicBlock, Set[IFDSFact], Option[Int], Option[Method], Option[IFDSFact])])(implicit state: State): Unit = {
        while (worklist.nonEmpty) {
            val (basicBlock, in, calleeWithUpdateIndex, calleeWithUpdate, calleeWithUpdateFact) =
                worklist.dequeue()
            val oldOut = state.outgoingFacts.getOrElse(basicBlock, Map.empty)
            val nextOut = analyzeBasicBlock(basicBlock, in, calleeWithUpdateIndex, calleeWithUpdate,
                calleeWithUpdateFact)
            val allOut = mergeMaps(oldOut, nextOut).mapValues(facts ⇒ subsume(facts, project))
            state.outgoingFacts = state.outgoingFacts.updated(basicBlock, allOut)

            for (successor ← nextNodes(basicBlock)) {
                if (isLastNode(successor)) {
                    // Re-analyze recursive call sites with the same input fact.
                    val nextOutSuccessors = nextOut.get(successor)
                    if (nextOutSuccessors.isDefined && nextOutSuccessors.get.nonEmpty) {
                        val oldOutSuccessors = oldOut.get(successor)
                        if (oldOutSuccessors.isEmpty || containsNewInformation(
                            nextOutSuccessors.get, oldOutSuccessors.get, project
                        )) {
                            val source = state.source
                            foundNewInformationForLastNode(
                                nextOutSuccessors.get, oldOutSuccessors, state
                            )
                            reAnalyzeCalls(
                                state.pendingIfdsCallSites(source),
                                source._1.definedMethod, Some(source._2)
                            )
                        }
                    }
                } else {
                    val successorBlock = successor.asBasicBlock
                    val nextIn = nextOut.getOrElse(successorBlock, Set.empty)
                    val oldIn = state.incomingFacts.getOrElse(successorBlock, Set.empty)
                    val newIn = notSubsumedBy(nextIn, oldIn, project)
                    val mergedIn =
                        if (nextIn.size > oldIn.size) nextIn ++ oldIn
                        else oldIn ++ nextIn
                    state.incomingFacts =
                        state.incomingFacts.updated(successorBlock, subsume(mergedIn, project))
                    /*
                     * Only process the successor with new facts.
                     * It is analyzed at least one time because of the null fact.
                     */
                    if (newIn.nonEmpty) worklist.enqueue((successorBlock, newIn, None, None, None))
                }
            }
        }
    }

    /**
     * Computes for one basic block the facts valid on each CFG edge leaving the block if `sources`
     * held before the block.
     *
     * @param basicBlock The basic block, that will be analyzed.
     * @param in The facts, that hold before the block.
     * @param calleeWithUpdateIndex If the basic block is analyzed because there is new information
     *                              for a callee, this is the call site's index.
     * @param calleeWithUpdate If the basic block is analyzed because there is new information for
     *                         a callee, this is the callee.
     * @param calleeWithUpdateFact If the basic block is analyzed because there is new information
     *                             for a callee with a specific input fact, this is the input fact.
     * @return A map, mapping each successor node to its input facts. Instead of catch nodes, this
     *         map contains their handler nodes.
     */
    private def analyzeBasicBlock(basicBlock: BasicBlock, in: Set[IFDSFact],
                                  calleeWithUpdateIndex: Option[Int],
                                  calleeWithUpdate:      Option[Method],
                                  calleeWithUpdateFact:  Option[IFDSFact])(
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
         *                 If `index` equals `calleeWithUpdateIndex`, only `calleeWithUpdate` will
         *                 be returned.
         *         calleeFact: If `index` equals `calleeWithUpdateIndex`, only
         *         `calleeWithUpdateFact` will be returned, None otherwise.
         */
        def collectInformation(index: Int): (Statement, Option[SomeSet[Method]], Option[IFDSFact]) = {
            val stmt = state.code(index)
            val statement = Statement(state.method, basicBlock, stmt, index, state.code, state.cfg)
            val calleesO =
                if (calleeWithUpdateIndex.contains(index)) calleeWithUpdate.map(Set(_))
                else getCalleesIfCallStatement(basicBlock, index)
            val calleeFact =
                if (calleeWithUpdateIndex.contains(index)) calleeWithUpdateFact
                else None
            (statement, calleesO, calleeFact)
        }

        val last = lastIndex(basicBlock)
        var flows: Set[IFDSFact] = in
        var index = firstIndex(basicBlock)

        // Iterate over all statements but the last one, only keeping the resulting DataFlowFacts.
        while (index != last) {
            val (statement, calleesO, calleeFact) = collectInformation(index)
            val next = nextIndex(index)
            flows =
                if (calleesO.isEmpty) {
                    val successor = Statement(state.method, basicBlock, state.code(next), next,
                        state.code, state.cfg)
                    numberOfCalls.normalFlow += 1
                    sumOfInputfactsForCallbacks += in.size
                    normalFlow(statement, successor, flows)
                } else
                    // Inside a basic block, we only have one successor --> Take the head
                    handleCall(basicBlock, statement, calleesO.get, flows, calleeFact).values.head
            index = next
        }

        // Analyze the last statement for each possible successor statement.
        val (statement, calleesO, callFact) = collectInformation(last)
        var result: Map[CFGNode, Set[IFDSFact]] =
            if (calleesO.isEmpty) {
                var result: Map[CFGNode, Set[IFDSFact]] = Map.empty
                for (node ← nextNodes(basicBlock)) {
                    numberOfCalls.normalFlow += 1
                    sumOfInputfactsForCallbacks += in.size
                    result += node → normalFlow(statement, firstStatement(node), flows)
                }
                result
            } else handleCall(basicBlock, statement, calleesO.get, flows, callFact)
                .map(entry ⇒ entry._1.node → entry._2)

        // Propagate the null fact.
        result = result.map(result ⇒ result._1 → propagateNullFact(in, result._2))
        result
    }

    /**
     * Re-analyzes some basic blocks.
     *
     * @param basicBlocks The basic blocks, that will be re-analyzed.
     */
    private def reAnalyzebasicBlocks(basicBlocks: Set[BasicBlock])(implicit state: State): Unit = {
        val queue: mutable.Queue[(BasicBlock, Set[IFDSFact], Option[Int], Option[Method], Option[IFDSFact])] = mutable.Queue.empty
        for (bb ← basicBlocks) queue.enqueue((bb, state.incomingFacts(bb), None, None, None))
        process(queue)
    }

    /**
     * Re-analyzes some call sites with respect to one specific callee.
     *
     * @param callSites The call sites, which are analyzed.
     * @param callee The callee, which will be considered at the `callSites`.
     * @param fact If defined, the `callee` will only be analyzed for this fact.
     */
    private def reAnalyzeCalls(callSites: Set[(BasicBlock, Int)], callee: Method,
                               fact: Option[IFDSFact])(implicit state: State): Unit = {
        val queue: mutable.Queue[(BasicBlock, Set[IFDSFact], Option[Int], Option[Method], Option[IFDSFact])] = mutable.Queue.empty
        for ((block, index) ← callSites)
            queue.enqueue((block, state.incomingFacts(block), Some(index), Some(callee), fact))
        process(queue)
    }

    /**
     * Gets the set of all methods possibly called at some statement.
     *
     * @param basicBlock The basic block containing the statement.
     * @param index The statement's index.
     * @return All methods possibly called at the statement index or None, if the statement does not
     *         contain a call.
     */
    private def getCalleesIfCallStatement(basicBlock: BasicBlock, index: Int)(
        implicit
        state: State
    ): Option[SomeSet[Method]] = {
        val statement = state.code(index)
        val pc = statement.pc
        statement.astID match {
            case StaticMethodCall.ASTID | NonVirtualMethodCall.ASTID | VirtualMethodCall.ASTID ⇒
                Some(getCallees(basicBlock, pc, state.source._1))
            case Assignment.ASTID | ExprStmt.ASTID ⇒ getExpression(statement).astID match {
                case StaticFunctionCall.ASTID | NonVirtualFunctionCall.ASTID |
                    VirtualFunctionCall.ASTID ⇒
                    Some(getCallees(basicBlock, pc, state.source._1))
                case _ ⇒ None
            }
            case _ ⇒ None
        }
    }

    /**
     * Maps some declared methods to their defined methods.
     *
     * @param declaredMethods Some declared methods.
     * @return All defined methods of `declaredMethods`.
     */
    private def definedMethods(declaredMethods: Iterator[DeclaredMethod]): SomeSet[Method] = {
        val result = scala.collection.mutable.Set.empty[Method]
        declaredMethods.filter(declaredMethod ⇒ declaredMethod.hasSingleDefinedMethod ||
            declaredMethod.hasMultipleDefinedMethods).
            foreach(declaredMethod ⇒ declaredMethod
                .foreachDefinedMethod(defineMethod ⇒ result.add(defineMethod)))
        result
    }

    /**
     * Retrieves the expression of an assignment or expression statement.
     *
     * @param statement The statement. Must be an Assignment or ExprStmt.
     * @return The statement's expression.
     */
    private def getExpression(statement: Stmt[V]): Expr[V] = statement.astID match {
        case Assignment.ASTID ⇒ statement.asAssignment.expr
        case ExprStmt.ASTID   ⇒ statement.asExprStmt.expr
        case _                ⇒ throw new UnknownError("Unexpected statement")
    }

    /**
     * If `from` contains a null fact, it will be added to `to`.
     *
     * @param from The set, which may contain the null fact initially.
     * @param to The set, to which the null fact may be added.
     * @return `to` with the null fact added, if it is contained in `from`.
     */
    private def propagateNullFact(from: Set[IFDSFact], to: Set[IFDSFact]): Set[IFDSFact] = {
        if (from.contains(nullFact)) to + nullFact
        else to
    }

    /**
     * Adds a method-fact-pair as to the IFDS call sites and dependees.
     *
     * @param entity The method-fact-pair.
     * @param calleeProperty The property, that was returned for `entity`.
     * @param callBB The basic block of the call site.
     * @param callIndex The index of the call site.
     */
    private def addIfdsDependee(
        entity:         (DeclaredMethod, IFDSFact),
        calleeProperty: EOptionP[(DeclaredMethod, IFDSFact), IFDSProperty[IFDSFact]],
        callBB:         BasicBlock,
        callIndex:      Int
    )(implicit state: State): Unit = {
        val callSites = state.pendingIfdsCallSites
        state.pendingIfdsCallSites = callSites.updated(
            entity,
            callSites.getOrElse(entity, Set.empty) + ((callBB, callIndex))
        )
        state.pendingIfdsDependees += entity → calleeProperty
    }

    /**
     * This method will be called if a non-overwritten declared method in a sub type shall be
     * analyzed. Analyzes the defined method of the supertype instead.
     *
     * @param source A pair consisting of the declared method of the subtype and an input fact.
     * @return The result of the analysis of the defined method of the supertype.
     */
    private def baseMethodResult(source: (DeclaredMethod, IFDSFact)): ProperPropertyComputationResult = {

        def c(eps: SomeEOptionP): ProperPropertyComputationResult = eps match {
            case FinalP(p) ⇒ Result(source, p)

            case ep @ InterimUBP(ub: Property) ⇒
                InterimResult.forUB(source, ub, Set(ep), c)

            case epk ⇒
                InterimResult.forUB(source, createPropertyValue(Map.empty), Set(epk), c)
        }
        c(propertyStore((declaredMethods(source._1.definedMethod), source._2), propertyKey.key))
    }
}

object AbstractIFDSAnalysis {

    /**
     * The type of the TAC domain.
     */
    type V = DUVar[ValueInformation]

    /**
     * When true, the cross product of exit and successor in returnFLow will be optimized.
     */
    var OPTIMIZE_CROSS_PRODUCT_IN_RETURN_FLOW: Boolean = true

    /**
     * Converts the index of a method's formal parameter to its tac index in the method's scope and
     * vice versa.
     *
     * @param index The index of a formal parameter in the parameter list or of a variable.
     * @param isStaticMethod States, whether the method is static.
     * @return A tac index if a parameter index was passed or a parameter index if a tac index was
     *         passed.
     */
    def switchParamAndVariableIndex(index: Int, isStaticMethod: Boolean): Int =
        (if (isStaticMethod) -2 else -1) - index

}

/**
 * A statement that is passed to the concrete analysis.
 *
 * @param method The method containing the statement.
 * @param node The basic block containing the statement.
 * @param stmt The TAC statement.
 * @param index The index of the Statement in the code.
 * @param code The method's TAC code.
 * @param cfg The method's CFG.
 */
case class Statement(
        method: Method,
        node:   CFGNode,
        stmt:   Stmt[V],
        index:  Int,
        code:   Array[Stmt[V]],
        cfg:    CFG[Stmt[V], TACStmts[V]]
) {

    override def hashCode(): Int = method.hashCode() * 31 + index

    override def equals(o: Any): Boolean = o match {
        case s: Statement ⇒ s.index == index && s.method == method
        case _            ⇒ false
    }

    override def toString: String = s"${method.toJava}"

}

/**
 * Contains int variables, which count, how many times some method was called.
 */
class NumberOfCalls {
    var normalFlow = 0
    var callFlow = 0
    var returnFlow = 0
    var callToReturnFlow = 0
}

abstract class IFDSAnalysis[IFDSFact <: AbstractIFDSFact] extends FPCFLazyAnalysisScheduler {

    final override type InitializationData = AbstractIFDSAnalysis[IFDSFact]

    def property: IFDSPropertyMetaInformation[IFDSFact]

    final override def derivesLazily: Some[PropertyBounds] = Some(PropertyBounds.ub(property))

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)

    override val uses: Set[PropertyBounds] = Set(PropertyBounds.finalP(TACAI))

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    final override def register(p: SomeProject, ps: PropertyStore,
                                analysis: AbstractIFDSAnalysis[IFDSFact]): FPCFAnalysis = {
        ps.registerLazyPropertyComputation(property.key, analysis.performAnalysis)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {
        val ifdsAnalysis = analysis.asInstanceOf[AbstractIFDSAnalysis[IFDSFact]]
        for (e ← ifdsAnalysis.entryPoints) { ps.force(e, ifdsAnalysis.propertyKey.key) }
    }

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore,
                                      analysis: FPCFAnalysis): Unit = {}

}

abstract class AbsractIFDSAnalysisRunner {

    protected def analysisClass: IFDSAnalysis[_]

    protected def printAnalysisResults(analysis: AbstractIFDSAnalysis[_], ps: PropertyStore): Unit

    protected def run(debug: Boolean, useL2: Boolean, delay: Boolean, evalSchedulingStrategies: Boolean,
                      evaluationFile: Option[File]): Unit = {

        if (debug) {
            PropertyStore.updateDebug(true)
        }

        def evalProject(p: SomeProject): (Milliseconds, NumberOfCalls, Option[Object], Long) = {
            if (useL2) {
                p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
                    case None               ⇒ Set(classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[_]])
                    case Some(requirements) ⇒ requirements + classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
                }
            } else {
                p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
                    case None               ⇒ Set(classOf[PrimitiveTACAIDomain])
                    case Some(requirements) ⇒ requirements + classOf[PrimitiveTACAIDomain]
                }
            }

            val ps = p.get(PropertyStoreKey)
            var analysisTime: Milliseconds = Milliseconds.None
            p.get(RTACallGraphKey)
            println("Start: "+new java.util.Date)
            org.opalj.util.gc()
            if (AbstractIFDSAnalysisRunner.MEASURE_MEMORY)
                JOptionPane.showMessageDialog(null, "Call Graph finished")
            val analysis =
                time {
                    p.get(FPCFAnalysesManagerKey).runAll(analysisClass)._2
                }(t ⇒ analysisTime = t.toMilliseconds).collect {
                    case (_, a: AbstractIFDSAnalysis[_]) ⇒ a
                }.head
            if (AbstractIFDSAnalysisRunner.MEASURE_MEMORY)
                JOptionPane.showMessageDialog(null, "Analysis finished")

            printAnalysisResults(analysis, ps)
            println(s"The analysis took $analysisTime.")
            println(
                ps.statistics.iterator.map(_.toString()).toList
                    .sorted
                    .mkString("PropertyStore Statistics:\n\t", "\n\t", "\n")
            )
            (analysisTime, analysis.numberOfCalls, additionalEvaluationResult(analysis), analysis.sumOfInputfactsForCallbacks)
        }

        val p = Project(bytecode.RTJar)

        if (delay) {
            println("Sleeping for three seconds.")
            Thread.sleep(3000)
        }

        if (evalSchedulingStrategies) {
            val results = for {
                i ← 1 to AbstractIFDSAnalysisRunner.NUM_EXECUTIONS_EVAL_SCHEDULING_STRATEGIES
                strategy ← PKESequentialPropertyStore.Strategies
            } yield {
                println(s"Round: $i - $strategy")
                val strategyValue = ConfigValueFactory.fromAnyRef(strategy)
                val newConfig =
                    p.config.withValue(PKESequentialPropertyStore.TasksManagerKey, strategyValue)
                val evaluationResult = evalProject(Project.recreate(p, newConfig))
                org.opalj.util.gc()
                (i, strategy, evaluationResult._1, evaluationResult._2)
            }
            println(results.mkString("AllResults:\n\t", "\n\t", "\n"))
            if (evaluationFile.nonEmpty) {
                val pw = new PrintWriter(evaluationFile.get)
                PKESequentialPropertyStore.Strategies.foreach { strategy ⇒
                    val strategyResults = results.filter(_._2 == strategy)
                    val averageTime = strategyResults.map(_._3.timeSpan).sum / strategyResults.size
                    val (normalFlow, callToStart, exitToReturn, callToReturn) = computeAverageNumberCalls(strategyResults.map(_._4))
                    pw.println(s"Strategy $strategy:")
                    pw.println(s"Average time: ${averageTime}ms")
                    pw.println(s"Average calls of normalFlow: $normalFlow")
                    pw.println(s"Average calls of callToStart: $callToStart")
                    pw.println(s"Average calls of exitToReturn: $exitToReturn")
                    pw.println(s"Average calls of callToReturn: $callToReturn")
                    pw.println()
                }
                pw.close()
            }
        } else {
            var times = Seq.empty[Milliseconds]
            var numberOfCalls = Seq.empty[NumberOfCalls]
            var sumsOfInputFactsForCallbacks = Seq.empty[Long]
            var additionalEvaluationResults = Seq.empty[Object]
            for {
                _ ← 1 to AbstractIFDSAnalysisRunner.NUM_EXECUTIONS
            } {
                val evaluationResult = evalProject(Project.recreate(p))
                val additionalEvaluationResult = evaluationResult._3
                times :+= evaluationResult._1
                numberOfCalls :+= evaluationResult._2
                sumsOfInputFactsForCallbacks :+= evaluationResult._4
                if (additionalEvaluationResult.isDefined)
                    additionalEvaluationResults :+= additionalEvaluationResult.get
            }
            if (evaluationFile.nonEmpty) {
                val (normalFlow, callFlow, returnFlow, callToReturnFlow) = computeAverageNumberCalls(numberOfCalls)
                val time = times.map(_.timeSpan).sum / times.size
                val sumOfInputFactsForCallbacks = sumsOfInputFactsForCallbacks.sum / sumsOfInputFactsForCallbacks.size
                val pw = new PrintWriter(evaluationFile.get)
                pw.println(s"Average time: ${time}ms")
                pw.println(s"Average calls of normalFlow: $normalFlow")
                pw.println(s"Average calls of callFlow: $callFlow")
                pw.println(s"Average calls of returnFlow: $returnFlow")
                pw.println(s"Average calls of callToReturnFlow: $callToReturnFlow")
                pw.println(s"Sum of input facts for callbacks: $sumOfInputFactsForCallbacks")
                if (additionalEvaluationResults.nonEmpty)
                    writeAdditionalEvaluationResultsToFile(pw, additionalEvaluationResults)
                pw.close()
            }
        }
    }

    protected def additionalEvaluationResult(analysis: AbstractIFDSAnalysis[_]): Option[Object] = None

    protected def writeAdditionalEvaluationResultsToFile(
        writer:                      PrintWriter,
        additionalEvaluationResults: Seq[Object]
    ): Unit = {}

    protected def canBeCalledFromOutside(method: DeclaredMethod, propertyStore: PropertyStore): Boolean =
        propertyStore(method, Callers.key) match {
            // This is the case, if the method may be called from outside the library.
            case FinalEP(_, p: Callers) ⇒ p.hasCallersWithUnknownContext
            case _ ⇒
                throw new IllegalStateException(
                    "call graph mut be computed before the analysis starts"
                )
        }

    private def computeAverageNumberCalls(numberOfCalls: Seq[NumberOfCalls]): (Int, Int, Int, Int) = {
        val length = numberOfCalls.length
        val normalFlow = numberOfCalls.map(_.normalFlow).sum / length
        val callFlow = numberOfCalls.map(_.callFlow).sum / length
        val returnFlow = numberOfCalls.map(_.returnFlow).sum / length
        val callToReturnFlow = numberOfCalls.map(_.callToReturnFlow).sum / length
        (normalFlow, callFlow, returnFlow, callToReturnFlow)
    }
}

object AbstractIFDSAnalysisRunner {
    var NUM_EXECUTIONS = 10
    var NUM_EXECUTIONS_EVAL_SCHEDULING_STRATEGIES = 2
    var MEASURE_MEMORY = false
}