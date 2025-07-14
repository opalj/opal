/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package scheduling

/**
 * Smallest Phase Merge Scheduling (SPS) Strategy.
 * Merging batches based on the number of analyses to keep merged batches of similar sizes.
 */
object SmallestPhaseMergeScheduling extends PhaseMergeScheduling {
    def nextPhasesToMerge(
        independentPhases:    Iterator[(Int, Int)],
        phaseIndexToAnalyses: scala.collection.Map[Int, Set[Int]]
    ): (Int, Int) = {

        def batchSize(phaseIndex: Int): Int = {
            phaseIndexToAnalyses(phaseIndex).size
        }

        var smallestBatches = independentPhases.next()
        var smallestSize = batchSize(smallestBatches._1) + batchSize(smallestBatches._2)

        independentPhases.foreach { phases =>
            val (phase1, phase2) = phases
            val combinedSize = batchSize(phase1) + batchSize(phase2)
            if (combinedSize < smallestSize) {
                smallestBatches = phases
                smallestSize = combinedSize
            }

        }

        smallestBatches
    }
}
