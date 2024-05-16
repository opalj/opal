/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package preprocessing

import scala.collection.mutable

import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFG

import scalax.collection.OneOrMore
import scalax.collection.edges.DiEdge
import scalax.collection.generic.Edge
import scalax.collection.hyperedges.DiHyperEdge
import scalax.collection.immutable.Graph
import scalax.collection.io.dot.DotAttr
import scalax.collection.io.dot.DotAttrStmt
import scalax.collection.io.dot.DotEdgeStmt
import scalax.collection.io.dot.DotGraph
import scalax.collection.io.dot.DotNodeStmt
import scalax.collection.io.dot.DotRootGraph
import scalax.collection.io.dot.Elem
import scalax.collection.io.dot.Graph2DotExport
import scalax.collection.io.dot.Id
import scalax.collection.io.dot.NodeId

trait RegionType extends Product

trait AcyclicRegionType extends RegionType
trait CyclicRegionType extends RegionType

case object Block extends AcyclicRegionType
case object IfThen extends AcyclicRegionType
case object IfThenElse extends AcyclicRegionType
case object Case extends AcyclicRegionType
case object Proper extends AcyclicRegionType
case object SelfLoop extends CyclicRegionType
case object WhileLoop extends CyclicRegionType
case object NaturalLoop extends CyclicRegionType
case object Improper extends CyclicRegionType

case class Region(regionType: RegionType, nodeIds: Set[Int]) {

    override def toString: String = s"Region(${regionType.productPrefix}; ${nodeIds.toList.sorted.mkString(",")})"
}

/**
 * @author Maximilian Rüsch
 */
class StructuralAnalysis(cfg: CFG[Stmt[V], TACStmts[V]]) {
    type SGraph = Graph[Region, DiEdge[Region]]
    type ControlTree = Graph[Region, DiEdge[Region]]

    val graph: SGraph = {
        val edges = cfg.allNodes.flatMap {
            case bb: BasicBlock =>
                val firstNode = Region(Block, Set(bb.startPC))
                var currentEdges = Seq.empty[DiEdge[Region]]
                if (bb.startPC != bb.endPC) {
                    Range.inclusive(bb.startPC, bb.endPC).tail.foreach { instrPC =>
                        currentEdges :+= DiEdge(
                            currentEdges.lastOption.map(_.target).getOrElse(firstNode),
                            Region(Block, Set(instrPC))
                        )
                    }
                }

                val lastNode = if (currentEdges.nonEmpty) currentEdges.last.target
                else firstNode
                currentEdges ++ bb.successors.map(s => DiEdge(lastNode, Region(Block, Set(s.nodeId))))
            case n =>
                n.successors.map(s => DiEdge(Region(Block, Set(n.nodeId)), Region(Block, Set(s.nodeId))))
        }.toSet
        var g = Graph.from(edges)

        val allReturnNode = Region(Block, Set(-42))
        g = g.incl(DiEdge(Region(Block, Set(cfg.normalReturnNode.nodeId)), allReturnNode))
        g = g.incl(DiEdge(Region(Block, Set(cfg.abnormalReturnNode.nodeId)), allReturnNode))

        g
    }
    val entry: Region = Region(Block, Set(cfg.startBlock.nodeId))

