/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package flowanalysis

import scala.collection.mutable

import org.opalj.br.fpcf.properties.string.StringTreeDynamicString
import org.opalj.br.fpcf.properties.string.StringTreeInvalidElement
import org.opalj.tac.fpcf.properties.string.StringFlowFunction
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment

import scalax.collection.GraphTraversal.BreadthFirst
import scalax.collection.GraphTraversal.Parameters

class DataFlowAnalysis(
    private val controlTree:    ControlTree,
    private val superFlowGraph: SuperFlowGraph,
    private val highSoundness:  Boolean
) {

    private val _nodeOrderings = mutable.Map.empty[FlowGraphNode, Seq[SuperFlowGraph#NodeT]]
    private val _removedBackEdgesGraphs = mutable.Map.empty[FlowGraphNode, (Boolean, SuperFlowGraph)]

    def compute(
        flowFunctionByPc: Map[Int, StringFlowFunction]
    )(startEnv: StringTreeEnvironment): StringTreeEnvironment = {
        val startNodeCandidates = controlTree.nodes.filter(!_.hasPredecessors)
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
            val entryNode = superFlowGraph.get(entry)
            val successors = entryNode.diSuccessors.intersect(innerChildNodes).map(_.outer).toList.sorted
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

        def handleProperSubregion[A <: FlowGraphNode, G <: SuperFlowGraph](
            g:          G,
            innerNodes: Set[G#NodeT],
            entry:      A
        ): StringTreeEnvironment = {
            val entryNode = g.get(entry)
            val sortedNodes = _nodeOrderings.getOrElseUpdate(
                node, {
                    val ordering = g.NodeOrdering((in1, in2) => in1.compare(in2))
                    val traverser = entryNode.innerNodeTraverser(Parameters(BreadthFirst))
                        .withOrdering(ordering)
                        .withSubgraph(nodes = innerNodes.contains)
                    // We know that the graph is acyclic here, so we can be sure that the topological sort never fails
                    traverser.topologicalSort().toOption.get.toSeq
                }
            )

            val currentNodeEnvs = mutable.Map((entryNode, pipe(entry, env)))
            for {
                _currentNode <- sortedNodes.filter(_ != entryNode)
                currentNode = _currentNode.asInstanceOf[g.NodeT]
            } {
                val previousEnvs = currentNode.diPredecessors.toList.map { dp =>
                    pipe(currentNode.outer, currentNodeEnvs(dp))
                }
                currentNodeEnvs.update(currentNode, StringTreeEnvironment.joinMany(previousEnvs))
            }

            currentNodeEnvs(sortedNodes.last.asInstanceOf[g.NodeT])
        }

        def processProper(entry: FlowGraphNode): StringTreeEnvironment = {
            handleProperSubregion[FlowGraphNode, superFlowGraph.type](superFlowGraph, innerChildNodes, entry)
        }

        def processSelfLoop(entry: FlowGraphNode): StringTreeEnvironment = {
            val resultEnv = pipe(entry, env)
            if (resultEnv != env && highSoundness) env.updateAll(StringTreeDynamicString)
            else resultEnv
        }

        def processWhileLoop(entry: FlowGraphNode): StringTreeEnvironment = {
            val entryNode = superFlowGraph.get(entry)
            val envAfterEntry = pipe(entry, env)

            superFlowGraph.innerNodeTraverser(entryNode, subgraphNodes = innerChildNodes)

            var resultEnv = env
            for {
                currentNode <- superFlowGraph
                    .innerNodeTraverser(entryNode)
                    .withSubgraph(n => n != entryNode && innerChildNodes.contains(n))
            } {
                resultEnv = pipe(currentNode.outer, resultEnv)
            }

            if (resultEnv != envAfterEntry && highSoundness) envAfterEntry.updateAll(StringTreeDynamicString)
            else resultEnv
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
                if (highSoundness) env.updateAll(StringTreeDynamicString)
                else env.updateAll(StringTreeInvalidElement)
            } else {
                // Handle resulting acyclic region
                val resultEnv = handleProperSubregion[FlowGraphNode, removedBackEdgesGraph.type](
                    removedBackEdgesGraph,
                    removedBackEdgesGraph.nodes.toSet,
                    entry
                )
                if (resultEnv != env && highSoundness) env.updateAll(StringTreeDynamicString)
                else resultEnv
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
