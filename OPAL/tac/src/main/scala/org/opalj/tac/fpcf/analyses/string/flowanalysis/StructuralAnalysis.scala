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

    def analyze(graph: FlowGraph, entry: FlowGraphNode): (FlowGraph, SuperFlowGraph, ControlTree) = {
        var g = graph
        var sg = graph.asInstanceOf[SuperFlowGraph]
        var curEntry = entry
        var controlTree = Graph.empty[FlowGraphNode, DiEdge[FlowGraphNode]]

        var outerIterations = 0
        while (g.order > 1 && outerIterations < 10000) {
            // Find post order depth first traversal order for nodes
            var postCtr = 1
            val post = mutable.ListBuffer.empty[FlowGraphNode]

            def replace(
                currentGraph:      FlowGraph,
                currentSuperGraph: SuperFlowGraph,
                subNodes:          Set[FlowGraphNode],
                entry:             FlowGraphNode,
                regionType:        RegionType
            ): (FlowGraph, SuperFlowGraph, Region) = {
                val newRegion = Region(regionType, subNodes.flatMap(_.nodeIds), entry)
                var newGraph: FlowGraph = currentGraph
                var newSuperGraph: SuperFlowGraph = currentSuperGraph

                // Compact
                newGraph = newGraph.incl(newRegion)
                newSuperGraph = newSuperGraph.incl(newRegion)
                val maxPost = post.indexOf(subNodes.maxBy(post.indexOf))
                post(maxPost) = newRegion
                // Removing old regions from the graph is done later
                post.filterInPlace(r => !subNodes.contains(r))
                postCtr = post.indexOf(newRegion)

                // Replace edges
                for {
                    e <- newGraph.edges
                } {
                    val source: FlowGraphNode = e.outer.source
                    val target: FlowGraphNode = e.outer.target

                    if (!subNodes.contains(source) && subNodes.contains(target)) {
                        newGraph += DiEdge(source, newRegion)
                        newSuperGraph += DiEdge(source, newRegion)
                        newSuperGraph -= DiEdge(source, target)
                    } else if (subNodes.contains(source) && !subNodes.contains(target)) {
                        newGraph += DiEdge(newRegion, target)
                        newSuperGraph += DiEdge(newRegion, target)
                        newSuperGraph -= DiEdge(source, target)
                    }
                }
                newGraph = newGraph.removedAll(subNodes, Set.empty)
                newSuperGraph = newSuperGraph.incl(DiHyperEdge(OneOrMore(newRegion), OneOrMore.from(subNodes).get))

                (newGraph, newSuperGraph, newRegion)
            }

            PostOrderTraversal.foreachInTraversalFrom[FlowGraphNode, FlowGraph](g, curEntry) { post.append }

            while (g.order > 1 && postCtr < post.size) {
                var n = post(postCtr)

                val gPost = post.map(g.get).reverse.asInstanceOf[mutable.ListBuffer[FlowGraph#NodeT]]
                val (newStartingNode, acyclicRegionOpt) = locateAcyclicRegion(g, gPost, n)
                n = newStartingNode
                if (acyclicRegionOpt.isDefined) {
                    val (arType, nodes, entry) = acyclicRegionOpt.get

                    val (newGraph, newSuperGraph, newRegion) = replace(g, sg, nodes, entry, arType)
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
                            g.get(indexedNodes(index)).diSuccessors.foreach(ds => f(indexedNodes.indexOf(ds.outer)))
                        },
                        index => { f =>
                            g.get(indexedNodes(index)).diPredecessors.foreach(ds => f(indexedNodes.indexOf(ds.outer)))
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
                        val (crType, nodes, entry) = cyclicRegionOpt.get

                        val (newGraph, newSuperGraph, newRegion) = replace(g, sg, nodes, entry, crType)
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
            val nonNFromMTraverser = graph.innerNodeTraverser(graph.get(m), subgraphNodes = _.outer != n)
            graph.nodes.outerIterable.exists { k =>
                k != n &&
                graph.find(DiEdge(k, n)).isDefined &&
                nonNFromMTraverser.pathTo(graph.get(k)).isDefined &&
                domTree.strictlyDominates(indexedNodes.indexOf(n), indexedNodes.indexOf(k))
            }
        }
    }

    private def locateAcyclicRegion[A <: FlowGraphNode, G <: Graph[A, DiEdge[A]]](
        graph:              G,
        postOrderTraversal: mutable.ListBuffer[G#NodeT],
        startingNode:       A
    ): (A, Option[(AcyclicRegionType, Set[A], A)]) = {
        var nSet = Set.empty[graph.NodeT]
        var entry: graph.NodeT = graph.get(startingNode)

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
            entry = n
            n = n.diPredecessors.head
        }
        if (n.diSuccessors.size == 1) {
            nSet += n
            entry = n
        }

        def locateProperAcyclicInterval: Option[AcyclicRegionType] = {
            var currentNodeSet = Set(n)
            var currentSuccessors = n.diSuccessors

            def isStillAcyclic: Boolean = {
                currentSuccessors.forall { node =>
                    val postOrderIndex = postOrderTraversal.indexOf(node)

                    node.diSuccessors.forall(successor => postOrderTraversal.indexOf(successor) >= postOrderIndex)
                }
            }

            var stillAcyclic = isStillAcyclic
            while (currentSuccessors.size > 1 && stillAcyclic) {
                currentNodeSet = currentNodeSet ++ currentSuccessors
                currentSuccessors = currentSuccessors.flatMap(node => node.diSuccessors)
                stillAcyclic = isStillAcyclic
            }

            val allPredecessors = currentNodeSet.excl(n).flatMap(node => node.diPredecessors)
            if (!stillAcyclic) {
                None
            } else if (!allPredecessors.equals(currentNodeSet.diff(currentSuccessors))) {
                None
            } else {
                nSet = currentNodeSet ++ currentSuccessors
                entry = n

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
                entry = n
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
                entry = n
                Some(IfThen)
            } else {
                locateProperAcyclicInterval
            }
        } else if (newDirectSuccessors.size > 2) {
            locateProperAcyclicInterval
        } else {
            None
        }

        (n.outer, rType.map((_, nSet.map(_.outer), entry)))
    }

    private def locateCyclicRegion[A, G <: Graph[A, DiEdge[A]]](
        graph:        G,
        startingNode: A,
        reachUnder:   Set[A]
    ): Option[(CyclicRegionType, Set[A], A)] = {
        if (reachUnder.size == 1) {
            return if (graph.find(DiEdge(startingNode, startingNode)).isDefined)
                Some((SelfLoop, reachUnder, reachUnder.head))
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
            Some((WhileLoop, reachUnder, startingNode))
        } else {
            val enteringNodes =
                reachUnder.filter(graph.get(_).diPredecessors.exists(dp => !reachUnder.contains(dp.outer)))

            if (enteringNodes.size > 1) {
                throw new IllegalStateException("Found more than one entering node for a natural loop!")
            }

            Some((NaturalLoop, reachUnder, enteringNodes.head))
        }
    }
}

object PostOrderTraversal {

    /** @note This function should be kept stable with regards to an ordering on the given graph nodes. */
    def foreachInTraversalFrom[A, G <: Graph[A, DiEdge[A]]](graph: G, initial: A)(nodeHandler: A => Unit)(
        implicit ordering: Ordering[A]
    ): Unit = {
        var visited = Set.empty[A]

        def foreachInTraversal(
            graph: G,
            node:  A
        )(nodeHandler: A => Unit)(implicit ordering: Ordering[A]): Unit = {
            visited = visited + node

            for {
                successor <- (graph.get(node).diSuccessors.map(_.outer) -- visited).toList.sorted
                if !visited.contains(successor)
            } {
                foreachInTraversal(graph, successor)(nodeHandler)
            }

            nodeHandler(node)
        }

        foreachInTraversal(graph, initial)(nodeHandler)
    }
}
