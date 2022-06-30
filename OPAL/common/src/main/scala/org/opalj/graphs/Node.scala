/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package graphs

import scala.collection.immutable

/**
 * Represents a node of some graph.
 *
 * Two nodes are considered equal if they have the same unique id.
 *
 * @see [[org.opalj.br.ClassHierarchy]]'s `toGraph` method for
 *      an example usage.
 *
 * @author Michael Eichberg
 */
trait Node {

    /**
     * Returns a human readable representation (HRR) of this node.
     */
    def toHRR: Option[String]

    def visualProperties: immutable.Map[String, String] = immutable.Map.empty[String, String]

    /**
     * An identifier that uniquely identifies this node in the graph to which this
     * node belongs. By default two nodes are considered equal if they have the same
     * unique id.
     */
    def nodeId: Int

    /**
     * Returns `true` if this node has successor nodes.
     */
    def hasSuccessors: Boolean

    /**
     * Applies the given function for each successor node.
     */
    def foreachSuccessor(f: Node => Unit): Unit

    /**
     * The hash code of this node. By default the hash code is the unique id.
     */
    override def hashCode(): Int = nodeId

    override def equals(other: Any): Boolean = {
        other match {
            case that: Node => this.nodeId == that.nodeId
            case _          => false
        }
    }

}
