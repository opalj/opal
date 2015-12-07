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

import scala.collection.mutable
import java.util.LinkedHashSet
import scala.collection.mutable.Stack

/**
 * Defines factory methods to generate specific representations of (multi-) graphs.
 *
 * @author Michael Eichberg
 */
package object graphs {

    /**
     * Generates a string that describes a (multi-)graph using the ".dot" file format
     * [[http://graphviz.org/pdf/dotguide.pdf]].
     * The graph is defined by the given set of nodes.
     *
     * Requires that `Node` implements a content-based `equals` and `hashCode` method.
     */
    def toDot(
        nodes:   Set[_ <: Node],
        dir:     String         = "forward",
        ranksep: String         = "1.0"
    ): String = {
        var nodesToProcess = Set.empty[Node] ++ nodes
        var processedNodes = Set.empty[Node]

        var s = s"digraph G {\n\tdir=$dir;\n\tranksep=$ranksep;\n"

        while (nodesToProcess.nonEmpty) {
            val nextNode = nodesToProcess.head
            // prepare the next iteration
            processedNodes += nextNode
            nodesToProcess = nodesToProcess.tail

            if (nextNode.toHRR.isDefined) {
                var visualProperties = nextNode.visualProperties
                visualProperties += (
                    "label" → nextNode.toHRR.get.replace("\"", "\\\"").replace("\n", "\\l")
                )
                s +=
                    "\t"+nextNode.nodeId +
                    visualProperties.map(e ⇒ "\""+e._1+"\"=\""+e._2+"\"").
                    mkString("[", ",", "];\n")
            }

            val f: (Node ⇒ Unit) = sn ⇒ {
                if (nextNode.toHRR.isDefined)
                    s += "\t"+nextNode.nodeId+" -> "+sn.nodeId+" [dir="+dir+"];\n"

                if (!(processedNodes contains sn)) {
                    nodesToProcess += sn
                }
            }
            nextNode.foreachSuccessor(f)
        }
        s += "}"
        s
    }

    def closedSCCs[N >: Null <: AnyRef](g: Graph[N]): List[scala.collection.Set[N]] = {
        println(s"Computing closed SCCs for $g")
        closedSCCs(g.vertices, g)
    }

    /**
     * A closed strongly connected component (cSCC) is a (necessarily minimal) set of nodes of
     * a graph where each node belonging to the cSCC can be reached from another node and no node
     * contains an edge to another node that does not belong to the cSCC.
     */
    def closedSCCs[N >: Null <: AnyRef](
        ns: Traversable[N],
        es: N ⇒ Traversable[N]
    ): List[scala.collection.Set[N]] = {

        type DFSNum = Int
        type CSCCId = Int
        val NoCSCCNode: CSCCId = Int.MinValue
        val Undetermined: CSCCId = -1
        val PathBoundary: Null = null

        case class NInfo(val dfsNum: DFSNum, var cSCCId: CSCCId = Undetermined) {
            override def toString: String = {
                val cSCCId = this.cSCCId match {
                    case NoCSCCNode   ⇒ "NoCSCCNode"
                    case Undetermined ⇒ "Undetermined"
                    case id           ⇒ id.toString
                }
                s"(dfsNum=$dfsNum,cSCCId=$cSCCId)"
            }
        }

        val visited = mutable.HashSet.empty[N]
        var cSCCs = List.empty[scala.collection.Set[N]]

        /*
         * Performs a depth-first search to locate an initial strongly connected component.
         * If we detect a connected component, we then check for every element belonging to
         * the connected component whether it also depends on an element which is not a member
         * of the strongly connected component. If Yes, we continue with the checking of the
         * other elements. If No, we perform a depth-first search based on the successor of the
         * node that does not belong to the SCC and try to determine if it is connected to some
         * previous SCC. If so, we merge all nodes as they belong to the same SCC.
         */

        def dfs(n: N): Unit = {
            if (visited.contains(n))
                return ;

            visited += n

            // CORE DATA STRUCTURES
            var nextDFSNum = 0
            var nextCSCCId = 1
            var path = mutable.LinkedHashMap.empty[N, NInfo]
            val nodes = mutable.Stack.empty[N]

            // HELPER METHODS
            def addToPath(n: N): NInfo = {
                val dfsNum = nextDFSNum
                val nInfo = NInfo(dfsNum)
                path.put(n, nInfo)
                nextDFSNum += 1
                nInfo
            }
            def pathLength = nextDFSNum
            def cSCCId(nNInfo: (N, NInfo)): CSCCId = { nNInfo._2.cSCCId }
            def cSCCIdOf(dfsNum: DFSNum): CSCCId = {
                // This is potentially computationally expensive
                path.drop(dfsNum).head._2.cSCCId
            }

            // INITIALIZATION
            addToPath(n)
            nodes.push(n).push(PathBoundary).pushAll(es(n))

            // PROCESSING
            while (nodes.nonEmpty) {
                println(s"path=${path.mkString(",")}; nextDFSNum=$nextDFSNum; nextCSCCId=$nextCSCCId; visited=$visited")

                val n = nodes.pop()
                if (n eq PathBoundary) { // i.e., we have visited all child elements 
                    val n = nodes.pop()
                    val nInfo @ NInfo(nDFSNum, nCSCCId) = path(n)
                    println(s"visited all children of $n$nInfo")

                    nCSCCId match {
                        case NoCSCCNode ⇒
                        /*Nothing to do.*/
                        case Undetermined ⇒
                            nInfo.cSCCId = NoCSCCNode
                        case nCSCCId if nCSCCId == cSCCId(path.last) &&
                            (nDFSNum == 0 || nCSCCId != cSCCIdOf(nDFSNum - 1)) ⇒
                            cSCCs ::= path.takeRight(pathLength - nDFSNum).keySet

                            // UPDATE THE CORE DATASTRUCTURES
                            nextDFSNum = nDFSNum
                            path = path.take(nDFSNum)
                            path.foreach(n ⇒ n._2.cSCCId = NoCSCCNode)

                        case someCSCCId ⇒
                        /*nothing to do*/
                    }
                } else { // i.e., we are (potentially) extending our path
                    visited += n
                    println("next node: "+n)

                    path.get(n) match {
                        case None ⇒
                            // we have not yet analyzed this node
                            val nInfo = addToPath(n)
                            nodes.push(n)
                            nodes.push(PathBoundary)
                            es(n) foreach { nextN ⇒
                                if (visited.contains(nextN)) {
                                    val nextNInfoOption = path.get(nextN)
                                    if (nextNInfoOption.isEmpty) {
                                        // some successor node was analyzed and the successor
                                        // node may take part in a cSCC, but certainly not this one
                                        nInfo.cSCCId = NoCSCCNode
                                    } else if (nextNInfoOption.get.cSCCId == NoCSCCNode) {
                                        nInfo.cSCCId = NoCSCCNode
                                    } else {
                                        nodes.push(nextN)
                                    }
                                } else {
                                    nodes.push(nextN)
                                }
                            }
                        case Some(nInfo @ NInfo(nDFSNum, nCSCCId)) ⇒
                            // this node was already on the path, hence, we have a cycle
                            nCSCCId match {
                                case Undetermined ⇒
                                    // we have a new cycle
                                    val lastCSCCId = cSCCId(path.last)
                                    if (lastCSCCId >= Undetermined) {
                                        val nCSCCId = nextCSCCId
                                        nextCSCCId += 1
                                        val cc = path.takeRight(pathLength - nDFSNum)
                                        cc.foreach(n ⇒ n._2.cSCCId = nCSCCId)
                                        println(cc.mkString(s"Nodes in a cSCC candidate $nCSCCId: ", ",", ""))
                                        println("path: "+path.mkString)
                                    } else // lastCSCCId == NoCSCCNode
                                        nInfo.cSCCId = NoCSCCNode

                                case nCSCCId if nCSCCId != NoCSCCNode ⇒
                                    val lastCSCCId = cSCCId(path.last)
                                    if (lastCSCCId >= Undetermined)
                                        path.takeRight(pathLength - nDFSNum).foreach(n ⇒ n._2.cSCCId = nCSCCId)
                                    else // lastCSCCId == NoCSCCNode
                                        nInfo.cSCCId = NoCSCCNode

                                case _ ⇒
                                // Nothing to do...
                            }
                    }
                }
            }
        }

        for (n ← ns) { dfs(n) }

        cSCCs

    }
}

