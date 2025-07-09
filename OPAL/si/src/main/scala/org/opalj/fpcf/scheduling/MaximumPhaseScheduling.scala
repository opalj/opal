/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package scheduling

import org.opalj.collection.IntIterator
import org.opalj.graphs.sccs
import org.opalj.graphs.topologicalSort

/**
 * Maximum Phase Scheduling (MPS) Strategy.
 * Breaks down computations into as many phases as possible based on dependencies and computation types.
 */

class MaximumPhaseScheduling[A](ps: PropertyStore, scheduleLazyTransformerInAllBatches: Boolean)
    extends AnalysisScenario[A](ps) with SchedulingStrategy[A] {

    val name = "MPS"

    override def schedule(
        ps:    PropertyStore,
        allCS: Set[ComputationSpecification[A]]
    ): List[PhaseConfiguration[A]] = {

        // Creates a map from ComputationSpecifications to their indices
        val computationSpecificationMap: Map[ComputationSpecification[A], Int] = allCS.zipWithIndex.toMap
        val computationSpecificationArray: Array[ComputationSpecification[A]] = allCS.toArray
        // Initializes an empty schedule graph
        var scheduleGraph: Map[Int, Set[Int]] = Map.empty
        print(computationSpecificationArray.mkString("Array(", ", ", ")"))
        // Helper functions and logic for MPS scheduling follow...
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
            css.foldLeft(Set.empty[PropertyBounds]) { (allUses, cs) =>
                val uses = computationSpecificationArray(cs).uses(ps)
                allUses ++ uses
            }
        }

        /**
         * Recursively updates the scheduling map (`map`) by setting specific computations (lazy/transformer) in all relevant batches.
         * The method ensures that computations with the type `lazyType` or `transformerType` are placed correctly in all batches,
         * adjusting dependencies to maintain a valid execution order.
         *
         * Steps:
         * 1. Iterates over batches to find occurrences of `firstElement` and merges it into other batches when necessary.
         * 2. Updates the scheduling graph (`aCyclicGraph`) to reflect changes and remove redundant batches.
         * 3. Recursively processes the next batch until all necessary batches are updated.
         * 4. Ensures computations are properly assigned across batches while preserving execution dependencies.
         */
        def setLazyInAllBatches(
            tmp_aCyclicGraph: Map[List[Int], Set[Int]],
            firstElement:     List[Int]
        ): Map[List[Int], Set[Int]] = {
            var visited_batches: List[List[Int]] = List.empty
            var aCyclicGraph = tmp_aCyclicGraph

            def setLazyInAllBatchesInternal(
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
                        setLazyInAllBatchesInternal(aCyclicGraph, aCyclicGraph.head._1)
                    } else {
                        visited_batches = visited_batches :+ firstElement
                        val keyList = aCyclicGraph.keys.toSet -- visited_batches
                        if (keyList.nonEmpty) {
                            aCyclicGraph = setLazyInAllBatchesInternal(aCyclicGraph, keyList.head)
                        }
                    }
                } else {
                    visited_batches = visited_batches :+ firstElement
                    val keyList = aCyclicGraph.keys.toSet -- visited_batches
                    if (keyList.nonEmpty) {
                        setLazyInAllBatchesInternal(aCyclicGraph, keyList.head)
                    }
                }
                aCyclicGraph
            }
            setLazyInAllBatchesInternal(aCyclicGraph, firstElement)
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

        val transformedGraph = graphWithoutSelfDependencies.map { case (node, deps) =>
            var dependencies: List[Int] = List.empty
            nodeIndexMap.foreach { tuple =>
                if (tuple._2.intersect(deps).nonEmpty) {
                    dependencies = dependencies :+ tuple._1
                }
            }
            nodeIndexMap.find(_._2 == node).map(_._1).head -> dependencies
        }

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

object MaximumPhaseScheduling {
    val name = "MPS"
}
