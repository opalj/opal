/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package graphs

import scala.collection.immutable

/**
 * Common interface of all mutable nodes of a directed graph.
 * This class basically serves as a small adapter class for some arbitrary node.
 *
 * @see The demo project for example usages.
 *
 * @tparam I The type of the object that is associated with this node/the type of
 *      the object for which this node object is created.
 * @tparam N The type of the node of the child nodes that can be added or removed.
 *
 * @author Michael Eichberg
 */
trait MutableNode[I, N <: Node] extends Node {

    def identifier: I

    def updateIdentifier(newIdentifier: I): Unit

    def identifierToString: I => String

    def mergeVisualProperties(other: immutable.Map[String, String]): Unit

    def children: List[N]

    def addChild(node: N): Unit

    def hasOneChild: Boolean

    def firstChild: N

    def removeLastAddedChild(): Unit

    def removeChild(node: N): Unit

}

