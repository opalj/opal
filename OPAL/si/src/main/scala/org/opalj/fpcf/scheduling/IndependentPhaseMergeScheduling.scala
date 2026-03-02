/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package scheduling

import scala.collection.mutable
import scala.util.control.Breaks

/**
 * Base class for scheduling strategies that create multiple computation phases by merging multiple smaller independent
 * phases into one.
 */
abstract class PhaseMergeScheduling extends MultiplePhaseScheduling {

    /**
     * Merges independent batches based on a merging strategy.
     */
    override def refineDependencies(
        initialPhaseDependencyGraph: Iterable[(Int, List[Int])],
        initialPhaseIndexToAnalyses: Iterable[(Int, Set[Int])]
    ): (Map[Int, List[Int]], Map[Int, Set[Int]]) = {
        val phaseIndexToAnalyses = initialPhaseIndexToAnalyses.to(mutable.Map)
        val phaseDependencyGraph = initialPhaseDependencyGraph.to(mutable.Map)

        var nextPhaseIndex = initialPhaseIndexToAnalyses.size

        def allDependencies(phase: Int): Set[Int] = {
            val directDependencies = phaseDependencyGraph(phase).toSet
            val indirectDependencies = directDependencies.flatMap(phase2 => allDependencies(phase2))
            directDependencies ++ indirectDependencies
        }

        val breaks = new Breaks
        import breaks.{break, breakable}
        breakable {
            while (true) {
                val independentPhases = for {
                    phaseIndex1 <- phaseDependencyGraph.keysIterator
                    dependencies1 = allDependencies(phaseIndex1)
                    phaseIndex2 <- phaseDependencyGraph.keysIterator
                    if !dependencies1.contains(phaseIndex2)
                    if phaseIndex1 < phaseIndex2 // Ensure we only look at each pair once
                    dependencies2 = allDependencies(phaseIndex2)
                    if !dependencies2.contains(phaseIndex1)
                } yield (phaseIndex1, phaseIndex2)

                if (independentPhases.isEmpty)
                    break()

                val (phase1, phase2) = nextPhasesToMerge(independentPhases, phaseIndexToAnalyses)

                phaseIndexToAnalyses(nextPhaseIndex) = phaseIndexToAnalyses(phase1) ++ phaseIndexToAnalyses(phase2)
                phaseIndexToAnalyses -= phase1
                phaseIndexToAnalyses -= phase2

                phaseDependencyGraph(nextPhaseIndex) = (phaseDependencyGraph(phase1) ++ phaseDependencyGraph(phase2))
                    .distinct
                phaseDependencyGraph -= phase1
                phaseDependencyGraph -= phase2

                // Map dependencies on old phases to new phase
                phaseDependencyGraph.foreach { case (key, values) =>
                    phaseDependencyGraph(key) = values.map(v => if (v == phase1 || v == phase2) nextPhaseIndex else v)
                }

                nextPhaseIndex = nextPhaseIndex + 1
            }
        }

        (phaseDependencyGraph.toMap, phaseIndexToAnalyses.toMap)
    }

    def nextPhasesToMerge(
        independentPhases:    Iterator[(Int, Int)],
        phaseIndexToAnalyses: scala.collection.Map[Int, Set[Int]]
    ): (Int, Int)
}

/**
 * Merges independent batches to optimize parallelism.
 */
object IndependentPhaseMergeScheduling extends PhaseMergeScheduling {

    def nextPhasesToMerge(
        independentPhases:    Iterator[(Int, Int)],
        phaseIndexToAnalyses: scala.collection.Map[Int, Set[Int]]
    ): (Int, Int) = {
        independentPhases.next()
    }

}
