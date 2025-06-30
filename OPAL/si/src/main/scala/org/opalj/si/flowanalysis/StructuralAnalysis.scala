/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package si
package flowanalysis

import scala.collection.mutable

import org.opalj.graphs.DominatorTree

import scalax.collection.GraphTraversal.DepthFirst
import scalax.collection.GraphTraversal.Parameters
import scalax.collection.OneOrMore
import scalax.collection.OuterEdge
import scalax.collection.edges.DiEdge
import scalax.collection.generic.Edge
import scalax.collection.hyperedges.DiHyperEdge
import scalax.collection.immutable.Graph
import scalax.collection.mutable.{Graph => MutableGraph}

/**
 * An algorithm that identifies several different types of flow regions in a given flow graph and reduces them to a
 * single node iteratively. The algorithm terminates either when a single node is left in the flow graph or such a state
 * could not be reached after [[maxIterations]].
 *
 * On termination, the [[analyze]] function returns:
 * <ol>
 *   <li>
 *     The reduced flow graph, a single node equal to the root node of the control tree.
 *   </li>
 *   <li>
 *     A <i>super flow graph</i> as a combination of the given source flow graph and the control tree. For each
 *     node contained in the control tree, the super flow graph contains the node itself and edges to its children
 *     as referenced the control tree. However, its children are still connected with edges as contained in the
 *     source flow graph.
 *     <br>
 *     This representation eases traversal for data flow analysis such as by [[DataFlowAnalysis]].
 *   </li>
 *   <li>
 *     The control tree, as a hierarchic representation of the control flow regions identified by the algorithm.
 *   </li>
 * </ol>
 *
 * This algorithm is adapted from Muchnick, S.S. (1997). Advanced Compiler Design and Implementation and optimized for
 * performance.
 *
 * @see [[FlowGraphNode]], [[DataFlowAnalysis]]
 *
 * @author Maximilian Rüsch
 */
object StructuralAnalysis {

    private final val maxIterations = 1000

    private type MFlowGraph = MutableGraph[FlowGraphNode, DiEdge[FlowGraphNode]]
    private type MControlTree = MutableGraph[FlowGraphNode, DiEdge[FlowGraphNode]]
    private type MSuperFlowGraph = MutableGraph[FlowGraphNode, Edge[FlowGraphNode]]

