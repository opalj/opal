/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string

import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFG

import scalax.collection.OneOrMore
import scalax.collection.edges.DiEdge
import scalax.collection.generic.Edge
import scalax.collection.hyperedges.DiHyperEdge
import scalax.collection.immutable.Graph
import scalax.collection.immutable.TypedGraphFactory
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

/**
 * @author Maximilian Rüsch
 */
package object flowanalysis {

    type ControlTree = Graph[FlowGraphNode, DiEdge[FlowGraphNode]]
    type FlowGraph = Graph[FlowGraphNode, DiEdge[FlowGraphNode]]
    type SuperFlowGraph = Graph[FlowGraphNode, Edge[FlowGraphNode]]

    object FlowGraph extends TypedGraphFactory[FlowGraphNode, DiEdge[FlowGraphNode]] {

        def apply[V <: Var[V]](cfg: CFG[Stmt[V], TACStmts[V]]): FlowGraph = {
            val edges = cfg.allNodes.flatMap {
                case bb: BasicBlock =>
                    val firstNode = Statement(bb.startPC)
                    var currentEdges = Seq.empty[DiEdge[FlowGraphNode]]
                    if (bb.startPC != bb.endPC) {
                        Range.inclusive(bb.startPC, bb.endPC).tail.foreach { instrPC =>
                            currentEdges :+= DiEdge(
                                currentEdges.lastOption.map(_.target).getOrElse(firstNode),
                                Statement(instrPC)
                            )
                        }
                    }

                    val lastNode = if (currentEdges.nonEmpty) currentEdges.last.target
                    else firstNode
                    currentEdges ++ bb.successors.map(s => DiEdge(lastNode, Statement(s.nodeId)))
                case n =>
                    n.successors.map(s => DiEdge(Statement(n.nodeId), Statement(s.nodeId)))
            }.toSet
            val g = Graph.from(edges)

            val normalReturnNode = Statement(cfg.normalReturnNode.nodeId)
            val abnormalReturnNode = Statement(cfg.abnormalReturnNode.nodeId)
            val hasNormalReturn = cfg.normalReturnNode.predecessors.nonEmpty
            val hasAbnormalReturn = cfg.abnormalReturnNode.predecessors.nonEmpty

            (hasNormalReturn, hasAbnormalReturn) match {
                case (true, true) =>
                    val allReturnNode = Statement(-42)
                    g.incl(DiEdge(normalReturnNode, allReturnNode)).incl(DiEdge(abnormalReturnNode, allReturnNode))

                case (true, false) =>
                    g.excl(abnormalReturnNode)

                case (false, true) =>
                    g.excl(normalReturnNode)

                case _ =>
                    throw new IllegalStateException(
                        "Cannot transform a CFG with neither normal nor abnormal return edges!"
                    )
            }
        }

        def entryFromCFG[V <: Var[V]](cfg: CFG[Stmt[V], TACStmts[V]]): Statement = Statement(cfg.startBlock.nodeId)

        def toDot[N <: FlowGraphNode, E <: Edge[N]](graph: Graph[N, E]): String = {
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
                    DotAttr(
                        Id("fillcolor"),
                        node match {
                            case Region(_: AcyclicRegionType, _) => Id(""""green"""")
                            case Region(_: CyclicRegionType, _)  => Id(""""purple"""")
                            case _                               => Id(""""white"""")
                        }
                    )
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

        def enrichWithControlTree(
            flowGraph:   FlowGraph,
            controlTree: ControlTree
        ): Graph[FlowGraphNode, Edge[FlowGraphNode]] = {
            var combinedGraph = flowGraph
                .++[FlowGraphNode, DiEdge[FlowGraphNode]](controlTree.nodes.map(_.outer), Iterable.empty)
                .asInstanceOf[Graph[FlowGraphNode, Edge[FlowGraphNode]]]

            for {
                node <- controlTree.nodes.toOuter
                nodes = combinedGraph.nodes.filter((n: Graph[FlowGraphNode, Edge[FlowGraphNode]]#NodeT) =>
                    n.outer.nodeIds.subsetOf(node.nodeIds)
                ).map(_.outer)
                actualSubsetNodes = nodes.filter(n => n.nodeIds != node.nodeIds)
                remainingNodes = actualSubsetNodes.filter(n =>
                    !actualSubsetNodes.exists(nn => n.nodeIds != nn.nodeIds && n.nodeIds.subsetOf(nn.nodeIds))
                )
                if remainingNodes.size > 1
            } {
                combinedGraph = combinedGraph.incl(DiHyperEdge(OneOrMore(node), OneOrMore.from(remainingNodes).get))
            }

            combinedGraph
        }
    }
}
