/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package flowanalysis

import scala.collection.mutable

import org.opalj.br.fpcf.properties.string.StringTreeDynamicString
import org.opalj.tac.fpcf.properties.string.StringFlowFunction
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment

import scalax.collection.GraphTraversal.BreadthFirst
import scalax.collection.GraphTraversal.Parameters
import scalax.collection.generic.Edge
import scalax.collection.immutable.Graph

class DataFlowAnalysis(
    private val controlTree:    ControlTree,
    private val superFlowGraph: SuperFlowGraph
) {

    private val _removedBackEdgesGraphs = mutable.Map.empty[FlowGraphNode, (Boolean, SuperFlowGraph)]

    def compute(
        flowFunctionByPc: Map[Int, StringFlowFunction]
    )(startEnv: StringTreeEnvironment): StringTreeEnvironment = {
        val startNodeCandidates = controlTree.nodes.filter(_.diPredecessors.isEmpty)
        if (startNodeCandidates.size != 1) {
            throw new IllegalStateException("Found more than one start node in the control tree!")
        }

        val startNode = startNodeCandidates.head.outer
        pipeThroughNode(flowFunctionByPc)(startNode, startEnv)
    }

    /**
     * @note This function should be stable with regards to an ordering on the piped flow graph nodes, e.g. a proper
     *       region should always be traversed in the same way.
     */
    private def pipeThroughNode(flowFunctionByPc: Map[Int, StringFlowFunction])(
        node: FlowGraphNode,
        env:  StringTreeEnvironment
    ): StringTreeEnvironment = {
        val pipe = pipeThroughNode(flowFunctionByPc) _
        val innerChildNodes = controlTree.get(node).diSuccessors.map(n => superFlowGraph.get(n.outer))

        def processBlock(entry: FlowGraphNode): StringTreeEnvironment = {
            var currentEnv = env
            for {
                currentNode <- superFlowGraph.innerNodeTraverser(
                    superFlowGraph.get(entry),
                    subgraphNodes = innerChildNodes.contains
                )
            } {
                currentEnv = pipe(currentNode.outer, currentEnv)
            }
            currentEnv
        }

        def processIfThenElse(entry: FlowGraphNode): StringTreeEnvironment = {
            val limitedFlowGraph = superFlowGraph.filter(innerChildNodes.contains)
            val entryNode = limitedFlowGraph.get(entry)
            val successors = entryNode.diSuccessors.map(_.outer).toList.sorted
            val branches = (successors.head, successors.tail.head)

            val envAfterEntry = pipe(entry, env)
            val envAfterBranches = (pipe(branches._1, envAfterEntry), pipe(branches._2, envAfterEntry))

            envAfterBranches._1.join(envAfterBranches._2)
        }

        def processIfThen(entry: FlowGraphNode): StringTreeEnvironment = {
            val limitedFlowGraph = superFlowGraph.filter(innerChildNodes.contains)
            val entryNode = limitedFlowGraph.get(entry)
            val (yesBranch, noBranch) = if (entryNode.diSuccessors.head.diSuccessors.nonEmpty) {
                (entryNode.diSuccessors.head, entryNode.diSuccessors.tail.head)
            } else {
                (entryNode.diSuccessors.tail.head, entryNode.diSuccessors.head)
            }

            val envAfterEntry = pipe(entry, env)
            val envAfterBranches = (
                pipe(yesBranch.diSuccessors.head, pipe(yesBranch, envAfterEntry)),
                pipe(noBranch, envAfterEntry)
            )

            envAfterBranches._1.join(envAfterBranches._2)
        }

        def handleProperSubregion[A <: FlowGraphNode, G <: Graph[A, Edge[A]]](
            g:          G,
            innerNodes: Set[G#NodeT],
            entry:      A
        ): StringTreeEnvironment = {
            val entryNode = g.get(entry)
            val ordering = g.NodeOrdering((in1, in2) => in1.compare(in2))
            val traverser = entryNode.innerNodeTraverser(Parameters(BreadthFirst))
                .withOrdering(ordering)
                .withSubgraph(nodes = innerNodes.contains)
            // We know that the graph is acyclic here, so we can be sure that the topological sort never fails
            val sortedNodes = traverser.topologicalSort().toOption.get.toSeq

            val currentNodeEnvs = mutable.Map((entryNode, pipe(entry, env)))
            for { currentNode <- sortedNodes.filter(_ != entryNode) } {
                val previousEnvs = currentNode.diPredecessors.toList.sortBy(_.outer).map { dp =>
                    pipe(currentNode.outer, currentNodeEnvs(dp))
                }
                currentNodeEnvs.update(currentNode, previousEnvs.head.joinMany(previousEnvs.tail))
            }

            currentNodeEnvs(sortedNodes.last)
        }

        def processProper(entry: FlowGraphNode): StringTreeEnvironment = {
            handleProperSubregion[FlowGraphNode, superFlowGraph.type](superFlowGraph, innerChildNodes, entry)
        }

        def processSelfLoop(entry: FlowGraphNode): StringTreeEnvironment = {
            val resultEnv = pipe(entry, env)
            // Looped operations that modify environment contents are not supported here
            if (resultEnv != env) env.updateAll(StringTreeDynamicString)
            else env
        }

        def processWhileLoop(entry: FlowGraphNode): StringTreeEnvironment = {
            val limitedFlowGraph = superFlowGraph.filter(innerChildNodes.contains)
            val entryNode = limitedFlowGraph.get(entry)
            val envAfterEntry = pipe(entry, env)

            var resultEnv = envAfterEntry
            var currentNode = entryNode.diSuccessors.head
            while (currentNode != entryNode) {
                resultEnv = pipe(currentNode.outer, resultEnv)
                currentNode = currentNode.diSuccessors.head
            }

            // Looped operations that modify environment contents are not supported here
            if (resultEnv != envAfterEntry) envAfterEntry.updateAll(StringTreeDynamicString)
            else envAfterEntry
        }

        def processNaturalLoop(entry: FlowGraphNode): StringTreeEnvironment = {
            val (isCyclic, removedBackEdgesGraph) = _removedBackEdgesGraphs.getOrElseUpdate(
                node, {
                    val limitedFlowGraph = superFlowGraph.filter(innerChildNodes.contains)
                    val entryPredecessors = limitedFlowGraph.get(entry).diPredecessors
                    val computedRemovedBackEdgesGraph = limitedFlowGraph.filterNot(
                        edgeP = edge =>
                            edge.sources.toList.toSet.intersect(entryPredecessors).nonEmpty
                                && edge.targets.contains(limitedFlowGraph.get(entry))
                    )
                    (computedRemovedBackEdgesGraph.isCyclic, computedRemovedBackEdgesGraph)
                }
            )

            if (isCyclic) {
                env.updateAll(StringTreeDynamicString)
            } else {
                // Handle resulting acyclic region
                val resultEnv = handleProperSubregion[FlowGraphNode, removedBackEdgesGraph.type](
                    removedBackEdgesGraph,
                    removedBackEdgesGraph.nodes.toSet,
                    entry
                )
                // Looped operations that modify string contents are not supported here
                if (resultEnv != env) env.updateAll(StringTreeDynamicString)
                else env
            }
        }

        node match {
            case Statement(pc) if pc >= 0 => flowFunctionByPc(pc)(env)
            case Statement(_)             => env

            case Region(Block, _, entry)       => processBlock(entry)
            case Region(IfThenElse, _, entry)  => processIfThenElse(entry)
            case Region(IfThen, _, entry)      => processIfThen(entry)
            case Region(Proper, _, entry)      => processProper(entry)
            case Region(SelfLoop, _, entry)    => processSelfLoop(entry)
            case Region(WhileLoop, _, entry)   => processWhileLoop(entry)
            case Region(NaturalLoop, _, entry) => processNaturalLoop(entry)

            case _ => env
        }
    }
}
