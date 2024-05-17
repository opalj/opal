/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package flowanalysis

import scala.collection.mutable

import org.opalj.graphs.DominatorTree

import scalax.collection.edges.DiEdge
import scalax.collection.immutable.Graph

/**
 * @author Maximilian Rüsch
 */
object StructuralAnalysis {

    def analyze(graph: FlowGraph, entry: Region): (FlowGraph, ControlTree) = {
        var g = graph
        var curEntry = entry
        var controlTree = Graph.empty[Region, DiEdge[Region]]

        var outerIterations = 0
        while (g.order > 1 && outerIterations < 100) {
            // Find post order depth first traversal order for nodes
            var postCtr = 1
            val post = mutable.ListBuffer.empty[Region]

            def replace(g: FlowGraph, subRegions: Set[Region], regionType: RegionType): (FlowGraph, Region) = {
                val newRegion = Region(regionType, subRegions.flatMap(_.nodeIds))
                var newGraph: FlowGraph = g

                // Compact
                newGraph = newGraph.incl(newRegion)
                val maxPost = post.indexOf(subRegions.maxBy(post.indexOf))
                post(maxPost) = newRegion
                // Removing old regions from the graph is done later
                post.filterInPlace(r => !subRegions.contains(r))
                postCtr = post.indexOf(newRegion)

                // Replace edges
                for {
                    e <- newGraph.edges
                } {
                    val source: Region = e.outer.source
                    val target: Region = e.outer.target

                    if (!subRegions.contains(source) && subRegions.contains(target)) {
                        newGraph += DiEdge(source, newRegion)
                    } else if (subRegions.contains(source) && !subRegions.contains(target)) {
                        newGraph += DiEdge(newRegion, target)
                    }
                }
                newGraph = newGraph.removedAll(subRegions, Set.empty)

                (newGraph, newRegion)
            }

            PostOrderTraversal.foreachInTraversalFrom[Region, FlowGraph](g, curEntry)(post.append) { (x, y) =>
                x.nodeIds.head.compare(y.nodeIds.head)
            }

            while (g.order > 1 && postCtr < post.size) {
                var n = post(postCtr)

                val (newStartingNode, acyclicRegionOpt) = locateAcyclicRegion(g, n)
                n = newStartingNode
                if (acyclicRegionOpt.isDefined) {
                    val (arType, nodes) = acyclicRegionOpt.get

                    val (newGraph, newRegion) = replace(g, nodes, arType)
                    g = newGraph
                    for {
                        node <- nodes
                    } {
                        controlTree = controlTree.incl(DiEdge(newRegion, node))
                    }

                    if (nodes.contains(curEntry)) {
                        curEntry = newRegion
                    }
                } else {
                    val indexedNodes = g.nodes.outerIterable.toList
                    val domTree = DominatorTree(
                        indexedNodes.indexOf(curEntry),
                        g.get(curEntry).diPredecessors.nonEmpty,
                        index => { f =>
                            g.get(indexedNodes(index)).diSuccessors.foreach(ds => f(indexedNodes.indexOf(ds)))
                        },
                        index => { f =>
                            g.get(indexedNodes(index)).diPredecessors.foreach(ds => f(indexedNodes.indexOf(ds)))
                        },
                        indexedNodes.size - 1
                    )

                    var reachUnder = Set(n)
                    for {
                        m <- g.nodes.outerIterator
                        if !controlTree.contains(m) || controlTree.get(m).diPredecessors.isEmpty
                        if StructuralAnalysis.pathBack(g, indexedNodes, domTree)(m, n)
                    } {
                        reachUnder = reachUnder.incl(m)
                    }

                    val cyclicRegionOpt = locateCyclicRegion(g, n, reachUnder)
                    if (cyclicRegionOpt.isDefined) {
                        val (crType, nodes) = cyclicRegionOpt.get

                        val (newGraph, newRegion) = replace(g, nodes, crType)
                        g = newGraph
                        for {
                            node <- nodes
                        } {
                            controlTree = controlTree.incl(DiEdge(newRegion, node))
                        }

                        if (nodes.contains(curEntry)) {
                            curEntry = newRegion
                        }
                    } else {
                        postCtr += 1
                    }
                }
            }

            outerIterations += 1
        }

        (g, controlTree)
    }

    private def pathBack[A, G <: Graph[A, DiEdge[A]]](graph: G, indexedNodes: Seq[A], domTree: DominatorTree)(
        m: A,
        n: A
    ): Boolean = {
        if (m == n) {
            false
        } else {
            val graphWithoutN = graph.excl(n)
            graphWithoutN.nodes.outerIterable.exists { k =>
                graphWithoutN.get(m).pathTo(
                    graphWithoutN.get(k)
                ).isDefined &&
                graph.edges.toOuter.contains(DiEdge(k, n)) &&
                (k == n || domTree.strictlyDominates(indexedNodes.indexOf(n), indexedNodes.indexOf(k)))
            }
        }
    }

