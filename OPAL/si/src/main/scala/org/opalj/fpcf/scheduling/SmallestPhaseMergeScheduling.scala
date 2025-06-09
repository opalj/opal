package org.opalj.fpcf.scheduling

import org.opalj.collection.IntIterator
import org.opalj.fpcf.AnalysisScenario
import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.LazyComputation
import org.opalj.fpcf.PhaseConfiguration
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Transformer
import org.opalj.graphs.sccs

/**
 * Single Phase Scheduling (SPS) Strategy.
 * Schedules all computations in a single batch without considering dependencies.
 */
class SmallestPhaseMergeScheduling[A](ps: PropertyStore, scheduleLazyTransformerInAllBatches: Boolean)
    extends AnalysisScenario[A](ps) with SchedulingStrategy[A] {

    val name = "SPMS"

    override def schedule(
        ps:    PropertyStore,
        allCS: Set[ComputationSpecification[A]]
    ): List[PhaseConfiguration[A]] = {

        val lazyType = LazyComputation
        val transformerType = Transformer

        // Creates a map from ComputationSpecifications to their indices
        val computationSpecificationMap: Map[ComputationSpecification[A], Int] = allCS.zipWithIndex.toMap
        val computationSpecificationArray: Array[ComputationSpecification[A]] = allCS.toArray
        // Initializes an empty schedule graph
        var scheduleGraph: Map[Int, Set[Int]] = Map.empty

        // Helper functions and logic for SPMS scheduling follow...
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
            map:          Map[List[Int], Set[Int]],
            firstElement: List[Int]
        ): Map[List[Int], Set[Int]] = {
            var visited_batches: List[List[Int]] = List.empty
            var aCyclicGraph = map.toMap

            def setLazyInAllBatches_rek(
                map:          Map[List[Int], Set[Int]],
                firstElement: List[Int]
            ): Map[List[Int], Set[Int]] = {

                if (firstElement.forall(csID =>
                        computationSpecificationArray(csID).computationType.equals(lazyType) ||
                            computationSpecificationArray(csID).computationType.equals(transformerType)
                    )
                ) {
                    var existInSomeBatch = false
                    map.foreach { batch =>
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

        def topologicalSort(graph: Map[Int, List[Int]]): List[Int] = {
            var sortedNodes: List[Int] = List.empty
            var permanent: Set[Int] = Set.empty
            var temporary: Set[Int] = Set.empty

            val preparedGraph = graph.map { case (node, deps) =>
                node -> deps.filter(_ != node)
            }

            def visit(node: Int): Unit = {
                if (!permanent.contains(node)) {
                    if (temporary.contains(node)) {
                        throw new IllegalStateException("Graph contains a cycle")
                    }
                    temporary = temporary + node

                    preparedGraph.get(node).head.foreach { otherNode => visit(otherNode) }

                    permanent = permanent + node
                    temporary = temporary - node

                    sortedNodes = sortedNodes :+ node
                }

            }
            for (node <- preparedGraph.keys) {
                visit(node)
            }

            sortedNodes
        }

        def mergeIndependentBatches(
            tfMap: Map[Int, List[Int]],
            ct:    Int,
            graph: Map[Int, List[Int]]
        ): (Map[Int, List[Int]], Map[Int, List[Int]]) = {
            var transformingMap = tfMap.toMap
            var counter = ct

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
                    def checkForLeastAmountOfAnalysis(): (Int, Int) = {
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

                    val toBeMerged = checkForLeastAmountOfAnalysis()

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
            (mergeIndependentBatches_rek(graph), transformingMap)
        }

        computationSpecificationMap.foreach { csID =>
            scheduleGraph += (csID._2 -> mapCSToNum(getAllCSFromPropertyBounds(csID._1.uses(ps))))
        }

        if (!scheduleLazyTransformerInAllBatches) {
            scheduleGraph.foreach { node =>
                if (computationSpecificationArray(node._1).computationType.equals(
                        lazyType
                    ) || computationSpecificationArray(node._1).computationType.equals(
                        transformerType
                    )
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

        val preparedGraph = aCyclicGraph.map { case (nodes, deps) =>
            nodes -> (deps -- nodes).toList
        }

        var transformingMap: Map[Int, List[Int]] = Map.empty
        var counter = 0
        preparedGraph.foreach { node =>
            transformingMap = transformingMap + (counter -> node._1)
            counter = counter + 1
        }

        var transformedGraph = preparedGraph.map { case (node, deps) =>
            var dependencies: List[Int] = List.empty
            transformingMap.foreach { tuple =>
                if (tuple._2.intersect(deps).nonEmpty) {
                    dependencies = dependencies :+ tuple._1
                }
            }
            transformingMap.find(_._2 == node).map(_._1).head -> dependencies
        }

        val (newGraph, newTransformingMap) =
            mergeIndependentBatches(transformingMap, counter, transformedGraph)
        transformedGraph = newGraph
        transformingMap = newTransformingMap
        val batchOrder = topologicalSort(transformedGraph)

        var scheduleBatches: List[PhaseConfiguration[A]] = List.empty

        var alreadyScheduledCS: Set[ComputationSpecification[A]] = Set.empty
        batchOrder.foreach { batch =>
            var scheduledInThisPhase: Set[ComputationSpecification[A]] = Set.empty
            transformingMap.get(batch).head.foreach { csID =>
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

object SmallestPhaseMergeScheduling {
    val name = "SPMS"
}
