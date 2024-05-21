/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package flowanalysis

import org.opalj.tac.fpcf.properties.string.StringFlowFunction
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment

object DataFlowAnalysis {

    def compute(
        controlTree:      ControlTree,
        superFlowGraph:   SuperFlowGraph,
        flowFunctionByPc: Map[Int, StringFlowFunction]
    )(startEnv: StringTreeEnvironment): StringTreeEnvironment = {
        val startNodeCandidates = controlTree.nodes.filter(_.diPredecessors.isEmpty)
        if (startNodeCandidates.size != 1) {
            throw new IllegalStateException("Found more than one start node in the control tree!")
        }

        val startNode = startNodeCandidates.head.outer
        pipeThroughNode(controlTree, superFlowGraph, flowFunctionByPc)(startNode, startEnv)
    }

    /**
     * @note This function should be stable with regards to an ordering on the piped flow graph nodes, e.g. a proper
     *       region should always be traversed in the same way.
     */
    private def pipeThroughNode(
        controlTree:      ControlTree,
        superFlowGraph:   SuperFlowGraph,
        flowFunctionByPc: Map[Int, StringFlowFunction]
    )(
        node: FlowGraphNode,
        env:  StringTreeEnvironment
    ): StringTreeEnvironment = {
        val pipe = pipeThroughNode(controlTree, superFlowGraph, flowFunctionByPc) _
        val childNodes = controlTree.get(node).diSuccessors.map(_.outer)
        val limitedFlowGraph = superFlowGraph.filter(n => childNodes.contains(n.outer))

        node match {
            case Statement(pc) if pc >= 0 => flowFunctionByPc(pc)(env)
            case Statement(_)             => env

            case Region(Block, _, entry) =>
                var currentEnv = env
                var currentNode = limitedFlowGraph.get(entry)
                while (currentNode.diSuccessors.nonEmpty) {
                    currentEnv = pipe(currentNode.outer, currentEnv)
                    currentNode = currentNode.diSuccessors.head
                }

                pipe(currentNode, currentEnv)

            case Region(IfThenElse, _, entry) =>
                val entryNode = limitedFlowGraph.get(entry)
                val successors = entryNode.diSuccessors.map(_.outer).toList.sorted
                val branches = (successors.head, successors.tail.head)

                val envAfterEntry = pipe(entry, env)
                val envAfterBranches = (pipe(branches._1, envAfterEntry), pipe(branches._2, envAfterEntry))

                envAfterBranches._1.join(envAfterBranches._2)

            case Region(IfThen, _, entry) =>
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

            case Region(Proper, _, entry) =>
                val entryNode = limitedFlowGraph.get(entry)

                var sortedCurrentNodes = List(entryNode)
                var currentNodeEnvs = Map((entryNode, pipe(entry, env)))
                while (currentNodeEnvs.keys.exists(_.diSuccessors.nonEmpty)) {
                    val nextNodeEnvs = sortedCurrentNodes.flatMap { node =>
                        if (node.diSuccessors.isEmpty) {
                            Iterable((node, currentNodeEnvs(node)))
                        } else {
                            node.diSuccessors.toList.sortBy(_.outer).map { successor =>
                                (successor, pipe(successor, currentNodeEnvs(node)))
                            }
                        }
                    }
                    sortedCurrentNodes = nextNodeEnvs.map(_._1).sortBy(_.outer)
                    currentNodeEnvs = nextNodeEnvs.groupMapReduce(_._1)(_._2) { (env, otherEnv) => env.join(otherEnv) }
                }

                sortedCurrentNodes.foldLeft(StringTreeEnvironment(Map.empty)) { (env, nextNode) =>
                    env.join(currentNodeEnvs(nextNode))
                }

            case _ => env
        }
    }
}
