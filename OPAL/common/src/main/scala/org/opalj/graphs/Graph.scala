/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package graphs

import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.Set
import scala.collection.mutable.HashMap
import scala.collection.{Map => AMap}
import org.opalj.collection.IntIterator

/**
 * Represents a mutable (multi-)graph with ordered edges.
 *
 * ==Thread Safety==
 * This class is not thread-safe!
 *
 * @author Michael Eichberg
 */
class Graph[@specialized(Int) N: ClassTag] private (
        val vertices:     Set[N],
        val successors:   LinkedHashMap[N, List[N]],
        val predecessors: LinkedHashMap[N, List[N]]
) extends AbstractGraph[N] {

    def apply(s: N): List[N] = successors.getOrElse(s, List.empty)

    def asIterable: N => Iterable[N] = (n: N) => { this(n) }

    /**
     * Adds a new vertice.
     */
    def addVertice(n: N): this.type = {
        vertices += n
        this
    }

    /**
     * Adds a new edge between the given vertices.
     *
     * (If the vertices were not previously added, they will be added.)
     */
    def addEdge(e: (N, N)): this.type = {
        val (s, t) = e
        this.addEdge(s, t)
    }

    /**
     * Adds a new edge between the given vertices.
     *
     * (If the vertices were not previously added, they will be added.)
     */
    def addEdge(s: N, t: N): this.type = {
        vertices += s += t
        successors += ((s, t :: successors.getOrElse(s, List.empty)))
        predecessors += ((t, s :: predecessors.getOrElse(t, List.empty)))
        this
    }

    /**
     * Removes the given vertice from this graph.
     */
    def removeVertice(v: N): this.type = {
        vertices -= v
        val oldSuccessorsOpt = successors.get(v)
        oldSuccessorsOpt.foreach(_ foreach { s => predecessors(s) = predecessors(s) filter (_ != v) })
        val oldPredecessorsOpt = predecessors.get(v)
        oldPredecessorsOpt.foreach(_ foreach { s => successors(s) = successors(s) filter (_ != v) })
        successors -= v
        predecessors -= v
        this
    }

    def --=(vs: IterableOnce[N]): this.type = { vs.iterator.foreach { v => this removeVertice v }; this }

    /**
     * All nodes which only have incoming dependencies/which have no successors.
     */
    def leafNodes: Set[N] = vertices.filter(v => !successors.contains(v) || successors(v).isEmpty)

    def sccs(filterSingletons: Boolean = false): Iterator[Iterator[N]] = {
        val size = vertices.size
        val indexToN = new Array[N](size)
        val nToIndex = new HashMap[N, Int](size, HashMap.defaultLoadFactor)
        for {
            e <- vertices.iterator.zipWithIndex // Scalac 2.12.2 will issue an incorrect warning for e @ (n, index)
        } {
            val (n, index) = e
            indexToN(index) = n
            nToIndex += e
        }
        val es: Int => IntIterator = (index: Int) => {
            successors.get(indexToN(index)) match {
                case Some(successors) => ??? // successors.mapToIntIterator(nToIndex)
                case None             => IntIterator.empty
            }
        }

        org.opalj.graphs.sccs(size, es, filterSingletons).iterator.map { scc =>
            scc.iterator.map(indexToN)
        }
    }
}

/**
 * Defines factory methods to create simple graphs.
 */
object Graph {

    implicit def intToInteger(i: Int): Integer = Integer.valueOf(i)

    implicit def AnyRefToAnyRef(o: AnyRef): AnyRef = o

    def empty[N: ClassTag]: Graph[N] = {
        new Graph[N](Set.empty, LinkedHashMap.empty, LinkedHashMap.empty)
    }

    def apply[N: ClassTag](edges: AMap[N, List[N]]): Graph[N] = {
        val g = Graph.empty[N]
        edges foreach { e =>
            val (s, ts) = e
            ts foreach { t =>
                g addEdge (s -> t)
            }
        }
        g
    }
}
