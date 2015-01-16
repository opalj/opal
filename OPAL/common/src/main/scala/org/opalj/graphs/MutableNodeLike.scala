/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package graphs

import scala.collection.Map

/**
 * Represents a mutable node of a directed graph. This class basically serves as a small
 * adapter class for some arbitrary node.
 *
 * ==Thread Safety==
 * This class is thread-safe. It is possible to add multiple child nodes concurrently.
 *
 * @see The demo project for example usages.
 *
 * @tparam I The type of the object that is associated with this node/the type of
 *      the object for which this node object is created.
 * @param identifier The underlying object.
 *       '''The underlying object must correctly implement the equals/hashCode contract.
 *       I.e., the `hashCode` of two object instances that are added to the same graph is
 *      different whenever `equals` is `false`'''.
 * @param identifierToString A function that converts "an" identifier to a string. By
 *      default the given object's `toString` method is called. It is possible
 *      that a graph has two nodes with the same textual representation representation
 *      but a different identity.
 * @author Michael Eichberg
 */
class MutableNodeLike[I, N <: Node](
    private[this] var theIdentifier: I,
    val identifierToString: I ⇒ String,
    private[this] var theVisualProperties: Map[String, String],
    private[this] var theChildren: List[N])
        extends MutableNode[I, N] {

    def identifier: I = this.synchronized(theIdentifier)

    override def visualProperties: Map[String, String] = this.synchronized(theVisualProperties)

    def mergeVisualProperties(other: Map[String, String]): Unit = {
        theVisualProperties ++= other
    }

    def children: List[N] = this.synchronized(theChildren)

    def updateIdentifier(newIdentifier: I) =
        this.synchronized(theIdentifier = newIdentifier)

    override def toHRR = Some(identifierToString(identifier))

    override def uniqueId: Int = identifier.hashCode()

    def addChild(node: N): Unit = {
        this.synchronized(theChildren = node :: children)
    }

    def addChildren(furtherChildren: List[N]): Unit = {
        this.synchronized(theChildren = theChildren ::: furtherChildren)
    }

    def hasOneChild: Boolean = this.synchronized(children.nonEmpty && children.tail.isEmpty)

    def firstChild: N = this.synchronized(children.head)

    def removeLastAddedChild(): Unit = {
        this.synchronized(theChildren = theChildren.tail)
    }

    def removeChild(node: N): Unit = {
        this.synchronized(theChildren = theChildren.filterNot(_ == node))
    }

    override def foreachSuccessor(f: Node ⇒ Unit): Unit = {
        this.synchronized(children.foreach(f))
    }

    override def hasSuccessors: Boolean = this.synchronized(children.nonEmpty)

}

