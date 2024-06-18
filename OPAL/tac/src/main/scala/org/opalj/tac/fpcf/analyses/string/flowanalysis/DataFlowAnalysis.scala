/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package flowanalysis

import org.opalj.br.fpcf.properties.string.StringTreeDynamicString
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

        def processBlock(entry: FlowGraphNode): StringTreeEnvironment = {
            var currentEnv = env
            var currentNode = limitedFlowGraph.get(entry)
            while (currentNode.diSuccessors.nonEmpty) {
                currentEnv = pipe(currentNode.outer, currentEnv)
                currentNode = currentNode.diSuccessors.head
            }

            pipe(currentNode, currentEnv)
        }

        def processIfThenElse(entry: FlowGraphNode): StringTreeEnvironment = {
            val entryNode = limitedFlowGraph.get(entry)
            val successors = entryNode.diSuccessors.map(_.outer).toList.sorted
            val branches = (successors.head, successors.tail.head)

            val envAfterEntry = pipe(entry, env)
            val envAfterBranches = (pipe(branches._1, envAfterEntry), pipe(branches._2, envAfterEntry))

            envAfterBranches._1.join(envAfterBranches._2)
        }

        def processIfThen(entry: FlowGraphNode): StringTreeEnvironment = {
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

        def processProper(entry: FlowGraphNode): StringTreeEnvironment = {
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
                sortedCurrentNodes = nextNodeEnvs.map(_._1).distinct.sortBy(_.outer)
                currentNodeEnvs = nextNodeEnvs.groupBy(_._1) map { kv =>
                    (kv._1, kv._2.head._2.joinMany(kv._2.tail.map(_._2)))
                }
            }

            sortedCurrentNodes.foldLeft(StringTreeEnvironment(Map.empty)) { (env, nextNode) =>
                env.join(currentNodeEnvs(nextNode))
            }
        }

        def processSelfLoop(entry: FlowGraphNode): StringTreeEnvironment = {
            val resultEnv = pipe(entry, env)
            // Looped operations that modify environment contents are not supported here
            if (resultEnv != env) env.updateAll(StringTreeDynamicString)
            else env
        }

        def processWhileLoop(entry: FlowGraphNode): StringTreeEnvironment = {
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
            val entryPredecessors = limitedFlowGraph.get(entry).diPredecessors
            val removedBackEdgesGraph = limitedFlowGraph.filterNot(
                edgeP = edge =>
                    edge.sources.toList.toSet.intersect(entryPredecessors).nonEmpty
                        && edge.targets.contains(limitedFlowGraph.get(entry))
            )
            if (removedBackEdgesGraph.isCyclic) {
                env.updateAll(StringTreeDynamicString)
            } else {
                // Handle resulting acyclic region
                val entryNode = removedBackEdgesGraph.get(entry)
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
                    sortedCurrentNodes = nextNodeEnvs.map(_._1).distinct.sortBy(_.outer)
                    currentNodeEnvs = nextNodeEnvs.groupMapReduce(_._1)(_._2) { (env, otherEnv) => env.join(otherEnv) }
                }

                val resultEnv = sortedCurrentNodes.foldLeft(StringTreeEnvironment(Map.empty)) { (env, nextNode) =>
                    env.join(currentNodeEnvs(nextNode))
                }

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
