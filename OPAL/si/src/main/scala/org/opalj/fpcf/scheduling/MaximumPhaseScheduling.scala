/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package scheduling

import com.typesafe.config.Config
import org.opalj.collection.IntIterator
import org.opalj.fpcf.AnalysisScenario.ConfigKeyPrefix
import org.opalj.fpcf.scheduling.CleanupCalculation.{EnableCleanup, PropertiesToKeepKey, PropertiesToRemoveKey}
import org.opalj.fpcf.scheduling.MultiplePhaseScheduling.AnalysisScheduleLazyTransformerInMultiplePhasesKey
import org.opalj.graphs.{sccs, topologicalSort}
import org.opalj.log.{LogContext, OPALLogger}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Base class for scheduling strategies that create multiple computation phases.
 */
abstract class MultiplePhaseScheduling extends SchedulingStrategy {

    override def schedule[A](ps: PropertyStore, allCS: Set[ComputationSpecification[A]])(implicit
        config:     Config,
        logContext: LogContext
    ): List[PhaseConfiguration[A]] = {

        // Create a mapping from analyses (ComputationSpecifications) to their indices
        val analysisToIndex: Map[ComputationSpecification[A], Int] = allCS.iterator.zipWithIndex.toMap
        val indexToAnalysis: Array[ComputationSpecification[A]] = allCS.toArray

        def propertiesToAnalyses(properties: Set[PropertyBounds]): mutable.Set[Int] = {
            analysisToIndex.keysIterator.collect {
                case analysis if analysis.derives.exists(properties.contains) =>
                    analysisToIndex(analysis)
            }.to(mutable.Set)
        }

        // Initialize the analysis dependency graph, mapping each analysisID to the ids of its dependees
        val analysisDependencyGraph: Map[Int, mutable.Set[Int]] =
            for { (analysis, analysisID) <- analysisToIndex } yield analysisID -> propertiesToAnalyses(analysis.uses(ps))

        val scheduleLazyTransformerInMultiplePhases =
            config.getBoolean(AnalysisScheduleLazyTransformerInMultiplePhasesKey)
        if (scheduleLazyTransformerInMultiplePhases)
            OPALLogger.info("scheduler", s"scheduling Lazy/Transformer analyses in multiple phases")

        // Set lazy/transformer analyses as dependers of their uses to ensure that they are scheduled in the same phase
        if (!scheduleLazyTransformerInMultiplePhases) {
            for {
                lazyAnalysisID <- analysisDependencyGraph.keysIterator
                computationType = indexToAnalysis(lazyAnalysisID).computationType
                if (computationType eq LazyComputation) || (computationType eq Transformer)
                (dependerID, dependees) <- analysisDependencyGraph
                if dependees.contains(lazyAnalysisID)
            }
                analysisDependencyGraph(lazyAnalysisID) += dependerID
        }

        def edges(scheduleGraph: Map[Int, scala.collection.Set[Int]])(node: Int): IntIterator = {
            val edges = scheduleGraph(node).iterator
            new IntIterator {
                def hasNext: Boolean = edges.hasNext
                def next(): Int = edges.next()
            }
        }

        // Strongly connected components must be in a single phase
        val phaseDependencies =
            for {
                phase <- sccs(analysisDependencyGraph.size, edges(analysisDependencyGraph))
            } yield phase.toSet -> (phase.iterator.flatMap(analysisDependencyGraph).toSet -- phase)

        // After potentially merging lazy analyses into multiple phases
        val initialPhases = if (scheduleLazyTransformerInMultiplePhases) {
            mergeLazyIntoEagerPhases(phaseDependencies, indexToAnalysis)
        } else phaseDependencies.toMap

        val initialPhaseIndexToAnalyses = (0 until initialPhases.size).zip(initialPhases.keysIterator)

        val initialPhaseDependencyGraph: Iterable[(Int, List[Int])] = for {
            (phaseIndex1, analyses1) <- initialPhaseIndexToAnalyses
            dependeeAnalyses = initialPhases(analyses1)
            dependeePhases = initialPhaseIndexToAnalyses.collect {
                case (phaseIndex2, analyses2) if analyses2.exists(dependeeAnalyses.contains) => phaseIndex2
            }.toList
        } yield phaseIndex1 -> dependeePhases

        val (phaseDependencyGraph, phaseIndexToAnalyses) =
            refineDependencies(initialPhaseDependencyGraph, initialPhaseIndexToAnalyses)

        val phaseOrder = topologicalSort(phaseDependencyGraph)

        var remainingAnalyses: Set[ComputationSpecification[A]] = allCS
        val schedule = phaseOrder.map { phaseIndex =>
            val phaseAnalyses = phaseIndexToAnalyses(phaseIndex).iterator.map(indexToAnalysis).toSet
            remainingAnalyses --= phaseAnalyses
            computePhase(ps, phaseAnalyses, remainingAnalyses)
        }
        val enableCleanup = config.getBoolean(EnableCleanup)

        if(enableCleanup) calculateDeletions(schedule, ps, config) else schedule
    }

