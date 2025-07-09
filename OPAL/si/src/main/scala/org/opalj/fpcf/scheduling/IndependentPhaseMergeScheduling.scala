/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package scheduling

/**
 * Independent Phase Merge Scheduling (IPMS) Strategy.
 * Merges independent batches to optimize parallelism.
 */
abstract class IndependentPhaseMergeScheduling extends MaximumPhaseScheduling {

    override def mergeIndependentBatches(
        batchCount:      Int,
        dependencyGraph: Map[Int, List[Int]],
        nodeIndexMap:    Map[Int, List[Int]]
    ): (Map[Int, List[Int]], Map[Int, List[Int]]) = {
        var transformingMap = nodeIndexMap
        var counter = batchCount

        def mergeIndependentBatches_rek(
            graph: Map[Int, List[Int]]
        ): Map[Int, List[Int]] = {

            def getUses(batch: Int, graph: Map[Int, List[Int]]): Set[Int] = {
                val directUses = graph.getOrElse(batch, List.empty).toSet
                val recursiveUses = directUses.flatMap(otherBatch => getUses(otherBatch, graph))
                directUses ++ recursiveUses
            }

            var map: Map[Int, Set[Int]] = Map.empty
            graph.foreach { batch =>
                val tempUses = getUses(batch._1, graph)
                map = map + (batch._1 -> tempUses)
            }

            var couldBeMerged: List[(Int, Int)] = List.empty
            map.foreach { batch =>
                map.foreach { subBatch =>
                    if (subBatch != batch) {
                        if ((!subBatch._2.contains(batch._1)) && (!batch._2.contains(subBatch._1))) {
                            if (!couldBeMerged.contains((subBatch._1, batch._1))) {
                                couldBeMerged = couldBeMerged :+ (batch._1, subBatch._1)
                            }

                        }
                    }

                }

            }

            var updatedGraph: Map[Int, List[Int]] = graph
            if (couldBeMerged.nonEmpty) {
                val toBeMerged = nextToMerge(couldBeMerged, transformingMap)

                val tempTransformation_2 = (transformingMap.get(toBeMerged._1).head ++
                    transformingMap.get(toBeMerged._2).head).distinct
                transformingMap =
                    transformingMap - toBeMerged._1 - toBeMerged._2
                transformingMap = transformingMap + (counter -> tempTransformation_2)

                val tempGraph_2: List[Int] = (graph.get(toBeMerged._1).head ++
                    graph.get(toBeMerged._2).head).distinct
                updatedGraph = updatedGraph - toBeMerged._1 - toBeMerged._2
                updatedGraph = updatedGraph + (counter -> tempGraph_2)

                def replaceIdInMap(oldId: Int, newId: Int): Unit = {
                    updatedGraph = updatedGraph.map { case (key, values) =>
                        key -> values.map(v => if (v == oldId) newId else v)
                    }
                }

                replaceIdInMap(toBeMerged._1, counter)
                replaceIdInMap(toBeMerged._2, counter)
                counter = counter + 1
                updatedGraph = mergeIndependentBatches_rek(updatedGraph)
            }
            updatedGraph
        }
        (mergeIndependentBatches_rek(dependencyGraph), transformingMap)
    }

    def nextToMerge(couldBeMerged: List[(Int, Int)], transformingMap: Map[Int, List[Int]]): (Int, Int) = {
        couldBeMerged.head
    }
}

object IndependentPhaseMergeScheduling extends IndependentPhaseMergeScheduling