    def analyze(initialGraph: FlowGraph, entry: FlowGraphNode): (FlowGraph, SuperFlowGraph, ControlTree) = {
        val flowGraph: MFlowGraph = MutableGraph.from(initialGraph.edges.outerIterable)
        val superFlowGraph: MSuperFlowGraph =
            MutableGraph.from(initialGraph.edges.outerIterable).asInstanceOf[MSuperFlowGraph]
        var currentEntry = entry
        val controlTree: MControlTree = MutableGraph.empty[FlowGraphNode, DiEdge[FlowGraphNode]]

        var (immediateDominators, allDominators) = computeDominators(flowGraph, entry)
        /**
         * @return True when the given node n strictly dominates the node w.
         */
        def strictlyDominates(n: FlowGraphNode, w: FlowGraphNode): Boolean = n != w && allDominators(w).contains(n)

        val knownPartOfNoCycle = mutable.Set.empty[FlowGraphNode]
        def inCycle(n: FlowGraphNode): Boolean = {
            if (knownPartOfNoCycle.contains(n)) {
                false
            } else {
                // IMPROVE if no cycle is found, we can use the visitor of `findCycleContaining` to identify more nodes
                // that are known to not be part of a cycle and add them to `knownPartOfNoCycle`.
                val cycleOpt = flowGraph.findCycleContaining(flowGraph.get(n))
                if (cycleOpt.isDefined) {
                    true
                } else {
                    knownPartOfNoCycle.add(n)
                    false
                }
            }
        }

        var iterations = 0
        while (flowGraph.order > 1 && iterations < maxIterations) {
            // Find post order depth first traversal order for nodes
            var postCtr = 1
            val post = mutable.ListBuffer.empty[FlowGraphNode]

            def replace(subNodes: Set[FlowGraphNode], entry: FlowGraphNode, regionType: RegionType): Unit = {
                val newRegion = Region(regionType, subNodes.flatMap(_.nodeIds), entry)

                // Compact
                // Note that adding the new region to the graph and superGraph is done anyways since we add edges later
                val maxPost = post.indexOf(subNodes.maxBy(post.indexOf))
                post(maxPost) = newRegion
                // Removing old regions from the graph is done later
                post.filterInPlace(r => !subNodes.contains(r))
                postCtr = post.indexOf(newRegion)

                if (subNodes.forall(knownPartOfNoCycle.contains)) {
                    knownPartOfNoCycle.add(newRegion)
                }
                knownPartOfNoCycle.subtractAll(subNodes)

                // Replace edges
                val incomingEdges = flowGraph.edges.filter { e =>
                    !subNodes.contains(e.outer.source) && subNodes.contains(e.outer.target)
                }
                val outgoingEdges = flowGraph.edges.filter { e =>
                    subNodes.contains(e.outer.source) && !subNodes.contains(e.outer.target)
                }

                val newRegionEdges = incomingEdges.map { e =>
                    OuterEdge[FlowGraphNode, DiEdge[FlowGraphNode]](DiEdge(e.outer.source, newRegion))
                }.concat(outgoingEdges.map { e =>
                    OuterEdge[FlowGraphNode, DiEdge[FlowGraphNode]](DiEdge(newRegion, e.outer.target))
                })
                flowGraph.addAll(newRegionEdges)
                flowGraph.removeAll(subNodes, Set.empty)

                superFlowGraph.addAll(newRegionEdges)
                superFlowGraph.removeAll {
                    incomingEdges.concat(outgoingEdges).map(e => DiEdge(e.outer.source, e.outer.target))
                        .concat(Seq(DiHyperEdge(OneOrMore(newRegion), OneOrMore.from(subNodes).get)))
                }

                // Update dominator data
                val commonDominators = subNodes.map(allDominators).reduce(_.intersect(_))
                allDominators.subtractAll(subNodes).update(newRegion, commonDominators)
                allDominators = allDominators.map(kv =>
                    (
                        kv._1, {
                            val index = kv._2.indexWhere(subNodes.contains)
                            if (index != -1)
                                kv._2.patch(index, Seq(newRegion), kv._2.lastIndexWhere(subNodes.contains) - index + 1)
                            else
                                kv._2
                        }
                    )
                )
                immediateDominators = allDominators.map(kv => (kv._1, kv._2.head))

                // Update remaining graph state
                controlTree.addAll(subNodes.map(node =>
                    OuterEdge[FlowGraphNode, DiEdge[FlowGraphNode]](DiEdge(newRegion, node))
                ))
                if (subNodes.contains(currentEntry)) {
                    currentEntry = newRegion
                }
            }

            // Determine post-order depth-first traversal in the given graph
            val ordering = flowGraph.NodeOrdering((in1, in2) => in1.compare(in2))
            flowGraph.innerNodeDownUpTraverser(
                flowGraph.get(currentEntry),
                Parameters(DepthFirst),
                ordering = ordering
            ).foreach {
                case (down, in) if !down => post.append(in.outer)
                case _                   =>
            }

            while (flowGraph.order > 1 && postCtr < post.size) {
                var n = post(postCtr)

                val gPostMap =
                    post.reverse.zipWithIndex.map(ni =>
                        (flowGraph.get(ni._1).asInstanceOf[MFlowGraph#NodeT], ni._2)
                    ).toMap
                val (newStartingNode, acyclicRegionOpt) =
                    locateAcyclicRegion[FlowGraphNode, MFlowGraph](flowGraph, gPostMap, allDominators)(n)
                n = newStartingNode
                if (acyclicRegionOpt.isDefined) {
                    val (arType, nodes, entry) = acyclicRegionOpt.get
                    replace(nodes, entry, arType)
                } else if (inCycle(n)) {
                    var reachUnder = Set(n)
                    for {
                        m <- flowGraph.nodes.outerIterator
                        if m != n
                        innerM = controlTree.find(m)
                        if innerM.isEmpty || !innerM.get.hasPredecessors
                        if StructuralAnalysis.pathBack[FlowGraphNode, MFlowGraph](flowGraph, strictlyDominates)(m, n)
                    } {
                        reachUnder = reachUnder.incl(m)
                    }

                    val cyclicRegionOpt = locateCyclicRegion[FlowGraphNode, MFlowGraph](flowGraph, n, reachUnder)
                    if (cyclicRegionOpt.isDefined) {
                        val (crType, nodes, entry) = cyclicRegionOpt.get
                        replace(nodes, entry, crType)
                    } else {
                        postCtr += 1
                    }
                } else {
                    postCtr += 1
                }
            }

            iterations += 1
        }

        if (iterations >= maxIterations) {
            throw new IllegalStateException(s"Could not reduce tree in $maxIterations iterations!")
        }

        (
            Graph.from(flowGraph.edges.outerIterable),
            Graph.from(superFlowGraph.edges.outerIterable),
            Graph.from(controlTree.edges.outerIterable)
        )
    }

    /**
     * Computes the immediate and global dominators for each of the nodes in the given graph.
     *
     * @param graph The graph to compute dominators for.
     * @param entry The entry node to the graph under analysis.
     * @return (A map for immediate dominators by graph node, A map for all dominators of a graph node by graph node)
     */
    private def computeDominators[A, G <: MutableGraph[A, DiEdge[A]]](
        graph: G,
        entry: A
    ): (mutable.Map[A, A], mutable.Map[A, Seq[A]]) = {
        val indexedNodes = graph.nodes.toIndexedSeq
        val indexOf = indexedNodes.zipWithIndex.toMap
        val domTree = DominatorTree(
            indexOf(graph.get(entry)),
            graph.get(entry).hasPredecessors,
            index => { f => indexedNodes(index).diSuccessors.foreach(ds => f(indexOf(ds))) },
            index => { f => indexedNodes(index).diPredecessors.foreach(ds => f(indexOf(ds))) },
            indexedNodes.size - 1
        )
        val outerIndexedNodes = indexedNodes.map(_.outer)
        val immediateDominators = mutable.Map.from {
            domTree.immediateDominators.zipWithIndex.map(iDomWithIndex => {
                (outerIndexedNodes(iDomWithIndex._2), outerIndexedNodes(iDomWithIndex._1))
            })
        }
        immediateDominators.update(entry, entry)

        def getAllDominators(n: A): Seq[A] = {
            val builder = Seq.newBuilder[A]
            var c = n
            while (c != entry) {
                builder.addOne(c)
                c = immediateDominators(c)
            }
            builder.addOne(entry)
            builder.result()
        }
        val allDominators = immediateDominators.map(kv => (kv._1, getAllDominators(kv._2)))

        (immediateDominators, allDominators)
    }

    /**
     * Determines if a path exists from the given node m to some other node k that does not contain the node n AND an
     * edge k -> n exists that is a back edge in the given graph. The latter is realized by using predecessors to find
     * eligible edges and using a strict domination predicate to determine a back edge from it.
     *
     * @param graph The graph under analysis.
     * @param strictlyDominates Predicate if a given first node strictly dominates the given second nodes in the given graph.
     * @param m The node that forms the starting node of the path.
     * @param n The node that forms the ending node of the path.
     * @return True if there is path back from m to n over some intermediate node k where m -> k does not contain n.
     */
    private def pathBack[A, G <: MutableGraph[A, DiEdge[A]]](graph: G, strictlyDominates: (A, A) => Boolean)(
        m: A,
        n: A
    ): Boolean = {
        val innerN = graph.get(n)
        val nonNFromMTraverser = graph.innerNodeTraverser(graph.get(m), subgraphNodes = _ != innerN)
        val predecessorsOfN = innerN.diPredecessors
        graph.nodes.exists { innerK =>
            innerK.outer != n &&
            predecessorsOfN.contains(innerK) &&
            strictlyDominates(n, innerK.outer) &&
            nonNFromMTraverser.pathTo(innerK).isDefined
        }
    }

    /**
     * Identifies a candidate acyclic region if one can be found starting the identification at the given start node.
     * If no region can be found, a new starting node is returned that can be used to resume searching for other region
     * types. If a region can be found, all information needed to replace the contained nodes with a new region node in
     * the graph is returned.
     *
     * @param graph The graph under analysis.
     * @param postOrderTraversal A post order DFS traversal of the given graph as a mapping from node to its DF position.
     * @param allDominators A dominator index as produced by [[computeDominators]].
     * @param startingNode The node to start locating the acyclic region at.
     * @return The new starting node of a candidate region as well as an option of: 1. The type of the found acyclic
     *         region, 2. a set containing all region nodes and 3. the entry node to the region.
     */
    private def locateAcyclicRegion[A <: FlowGraphNode, G <: MutableGraph[A, DiEdge[A]]](
        graph:              G,
        postOrderTraversal: Map[G#NodeT, Int],
        allDominators:      mutable.Map[A, Seq[A]]
    )(startingNode: A): (A, Option[(AcyclicRegionType, Set[A], A)]) = {
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
            val dominatedNodes = allDominators.filter(_._2.contains(n.outer)).map(kv => graph.get(kv._1)).toSet ++ Set(n)
            if (dominatedNodes.size == 1 ||
                !isAcyclic(dominatedNodes) ||
                // Check if no dominated node is reached from a non-dominated node
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
            // Condition is added to ensure chosen bb does not contain any self loops or other cyclic regions
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

        (n.outer, rType.map((_, nSet.map(_.outer), entry.outer)))
    }

    /**
     * Identifies a candidate cyclic region if one can be found starting the identification at the given start node.
     *
     * @param graph The graph under analysis.
     * @param startingNode The node to start identifying cyclic regions at.
     * @param reachUnder A set of all nodes that can be reached starting from the given starting node that have a path
     *                   back to the given start node.
     * @return An option of: 1. The cyclic region type, 2. the nodes contained in the region and 3. the entry node of
     *         the region.
     *
     * @note This implementation does not yet support improper regions and their reduction and will throw upon their
     *       detection.
     */
    private def locateCyclicRegion[A, G <: MutableGraph[A, DiEdge[A]]](
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
            // IMPROVE reliably detect size of improper regions and reduce
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
