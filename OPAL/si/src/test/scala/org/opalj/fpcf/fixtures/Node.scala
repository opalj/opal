/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package fixtures

import scala.collection.mutable

/**
 * Models a simple mutable node of a fictitious graph.
 *
 * @note Only intended to be used as a test fixture.
 */
final class Node(
        val name:    String,
        val targets: mutable.Set[Node] = mutable.Set.empty
) {

    override def hashCode: Int = name.hashCode()
    override def equals(other: Any): Boolean = other match {
        case that: Node => this.name equals that.name
        case _          => false
    }

    override def toString: String = name // RECALL: Nodes are potentially used in cycles.
}
object Node { def apply(name: String) = new Node(name) }
