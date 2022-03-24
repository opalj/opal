/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ifds

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.{FPCFAnalysis, FPCFLazyAnalysisScheduler}
import org.opalj.fpcf._

import scala.collection.{mutable, Set => SomeSet}

abstract class Statement[C, Node] {
  def node(): Node
  def callable(): C
}

case class Dependees[Work]() {
  case class Dependee(eOptionP: SomeEOptionP, worklist: Set[Work] = Set.empty)
  var dependees = Map.empty[SomeEPK, Dependee]
  def get(entity: Entity, propertyKey: PropertyKey[Property])(implicit propertyStore: PropertyStore, work: Work): SomeEOptionP = {
    val epk = EPK(entity, propertyKey)
    val dependee = dependees.get(epk) match {
      case Some(dependee) => Dependee(dependee.eOptionP, dependee.worklist + work)
      case None => Dependee(propertyStore(epk), Set(work))
    }
    dependees += epk -> dependee
    dependee.eOptionP
  }

  def forResult(): Set[SomeEOptionP] = {
    dependees.values.map(_.eOptionP).toSet
  }
  def takeWork(epk: SomeEPK): Set[Work] = {
    val dependee = dependees(epk)
    dependees -= epk
    dependee.worklist
  }
}

/**
 * Keeps book of the path edges.
 * An entry of (statement, fact) means an edge (s0, source fact) -> (statement, fact) exists,
 * that is the fact reaches the statement as an input.
 * Source fact is the fact within the analysis entity.
 * */
case class PathEdges[IFDSFact <: AbstractIFDSFact, S <: Statement[C, _], C]() {
  var edges = Map.empty[S, Either[Set[IFDSFact], Map[S, Set[IFDSFact]]]]

  /**
   * Add the edge (s0, source fact) -> (statement, fact) to the path edges.
   * Optionally give a predecessor for the statement. This is used for phi statements
   * to distinguish the input flow and merge the facts later.
   * @param statement the destination statement of the edge
   * @param predecessor the predecessor of the statement.
   * @return whether the edge was new
   */
  def add(statement: S, fact: IFDSFact, predecessor: Option[S] = None): Boolean = {
    // TODO: subsuming
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
        if (predecessor.isDefined) throw new IllegalArgumentException(s"${statement} does not accept a predecessor")
        val isNew = !existingFacts.contains(fact)
        edges = edges.updated(statement, Left(existingFacts + fact))
        isNew
      case Some(Right(existingFacts)) =>
            predecessor match {
              case None => throw new IllegalArgumentException(s"${statement} requires a predecessor")
              case Some(predecessor) => existingFacts.get(statement) match {
                case Some(existingPredecessorFacts) => {
                  val isNew = !existingPredecessorFacts.contains(fact)
                  edges = edges.updated(statement, Right(existingFacts.updated(predecessor, existingPredecessorFacts + fact)))
                  isNew
                }
                case None => {
                  edges = edges.updated(statement, Right(existingFacts.updated(predecessor, Set(fact))))
                  true
                }
              }
            }
    }
  }

  /**
   * @param statement
   * @return The edges reaching statement if any. In case the statement minds about predecessors it is a map with an entry for each predecessor
   */
  def get(statement: S): Option[Either[Set[IFDSFact], Map[S, Set[IFDSFact]]]] = edges.get(statement)
}

/**
 * The state of the analysis. For each method and source fact, there is a separate state.
 *
 * @param source The callable and input fact for which the callable is analyzed.
 * @param endSummaries Output facts of the analyzed callable as pairs of exit statement and fact
 */
protected class IFDSState[IFDSFact <: AbstractIFDSFact, C <: AnyRef, S <: Statement[C, _], Work](
      val source:               (C, IFDSFact),
      val dependees: Dependees[Work] = Dependees[Work](),
      val pathEdges: PathEdges[IFDSFact, S, C] = PathEdges[IFDSFact, S, C](),
      var endSummaries: Set[(S, IFDSFact)] = Set.empty[(S, IFDSFact)]
                                                                                                )

/**
 * Contains int variables, which count, how many times some method was called.
 */
protected class Statistics {
  var normalFlow = 0
  var callFlow = 0
  var returnFlow = 0
  var callToReturnFlow = 0
}

protected class ProjectFPCFAnalysis(val project: SomeProject) extends FPCFAnalysis

/**
 *
 * @param ifdsProblem
 * @param propertyKey Provides the concrete property key that must be unique for every distinct concrete analysis and the lower bound for the IFDSProperty.
 * @tparam IFDSFact
 */
