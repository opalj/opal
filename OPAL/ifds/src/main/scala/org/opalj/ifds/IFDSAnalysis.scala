/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ifds

import scala.collection.{Set => SomeSet}
import scala.collection.mutable

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPK
import org.opalj.fpcf.SomeEPS
import org.opalj.ifds.Dependees.Getter

/**
 * A container for a data flow fact, holding also information for unbalanced returns.
 *
 * @tparam Fact The type of flow facts that are tracked by the concrete analysis.
 *
 * @param fact the actual flow fact.
 * @param isUnbalancedReturn whether this fact was created by an unbalanced return.
 * @param callChain if unbalanced return, the current call chain.
 *
 * @author Marc Clement
 */
class IFDSFact[Fact <: AbstractIFDSFact, S <: Statement[_, _]](
        val fact:               Fact,
        val isUnbalancedReturn: Boolean,
        val callStmt:           Option[S],
        val callChain:          Option[Seq[Callable]]
) {

    def this(fact: Fact) = {
        this(fact, false, None, None)
    }

    // ignore call chain for hashing/equality
    // call chain does not affect end summaries, they are only affected by fact and index
    // thus, ignore call chain such that property store works accordingly

    override def equals(obj: Any): Boolean = obj match {
        case other: IFDSFact[Fact @unchecked, S @unchecked] =>
            this.eq(other) ||
                (this.hashCode() == other.hashCode()
                    && this.fact == other.fact
                    && this.isUnbalancedReturn == other.isUnbalancedReturn
                    && this.callStmt == other.callStmt)
        case _ => false
    }

    override def hashCode(): Int = {
        64 * (fact.hashCode() + isUnbalancedReturn.hashCode() + callStmt.hashCode())
    }
}

/**
 * This represents a map of entities to the worklist items that depend on the given entity
 * @tparam WorklistItem The type of the work list item that depends on a given entity
 */
case class Dependees[WorklistItem]() {

    case class Dependee(eOptionP: SomeEOptionP, worklist: Set[WorklistItem] = Set.empty)

    var dependees = Map.empty[SomeEPK, Dependee]
    def get(
        entity:      Entity,
        propertyKey: PropertyKey[Property]
    )(implicit propertyStore: PropertyStore, worklistItem: WorklistItem): SomeEOptionP = {
        val epk = EPK(entity, propertyKey)
        val dependee = dependees.get(epk) match {
            case Some(dependee) => Dependee(dependee.eOptionP, dependee.worklist + worklistItem)
            case None           => Dependee(propertyStore(epk), Set(worklistItem))
        }
        if (dependee.eOptionP.isRefinable) dependees += epk -> dependee
        dependee.eOptionP
    }

    def forResult: Set[SomeEOptionP] = {
        dependees.values.map(_.eOptionP).toSet
    }
    def takeWork(epk: SomeEPK): Set[WorklistItem] = {
        val dependee = dependees(epk)
        dependees -= epk
        dependee.worklist
    }

    def getter()(implicit propertyStore: PropertyStore, work: WorklistItem): Getter =
        (entity: Entity, propertyKey: PropertyKey[Property]) => get(entity, propertyKey)
}

object Dependees {
    type Getter = (Entity, PropertyKey[Property]) => SomeEOptionP
}

/**
 * Keeps book of the path edges.
 * An entry of (statement, fact) means an edge (s0, source fact) -> (statement, fact) exists,
 * that is the fact reaches the statement as an input.
 * Source fact is the fact within the analysis entity.
 */
