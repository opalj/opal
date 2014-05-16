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

/**
 * Represents a node of some graph.
 *
 * Two nodes are considered equal if they have the same unique id.
 *
 * @see [[org.opalj.br.analyses.ClassHierarchy]]'s `toGraph` method for
 *      an example usage.
 *
 * @author Michael Eichberg
 */
trait Node {

    /**
     * Returns a humane readable representation (HRR) of this node.
     */
    def toHRR: Option[String]

    /**
     * The name of the background color based on the X11 color scheme.
     *
     * See [[http://www.graphviz.org/content/color-names]] for further details.
     */
    def backgroundColor: Option[String]

    /**
     * An identifier that uniquely identifies this node in the graph to which this
     * node belongs. By default two nodes are considered equal if they have the same
     * unique id.
     */
    def uniqueId: Int

    /**
     * Returns `true` if this node has successor nodes.
     */
    def hasSuccessors: Boolean

    /**
     * Applies the given function for each successor node.
     */
    def foreachSuccessor(f: Node ⇒ Unit): Unit

    /**
     * The hash code of this node. By default the hash code is the unique id.
     */
    override def hashCode: Int = uniqueId

    override def equals(other: Any): Boolean = {
        other match {
            case otherNode: Node ⇒ otherNode.uniqueId == this.uniqueId
            case _               ⇒ false
        }
    }
}


