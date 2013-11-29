/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
 * Generates a dot file for the given graph.
 *
 * @author Michael Eichberg
 */
trait toDot {

    /**
     * Generates a DOT file for the given graph. Requires that Node implements
     * a content-based equals and hashCode method.
     */
    def generateDot(
        nodes: Set[Node],
        dir: String = "forward"): String = {
        var nodesToProcess = nodes
        var processedNodes = Set.empty[Node]

        var s = "digraph G {\n\tdir="+dir+"\n"

        while (nodesToProcess.nonEmpty) {
            val nextNode = nodesToProcess.head
            // prepare the next iteration
            processedNodes += nextNode
            nodesToProcess = nodesToProcess.tail

            if (nextNode.toHRR.isDefined) {
                val label = nextNode.toHRR.get.replace("\"", "\\\"")
                s +=
                    "\t"+nextNode.uniqueId+
                    "[label=\""+label+"\""+
                    nextNode.backgroundColor.map(",style=filled,fillcolor=\""+_+"\"").getOrElse("")+
                    "];\n"
            }

            val f = (sn: Node) ⇒ {
                if (nextNode.toHRR.isDefined)
                    s += "\t"+nextNode.uniqueId+" -> "+sn.uniqueId+" [dir="+dir+"];\n"

                if (!(processedNodes contains sn)) {
                    nodesToProcess += sn
                }
            }
            nextNode.foreachSuccessor(f)
        }
        s += "}"
        s
    }

    def generateAndOpenDOT(
        fileNamePrefix: String,
        nodes: Set[Node],
        dir: String = "forward") {

        import util.ControlAbstractions._

        val graph = generateDot(nodes, dir)
        try {
            val desktop = java.awt.Desktop.getDesktop()
            val file = java.io.File.createTempFile(fileNamePrefix, ".dot")
            process { new java.io.FileOutputStream(file) } { fos ⇒
                fos.write(graph.getBytes("UTF-8"))
            }
            desktop.open(file)
        } catch {
            case _: Error | _: Exception ⇒ println(graph)
        }
    }
}

object toDot extends toDot

