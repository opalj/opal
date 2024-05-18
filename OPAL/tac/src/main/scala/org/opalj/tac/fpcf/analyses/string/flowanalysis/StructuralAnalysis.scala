/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package flowanalysis

import scala.collection.mutable

import org.opalj.graphs.DominatorTree

import scalax.collection.OneOrMore
import scalax.collection.edges.DiEdge
import scalax.collection.hyperedges.DiHyperEdge
import scalax.collection.immutable.Graph

/**
 * @author Maximilian Rüsch
 */
object StructuralAnalysis {

    def analyze(graph: FlowGraph, entry: Region): (FlowGraph, SuperFlowGraph, ControlTree) = {
        var g = graph
        var sg = graph.asInstanceOf[SuperFlowGraph]
        var curEntry = entry
        var controlTree = Graph.empty[Region, DiEdge[Region]]

        var outerIterations = 0
        while (g.order > 1 && outerIterations < 100) {
            // Find post order depth first traversal order for nodes
            var postCtr = 1
            val post = mutable.ListBuffer.empty[Region]

            def replace(
                currentGraph:      FlowGraph,
                currentSuperGraph: SuperFlowGraph,
                subRegions:        Set[Region],
                regionType:        RegionType
            ): (FlowGraph, SuperFlowGraph, Region) = {
                val newRegion = Region(regionType, subRegions.flatMap(_.nodeIds))
                var newGraph: FlowGraph = currentGraph
                var newSuperGraph: SuperFlowGraph = currentSuperGraph

                // Compact
                newGraph = newGraph.incl(newRegion)
                newSuperGraph = newSuperGraph.incl(newRegion)
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
                        newSuperGraph += DiEdge(source, newRegion)
                        newSuperGraph -= DiEdge(source, target)
                    } else if (subRegions.contains(source) && !subRegions.contains(target)) {
                        newGraph += DiEdge(newRegion, target)
                        newSuperGraph += DiEdge(newRegion, target)
                        newSuperGraph -= DiEdge(source, target)
                    }
                }
                newGraph = newGraph.removedAll(subRegions, Set.empty)
                newSuperGraph = newSuperGraph.incl(DiHyperEdge(OneOrMore(newRegion), OneOrMore.from(subRegions).get))

                (newGraph, newSuperGraph, newRegion)
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

                    val (newGraph, newSuperGraph, newRegion) = replace(g, sg, nodes, arType)
                    g = newGraph
                    sg = newSuperGraph
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

                        val (newGraph, newSuperGraph, newRegion) = replace(g, sg, nodes, crType)
                        g = newGraph
                        sg = newSuperGraph
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

        (g, sg, controlTree)
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
        var nSet = Set.empty[graph.NodeT]

        // Expand nSet down
        var n = graph.get(startingNode)
        while ((n.outer == startingNode || n.diPredecessors.size == 1) && n.diSuccessors.size == 1) {
            nSet += n
            n = n.diSuccessors.head
        }
        if (n.diPredecessors.size == 1) {
            nSet += n
        }

        // Expand nSet up
        n = graph.get(startingNode)
        while (n.diPredecessors.size == 1 && (n.outer == startingNode || n.diSuccessors.size == 1)) {
            nSet += n
            n = n.diPredecessors.head
        }
        if (n.diSuccessors.size == 1) {
            nSet += n
        }

        def locateProperAcyclicInterval: Option[AcyclicRegionType] = {
            var currentNodeSet = Set(n)
            var currentSuccessors = n.diSuccessors
            while (currentSuccessors.size > 1 && graph.filter(node => currentNodeSet.contains(node)).isAcyclic) {
                currentNodeSet = currentNodeSet ++ currentSuccessors
                currentSuccessors = currentSuccessors.flatMap(node => node.diSuccessors)
            }

            val allPredecessors = currentNodeSet.excl(n).flatMap(node => node.diPredecessors)
            if (graph.filter(node => currentNodeSet.contains(node)).isCyclic) {
                None
            } else if (!allPredecessors.equals(currentNodeSet.diff(currentSuccessors))) {
                None
            } else {
                nSet = currentNodeSet ++ currentSuccessors

                Some(Proper)
            }
        }

        val newDirectSuccessors = n.diSuccessors
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
            if (m.diSuccessors.headOption == k.diSuccessors.headOption
                && m.diSuccessors.size == 1
                && m.diPredecessors.size == 1
                && k.diPredecessors.size == 1
            ) {
                nSet = Set(n, m, k)
                Some(IfThenElse)
            } else if ((
                           m.diSuccessors.size == 1
                               && m.diSuccessors.head == k
                               && m.diPredecessors.size == 1
                               && k.diPredecessors.size == 2
                       ) || (
                           k.diSuccessors.size == 1
                           && k.diSuccessors.head == m
                           && k.diPredecessors.size == 1
                           && m.diPredecessors.size == 2
                       )
            ) {
                nSet = Set(n, m, k)
                Some(IfThen)
            } else {
                locateProperAcyclicInterval
            }
        } else if (newDirectSuccessors.size > 2) {
            locateProperAcyclicInterval
        } else {
            None
        }

        (n.outer, rType.map((_, nSet.map(_.outer))))
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