    def graphDot[N <: Region, E <: Edge[N]](graph: Graph[N, E]): String = {
        val root = DotRootGraph(
            directed = true,
            id = Some(Id("MyDot")),
            attrStmts = List(DotAttrStmt(Elem.node, List(DotAttr(Id("shape"), Id("record"))))),
            attrList = List(DotAttr(Id("attr_1"), Id(""""one"""")), DotAttr(Id("attr_2"), Id("<two>")))
        )

        def edgeTransformer(innerEdge: Graph[N, E]#EdgeT): Option[(DotGraph, DotEdgeStmt)] = {
            val edge = innerEdge.outer
            Some(
                (
                    root,
                    DotEdgeStmt(NodeId(edge.sources.head.toString), NodeId(edge.targets.head.toString))
                )
            )
        }

        def hEdgeTransformer(innerHEdge: Graph[N, E]#EdgeT): Iterable[(DotGraph, DotEdgeStmt)] = {
            val color = DotAttr(Id("color"), Id(s""""#%06x"""".format(scala.util.Random.nextInt(1 << 24))))

            innerHEdge.outer.targets.toList map (target =>
                (
                    root,
                    DotEdgeStmt(
                        NodeId(innerHEdge.outer.sources.head.toString),
                        NodeId(target.toString),
                        Seq(color)
                    )
                )
            )
        }

        def nodeTransformer(innerNode: Graph[N, E]#NodeT): Option[(DotGraph, DotNodeStmt)] = {
            val node = innerNode.outer
            val attributes = if (node.nodeIds.size == 1) Seq.empty
            else Seq(
                DotAttr(Id("style"), Id("filled")),
                DotAttr(Id("fillcolor"), Id("\"green\""))
            )
            Some(
                (
                    root,
                    DotNodeStmt(NodeId(node.toString), attributes)
                )
            )
        }

        graph.toDot(
            root,
            edgeTransformer,
            hEdgeTransformer = Some(hEdgeTransformer),
            cNodeTransformer = Some(nodeTransformer),
            iNodeTransformer = Some(nodeTransformer)
        )
    }

    def analyze(graph: SGraph, entry: Region): (Graph[Region, DiEdge[Region]], Graph[Region, DiEdge[Region]]) = {
        if (graph.isCyclic) {
            throw new IllegalArgumentException("The passed graph must not be cyclic!")
        }

        var g = graph
        var curEntry = entry
        var controlTree = Graph.empty[Region, DiEdge[Region]]

        var outerIterations = 0
        while (g.order > 1 && outerIterations < 100) {
            // Find post order depth first traversal order for nodes
            var postCtr = 1
            val post = mutable.ListBuffer.empty[Region]

            def replace(g: SGraph, subRegions: Set[Region], regionType: RegionType): (SGraph, Region) = {
                val newRegion = Region(regionType, subRegions.flatMap(_.nodeIds))

                var newGraph: SGraph = g

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

                    if (!subRegions.contains(source) && subRegions.contains(target) && source != newRegion) {
                        newGraph += DiEdge(source, newRegion)
                    } else if (subRegions.contains(source) && !subRegions.contains(target) && target != newRegion) {
                        newGraph += DiEdge(newRegion, target)
                    }
                }
                newGraph = newGraph.removedAll(subRegions, Set.empty)

                (newGraph, newRegion)
            }

            PostOrderTraversal.foreachInTraversalFrom[Region, SGraph](g, curEntry)(post.append) { (x, y) =>
                x.nodeIds.head.compare(y.nodeIds.head)
            }

            while (g.order > 1 && postCtr < post.size) {
                var n = post(postCtr)

                val (newStartingNode, acyclicRegionOpt) = AcyclicRegionType.locate(g, n)
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
                    // Detect cyclic region
                    postCtr += 1
                }
            }

            outerIterations += 1
        }

        (g, controlTree)
    }

    def combine(cfg: SGraph, controlTree: SGraph): Graph[Region, Edge[Region]] = {
        var combinedGraph = cfg
            .++[Region, DiEdge[Region]](controlTree.nodes.map(_.outer), Iterable.empty)
            .asInstanceOf[Graph[Region, Edge[Region]]]

        for {
            iNode <- controlTree.nodes
            nodes = combinedGraph.nodes.filter((n: Graph[Region, Edge[Region]]#NodeT) =>
                n.outer.nodeIds.subsetOf(iNode.outer.nodeIds)
            ).map(_.outer)
            actualSubsetNodes = nodes.filter(n => n.nodeIds != iNode.outer.nodeIds)
            remainingNodes = actualSubsetNodes.filter(n =>
                !actualSubsetNodes.exists(nn => n.nodeIds != nn.nodeIds && n.nodeIds.subsetOf(nn.nodeIds))
            )
            if remainingNodes.size > 1
        } {
            combinedGraph = combinedGraph.incl(DiHyperEdge(OneOrMore(iNode.outer), OneOrMore.from(remainingNodes).get))
        }

        combinedGraph
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

object AcyclicRegionType {
    def locate[A, G <: Graph[A, DiEdge[A]]](graph: G, startingNode: A): (A, Option[(AcyclicRegionType, Set[A])]) = {
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
            Some(Block)
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
}