    private def locateAcyclicRegion[A, G <: Graph[A, DiEdge[A]]](
        graph:        G,
        startingNode: A
    ): (A, Option[(AcyclicRegionType, Set[A])]) = {
        var nSet = Set.empty[A]

        // Expand nSet down
        var n = startingNode
        var p = true
        var s = graph.get(n).diSuccessors.size == 1 // TODO refactor into own node type and `hasSingleSuccessor` / `getSingleSuccessor once running
        while (p & s) {
            nSet += n
            n = graph.get(n).diSuccessors.head.outer
            p = graph.get(n).diPredecessors.size == 1
            s = graph.get(n).diSuccessors.size == 1
        }
        if (p) {
            nSet += n
        }

        // Expand nSet up
        n = startingNode
        p = graph.get(n).diPredecessors.size == 1
        s = true
        while (p & s) {
            nSet += n
            n = graph.get(n).diPredecessors.head.outer
            p = graph.get(n).diPredecessors.size == 1
            s = graph.get(n).diSuccessors.size == 1
        }
        if (s) {
            nSet += n
        }

        val newStartingNode = n
        val newDirectSuccessors = graph.get(newStartingNode).diSuccessors.map(_.outer)

        def locateProperAcyclicInterval: Option[AcyclicRegionType] = {
            assert(newDirectSuccessors.size > 1, "Detection for single direct successors should have already run!")

            var currentNodeSet = Set(n)
            var currentSuccessors = graph.get(n).diSuccessors.map(_.outer)
            while (currentSuccessors.size > 1 && graph.filter(nodeP =
                       node => currentNodeSet.contains(node.outer)
                   ).isAcyclic
            ) {
                currentNodeSet = currentNodeSet ++ currentSuccessors
                currentSuccessors = currentSuccessors.flatMap(node => graph.get(node).diSuccessors.map(_.outer))
            }

            val allPredecessors = currentNodeSet.excl(n).flatMap(node => graph.get(node).diPredecessors.map(_.outer))
            if (graph.filter(nodeP = node => currentNodeSet.contains(node.outer)).isCyclic) {
                None
            } else if (!allPredecessors.equals(currentNodeSet.diff(currentSuccessors))) {
                None
            } else {
                nSet = currentNodeSet ++ currentSuccessors

                Some(Proper)
            }
        }

        val rType = if (nSet.size > 1) {
            // Condition is added to ensure chosen bb does not contain any self loops or other cyclic stuff
            // IMPROVE weaken to allow back edges from the "last" nSet member to the first to enable reductions to self loops
            if (graph.filter(nSet.contains(_)).isAcyclic)
                Some(Block)
            else
                None
        } else if (newDirectSuccessors.size == 2) {
            val m = newDirectSuccessors.head
            val k = newDirectSuccessors.tail.head
            if (graph.get(m).diSuccessors.headOption == graph.get(k).diSuccessors.headOption
                && graph.get(m).diSuccessors.size == 1
                && graph.get(m).diPredecessors.size == 1
                && graph.get(k).diPredecessors.size == 1
            ) {
                nSet = Set(newStartingNode, m, k)
                Some(IfThenElse)
            } else if ((
                           graph.get(m).diSuccessors.size == 1
                               && graph.get(m).diSuccessors.head.outer == k
                               && graph.get(m).diPredecessors.size == 1
                               && graph.get(k).diPredecessors.size == 2
                       ) || (
                           graph.get(k).diSuccessors.size == 1
                           && graph.get(k).diSuccessors.head.outer == m
                           && graph.get(k).diPredecessors.size == 1
                           && graph.get(m).diPredecessors.size == 2
                       )
            ) {
                nSet = Set(newStartingNode, m, k)
                Some(IfThen)
            } else {
                locateProperAcyclicInterval
            }
        } else if (newDirectSuccessors.size > 2) {
            // TODO implement Case as well
            locateProperAcyclicInterval
        } else {
            None
        }

        (newStartingNode, rType.map((_, nSet)))
    }

    private def locateCyclicRegion[A, G <: Graph[A, DiEdge[A]]](
        graph:        G,
        startingNode: A,
        reachUnder:   Set[A]
    ): Option[(CyclicRegionType, Set[A])] = {
        if (reachUnder.size == 1) {
            return if (graph.find(DiEdge(startingNode, startingNode)).isDefined) Some((SelfLoop, reachUnder))
            else None
        }

        if (reachUnder.exists(m => graph.get(startingNode).pathTo(graph.get(m)).isEmpty)) {
            throw new IllegalStateException("This implementation of structural analysis cannot handle improper regions!")
        }

        val m = reachUnder.excl(startingNode).head
        if (graph.get(startingNode).diPredecessors.size == 2
            && graph.get(startingNode).diSuccessors.size == 2
            && graph.get(m).diPredecessors.size == 1
            && graph.get(m).diSuccessors.size == 1
        ) {
            Some((WhileLoop, reachUnder))
        } else {
            Some((NaturalLoop, reachUnder))
        }
    }
}

object PostOrderTraversal {

    private def foreachInTraversal[A, G <: Graph[A, DiEdge[A]]](
        graph:   G,
        toVisit: Seq[A],
        visited: Set[A]
    )(nodeHandler: A => Unit)(implicit ordering: Ordering[A]): Unit = {
        if (toVisit.nonEmpty) {
            val next = toVisit.head
            val nextSuccessors = (graph.get(next).diSuccessors.map(_.outer) -- visited -- toVisit).toList.sorted

            foreachInTraversal(graph, nextSuccessors ++ toVisit.tail, visited + next)(nodeHandler)
            nodeHandler(next)
        }
    }

    def foreachInTraversalFrom[A, G <: Graph[A, DiEdge[A]]](graph: G, initial: A)(nodeHandler: A => Unit)(
        implicit ordering: Ordering[A]
    ): Unit = foreachInTraversal(graph, Seq(initial), Set.empty)(nodeHandler)
}