class IFDSAnalysis[IFDSFact <: AbstractIFDSFact, C <: AnyRef, S <: Statement[C, _]](
     implicit
     project:         SomeProject,
     val ifdsProblem: IFDSProblem[IFDSFact, C, S],
     val propertyKey: IFDSPropertyMetaInformation[S, IFDSFact]) extends ProjectFPCFAnalysis(project) {
  type Work = (S, IFDSFact, Option[S]) // statement, fact, predecessor
  type Worklist = mutable.Queue[Work]
  type State = IFDSState[IFDSFact, C, S, Work]

  implicit var statistics = new Statistics
  val icfg = ifdsProblem.icfg

  /**
   * Performs an IFDS analysis for a method-fact-pair.
   *
   * @param entity The method-fact-pair, that will be analyzed.
   * @return An IFDS property mapping from exit statements to the facts valid after these exit
   *         statements. Returns an interim result, if the TAC or call graph of this method or the
   *         IFDS analysis for a callee is still pending.
   */
  def performAnalysis(entity: (C, IFDSFact)): ProperPropertyComputationResult = {
    val (function, sourceFact) = entity

    // Start processing at the start of the icfg with the given source fact
    implicit val state: State = new IFDSState[IFDSFact, C, S, Work](entity)
    implicit val queue: Worklist = mutable.Queue
      .empty[Work]
    icfg.startStatements(function).foreach { start ⇒
      state.pathEdges.add(start, sourceFact)  // ifds line 2
      queue.enqueue((start, sourceFact, None))  // ifds line 3
    }
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
   *
   */
  private def createResult()(implicit state: State): ProperPropertyComputationResult = {
    val propertyValue = createPropertyValue(collectResult)
    val dependees = state.dependees.forResult
    if (dependees.isEmpty) Result(state.source, propertyValue)
    else InterimResult.forUB(state.source, propertyValue, dependees, propertyUpdate)
  }

  /**
   * Creates an IFDSProperty containing the result of this analysis.
   *
   * @param result Maps each exit statement to the facts, which hold after the exit statement.
   * @return An IFDSProperty containing the `result`.
   */
  private def createPropertyValue(result: Map[S, Set[IFDSFact]]): IFDSProperty[S, IFDSFact] =
    propertyKey.create(result)

  /**
   * Collects the facts valid at all exit nodes based on the current results.
   *
   * @return A map, mapping from each exit statement to the facts, which hold at
   *         after the exit statement.
   */
  private def collectResult(implicit state: State): Map[S, Set[IFDSFact]] = {
    var result = Map.empty[S, Set[IFDSFact]]
    for {entry <- state.endSummaries } {
      result.get(entry._1) match {
        case Some(existingFacts) => result = result.updated(entry._1, existingFacts + entry._2)
        case None => result = result.updated(entry._1, Set(entry._2))
      }
    }
    result
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
    implicit val queue: mutable.Queue[Work] = mutable.Queue()
    state.dependees.takeWork(eps.toEPK).foreach(queue.enqueue(_))
    process()
    createResult()
  }

  /**
   * Analyzes a queue of BasicBlocks.
   *
   * @param worklist
   */
  private def process()(implicit state: State, worklist: Worklist): Unit = {
    while (worklist.nonEmpty) {  // ifds line 10
      implicit val work = worklist.dequeue()  // ifds line 11
      val (statement, in, predecessor) = work
      icfg.getCalleesIfCallStatement(statement) match {
        case Some(callees) => handleCall(statement, callees, in)  // ifds line 13
        case None => if (icfg.isExitStatement(statement)) handleExit(statement, in) // ifds line 21
        else handleOther(statement, in, predecessor)  // ifds line 33
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
  private def handleCall(call:                 S,
                         callees:              SomeSet[C],
                         in:                   IFDSFact,
                        )(
                          implicit
                          state: State,
                          worklist: Worklist,
                          work: Work
                        ): Unit = {
    val successors = icfg.nextStatements(call)
    for (callee ← callees) {
      ifdsProblem.outsideAnalysisContext(callee) match {
        case Some(outsideAnalysisHandler) ⇒
          // Let the concrete analysis decide what to do.
          for {
            successor ← successors
            out <- outsideAnalysisHandler(call, successor, in)  // ifds line 17 (only summary edges)
          } {
            propagate(successor, out, call)  // ifds line 18
          }
        case None ⇒
          for {
            successor <- successors
            out <- concreteCallFlow(call, callee, in)  // ifds line 17 (only summary edges)
          } {
            propagate(successor, out, call)  // ifds line 18
          }
      }

      for {
        successor ← successors
        out <- callToReturnFlow(call, in)  // ifds line 17 (without summary edge propagation)
      } {
        propagate(successor, out, call)  // ifds line 18
      }
    }
  }

  private def concreteCallFlow(call: S, callee: C, in: IFDSFact)(implicit state: State, work: Work): Set[IFDSFact] = {
    var result = Set.empty[IFDSFact]
    for (entryFact ← callFlow(call, callee, in)) {  // ifds line 14
      val e = (callee, entryFact)
      val callFlows = state.dependees.get(e, propertyKey.key).asInstanceOf[EOptionP[(C, IFDSFact), IFDSProperty[S, IFDSFact]]]
      val exitFacts: Map[S, Set[IFDSFact]] = callFlows match {
        case ep: FinalEP[_, IFDSProperty[S, IFDSFact]] ⇒
          ep.p.flows
        case ep: InterimEUBP[_, IFDSProperty[S, IFDSFact]] ⇒
          ep.ub.flows
        case _ ⇒
          Map.empty
      }
      for {
        (exitStatement, exitStatementFacts) <- exitFacts  // ifds line 15.2
        exitStatementFact <- exitStatementFacts  // ifds line 15.3
      } {
        result ++= returnFlow(exitStatement, exitStatementFact, call, in)
      }
    }
    result
  }

  private def handleExit(statement: S, in: IFDSFact)(implicit state: State): Unit = {
    state.endSummaries += ((statement, in)) // ifds line 21.1
    // ifds lines 22 - 31 are handled by the dependency propagation of the property store
  }

  private def handleOther(statement: S, in: IFDSFact, predecessor: Option[S])(implicit state: State, worklist: Worklist): Unit = {
    for {  // ifds line 34
      successor <- icfg.nextStatements(statement)
      out <- normalFlow(statement, in, predecessor)
    } {
      propagate(successor, out, statement) // ifds line 35
    }
  }

  private def propagate(successor: S, out: IFDSFact, predecessor: S)(implicit state: State, worklist: Worklist): Unit = {
    val predecessorOption = if (ifdsProblem.needsPredecessor(successor)) Some(predecessor) else None

    // ifds line 9
    if (state.pathEdges.add(successor, out, predecessorOption)) {
      worklist.enqueue((successor, out, predecessorOption))
    }
  }

  /**
   * ifds flow function
   * @param statement n
   * @param in d2
   * @param predecessor pi
   */
  private def normalFlow(statement: S, in: IFDSFact, predecessor: Option[S]): Set[IFDSFact] = {
      statistics.normalFlow += 1
      ifdsProblem.normalFlow(statement, in, predecessor)
  }

  /**
   * ifds passArgs function
   * @param call n
   * @param callee
   * @param in d2
   * @return
   */
  private def callFlow(call: S, callee: C, in: IFDSFact): Set[IFDSFact] = {
    statistics.callFlow += 1
    ifdsProblem.callFlow(call, callee, in)
  }

  /**
   * ifds returnVal function
   * @param exit n
   * @param in d2
   * @param call c
   * @param callFact d4
   * @return
   */
  private def returnFlow(exit: S, in: IFDSFact, call: S, callFact: IFDSFact): Set[IFDSFact] = {
    statistics.returnFlow += 1
    ifdsProblem.returnFlow(exit, in, call, callFact)
  }

  /**
   * ifds callFlow function
   * @param call n
   * @param in d2
   * @return
   */
  private def callToReturnFlow(call: S, in: IFDSFact): Set[IFDSFact] = {
    statistics.callToReturnFlow += 1
    ifdsProblem.callToReturnFlow(call, in)
  }
}

abstract class IFDSAnalysisScheduler[IFDSFact <: AbstractIFDSFact, C <: AnyRef, S <: Statement[C, _]]
  extends FPCFLazyAnalysisScheduler {
  final override type InitializationData = IFDSAnalysis[IFDSFact, C, S]
  def property: IFDSPropertyMetaInformation[S, IFDSFact]
  final override def derivesLazily: Some[PropertyBounds] = Some(PropertyBounds.ub(property))
  override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

  /**
   * Registers the analysis as a lazy computation, that is, the method
   * will call `ProperytStore.scheduleLazyComputation`.
   */
  override def register(
                         p:        SomeProject,
                         ps:       PropertyStore,
                         analysis: IFDSAnalysis[IFDSFact, C, S]
                       ): FPCFAnalysis = {
    ps.registerLazyPropertyComputation(property.key, analysis.performAnalysis)
    analysis
  }

  override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {
    val ifdsAnalysis = analysis.asInstanceOf[IFDSAnalysis[IFDSFact, C, S]]
    for (e ← ifdsAnalysis.ifdsProblem.entryPoints) {
      ps.force(e, ifdsAnalysis.propertyKey.key)
    }
  }

  override def afterPhaseCompletion(
                                     p:        SomeProject,
                                     ps:       PropertyStore,
                                     analysis: FPCFAnalysis
                                   ): Unit = {}
}
