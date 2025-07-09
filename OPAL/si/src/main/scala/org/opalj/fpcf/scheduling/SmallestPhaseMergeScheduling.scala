/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package scheduling

/**
 * Smallest Phase Merge Scheduling (SPS) Strategy.
 * Merging batches based on the number of analyses to keep merged batches of similar sizes.
 */
abstract class SmallestPhaseMergeScheduling extends IndependentPhaseMergeScheduling {

    override def nextToMerge(couldBeMerged: List[(Int, Int)], transformingMap: Map[Int, List[Int]]): (Int, Int) = {
        var twoBatchesWithLeastAmountOfAnalysis = (0, 0)
        var otherSize = 0
        couldBeMerged.foreach { tuple =>
            if (otherSize == 0) {
                twoBatchesWithLeastAmountOfAnalysis = tuple
                otherSize = transformingMap.get(tuple._1).head.size + transformingMap.get(
                    tuple._2
                ).head.size
            } else if (transformingMap.get(tuple._1).head.size + transformingMap.get(
                           tuple._2
                       ).head.size < otherSize
            ) {
                twoBatchesWithLeastAmountOfAnalysis = tuple
                otherSize = transformingMap.get(tuple._1).head.size + transformingMap.get(
                    tuple._2
                ).head.size
            }

        }
        twoBatchesWithLeastAmountOfAnalysis
    }
}

object SmallestPhaseMergeScheduling extends SmallestPhaseMergeScheduling
