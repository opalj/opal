/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package graphs

import scala.collection.immutable

/**
 * Represents a mutable node of a directed graph.
 * This class serves as a base implementation of the [[MutableNode]] trait.
 *
 * ==Thread Safety==
 * This class is thread-safe. It is possible to add multiple child nodes concurrently.
 *
 * @see    The demo project for example usages.
 *
 * @tparam I The type of the object that is associated with this node/the type of
 *         the object for which this node object is created.
 * @param  theIdentifier The underlying object. '''For all nodes of a graph the hashCode method
 *         has to return a unique id unless the nodeId method is overridden.'''
 *         '''The underlying object must correctly implement the equals/hashCode contract.
 *         I.e., the `hashCode` of two object instances that are added to the same graph is
 *         different whenever `equals` is `false`'''.
 * @param  identifierToString A function that converts "an" identifier to a string. By
 *         default the given object's `toString` method is called. It is possible
 *         that a graph has two nodes with the same textual representation representation
 *         but a different identity.
 * @author Michael Eichberg
 */
class MutableNodeLike[I, N <: Node](
        private[this] var theIdentifier:       I,
        val identifierToString:                I => String,
        private[this] var theVisualProperties: immutable.Map[String, String],
        private[this] var theChildren:         List[N]
) extends MutableNode[I, N] {

    def identifier: I = this.synchronized(theIdentifier)

    override def visualProperties: immutable.Map[String, String] = this.synchronized(theVisualProperties)

    def mergeVisualProperties(other: immutable.Map[String, String]): Unit = {
        theVisualProperties ++= other
    }

    def children: List[N] = this.synchronized(theChildren)

    def updateIdentifier(newIdentifier: I): Unit = this.synchronized {
        theIdentifier = newIdentifier
    }

    override def toHRR: Option[String] = Some(identifierToString(identifier))

    override def nodeId: Int = identifier.hashCode()

    def addChild(node: N): Unit = this.synchronized { theChildren = node :: children }

    def addChildren(furtherChildren: List[N]): Unit = this.synchronized {
        theChildren = theChildren ::: furtherChildren
    }

    def hasOneChild: Boolean = this.synchronized(children.nonEmpty && children.tail.isEmpty)

    def firstChild: N = this.synchronized(children.head)

    def removeLastAddedChild(): Unit = this.synchronized { theChildren = theChildren.tail }

    def removeChild(node: N): Unit = this.synchronized {
        theChildren = theChildren.filterNot(_ == node)
    }

    override def foreachSuccessor(f: Node => Unit): Unit = this.synchronized { children.foreach(f) }

    override def hasSuccessors: Boolean = this.synchronized { children.nonEmpty }

}