case class PathEdges[Fact <: AbstractIFDSFact, S <: Statement[_ <: C, _], C <: AnyRef](
        subsumes: (Set[Fact], Fact) => Boolean
) {
    /**
     * Left denotes a single relevant predecessor
     * Right denotes several relevant predecessors
     */
    var edges = Map.empty[S, Either[Set[Fact], Map[S, Set[Fact]]]]

    /**
     * Add the edge (s0, source fact) -> (statement, fact) to the path edges.
     * Optionally give a predecessor for the statement. This is used for phi statements
     * to distinguish the input flow and merge the facts later.
     * @param statement the destination statement of the edge
     * @param predecessor the predecessor of the statement.
     * @return whether the edge was new
     */
    def add(statement: S, fact: Fact, predecessor: Option[S] = None): Boolean = {
        edges.get(statement) match {
            case None =>
                predecessor match {
                    case Some(predecessor) =>
                        edges = edges.updated(statement, Right(Map(predecessor -> Set(fact))))
                    case None =>
                        edges = edges.updated(statement, Left(Set(fact)))
                }
                true
            case Some(Left(existingFacts)) =>
                if (predecessor.isDefined)
                    throw new IllegalArgumentException(s"$statement does not accept a predecessor")
                if (isNew(existingFacts, fact)) {
                    edges = edges.updated(statement, Left(existingFacts + fact))
                    true
                } else false
            case Some(Right(existingFacts)) =>
                predecessor match {
                    case None => throw new IllegalArgumentException(s"$statement requires a predecessor")
                    case Some(predecessor) =>
                        existingFacts.get(statement) match {
                            case Some(existingPredecessorFacts) =>
                                if (isNew(existingPredecessorFacts, fact)) {
                                    edges = edges.updated(
                                        statement,
                                        Right(existingFacts.updated(predecessor, existingPredecessorFacts + fact))
                                    )
                                    true
                                } else false
                            case None =>
                                edges =
                                    edges.updated(statement, Right(existingFacts.updated(predecessor, Set(fact))))
                                true
                        }
                }
        }
    }

    private def isNew(existingFacts: Set[Fact], newFact: Fact): Boolean = {
        !existingFacts.contains(newFact) && !subsumes(existingFacts, newFact)
    }

    /**
     * @return The edges reaching statement if any. In case the statement minds about predecessors it is a map with an
     *         entry for each predecessor
     */
    def get(statement: S): Option[Either[Set[Fact], Map[S, Set[Fact]]]] = edges.get(statement)

    def debugData: Map[S, Set[Fact]] =
        edges.foldLeft(Map.empty[S, Set[Fact]])((result, elem) => {
            val facts: Set[Fact] = elem._2 match {
                case Right(facts) => facts.foldLeft(Set.empty[Fact])(_ ++ _._2)
                case Left(facts)  => facts
            }
            result.updated(elem._1, result.getOrElse(elem._1, Set.empty) ++ facts)
        })
}

/**
 * The state of the analysis. For each method and source fact, there is a separate state.
 *
 * @param source The callable and input fact for which the callable is analyzed.
 * @param subsumes The subsuming function, return whether a new fact is subsume by the existing ones
 */
protected class IFDSState[Fact <: AbstractIFDSFact, C <: AnyRef, S <: Statement[_ <: C, _], WorklistItem](
        val source: (C, IFDSFact[Fact, S]),
        subsumes:   (Set[Fact], Fact) => Boolean
) {
    val dependees: Dependees[WorklistItem] = Dependees()
    val pathEdges: PathEdges[Fact, S, C] = PathEdges(subsumes)
    var endSummaries: Set[(S, Fact)] = Set.empty
    var selfDependees: Set[WorklistItem] = Set.empty
}

/**
 * Contains int variables, which count how many times some method was called.
 */
case class Statistics(
        var normalFlow:       Int = 0,
        var callFlow:         Int = 0,
        var returnFlow:       Int = 0,
        var callToReturnFlow: Int = 0,
        // TODO unbalanced return flow
        var subsumeTries: Int = 0,
        var subsumptions: Int = 0
)

/**
 * The IFDS analysis framework
 *
 * @param ifdsProblem the problem class that handles the actual solving of the problem
 * @param propertyKey Provides the concrete property key that must be unique for every distinct concrete analysis and
 *                    the lower bound for the IFDSProperty.
 * @tparam Fact the generated facts
 *
 * @author Marc Clement
 */
