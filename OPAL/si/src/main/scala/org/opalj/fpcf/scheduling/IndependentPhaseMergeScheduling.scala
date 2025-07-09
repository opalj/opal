/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package scheduling

import org.opalj.collection.IntIterator
import org.opalj.graphs.sccs
import org.opalj.graphs.topologicalSort

/**
 * Independent Phase Merge Scheduling (IPMS) Strategy.
 * Merges independent batches to optimize parallelism.
 */
class IndependentPhaseMergeScheduling[A](ps: PropertyStore, scheduleLazyTransformerInAllBatches: Boolean)
    extends SchedulingStrategy[A] {

    val name = "IPMS"

    override def schedule(
        ps:    PropertyStore,
        allCS: Set[ComputationSpecification[A]]
    ): List[PhaseConfiguration[A]] = {

        // Creates a map from ComputationSpecifications to their indices
        val computationSpecificationMap: Map[ComputationSpecification[A], Int] = allCS.zipWithIndex.toMap
        val computationSpecificationArray: Array[ComputationSpecification[A]] = allCS.toArray

        // Initializes an empty schedule graph
        var scheduleGraph: Map[Int, Set[Int]] = Map.empty

        // Helper functions and logic for IPMS scheduling follow...
        def getAllCSFromPropertyBounds(
            properties: Set[PropertyBounds]
        ): Set[ComputationSpecification[A]] = {
            def containsProperty(cs: ComputationSpecification[A], property: PropertyBounds): Boolean =
                cs.derivesLazily.contains(property) ||
                    cs.derivesCollaboratively.contains(property) ||
                    cs.derivesEagerly.contains(property)

            allCS.filter(cs => properties.exists(containsProperty(cs, _)))
        }

        def mapCSToNum(specifications: Set[ComputationSpecification[A]]): Set[Int] = {
            specifications.flatMap(computationSpecificationMap.get)
        }

        def edgeFunctionForSCCS(node: Int): IntIterator = {
            val edges = scheduleGraph.getOrElse(node, Set.empty).iterator
            new IntIterator {
                def hasNext: Boolean = edges.hasNext
                def next(): Int = edges.next()
            }
        }

        def getAllUses(css: List[Int]): Set[PropertyBounds] = {
            var allUses: Set[PropertyBounds] = Set.empty
            css.foreach { cs => allUses = allUses ++ computationSpecificationArray(cs).uses(ps) }
            allUses
        }

        def setLazyInAllBatches(
            tmp_aCyclicGraph: Map[List[Int], Set[Int]],
            firstElement:     List[Int]
        ): Map[List[Int], Set[Int]] = {
            var visited_batches: List[List[Int]] = List.empty
            var aCyclicGraph = tmp_aCyclicGraph

            def setLazyInAllBatches_rek(
                tmp_aCyclicGraphInternal: Map[List[Int], Set[Int]],
                firstElement:             List[Int]
            ): Map[List[Int], Set[Int]] = {

                if (firstElement.forall(csID =>
                        computationSpecificationArray(csID).computationType.equals(LazyComputation) ||
                            computationSpecificationArray(csID).computationType.equals(Transformer)
                    )
                ) {
                    var existInSomeBatch = false
                    tmp_aCyclicGraphInternal.foreach { batch =>
                        if (batch._2.toList.intersect(firstElement).nonEmpty && batch._1 != firstElement) {
                            aCyclicGraph = aCyclicGraph + ((batch._1 ++ firstElement) -> mapCSToNum(
                                getAllCSFromPropertyBounds(getAllUses(batch._1 ++ firstElement))
                            ).diff((batch._1 ++ firstElement).toSet))
                            aCyclicGraph = aCyclicGraph - batch._1
                            existInSomeBatch = true
                        }
                    }
                    if (existInSomeBatch) {
                        aCyclicGraph = aCyclicGraph - firstElement
                        setLazyInAllBatches_rek(aCyclicGraph, aCyclicGraph.head._1)
                    } else {
                        visited_batches = visited_batches :+ firstElement
                        val keyList = aCyclicGraph.keys.toSet -- visited_batches
                        if (keyList.nonEmpty) {
                            aCyclicGraph = setLazyInAllBatches_rek(aCyclicGraph, keyList.head)
                        }
                    }
                } else {
                    visited_batches = visited_batches :+ firstElement
                    val keyList = aCyclicGraph.keys.toSet -- visited_batches
                    if (keyList.nonEmpty) {
                        setLazyInAllBatches_rek(aCyclicGraph, keyList.head)
                    }
                }
                aCyclicGraph
            }
            setLazyInAllBatches_rek(aCyclicGraph, firstElement)
        }

        def mergeIndependentBatches(
            nodeIndexMap:    Map[Int, List[Int]],
            batchCount:      Int,
            dependencyGraph: Map[Int, List[Int]]
        ): (Map[Int, List[Int]], Map[Int, List[Int]]) = {
            var transformingMap = nodeIndexMap
            var counter = batchCount

            def mergeIndependentBatches_rek(
                graph: Map[Int, List[Int]]
            ): Map[Int, List[Int]] = {

                var allUses: Set[Int] = Set.empty

                def getUses(batch: Int): Set[Int] = {
                    val uses = graph.get(batch).head
                    allUses = allUses ++ uses

                    uses.foreach { otherBatch => getUses(otherBatch) }

                    val returnUses = allUses
                    returnUses
                }

                var map: Map[Int, Set[Int]] = Map.empty
                graph.foreach { batch =>
                    val tempUses = getUses(batch._1)
                    map = map + (batch._1 -> tempUses)
                    allUses = Set.empty
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
                    val tempTransformation_2 =
                        (transformingMap.get(couldBeMerged.head._1).head ++
                            transformingMap.get(couldBeMerged.head._2).head).distinct
                    transformingMap =
                        transformingMap - couldBeMerged.head._1 - couldBeMerged.head._2
                    transformingMap = transformingMap + (counter -> tempTransformation_2)

                    val tempGraph_2: List[Int] = (graph.get(couldBeMerged.head._1).head ++
                        graph.get(couldBeMerged.head._2).head).distinct
                    updatedGraph = updatedGraph - couldBeMerged.head._1 - couldBeMerged.head._2
                    updatedGraph = updatedGraph + (counter -> tempGraph_2)

                    def replaceIdInMap(oldId: Int, newId: Int): Unit = {
                        updatedGraph = updatedGraph.map { case (key, values) =>
                            key -> values.map(v => if (v == oldId) newId else v)
                        }
                    }

                    replaceIdInMap(couldBeMerged.head._1, counter)
                    replaceIdInMap(couldBeMerged.head._2, counter)
                    counter = counter + 1
                    updatedGraph = mergeIndependentBatches_rek(updatedGraph)
                }
                updatedGraph
            }
            (mergeIndependentBatches_rek(dependencyGraph), transformingMap)
        }

        computationSpecificationMap.foreach { csID =>
            scheduleGraph += (csID._2 -> mapCSToNum(getAllCSFromPropertyBounds(csID._1.uses(ps))))
        }

        if (!scheduleLazyTransformerInAllBatches) {
            scheduleGraph.foreach { node =>
                if (computationSpecificationArray(node._1).computationType.equals(
                        LazyComputation
                    ) || computationSpecificationArray(node._1).computationType.equals(Transformer)
                ) {
                    scheduleGraph.foreach { subNode =>
                        if (subNode._2.contains(node._1)) {
                            scheduleGraph =
                                scheduleGraph +
                                    (node._1 -> (scheduleGraph.get(node._1).head ++ Set(subNode._1)))
                        }
                    }
                }
            }
        }

        var aCyclicGraph = sccs(scheduleGraph.size, edgeFunctionForSCCS)
            .map(batch => batch -> mapCSToNum(getAllCSFromPropertyBounds(getAllUses(batch))))
            .toMap

        if (scheduleLazyTransformerInAllBatches) {
            aCyclicGraph = setLazyInAllBatches(aCyclicGraph, aCyclicGraph.head._1)
        }

        val graphWithoutSelfDependencies = aCyclicGraph.map { case (nodes, deps) =>
            nodes -> (deps -- nodes).toList
        }

        var nodeIndexMap: Map[Int, List[Int]] = Map.empty
        var counter = 0
        graphWithoutSelfDependencies.foreach { node =>
            nodeIndexMap = nodeIndexMap + (counter -> node._1)
            counter = counter + 1
        }

        var transformedGraph = graphWithoutSelfDependencies.map { case (node, deps) =>
            var dependencies: List[Int] = List.empty
            nodeIndexMap.foreach { tuple =>
                if (tuple._2.intersect(deps).nonEmpty) {
                    dependencies = dependencies :+ tuple._1
                }
            }
            nodeIndexMap.find(_._2 == node).map(_._1).head -> dependencies
        }

        val (newGraph, newTransformingMap) =
            mergeIndependentBatches(nodeIndexMap, counter, transformedGraph)
        transformedGraph = newGraph
        nodeIndexMap = newTransformingMap

        val batchOrder = topologicalSort(transformedGraph)

        var scheduleBatches: List[PhaseConfiguration[A]] = List.empty

        var alreadyScheduledCS: Set[ComputationSpecification[A]] = Set.empty
        batchOrder.foreach { batch =>
            var scheduledInThisPhase: Set[ComputationSpecification[A]] = Set.empty
            nodeIndexMap.get(batch).head.foreach { csID =>
                scheduledInThisPhase =
                    scheduledInThisPhase + computationSpecificationArray(csID)
            }

            scheduleBatches = scheduleBatches :+ computePhase(
                ps,
                scheduledInThisPhase,
                allCS -- scheduledInThisPhase -- alreadyScheduledCS
            )
            alreadyScheduledCS = alreadyScheduledCS ++ scheduledInThisPhase
        }

        scheduleBatches
    }
}

object IndependentPhaseMergeScheduling {
    val name = "IPMS"
}
