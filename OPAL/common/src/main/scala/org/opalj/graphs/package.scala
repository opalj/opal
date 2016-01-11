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

    final def closedSCCs[N >: Null <: AnyRef](g: Graph[N]): List[Iterable[N]] = {
        // println(s"Computing closed SCCs for $g")
        closedSCCs(g.vertices, g)
    }

    type DFSNum = Int // always a positive number >= 0
    type CSCCId = Int // always a positive number >= 1

    private[this] val Undetermined: CSCCId = -1

    final def closedSCCs[N >: Null <: AnyRef](
        ns: Traversable[N],
        es: N ⇒ Traversable[N]
    ): List[Iterable[N]] = {

        case class NInfo(val dfsNum: DFSNum, var cSCCId: CSCCId = Undetermined) {
            override def toString: String = {
                val cSCCId = this.cSCCId match {

                    case Undetermined ⇒ "Undetermined"
                    case id           ⇒ id.toString
                }
                s"(dfsNum=$dfsNum,cSCCId=$cSCCId)"
            }
        }

        val nodeInfo: mutable.HashMap[N, NInfo] = mutable.HashMap.empty

        def setDFSNum(n: N, dfsNum: DFSNum): Unit = {
            assert(nodeInfo.get(n).isEmpty)
            nodeInfo.put(n, NInfo(dfsNum))
        }
        val hasDFSNum: (N) ⇒ Boolean = (n: N) ⇒ nodeInfo.get(n).isDefined
        val dfsNum: (N) ⇒ DFSNum = (n: N) ⇒ nodeInfo(n).dfsNum
        val setCSCCId: (N, CSCCId) ⇒ Unit = (n: N, cSCCId: CSCCId) ⇒ nodeInfo(n).cSCCId = cSCCId
        val cSCCId: (N) ⇒ CSCCId = (n: N) ⇒ nodeInfo(n).cSCCId

        closedSCCs(ns, es, setDFSNum, hasDFSNum, dfsNum, setCSCCId, cSCCId)
    }

    /**
     * A closed strongly connected component (cSCC) is a (necessarily minimal) set of nodes of
     * a graph where each node belonging to the cSCC can be reached from another node and no node
     * contains an edge to another node that does not belong to the cSCC.
     */
    def closedSCCs[N >: Null <: AnyRef](
        ns:        Traversable[N],
        es:        N ⇒ Traversable[N],
        setDFSNum: (N, DFSNum) ⇒ Unit,
        hasDFSNum: (N) ⇒ Boolean,
        dfsNum:    (N) ⇒ DFSNum,
        setCSCCId: (N, CSCCId) ⇒ Unit,
        cSCCId:    (N) ⇒ CSCCId
    ): List[Iterable[N]] = {

        // IMPROVE Instead of associating every node with its cSCCID it is also conceivable to just store the respective boundary nodes where a new cSCC candiate starts!

        // The algorithm used to compute the scc is loosely inspired by:
        // Information Processing Letters 74 (2000) 107–114
        // Path-based depth-first search for strong and biconnected components
        // Harold N. Gabow 1
        // Department of Computer Science, University of Colorado at Boulder
        //
        // However, we are interested in finding closed sccs; i.e., those strongly connected
        // components that have no outgoing dependencies.

        val PathElementSeparator: Null = null

        var cSCCs = List.empty[Iterable[N]]

        /*
         * Performs a depth-first search to locate an initial strongly connected component.
         * If we detect a connected component, we then check for every element belonging to
         * the connected component whether it also depends on an element which is not a member
         * of the strongly connected component. If Yes, we continue with the checking of the
         * other elements. If No, we perform a depth-first search based on the successor of the
         * node that does not belong to the SCC and try to determine if it is connected to some
         * previous SCC. If so, we merge all nodes as they belong to the same SCC.
         */

        def dfs(n: N, initialDFSNum: DFSNum): DFSNum = {
            if (hasDFSNum(n))
                return initialDFSNum;

            // CORE DATA STRUCTURES
            var thisPathFirstDFSNum = initialDFSNum
            var nextDFSNum = thisPathFirstDFSNum
            var nextCSCCId = 1
            val path = mutable.ArrayBuffer.empty[N]
            val worklist = mutable.Stack.empty[N]

            // HELPER METHODS
            def addToPath(n: N): DFSNum = {
                assert(!hasDFSNum(n))
                val dfsNum = nextDFSNum
                setDFSNum(n, dfsNum)
                path += n
                nextDFSNum += 1
                dfsNum
            }
            def pathLength = nextDFSNum - initialDFSNum // <=> path.length
            def killPath(): Unit = { path.clear(); thisPathFirstDFSNum = nextDFSNum }
            def reportPath(p: Iterable[N]): Unit = { cSCCs ::= p }

            // INITIALIZATION
            addToPath(n)
            worklist.push(n).push(PathElementSeparator).pushAll(es(n))

            // PROCESSING
            while (worklist.nonEmpty) {
                //                println(s"next iteration { path=${path.map(n ⇒ dfsNum(n)+":"+n).mkString(",")}; "+
                //                    s"thisParthFirstDFSNum=$thisPathFirstDFSNum; nextDFSNum=$nextDFSNum; nextCSCCId=$nextCSCCId }")

                val n = worklist.pop()
                if (n eq PathElementSeparator) { // i.e., we have visited all child elements 
                    val n = worklist.pop()
                    val nDFSNum = dfsNum(n)
                    if (nDFSNum >= thisPathFirstDFSNum) {
                        //                        println(s"visited all children of path element $n")
                        val thisPathNDFSNum = nDFSNum - thisPathFirstDFSNum
                        val nCSCCId = cSCCId(n)
                        nCSCCId match {
                            case Undetermined ⇒
                                killPath()
                            case nCSCCId if nCSCCId == cSCCId(path.last) &&
                                (
                                    thisPathNDFSNum == 0 /*all elements on the path define a cSCC*/ ||
                                    nCSCCId != cSCCId(path(thisPathNDFSNum - 1))
                                ) ⇒
                                reportPath(path.takeRight(pathLength - thisPathNDFSNum))
                                killPath()

                            case someCSCCId ⇒
                                /*nothing to do*/
                                assert(nDFSNum == 0 || nCSCCId == cSCCId(path.last))

                        }
                    } else {
                        //                        println(s"visited all children of non-cSCC path element $n")
                    }

                } else { // i.e., we are (potentially) extending our path
                    //                    println(s"next node { $n; dfsNum=${if (hasDFSNum(n)) dfsNum(n) else Undetermined} }")

                    if (hasDFSNum(n)) {
                        // we have (at least) a cycle...
                        val nDFSNum = dfsNum(n)
                        if (nDFSNum >= thisPathFirstDFSNum) {
                            // this cycle may become a cSCC
                            val nCSCCId = cSCCId(n)
                            nCSCCId match {
                                case Undetermined ⇒
                                    // we have a new cycle
                                    val nCSCCId = nextCSCCId
                                    nextCSCCId += 1
                                    val thisPathNDFSNum = nDFSNum - thisPathFirstDFSNum
                                    val cc = path.view.takeRight(pathLength - thisPathNDFSNum)
                                    cc.foreach(n ⇒ setCSCCId(n, nCSCCId))
                                //                                    println(cc.mkString(s"Nodes in a cSCC candidate $nCSCCId: ", ",", ""))
                                //                                    println("path: "+path.mkString)

                                case nCSCCId ⇒
                                    val thisPathNDFSNum = nDFSNum - thisPathFirstDFSNum
                                    path.view.takeRight(pathLength - thisPathNDFSNum).foreach(n ⇒ setCSCCId(n, nCSCCId))
                            }
                        } else {
                            //                            println("this cycle is related to a node that does not take part in a cSCC")
                            killPath()
                        }
                    } else {
                        // we are visiting the element for the first time
                        addToPath(n)
                        worklist.push(n)
                        worklist.push(PathElementSeparator)
                        es(n) foreach { nextN ⇒
                            if (hasDFSNum(nextN) && dfsNum(nextN) < thisPathFirstDFSNum) {
                                killPath()
                            } else {
                                worklist.push(nextN)
                            }
                        }
                    }
                }
            }
            nextDFSNum
        }

        ns.foldLeft(0)((initialDFSNum, n) ⇒ dfs(n, initialDFSNum))

        cSCCs

    }
}