class IFDSAnalysis[Fact <: AbstractIFDSFact, C <: AnyRef, S <: Statement[_ <: C, _]](
        val project:     SomeProject,
        val ifdsProblem: IFDSProblem[Fact, C, S],
        val propertyKey: IFDSPropertyMetaInformation[S, Fact]
) extends FPCFAnalysis {
    private type WorklistItem = (Option[S], IFDSFact[Fact, S], Option[S]) // statement, fact, predecessor
    private type Worklist = mutable.Queue[WorklistItem]
    type State = IFDSState[Fact, C, S, WorklistItem]

    implicit var statistics: Statistics = Statistics()
    val icfg: ICFG[C, S] = ifdsProblem.icfg

    /**
     * Performs an IFDS analysis for a method-fact-pair.
     *
     * @param entity The function-fact-pair, that will be analyzed.
     * @return An IFDS property mapping from exit statements to the facts valid after these exit
     *         statements. Returns an interim result, if the TAC or call graph of this method or the
     *         IFDS analysis for a callee is still pending.
     */
    def performAnalysis(entity: (C, IFDSFact[Fact, S])): ProperPropertyComputationResult = {
        val (function, sourceFact) = entity

        // Start processing at the start of the icfg with the given source fact
        implicit val state: State = new IFDSState[Fact, C, S, WorklistItem](entity, subsumes)
        implicit val queue: Worklist = mutable.Queue
            .empty[WorklistItem]

        // if method is analyzed for unbalanced return fact, start with next statements after call statement
        val startStatements: Set[(Option[S], Option[S])] = if (sourceFact.isUnbalancedReturn) {
            val call = sourceFact.callStmt.get
            val startStmts: Set[(Option[S], Option[S])] =
                icfg.nextStatements(call).map(stmt => (Some(stmt), Some(call)))
            if (startStmts.isEmpty) Set((None, Some(call))) // unbalanced return to call with index 0
            else startStmts
        } else icfg.startStatements(function).map(stmt => (Some(stmt), None))

        startStatements.foreach(start => {
            if (start._1.isDefined) state.pathEdges.add(start._1.get, sourceFact.fact) // ifds line 2
            queue.enqueue((start._1, sourceFact, start._2)) // ifds line 3
        })
        process()
        createResult()
    }

    /**
     * Creates the current (intermediate) result for the analysis.
     *
     * @return A result containing a map, which maps each exit statement to the facts valid after
     *         the statement, based on the current results. If the analysis is still waiting for its
     *         method's TAC or call graph or the result of another method-fact-pair, an interim
     *         result will be returned.
     */
    private def createResult()(implicit state: State): ProperPropertyComputationResult = {
        val propertyValue = createPropertyValue()
        val dependees = state.dependees.forResult
        if (dependees.isEmpty) Result(state.source, propertyValue)
        else InterimResult.forUB(state.source, propertyValue, dependees, propertyUpdate)
    }

    /**
     * Creates an IFDSProperty containing the result of this analysis, which
     * maps each exit statement to the facts that hold after the exit statement.
     *
     * @return An IFDSProperty containing the `result`.
     */
    private def createPropertyValue()(implicit state: State): IFDSProperty[S, Fact] = {
        if (project.config.getBoolean(ConfigKeyPrefix + "debug"))
            propertyKey.create(collectResult, state.pathEdges.debugData)
        else
            propertyKey.create(collectResult)
    }

    /**
     * Collects the facts valid at all exit nodes based on the current results.
     *
     * @return A map, mapping from each exit statement to the facts, which flow into exit statement.
     */
    private def collectResult(implicit state: State): Map[S, Set[Fact]] = {
        state.endSummaries.foldLeft(Map.empty[S, Set[Fact]])(
            (result, entry) =>
                result.updated(entry._1, result.getOrElse(entry._1, Set.empty[Fact]) + entry._2)
        )
    }

    /**
     * Called, when there is an updated result for a tac, call graph or another method-fact-pair,
     * for which the current analysis is waiting. Re-analyzes the relevant parts of this method and
     * returns the new analysis result.
     *
     * @param eps The new property value.
     * @return The new (interim) result of this analysis.
     */
    private def propertyUpdate(
        eps: SomeEPS
    )(implicit state: State): ProperPropertyComputationResult = {
        implicit val queue: mutable.Queue[WorklistItem] = mutable.Queue()
        state.dependees.takeWork(eps.toEPK).foreach(queue.enqueue)
        process()
        createResult()
    }

    /**
     * Analyzes a queue of BasicBlocks.
     *
     * @param worklist the current worklist that needs to be processed
     */
    private def process()(implicit state: State, worklist: Worklist): Unit = {
        while (worklist.nonEmpty) { // ifds line 10
            implicit val work: (Option[S], IFDSFact[Fact, S], Option[S]) = worklist.dequeue() // ifds line 11
            val (statement, in, predecessor) = work
            statement match {
                case Some(stmt) =>
                    icfg.getCalleesIfCallStatement(stmt) match {
                        case Some(callees) => handleCall(stmt, callees, in.fact) // ifds line 13
                        case None          => handleOther(stmt, in.fact, predecessor) // ifds line 33
                    }
                case None if icfg.isExitStatement(predecessor.get) =>
                    handleExit(predecessor.get, in.fact) // ifds line 21
                case _ => // last statement was no exit statement, should not happen
            }
        }
    }

    /**
     * Processes a statement with a call.
     *
     * @param call The call statement.
     * @param callees All possible callees.
     * @param in The facts, which hold before the call statement.
     *                             instead of the facts returned by callToStartFacts.
     * @return A map, mapping from each successor statement of the `call` to the facts, which hold
     *         at their start.
     */
    private def handleCall(
        call:    S,
        callees: SomeSet[_ <: C],
        in:      Fact
    )(
        implicit
        state:    State,
        worklist: Worklist,
        work:     WorklistItem
    ): Unit = {
        val successors = {
            val successors = icfg.nextStatements(call).map(Some(_))
            if (successors.isEmpty) {
                Set(None)
            } else {
                successors
            }
        }
        val existingCallChain = state.source._2.callChain.getOrElse(Seq.empty)
        for (callee <- callees) {
            val callFlowHandler = ifdsProblem.outsideAnalysisContextCall(callee) match {
                case Some(outsideAnalysisHandler) =>
                    outsideAnalysisHandler
                case None => (call: S, successor: Option[S], in: Fact, _: Seq[Callable], _: Getter) =>
                    concreteCallFlow(call, callee, in, successor)
            }
            for {
                successor <- successors
                out <- callFlowHandler(
                    call,
                    successor,
                    in,
                    existingCallChain,
                    state.dependees.getter()
                ) // ifds line 17 (only summary edges)
            } {
                propagate(successor, out, call) // ifds line 18
            }
        }
        for {
            successor <- successors
            out <- callToReturnFlow(call, in, successor, existingCallChain) // ifds line 17 (without summary edge propagation)
        } {
            propagate(successor, out, call) // ifds line 18
        }
    }

    private def concreteCallFlow(call: S, callee: C, in: Fact, successor: Option[S])(
        implicit
        state: State,
        work:  WorklistItem
    ): Set[Fact] = {
        var result = Set.empty[Fact]
        val unbCallChain = state.source._2.callChain.getOrElse(Seq.empty)
        // callee could have multiple return statements
        // thus, backward analysis could have multiple entry points to a callee
        val entryFacts =
            icfg.startStatements(callee).flatMap(entry => callFlow(entry, in, call, callee))
        val entryIFDSFacts = entryFacts.map(new IFDSFact(_)) // obviously no unbalanced return since this is a call
        for (entryFact <- entryIFDSFacts) { // ifds line 14
            val newEntity = (callee, entryFact)
            val exitFacts: Map[S, Set[Fact]] = if (newEntity == state.source) {
                // handle self dependency on our own because property store can't handle it
                state.selfDependees += work
                collectResult(state)
            } else {
                // handle all other dependencies using property store
                val callFlows = state.dependees
                    .get(newEntity, propertyKey.key)
                    .asInstanceOf[EOptionP[(C, IFDSFact[Fact, S]), IFDSProperty[S, Fact]]]
                callFlows match {
                    case ep: FinalEP[_, IFDSProperty[S, Fact]] =>
                        ep.p.flows
                    case ep: InterimEUBP[_, IFDSProperty[S, Fact]] =>
                        ep.ub.flows
                    case _ =>
                        Map.empty
                }
            }
            for {
                (exitStatement, exitStatementFacts) <- exitFacts // ifds line 15.2
                exitStatementFact <- exitStatementFacts // ifds line 15.3
            } {
                result ++= returnFlow(exitStatement, exitStatementFact, call, in, successor, unbCallChain)
            }
        }
        result
    }

    private def handleExit(
        statement: S,
        in:        Fact
    )(implicit state: State, worklist: Worklist, work: WorklistItem): Unit = {
        // analysis might create new facts at exit
        val unbCallChain = state.source._2.callChain.getOrElse(Seq.empty)
        val createdFact = ifdsProblem.createFlowFactAtExit(statement.callable, in, unbCallChain)
        val newEdges =
            if (createdFact.isDefined) Set((statement, in), (statement, createdFact.get))
            else Set((statement, in))

        for (newEdge <- newEdges) {
            if (!state.endSummaries.contains(newEdge)) {
                state.endSummaries += ((statement, in)) // ifds line 21.1
                state.selfDependees.foreach(selfDependee => worklist.enqueue(selfDependee))

                if (ifdsProblem.enableUnbalancedReturns &&
                    ifdsProblem.shouldPerformUnbalancedReturn(state.source)) {
                    handleUnbalancedReturn(statement, in)
                }
            }
        }
        // ifds lines 22 - 31 are handled by the dependency propagation of the property store
        // except for self dependencies which are handled above
    }

    private def handleOther(statement: S, in: Fact, predecessor: Option[S])(
        implicit
        state:    State,
        worklist: Worklist
    ): Unit = {
        val successors = icfg.nextStatements(statement)
        for (out <- normalFlow(statement, in, predecessor)) { // ifds line 34
            if (successors.isEmpty) {
                // exit reached
                propagate(None, out, statement) // ifds line 35
            } else {
                for (successor <- successors) {
                    propagate(Some(successor), out, statement) // ifds line 35
                }
            }
        }
    }

    private def handleUnbalancedReturn(exit: S, in: Fact)(implicit state: State, work: WorklistItem): Unit = {
        val callee = exit.callable
        val existingCallChain = state.source._2.callChain.getOrElse(Seq.empty)
        // avoid infinite loops
        val callable = ifdsProblem.createCallable(callee)
        if (existingCallChain.contains(callable)) return

        // unbalanced returns inside analysis context
        val callers = icfg.getCallers(callee)
        for (callStmt <- callers) {
            val normalReturnFacts = ifdsProblem.returnFlow(exit, in, callStmt, None, existingCallChain)
            val unbalancedReturnFacts = normalReturnFacts
                // map to unbalanced return facts
                .map(new IFDSFact(_, true, Some(callStmt), Some(existingCallChain.prepended(callable))))

            // Add the caller with the unbalanced return facts as a dependency to start its analysis
            for (unbRetFact <- unbalancedReturnFacts) {
                val newEntity = (callStmt.callable, unbRetFact)
                if (newEntity == state.source) {
                    // handle self dependency on our own because property store can't handle it
                    state.selfDependees += work
                } else {
                    // handle all other dependencies using property store
                    state.dependees.get(newEntity, propertyKey.key)
                }
            }
        }

        // unbalanced returns outside analysis context (java -> native or native -> java)
        ifdsProblem.outsideAnalysisContextUnbReturn(callee) match {
            case Some(handler) => handler(callee, in, existingCallChain, state.dependees.getter())
            case None          => // unbalanced returns outside analysis context are not handled
        }
    }

    private def propagate(successor: Option[S], out: Fact, predecessor: S)(
        implicit
        state:    State,
        worklist: Worklist
    ): Unit = {
        val predecessorOption =
            if (successor.isDefined && ifdsProblem.needsPredecessor(successor.get)) Some(predecessor)
            else None

        // ifds line 9
        if (successor.isEmpty // last statement was reached, must be processed to trigger handleExit
            || state.pathEdges.add(successor.get, out, predecessorOption)) {
            worklist.enqueue((successor, new IFDSFact(out), Some(predecessor)))
        }
    }

    /**
     * ifds normal flow function
     * @param statement the current statement (n in the original paper)
     * @param in the processed fact (d2 in the original paper)
     * @param predecessor an optional predecessor statement (pi in the original paper)
     * @return the generated facts
     */
    private def normalFlow(statement: S, in: Fact, predecessor: Option[S]): Set[Fact] = {
        statistics.normalFlow += 1
        addNullFactIfConfigured(in, ifdsProblem.normalFlow(statement, in, predecessor))
    }

    /**
     * ifds passArgs function
     *
     * @param call the calling statement (n in the original paper)
     * @param callee the called function
     * @param in the processed fact (d2 in the original paper)
     * @param entry the first statement in the called function
     * @return the generated facts
     */
    private def callFlow(entry: S, in: Fact, call: S, callee: C): Set[Fact] = {
        statistics.callFlow += 1
        addNullFactIfConfigured(in, ifdsProblem.callFlow(entry, in, call, callee))
    }

    /**
     * maps the facts back to the calling contexts
     * @param exit the returning statement n
     * @param in the processed fact (d2 in the original paper)
     * @param call the calling statement
     * @param callFact the fact holding before the call
     * @param successor an optional successor statement
     * @param unbCallChain the callchain up to here
     * @return the generated facts
     */
    private def returnFlow(
        exit:         S,
        in:           Fact,
        call:         S,
        callFact:     Fact,
        successor:    Option[S],
        unbCallChain: Seq[Callable]
    ): Set[Fact] = {
        statistics.returnFlow += 1
        addNullFactIfConfigured(in, ifdsProblem.returnFlow(exit, in, call, successor, unbCallChain))
    }

    /**
     * Processes the facts around a call
     * @param call the calling statement
     * @param in the processed fact (d2 in the original paper)
     * @param successor an optional successor statement
     * @param unbCallChain the callchain up to here
     * @return the generated facts
     */
    private def callToReturnFlow(
        call:         S,
        in:           Fact,
        successor:    Option[S],
        unbCallChain: Seq[Callable]
    ): Set[Fact] = {
        statistics.callToReturnFlow += 1
        addNullFactIfConfigured(in, ifdsProblem.callToReturnFlow(call, in, successor, unbCallChain))
    }

    private def addNullFactIfConfigured(in: Fact, out: Set[Fact]): Set[Fact] = {
        if (ifdsProblem.automaticallyPropagateNullFactInFlowFunctions && in == ifdsProblem.nullFact)
            out + ifdsProblem.nullFact
        else out
    }

    private def subsumes(existingFacts: Set[Fact], newFact: Fact)(
        implicit
        project: SomeProject
    ): Boolean = {
        statistics.subsumeTries += 1
        if (ifdsProblem.subsumeFacts && existingFacts.exists(_.subsumes(newFact, project))) {
            statistics.subsumptions += 1
            true
        } else false
    }
}

abstract class IFDSAnalysisScheduler[Fact <: AbstractIFDSFact, C <: AnyRef, S <: Statement[_ <: C, _]]
    extends FPCFLazyAnalysisScheduler {

    final override type InitializationData = IFDSAnalysis[Fact, C, S]
    def property: IFDSPropertyMetaInformation[S, Fact]
    final override def derivesLazily: Some[PropertyBounds] = Some(PropertyBounds.ub(property))
    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def register(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: IFDSAnalysis[Fact, C, S]
    ): FPCFAnalysis = {
        ps.registerLazyPropertyComputation(property.key, analysis.performAnalysis)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}
}