    private def calculateDeletions[A](schedule: List[PhaseConfiguration[A]], ps: PropertyStore, config: Config): List[PhaseConfiguration[A]] = {
        val pkToKeep = if(config.getString(PropertiesToKeepKey).trim.isEmpty) List.empty else config.getString(PropertiesToKeepKey).split(",").map(_.trim.toInt).toList
        val pkToDelete = if(config.getString(PropertiesToRemoveKey).trim.isEmpty) List.empty else config.getString(PropertiesToRemoveKey).split(",").map(_.trim.toInt).toSet
        val properties: mutable.Set[Int] =
            schedule.iterator.flatMap(_.propertyKinds.propertyKindsComputedInThisPhase.map(key => key.id)).to(mutable.Set)

        val modifiedSchedule: ListBuffer[PhaseConfiguration[A]] = ListBuffer.from(schedule)

        var usedDependencies: Set[Int] = Set.empty
        var alreadyDeleted: Set[Int] = Set.empty
        for (elem <- modifiedSchedule.reverseIterator) {

            val currentSchedule = elem.scheduled
            usedDependencies ++= currentSchedule.iterator.flatMap(_.uses(ps).iterator.map(_.pk.id))
            usedDependencies ++= pkToKeep
            val remaining: Set[Int] = properties.toSet diff usedDependencies diff alreadyDeleted

            elem.toDelete = remaining
            alreadyDeleted ++= remaining
        }
        modifiedSchedule.last.toDelete ++= pkToDelete
        modifiedSchedule.toList
    }

    /**
     * Computes a new phase map where phases that consist completely of lazy analyses are merged into the phases that
     * depend on them.
     */
    private def mergeLazyIntoEagerPhases[A](
        initialPhaseDependencies: List[(Set[Int], Set[Int])],
        indexToAnalysis:          Array[ComputationSpecification[A]]
    ): Map[Set[Int], Set[Int]] = {
        var phaseDependencies = initialPhaseDependencies.toMap
        var worklist: List[(Set[Int], Set[Int])] = initialPhaseDependencies

        while (worklist.nonEmpty) {
            val currentPhase = worklist.head
            val (analyses, dependencies) = currentPhase
            worklist = worklist.tail

            val allLazy = analyses.forall { analysisID =>
                val computationType = indexToAnalysis(analysisID).computationType
                (computationType eq LazyComputation) || (computationType eq Transformer)
            }

            if (allLazy) {
                var mergedPhase = false
                phaseDependencies -= analyses // Remove the all-lazy phase in anticipation of merging it
                for {
                    (otherAnalyses, otherDependencies) <- phaseDependencies
                    if otherDependencies.exists(analyses.contains)
                    newAnalyses = otherAnalyses ++ analyses
                    newDependencies = (otherDependencies ++ dependencies).diff(newAnalyses)
                    newPhase = newAnalyses -> newDependencies
                } {
                    phaseDependencies -= otherAnalyses
                    phaseDependencies += newPhase
                    worklist :+= newPhase
                    mergedPhase = true
                }
                if (!mergedPhase) { // If we didn't merge the lazy phase into another phase, we need to keep it
                    phaseDependencies += currentPhase
                }
            }
        }

        phaseDependencies
    }

    def refineDependencies(
        initialPhaseDependencyGraph: Iterable[(Int, List[Int])],
        initialPhaseIndexToAnalyses: Iterable[(Int, Set[Int])]
    ): (Map[Int, List[Int]], Map[Int, Set[Int]])
}

object MultiplePhaseScheduling {
    final val AnalysisScheduleLazyTransformerInMultiplePhasesKey = s"${ConfigKeyPrefix}ScheduleLazyInMultiplePhases"
}

/**
 * Maximum Phase Scheduling (MPS) Strategy.
 * Breaks down computations into as many phases as possible based on dependencies and computation types.
 */
object MaximumPhaseScheduling extends MultiplePhaseScheduling {

    def refineDependencies(
        initialPhaseDependencyGraph: Iterable[(Int, List[Int])],
        initialPhaseIndexToAnalyses: Iterable[(Int, Set[Int])]
    ): (Map[Int, List[Int]], Map[Int, Set[Int]]) = {
        (initialPhaseDependencyGraph.toMap, initialPhaseIndexToAnalyses.toMap)
    }
}

object CleanupCalculation {
    final val PropertiesToKeepKey = s"${ConfigKeyPrefix}KeepPropertyKeys"
    final val PropertiesToRemoveKey = s"${ConfigKeyPrefix}RemovePropertyKeys"
    final val EnableCleanup = s"${ConfigKeyPrefix}EnableCleanup"
}