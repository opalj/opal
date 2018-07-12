/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package graphs

import java.util.function.IntFunction

import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.Set
import scala.collection.mutable.HashMap
import scala.collection.{Map ⇒ AMap}
import org.opalj.collection.IntIterator
import org.opalj.collection.immutable.Chain
import org.opalj.collection.immutable.Naught

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
        val successors:   LinkedHashMap[N, Chain[N]],
        val predecessors: LinkedHashMap[N, Chain[N]]
) extends AbstractGraph[N] {

    def apply(s: N): Chain[N] = successors.getOrElse(s, Naught)

    def asTraversable: N ⇒ Traversable[N] = (n: N) ⇒ { this(n).toTraversable }

    /**
     * Adds a new vertice.
     */
    def +=(n: N): this.type = {
        vertices += n
        this
    }

    /**
     * Adds a new edge between the given vertices.
     *
     * (If the vertices were not previously added, they will be added.)
     */
    def +=(e: (N, N)): this.type = {
        val (s, t) = e
        this += (s, t)
    }

    /**
     * Adds a new edge between the given vertices.
     *
     * (If the vertices were not previously added, they will be added.)
     */
    def +=(s: N, t: N): this.type = {
        vertices += s += t
        successors += ((s, t :&: successors.getOrElse(s, Naught)))
        predecessors += ((t, s :&: predecessors.getOrElse(t, Naught)))
        this
    }

    /**
     * Removes the given vertice from this graph.
     */
    def -=(v: N): this.type = {
        vertices -= v
        val oldSuccessorsOpt = successors.get(v)
        oldSuccessorsOpt.foreach(_ foreach { s ⇒ predecessors(s) = predecessors(s) filter (_ != v) })
        val oldPredecessorsOpt = predecessors.get(v)
        oldPredecessorsOpt.foreach(_ foreach { s ⇒ successors(s) = successors(s) filter (_ != v) })
        successors -= v
        predecessors -= v
        this
    }

    def --=(vs: TraversableOnce[N]): this.type = { vs foreach { v ⇒ this -= v }; this }

    /**
     * All nodes which only have incoming dependencies/which have no successors.
     */
    def leafNodes: Set[N] = vertices.filter(v ⇒ !successors.contains(v) || successors(v).isEmpty)

    def sccs(filterSingletons: Boolean = false): Iterator[Iterator[N]] = {
        val size = vertices.size
        val indexToN = new Array[N](size)
        val nToIndex = new HashMap[N, Int] { override def initialSize = size }
        for {
            e ← vertices.iterator.zipWithIndex // Scalac 2.12.2 will issue an incorrect warning for e @ (n, index)
        } {
            val (n, index) = e
            indexToN(index) = n
            nToIndex += e
        }
        val es: IntFunction[IntIterator] = (index: Int) ⇒ {
            successors.get(indexToN(index)) match {
                case Some(successors) ⇒ successors.mapToIntIterator(nToIndex)
                case None             ⇒ IntIterator.empty
            }
        }

        org.opalj.graphs.sccs(size, es, filterSingletons).toIterator.map { scc ⇒
            scc.toIterator.map(indexToN)
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
        edges foreach { e ⇒
            val (s, ts) = e
            ts foreach { t ⇒
                g += (s → t)
            }
        }
        g
    }
}
