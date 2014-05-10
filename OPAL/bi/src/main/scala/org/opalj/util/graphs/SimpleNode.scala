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
package de.tud.cs.st
package util
package graphs

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
final class SimpleNode[I](
    val identifier: I,
    val identifierToString: I ⇒ String = (_: Any).toString,
    override val backgroundColor: Option[String] = None,
    private[this] var children: List[Node] = List.empty)
        extends Node {

    override def toHRR = Some(identifierToString(identifier))

    override def uniqueId: Int = identifier.hashCode()

    def addChild(node: Node): Unit = {
        this.synchronized(children = node :: children)
    }

    def removeLastAddedChild(): Unit = {
        this.synchronized(children = children.tail)
    }

    def removeChild(node: Node): Unit = {
        this.synchronized(children = children.filterNot(_ == node))
    }

    override def foreachSuccessor(f: Node ⇒ Unit): Unit = {
        this.synchronized(children.foreach(f))
    }

    override def hasSuccessors: Boolean = this.synchronized(children.nonEmpty)

}

