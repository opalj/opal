/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

import scala.reflect.ClassTag
import org.opalj.collection.IntIterator
import org.opalj.collection.mutable.IntArrayStack

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * This package defines graph algorithms as well as factory methods to describe and compute graphs
 * and trees.
 *
 * This package supports the following types of graphs:
 *  1.  graphs based on explicitly connected nodes ([[org.opalj.graphs.Node]]),
 *  1.  graphs where the relationship between the nodes are encoded externally
 *      ([[org.opalj.graphs.Graph]]).
 *
 * @author Michael Eichberg
 */
package object graphs {

    /**
     * Returns the given graph as a CSV encoded adjacency matrix.
     *
     * @example
     * For example, the graph p with the nodes A (id = 0),B (id = 1),C (id =2) and
     *  - an edge from A to B,
     *  - an edge from A to C and
     *  - an edge from C to B
     * would be encoded as follows:
     * <pre>
     * 0,1,1
     * 0,0,0
     * 0,1,0
     * </pre>
     *
     * @note Though the function is optimized to handle very large graphs, encoding sparse
     *       graphs using adjacency matrixes is not recommended.
     *
     * @param  maxNodeId The id of the last node. The first node has to have the id 0. I.e.,
     *                   in case of a graph with just two nodes, the maxNodeId is 1.
     * @param  successors The successor nodes of the node with the given id; the function has to
     *                    be defined for every node in the range [0..maxNodeId].
     * @return an adjacency matrix describing the given graph encoded using CSV. The returned
     *         byte array an be directly saved and represents a valid CSV file.
     */
    def toAdjacencyMatrix(maxNodeId: Int, successors: Int => Set[Int]): Array[Byte] = {
        val columns = (maxNodeId + 1) * 2
        /* <=> nodes + (',' OR '\n') */
        val rows = maxNodeId + 1
        val g = new Array[Byte](rows * columns) // pre-allocate final array for CSV data
        var r = 0
        while (r <= maxNodeId) {
            val s = successors(r)
            var c = 0
            while (c <= maxNodeId) {
                val i = r * columns + c * 2
                g(i) = if (s.contains(c)) '1' else '0'
                c += 1
                if (c <= maxNodeId) g(i + 1) = ',' else g(i + 1) = '\n'
            }
            r += 1
        }
        g
    }

    /**
     * Generates a string that describes a (multi-)graph using the ".dot/.gv" file format
     * [[http://graphviz.org/pdf/dotguide.pdf]].
     * The graph is defined by the given set of nodes.
     *
     * Requires that `Node` implements a content-based `equals` and `hashCode` method.
     */
    def toDot(
        rootNodes: Iterable[_ <: Node],
        dir:       String              = "forward",
        ranksep:   String              = "0.8",
        fontname:  String              = "Helvetica",
        rankdir:   String              = "TB"
    ): String = {
        var nodesToProcess = Set.empty[Node] ++ rootNodes
        var processedNodes = Set.empty[Node]

        var s = "digraph G {\n"+
            s"\tdir=$dir;\n"+
            s"\tranksep=$ranksep;\n"+
            s"\trankdir=$rankdir;\n"+
            s"\tnode [fontname=$fontname,shape=rectangle];\n"

        while (nodesToProcess.nonEmpty) {
            val nextNode = nodesToProcess.head
            // prepare the next iteration
            processedNodes += nextNode
            nodesToProcess = nodesToProcess.tail

            if (nextNode.toHRR.isDefined) {
                var visualProperties = nextNode.visualProperties
                visualProperties += (
                    "label" -> nextNode.toHRR.get.replace("\"", "\\\"").replace("\n", "\\l")
                )
                s +=
                    "\t"+nextNode.nodeId +
                    visualProperties.map(e => "\""+e._1+"\"=\""+e._2+"\"").
                    mkString("[", ",", "];\n")
            }

            val f: (Node => Unit) = sn => {
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

    /**
     * Function to convert a given graphviz dot file to SVG. The transformation is done using the
     * vis-js.com library which is a translated version of graphviz to JavaScript.
     *
     * The first call, which will initialize the JavaScript engine, will take some time.
     * Afterwards, the tranformation is much faster.
     */
    final lazy val dotToSVG: String => String = {
        import javax.script.Invocable
        import javax.script.ScriptEngine
        import javax.script.ScriptEngineManager
        import java.io.BufferedReader
        import java.io.InputStream
        import java.io.InputStreamReader
        import org.opalj.log.OPALLogger
        import org.opalj.log.GlobalLogContext

        OPALLogger.info(
            "setup",
            "initialzing JavaScript engine for rendering dot graphics"
        )(GlobalLogContext)
        val engineManager = new ScriptEngineManager()
        val engine: ScriptEngine = engineManager.getEngineByName("nashorn")
        var visJS: InputStream = null
        val invocable: Invocable = try {
            visJS = this.getClass.getResourceAsStream("viz-lite.js")
            val reader = new BufferedReader(new InputStreamReader(visJS))
            engine.eval(reader)
            engine.asInstanceOf[Invocable]
        } finally {
            if (visJS ne null) visJS.close()
        }
        OPALLogger.info(
            "setup",
            "finished initialization of JavaScript engine for rendering dot graphics"
        )(GlobalLogContext)

        (dot: String) => invocable.invokeFunction("Viz", dot).toString
    }

    // ---------------------------------------------------------------------------------------
    //
    // Closed Strongly Connected Components
    //
    // ---------------------------------------------------------------------------------------

    final def closedSCCs[N >: Null <: AnyRef: ClassTag](g: Graph[N]): List[Iterable[N]] = {
        closedSCCs(g.vertices, g.asIterable)
    }

    /**
     * Identifies closed strongly connected components in directed (multi-)graphs.
     *
     * A closed strongly connected component (cSCC) is a non-empty set of nodes of a graph where
     * each node belonging to the cSCC can explicitly be reached from another node and no node
     * contains an edge to some node that does not belong to the same cSCC.
     *
     * Every such set is necessarily minimal/maximal.
     *
     * @note    This implementation can handle (arbitrarily degenerated) graphs with up to
     *          Int.MaxValue nodes (if the VM is given enough memory!)
     *
     * @tparam N The type of the graph's nodes. The nodes have to correctly implements equals
     *         and hashCode.
     * @param  ns The nodes of the graph.
     * @param  es A function that, given a node, returns all successor nodes. Basically, the edges
     *         of the graph.
     */
    def closedSCCs[N >: Null <: AnyRef: ClassTag](
        ns: Iterable[N],
        es: N => Iterable[N] // TODO Improve(?) N => Iterator[N]
    ): List[Iterable[N]] = {

        val nDFSNums = new it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap[N]()
        def setDFSNum(n: N, i: Int): Unit = nDFSNums.put(n, i)
        def hasDFSNum(n: N): Boolean = nDFSNums.containsKey(n)
        def dfsNum(n: N): Int = nDFSNums.getInt(n)

        // Core Idea: perform depth-first search
        val ProcessedNodeNum: Int = -1
        val PathSegmentSeparator: Null = null

        val workstack = new mutable.Stack[N](initialSize = 8)
        val path = new ArrayBuffer[N](initialSize = 16)

        var cSCCs = List.empty[Iterable[N]]

        var nextDFSNum = ProcessedNodeNum + 1 // the next (not yet used) dfsNum

        def dfs(initialN: N): Unit = {

            var initialDFSNum: Int = nextDFSNum

            path.clear()

            workstack.clear()
            workstack.push(initialN)

            def markPathAsProcessed(): Unit = {
                path.foreach(n => setDFSNum(n, ProcessedNodeNum))
                path.clear()
            }
            def addToPath(n: N): Unit = {
                if (path.isEmpty) {
                    // This happens if we have identified a path which does not end
                    // in a cSCC and we have killed the path.
                    initialDFSNum = nextDFSNum
                }
                path += n
                setDFSNum(n, nextDFSNum)
                nextDFSNum += 1
            }

            while (workstack.nonEmpty) {
                val n = workstack.pop()
                if (n == PathSegmentSeparator) {
                    // We have visited all child elements of the "previous element";
                    // we can now report a path, if we have one (note that we eagerly kill
                    // non-cSCC paths and hence every path that still exists, is a valid path.
                    // However, we have to ensure that we have visited _all_ successors belonging
                    // to the current candidate cSCC)
                    val n = workstack.pop()
                    if (path.nonEmpty) {
                        val nDFSNum = dfsNum(n)
                        val cSCCDFSNum = dfsNum(path.last)
                        assert(cSCCDFSNum != ProcessedNodeNum)

                        if (nDFSNum != cSCCDFSNum) {
                            // This is the trivial case... obviously the end of the path is a
                            // closed SCC.
                            // ALTERNATIVE: val cSCC = path.dropWhile(n => dfsNum(n) != cSCCDFSNum)
                            val cSCC = path.drop(cSCCDFSNum - initialDFSNum)
                            cSCCs ::= cSCC
                            markPathAsProcessed()
                        } else {
                            // Test if we are done exploring all paths potentially related to
                            // the cSCC...
                            // ALTERNATIVE CHECK:
                            //val cSCCandidate = path.iterator.drop(cSCCDFSNum - initialDFSNum)
                            if (workstack.isEmpty ||
                                path.iterator.drop(cSCCDFSNum - initialDFSNum).forall(n =>
                                    // ... for all cSCCandidates
                                    es(n).forall(succN =>
                                        hasDFSNum(succN) &&
                                            dfsNum(succN) == cSCCDFSNum // <= prevents premature cscc identifications
                                    ))) {
                                cSCCs ::= path.drop(cSCCDFSNum - initialDFSNum)
                                markPathAsProcessed()
                            }
                        }
                    }
                } else if (hasDFSNum(n)) {
                    // this node was visited before....

                    val nDFSNum = dfsNum(n)
                    if (nDFSNum < initialDFSNum) {
                        // The current node either belongs to a (previously identified) path which
                        // ends in a node with no outgoing edges or belongs to a (previously)
                        // identified cSCC; hence, all nodes on the path cannot be part of
                        // a(nother) cSCC.
                        markPathAsProcessed()
                    } else {
                        // We have found a new cycle; we now use the dfs num to mark
                        // all nodes as belonging to the cycle.
                        val startPathIndex = nDFSNum - initialDFSNum // the (start) index of the cycle
                        var pathIndex = path.length - 1
                        if (dfsNum(path(pathIndex)) != nDFSNum) { // test if the node is already correctly marked
                            while (pathIndex >= startPathIndex) {
                                setDFSNum(path(pathIndex), nDFSNum)
                                pathIndex -= 1
                            }
                        }
                    }

                } else {
                    // The node was not visited before; we are extending our path
                    addToPath(n)

                    val succNs = es(n)
                    if (succNs.nonEmpty) {
                        workstack.push(n)
                        workstack.push(PathSegmentSeparator: N)
                        workstack prependAll succNs
                    } else {
                        // We have a path which leads to a node with no outgoing edge;
                        // hence, all nodes on the path cannot be part of a cSCC.
                        markPathAsProcessed()
                    }
                }
            }
            assert(path.isEmpty)
        }

        ns.foreach(n => if (!hasDFSNum(n)) dfs(n))
        cSCCs
    }

    /*
    private[this] val Undetermined: Int = -1

    final def closedSCCs[N >: Null <: AnyRef](
        ns: Traversable[N],
        es: N => Traversable[N]
    ): List[Iterable[N]] = {

        case class NInfo(dfsNum: Int, var cSCCId: Int = Undetermined) {
            override def toString: String = {
                val cSCCId = this.cSCCId match {
                    case Undetermined => "Undetermined"
                    case id           => id.toString
                }
                s"(dfsNum=$dfsNum,cSCCId=$cSCCId)"
            }
        }

        val nodeInfo: mutable.HashMap[N, NInfo] = mutable.HashMap.empty

        def setDFSNum(n: N, dfsNum: Int): Unit = {
            nodeInfo.put(n, NInfo(dfsNum))
        }
        val hasDFSNum: (N) => Boolean = (n: N) => nodeInfo.get(n).isDefined
        val dfsNum: (N) => Int = (n: N) => nodeInfo(n).dfsNum
        val setCSCCId: (N, Int) => Unit = (n: N, cSCCId: Int) => nodeInfo(n).cSCCId = cSCCId
        val cSCCId: (N) => Int = (n: N) => nodeInfo(n).cSCCId

        closedSCCs(ns, es, setDFSNum, hasDFSNum, dfsNum, setCSCCId, cSCCId)
    }

    def closedSCCs[N >: Null <: AnyRef](
        ns:        Traversable[N],
        es:        N => Traversable[N],
        setDFSNum: (N, Int) => Unit,
        hasDFSNum: (N) => Boolean,
        dfsNum:    (N) => Int,
        setCSCCId: (N, Int) => Unit,
        cSCCId:    (N) => Int
    ): List[Iterable[N]] = {
        /* The following is not a strict requirement, more an expectation (however, (c)sccs
         * not reachable from a node in ns will not be detected!
        assert(
            { val allNodes = ns.toSet; allNodes.forall { n => es(n).forall(allNodes.contains) } },
            "the graph references nodes which are not in the set of all nodes"
        )
        */

        // The algorithm used to compute the closed scc is loosely inspired by:
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
        def dfs(initialDFSNum: Int, n: N): Int = {
            if (hasDFSNum(n))
                return initialDFSNum;

            // CORE DATA STRUCTURES
            var thisPathFirstDFSNum = initialDFSNum
            var nextDFSNum = thisPathFirstDFSNum
            var nextCSCCId = 1
            val path = mutable.ArrayBuffer.empty[N]
            val worklist = mutable.(Ref?)ArrayStack.empty[N]

            // HELPER METHODS
            def addToPath(n: N): Int = {
                assert(!hasDFSNum(n))
                val dfsNum = nextDFSNum
                setDFSNum(n, dfsNum)
                path += n
                nextDFSNum += 1
                dfsNum
            }
            def pathLength = path.length
            def killPath(): Unit = { path.clear(); thisPathFirstDFSNum = nextDFSNum }
            def reportPath(p: Iterable[N]): Unit = { cSCCs ::= p }

            // INITIALIZATION
            addToPath(n)
            worklist.push(n)
            worklist.push(PathElementSeparator)
            worklist ++= es(n)

            // PROCESSING
            while (worklist.nonEmpty) {
                // println(
                //  s"next iteration { path=${path.map(n => dfsNum(n)+":"+n).mkString(",")}; "+
                //  s"thisParthFirstDFSNum=$thisPathFirstDFSNum; "+
                //  s"nextDFSNum=$nextDFSNum; nextCSCCId=$nextCSCCId }")

                val n = worklist.pop()
                if (n eq PathElementSeparator) { // i.e., we have visited all child elements
                    val n = worklist.pop()
                    val nDFSNum = dfsNum(n)
                    if (nDFSNum >= thisPathFirstDFSNum) {
                        //                        println(s"visited all children of $n")
                        val thisPathNDFSNum = nDFSNum - thisPathFirstDFSNum
                        val nCSCCId = cSCCId(n)
                        nCSCCId match {
                            case Undetermined =>
                                killPath()
                            case nCSCCId if nCSCCId == cSCCId(path.last) &&
                                (
                                    thisPathNDFSNum == 0 /*all elements on the path define a cSCC*/ ||
                                    nCSCCId != cSCCId(path(thisPathNDFSNum - 1))
                                ) =>
                                reportPath(path.takeRight(pathLength - thisPathNDFSNum))
                                killPath()

                            case someCSCCId =>
                                /*nothing to do*/
                                assert(
                                    // nDFSNum == 0 ???
                                    nDFSNum == initialDFSNum || someCSCCId == cSCCId(path.last),
                                    s"nDFSNum=$nDFSNum; nCSCCId=$nCSCCId; "+
                                        s"cSCCId(path.last)=${cSCCId(path.last)}\n"+
                                        s"(n=$n; initialDFSNum=$initialDFSNum; "+
                                        s"thisPathFirstDFSNum=$thisPathFirstDFSNum\n"+
                                        cSCCs.map(_.map(_.toString)).
                                        mkString("found csccs:\n\t", "\n\t", "\n")
                                )

                        }
                    } else {
                        // println(s"visited all children of non-cSCC path element $n")
                    }

                } else { // i.e., we are (potentially) extending our path
                    // println(s"next node { $n; dfsNum=${if (hasDFSNum(n)) dfsNum(n) else N/A} }")

                    if (hasDFSNum(n)) {
                        // we have (at least) a cycle...
                        val nDFSNum = dfsNum(n)
                        if (nDFSNum >= thisPathFirstDFSNum) {
                            // this cycle may become a cSCC
                            val nCSCCId = cSCCId(n)
                            nCSCCId match {
                                case Undetermined =>
                                    // we have a new cycle
                                    val nCSCCId = nextCSCCId
                                    nextCSCCId += 1
                                    val thisPathNDFSNum = nDFSNum - thisPathFirstDFSNum
                                    val cc = path.view.takeRight(pathLength - thisPathNDFSNum)
                                    cc.foreach(n => setCSCCId(n, nCSCCId))
                                // val header = s"Nodes in a cSCC candidate $nCSCCId: "
                                // println(cc.mkString(header, ",", ""))
                                // println("path: "+path.mkString)

                                case nCSCCId =>
                                    val thisPathNDFSNum = nDFSNum - thisPathFirstDFSNum
                                    path.view.takeRight(pathLength - thisPathNDFSNum).foreach { n =>
                                        setCSCCId(n, nCSCCId)
                                    }
                            }
                        } else {
                            // this cycle is related to a node that does not take part in a cSCC
                            killPath()
                        }
                    } else {
                        // we are visiting the element for the first time
                        addToPath(n)
                        worklist.push(n)
                        worklist.push(PathElementSeparator)
                        es(n) foreach { nextN =>
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

        ns.foldLeft(0)((initialDFSNum, n) => dfs(initialDFSNum, n))

        cSCCs
    }
    */

    /**
     * Implementation of Tarjan's algorithm for finding strongly connected components. Compared
     * to the standard implementation using non-tail recursive calls, this one uses an explicit
     * stack to make the implementation scale to very large (degenerated) graphs. E.g.,
     * this implementation can handle graphs containing up to XXX nodes in a single cycle.
     *
     * @example
     * A very simple, but very large cycle:
     * {{{
     * def genGraph(max : Int) = {
     *      var i = 1;
     *      var g = Map[Int,List[Int]]((0,List(max-1)));
     *      while(i < max){ g += ((i,List(i-1))); i+=1; }
     *      g
     * }
     * val g = genGraph(100000)
     * val es = (i:Int) => {g(i).toIterator}
     * org.opalj.graphs.sccs(g.size,es).mkString("\n")
     * }}}
     *
     * A large graph:
     * {{{
     * val g = Map((0,List(5)),(1,List(2)),(2,List(1,4)),(3,List(0)),(4,List(2)),(5,List(4,3,6)),(6,List(6)),(7, List()))
     * val es = (i:Int) => { g(i).toIterator }
     * org.opalj.graphs.sccs(g.size,es,filterSingletons = true).mkString("\n")
     * }}}
     *
     * @param  ns The number of nodes. The nodes have to be consecutively numbered [0..ns-1].
     * @param  es A function that returns for a given node n the immediate successors for that node.
     * @param  filterSingletons Removes SCCs with just one node, where the node is not connected to
     *         itself. I.e., nodes which have a self-edge will be kept and other will be discarded.
     */
    def sccs(
        ns:               Int,
        es:               Int => IntIterator,
        filterSingletons: Boolean            = false
    ): List[List[Int]] = {

        /* TEXTBOOK DESCRIPTION
        * (cannot handle very large, degenerated graphs due to non-tail recursion)
        *
        * algorithm tarjan is
        * input: graph G = (V, E)
        * output: set of strongly connected components (sets of vertices)
        *
        * index := 0
        * S := empty array
        * for each v in V do
        *   if (v.index is undefined) then strongconnect(v) end if
        * end for
        *
        * function strongconnect(v)
        *   // Set the depth index for v to the smallest unused index
        *   v.index := index
        *   v.lowlink := index
        *    index := index + 1
        *    S.push(v)
        *    v.onStack := true
        *
        *    // Consider successors of v
        *   for each (v, w) in E do
        *    if (w.index is undefined) then
        *        // Successor w has not yet been visited; recurse on it
        *           strongconnect(w)
        *           v.lowlink  := min(v.lowlink, w.lowlink)
        *       else if (w.onStack) then
        *           // Successor w is in stack S and hence in the current SCC
        *           v.lowlink  := min(v.lowlink, w.lowlink)
        *       end if
        *   end for
        *
        *   // If v is a root node, pop the stack and generate an SCC
        *   if (v.lowlink = v.index) then
        *       start a new strongly connected component
        *       repeat
        *           w := S.pop()
        *           w.onStack := false
        *           add w to current strongly connected component
        *       while (w != v)
        *       output the current strongly connected component
        *   end if
        * end function
        */

        // output data structure
        var sccs: List[List[Int]] = List.empty

        val nIndex = new Array[Int](ns + 1)
        val nLowLink = new Array[Int](ns + 1)
        val nOnStack = new Array[Boolean](ns + 1)

        // we use the "index == 0" to identify the case that no index has been assigned
        val UndefinedIndex: Int = 0
        var index: Int = 1
        val s = new IntArrayStack(Math.max(8, ns / 4))

        /* RECURSIVE VERSION (FOLLOWING TEXTBOOK IMPLEMENTATION)
        var n = 0
        while (n < ns) {
            if (nIndex(n) == UndefinedIndex) scc(n)
            n += 1
        }

        def scc(n: Int): Unit = {
            nIndex(n) = index
            nLowLink(n) = index
            index += 1
            s.push(n)
            nOnStack(n) = true
            es(n).foreach { w =>
                if (nIndex(w) == UndefinedIndex) {
                    scc(w)
                    nLowLink(n) = Math.min(nLowLink(n), nLowLink(w))
                } else if (nOnStack(w)) {
                    nLowLink(n) = Math.min(nLowLink(n), nLowLink(w))
                }
            }

            if (nLowLink(n) == nIndex(n)) {
                var nextSCC = Chain.empty[Int]
                var w: Int = -1
                do {
                    w = s.pop()
                    nOnStack(w) = false
                    nextSCC :&:= w
                } while (n != w)
                sccs :&:= nextSCC
            }
        }
        */

        var n = 0
        while (n < ns) {
            if (nIndex(n) == UndefinedIndex) {
                // The following two stacks are maintained in parallel:
                // ws           Contains the set of nodes which we have to process/for which we need
                //              to continue processing later on.
                // wsSuccessors Contains for the respective node from ws the remaining not yet
                //              processed successors unless the value is null; "null" is used to
                //              signal that the node was not yet processed.

                val ws = IntArrayStack(n)
                val wsSuccessors = mutable.Stack[IntIterator](null)
                // INVARIANT:
                // If wsSuccessors(x) is not null then we have to pop the two values which identify
                // the processed edge; if wsSuccessors is null, the stack just contains the id of
                // the next node that should be processed.
                do {
                    val n = ws.pop()
                    var remainingSuccessors = wsSuccessors.pop()
                    if (remainingSuccessors eq null) {
                        // ... we are processing the node n for the first time
                        nIndex(n) = index
                        nLowLink(n) = index
                        index += 1
                        s.push(n)
                        nOnStack(n) = true
                        remainingSuccessors = es(n)
                    } else {
                        // we have visisted a successor node "w" and now continue with "n"
                        val w = ws.pop()
                        nLowLink(n) = Math.min(nLowLink(n), nLowLink(w))
                    }

                    var continue = true
                    while (continue && remainingSuccessors.hasNext) {
                        val w = remainingSuccessors.next()
                        if (nIndex(w) == UndefinedIndex) {
                            // We basically simulate the recursive call by storing the current
                            // evaluation state for n: the current edge "n->w" and the "remaining
                            // successors"; and the push the succesor node "w"
                            ws.push(w)
                            ws.push(n)
                            wsSuccessors.push(remainingSuccessors)
                            ws.push(w)
                            wsSuccessors.push(null)
                            continue = false
                        } else if (nOnStack(w)) {
                            nLowLink(n) = Math.min(nLowLink(n), nLowLink(w))
                        }
                    }
                    if (continue && !remainingSuccessors.hasNext) {
                        // ... there are no more successors
                        if (nLowLink(n) == nIndex(n)) {
                            var nextSCC = List.empty[Int]
                            var w: Int = -1
                            do {
                                w = s.pop()
                                nOnStack(w) = false
                                nextSCC ::= w
                            } while (n != w)
                            if (!filterSingletons ||
                                nextSCC.tail.nonEmpty ||
                                es(n).exists(_ == n)) {
                                sccs ::= nextSCC
                            }
                        }
                    }
                } while (ws.nonEmpty)

            }
            n += 1
        }

        sccs
    }

}
