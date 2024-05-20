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
                var currentEnv = pipe(entry, env)
                var currentNode = limitedFlowGraph.get(entry)
                while (currentNode.diSuccessors.nonEmpty) {
                    currentEnv = pipe(currentNode.outer, currentEnv)
                    currentNode = currentNode.diSuccessors.head
                }

                currentEnv

            case Region(IfThenElse, _, entry) =>
                val entryNode = limitedFlowGraph.get(entry)
                val branches = (entryNode.diSuccessors.head, entryNode.diSuccessors.tail.head)

                val envAfterEntry = pipe(entry, env)
                val envAfterBranches = (pipe(branches._1, envAfterEntry), pipe(branches._2, envAfterEntry))

                envAfterBranches._1.join(envAfterBranches._2)

            case _ => env
        }
    }
}
