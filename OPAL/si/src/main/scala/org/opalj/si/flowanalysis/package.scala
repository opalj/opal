/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package si

import scalax.collection.edges.DiEdge
import scalax.collection.generic.Edge
import scalax.collection.immutable.Graph

/**
 * @author Maximilian Rüsch
 */
package object flowanalysis {

    /**
     * Region control tree representing nesting of control structures; obtained from structural analysis.
     */
    type ControlTree = Graph[FlowGraphNode, DiEdge[FlowGraphNode]]

    /**
     * CFG-like basis for data-flow analysis and super flow graph.
     */
    type FlowGraph = Graph[FlowGraphNode, DiEdge[FlowGraphNode]]

    /**
     * Flow graph with including region control tree information from structural analysis.
     */
    type SuperFlowGraph = Graph[FlowGraphNode, Edge[FlowGraphNode]]

}
