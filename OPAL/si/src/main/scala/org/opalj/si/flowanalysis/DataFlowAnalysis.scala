/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package si
package flowanalysis

import scala.collection.mutable

import scalax.collection.GraphTraversal.BreadthFirst
import scalax.collection.GraphTraversal.Parameters

/**
 * Performs structural data flow analysis based on the results of a [[StructuralAnalysis]]. In more detail, this means
 * that the control tree produced by the [[StructuralAnalysis]] is traversed recursively in a depth-first manner.
 * Individual regions are processed by piping an [[Environment]] through their nodes and joining the
 * environments where paths meet up. Thus, the individual flow functions defined at the statement PCs of a method are
 * combined using region-type-specific patterns to effectively act as a flow function of the entire region, which is
 * then processed itself due to the recursive nature of the algorithm.
 *
 * @param controlTree The control tree from the structural analysis.
 * @param superFlowGraph The super flow graph from the structural analysis
 * @param highSoundness Whether to use high soundness mode or not. Currently, this influences the handling of loops,
 *                      i.e. whether they are approximated by one execution of the loop body (low soundness) or via
 *                      a top value on all variables in the method (high soundness).
 *
 * @see [[StructuralAnalysis]], [[Environment]]
 *
 * @author Maximilian Rüsch
 */
class DataFlowAnalysis[Data, Environment <: DataFlowEnvironment[Data, Environment]](
    private val controlTree:    ControlTree,
    private val superFlowGraph: SuperFlowGraph,
    private val highSoundness:  Boolean
) {

    type FlowFunction = (Environment => Environment)

    private val _nodeOrderings = mutable.Map.empty[FlowGraphNode, Seq[SuperFlowGraph#NodeT]]
    private val _removedBackEdgesGraphs = mutable.Map.empty[FlowGraphNode, (Boolean, SuperFlowGraph)]

    /**
     * Computes the resulting environment after the data flow analysis.
     *
     * @param flowFunctionByPc A mapping from PC to the flow functions to be used
     * @param startEnv The base environment which is piped into the first region to be processed.
     * @return The resulting [[Environment]] after execution of all flow functions using the region
     *         hierarchy given in the control tree.
     */
    def compute(
        flowFunctionByPc: Map[Int, FlowFunction]
    )(startEnv: Environment): Environment = {
        val startNodeCandidates = controlTree.nodes.filter(!_.hasPredecessors)
        if (startNodeCandidates.size != 1) {
            throw new IllegalStateException("Found more than one start node in the control tree!")
        }

        val startNode = startNodeCandidates.head.outer
        pipeThroughNode(flowFunctionByPc)(startNode, startEnv)
    }

    private def pipeThroughNode(flowFunctionByPc: Map[Int, FlowFunction])(
        node: FlowGraphNode,
        env:  Environment
    ): Environment = {
        val pipe = pipeThroughNode(flowFunctionByPc) _
        val innerChildNodes = controlTree.get(node).diSuccessors.map(n => superFlowGraph.get(n.outer))

        def processBlock(entry: FlowGraphNode): Environment = {
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

        def processIfThenElse(entry: FlowGraphNode): Environment = {
            val entryNode = superFlowGraph.get(entry)
            val successors = entryNode.diSuccessors.intersect(innerChildNodes).map(_.outer).toList.sorted
            val branches = (successors.head, successors.tail.head)

            val envAfterEntry = pipe(entry, env)
            val envAfterBranches = (pipe(branches._1, envAfterEntry), pipe(branches._2, envAfterEntry))

            envAfterBranches._1.join(envAfterBranches._2)
        }

        def processIfThen(entry: FlowGraphNode): Environment = {
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
        ): Environment = {
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
                currentNodeEnvs.update(currentNode, env.joinMany(previousEnvs))
            }

            currentNodeEnvs(sortedNodes.last.asInstanceOf[g.NodeT])
        }

        def processProper(entry: FlowGraphNode): Environment = {
            handleProperSubregion[FlowGraphNode, superFlowGraph.type](superFlowGraph, innerChildNodes, entry)
        }

        def processSelfLoop(entry: FlowGraphNode): Environment = {
            val resultEnv = pipe(entry, env)
            // IMPROVE only update affected variables instead of all
            if (resultEnv != env && highSoundness) env.updateAll(env.top)
            else resultEnv
        }

        def processWhileLoop(entry: FlowGraphNode): Environment = {
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

            // IMPROVE only update affected variables instead of all
            if (resultEnv != envAfterEntry && highSoundness) envAfterEntry.updateAll(env.top)
            else resultEnv
        }

        def processNaturalLoop(entry: FlowGraphNode): Environment = {
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
                // IMPROVE only update affected variables instead of all
                if (highSoundness) env.updateAll(env.top)
                else env.updateAll(env.bottom)
            } else {
                // Handle resulting acyclic region
                val resultEnv = handleProperSubregion[FlowGraphNode, removedBackEdgesGraph.type](
                    removedBackEdgesGraph,
                    removedBackEdgesGraph.nodes.toSet,
                    entry
                )
                // IMPROVE only update affected variables instead of all
                if (resultEnv != env && highSoundness) env.updateAll(env.top)
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
