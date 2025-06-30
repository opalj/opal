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

    type ControlTree = Graph[FlowGraphNode, DiEdge[FlowGraphNode]]
    type FlowGraph = Graph[FlowGraphNode, DiEdge[FlowGraphNode]]
    type SuperFlowGraph = Graph[FlowGraphNode, Edge[FlowGraphNode]]

}
