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

    def identifierToString: I ⇒ String

    def mergeVisualProperties(other: Map[String, String]): Unit

    def children: List[N]

    var parents: List[Node]

    def addChild(node: N): Unit

    def addParent(node: Node): Unit

    def exitNode: DefaultMutableNode[String]

    def linkChildren(exitNode: DefaultMutableNode[String]): Unit

    def hasOneChild: Boolean

    def firstChild: N

    def removeLastAddedChild(): Unit

    def removeChild(node: N): Unit

}

