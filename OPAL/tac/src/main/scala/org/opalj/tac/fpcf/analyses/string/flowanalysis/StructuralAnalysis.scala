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

    private final val maxIterations = 1000

    def analyze(graph: FlowGraph, entry: FlowGraphNode): (FlowGraph, SuperFlowGraph, ControlTree) = {
        var g = graph
        var sg = graph.asInstanceOf[SuperFlowGraph]
        var curEntry = entry
        var controlTree = Graph.empty[FlowGraphNode, DiEdge[FlowGraphNode]]

        var iterations = 0
        while (g.order > 1 && iterations < maxIterations) {
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
                // Note that adding the new region to the graph and superGraph is done anyways since we add edges later
                val maxPost = post.indexOf(subNodes.maxBy(post.indexOf))
                post(maxPost) = newRegion
                // Removing old regions from the graph is done later
                post.filterInPlace(r => !subNodes.contains(r))
                postCtr = post.indexOf(newRegion)

                // Replace edges
                val incomingEdges = currentGraph.edges.filter { e =>
                    !subNodes.contains(e.outer.source) && subNodes.contains(e.outer.target)
                }
                val outgoingEdges = currentGraph.edges.filter { e =>
                    subNodes.contains(e.outer.source) && !subNodes.contains(e.outer.target)
                }

                newGraph ++= incomingEdges.map(e => DiEdge(e.outer.source, newRegion))
                    .concat(outgoingEdges.map(e => DiEdge(newRegion, e.outer.target)))
                newGraph = newGraph.removedAll(subNodes, Set.empty)

                newSuperGraph ++= incomingEdges.map(e => DiEdge(e.outer.source, newRegion))
                    .concat(outgoingEdges.map(e => DiEdge(newRegion, e.outer.target)))
                newSuperGraph --= incomingEdges.concat(outgoingEdges).map(e => DiEdge(e.outer.source, e.outer.target))
                    .concat(Seq(DiHyperEdge(OneOrMore(newRegion), OneOrMore.from(subNodes).get)))

                (newGraph, newSuperGraph, newRegion)
            }

            PostOrderTraversal.foreachInTraversalFrom[FlowGraphNode, FlowGraph](g, curEntry) { post.append }

            while (g.order > 1 && postCtr < post.size) {
                var n = post(postCtr)

                val indexedNodes = g.nodes.toIndexedSeq
                val indexOf = indexedNodes.zipWithIndex.toMap
                val domTree = DominatorTree(
                    indexOf(g.get(curEntry)),
                    g.get(curEntry).hasPredecessors,
                    index => { f => indexedNodes(index).diSuccessors.foreach(ds => f(indexOf(ds))) },
                    index => { f => indexedNodes(index).diPredecessors.foreach(ds => f(indexOf(ds))) },
                    indexedNodes.size - 1
                )
                val gPostMap = post.reverse.zipWithIndex.map(ni => (g.get(ni._1), ni._2)).toMap
                val (newStartingNode, acyclicRegionOpt) = locateAcyclicRegion(g, gPostMap, n, indexedNodes, domTree)
                n = newStartingNode
                if (acyclicRegionOpt.isDefined) {
                    val (arType, nodes, entry) = acyclicRegionOpt.get

                    val (newGraph, newSuperGraph, newRegion) = replace(g, sg, nodes, entry, arType)
                    g = newGraph
                    sg = newSuperGraph
                    controlTree = controlTree.concat(nodes.map(node => DiEdge(newRegion, node)))

                    if (nodes.contains(curEntry)) {
                        curEntry = newRegion
                    }
                } else {
                    var reachUnder = Set(n)
                    for {
                        m <- g.nodes.outerIterator
                        innerM = controlTree.find(m)
                        if innerM.isEmpty || !innerM.get.hasPredecessors
                        if StructuralAnalysis.pathBack[FlowGraphNode, FlowGraph](g, indexOf, domTree)(m, n)
                    } {
                        reachUnder = reachUnder.incl(m)
                    }

                    val cyclicRegionOpt = locateCyclicRegion(g, n, reachUnder)
                    if (cyclicRegionOpt.isDefined) {
                        val (crType, nodes, entry) = cyclicRegionOpt.get

                        val (newGraph, newSuperGraph, newRegion) = replace(g, sg, nodes, entry, crType)
                        g = newGraph
                        sg = newSuperGraph
                        controlTree = controlTree.concat(nodes.map(node => DiEdge(newRegion, node)))

                        if (nodes.contains(curEntry)) {
                            curEntry = newRegion
                        }
                    } else {
                        postCtr += 1
                    }
                }
            }

            iterations += 1
        }

        if (iterations >= maxIterations) {
            throw new IllegalStateException(s"Could not reduce tree in $maxIterations iterations!")
        }

        (g, sg, controlTree)
    }

    private def pathBack[A, G <: Graph[A, DiEdge[A]]](graph: G, indexOf: Map[G#NodeT, Int], domTree: DominatorTree)(
        m: A,
        n: A
    ): Boolean = {
        if (m == n) {
            false
        } else {
            val innerN = graph.get(n)
            val nonNFromMTraverser = graph.innerNodeTraverser(graph.get(m), subgraphNodes = _ != innerN)
            val predecessorsOfN = innerN.diPredecessors
            graph.nodes.exists { innerK =>
                innerK != innerN &&
                predecessorsOfN.contains(innerK) &&
                domTree.strictlyDominates(indexOf(innerN), indexOf(innerK)) &&
                nonNFromMTraverser.pathTo(innerK).isDefined
            }
        }
    }

    private def locateAcyclicRegion[A <: FlowGraphNode, G <: Graph[A, DiEdge[A]]](
        graph:              G,
        postOrderTraversal: Map[G#NodeT, Int],
        startingNode:       A,
        indexedNodes:       IndexedSeq[G#NodeT],
        domTree:            DominatorTree
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

        def isAcyclic(nodes: Set[graph.NodeT]): Boolean = {
            nodes.forall { node =>
                val postOrderIndex = postOrderTraversal(node)
                node.diSuccessors.forall { successor =>
                    !nodes.contains(successor) || postOrderTraversal(successor) >= postOrderIndex
                }
            }
        }

        def locateProperAcyclicInterval: Option[AcyclicRegionType] = {
            val zippedImmediateDominators = domTree.immediateDominators.zipWithIndex;
            var accumulatedDominatedIndexes = Set.empty[Int];
            var newDominatedIndexes = Set(indexedNodes.indexOf(n))
            while (newDominatedIndexes.nonEmpty) {
                accumulatedDominatedIndexes = accumulatedDominatedIndexes.union(newDominatedIndexes)
                newDominatedIndexes = newDominatedIndexes
                    .flatMap(ndi => zippedImmediateDominators.filter(ndi == _._1).map(_._2))
                    .diff(accumulatedDominatedIndexes)
            }

            val dominatedNodes = accumulatedDominatedIndexes.map(indexedNodes).asInstanceOf[Set[graph.NodeT]]
            if (dominatedNodes.size == 1 ||
                !isAcyclic(dominatedNodes) ||
                // Check if no dominated node is reached from an non-dominated node
                !dominatedNodes.excl(n).forall(_.diPredecessors.subsetOf(dominatedNodes)) ||
                // Check if all dominated nodes agree on a single successor outside the set (if it exists)
                dominatedNodes.flatMap(node => node.diSuccessors.diff(dominatedNodes)).size > 1
            ) {
                None
            } else {
                nSet = dominatedNodes
                entry = n

                Some(Proper)
            }
        }

        val newDirectSuccessors = n.diSuccessors
        val rType = if (nSet.size > 1) {
            // Condition is added to ensure chosen bb does not contain any self loops or other cyclic stuff
            // IMPROVE weaken to allow back edges from the "last" nSet member to the first to enable reductions to self loops
            if (isAcyclic(nSet))
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
            } else if (enteringNodes.isEmpty) {
                throw new IllegalStateException("Found no entering node for a natural loop!")
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
        implicit val innerOrdering: Ordering[graph.NodeT] = ordering.on(_.outer)
        var visited = Set.empty[graph.NodeT]

        def foreachInTraversal(
            node: graph.NodeT
        )(nodeHandler: A => Unit): Unit = {
            visited = visited + node

            for {
                successor <- (node.diSuccessors -- visited).toList.sorted
            } {
                foreachInTraversal(successor)(nodeHandler)
            }

            nodeHandler(node.outer)
        }

        foreachInTraversal(graph.get(initial))(nodeHandler)
    }
}
