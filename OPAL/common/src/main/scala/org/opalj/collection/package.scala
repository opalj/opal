/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

import java.util.concurrent.ConcurrentHashMap
import scala.collection.immutable.HashMap
import scala.collection.JavaConverters._

/**
 * Defines helper methods related to Scala's and OPAL's collections APIs.
 *
 * @author Michael Eichberg
 */
package object collection {

    type SomeIntSet = IntSet[_]

    //
    // Helpers related to Lists
    //

    /**
     * Returns the common prefix of the given lists. If l1 is a prefix of l2, then
     * l1 is returned. If l2 is a prefix of l1, l2 is returned, otherwise a new list
     * that contains the prefix is returned. Hence, if `l1===l2` then l1 is returned.
     */
    def commonPrefix[T](l1: List[T], l2: List[T]): List[T] = {
        if (l1 eq l2)
            return l1;

        val prefix = List.newBuilder[T]
        var l1Tail = l1
        var l2Tail = l2
        while (l1Tail.nonEmpty && l2Tail.nonEmpty && l1Tail.head == l2Tail.head) {
            prefix += l1Tail.head
            l1Tail = l1Tail.tail
            l2Tail = l2Tail.tail
        }
        if (l1Tail.isEmpty)
            l1
        else if (l2Tail.isEmpty)
            l2
        else
            prefix.result()
    }

    //
    // Helpers related to java.util.concurrent.ConcurrentHashMaps
    //

    /**
     * Converts a multi-map (a Map that contains Maps) based on
     * `java.util.concurrent.ConcurrentHashMap`s into a corresponding multi-map
     * based on `scala.collection.immutable.HashMap`s.
     * E.g.,
     * {{{
     * val source : ConcurrentHashMap[SourceElement, CMap[ArrayType, Set[DType]]] =...
     * val target : Map[SourceElement, Map[ArrayType, Set[DType]]] = asScala(source)
     * }}}
     */
    def asScala[K, SubK, V](
        map: ConcurrentHashMap[K, ConcurrentHashMap[SubK, V]]
    ): Map[K, Map[SubK, V]] = {

        map.entrySet.asScala.foldLeft(HashMap.empty[K, Map[SubK, V]]) { (c, n) ⇒
            val key = n.getKey()
            val values = n.getValue().entrySet.asScala
            val entry = (
                key,
                values.foldLeft(HashMap.empty[SubK, V])((c, n) ⇒ c + ((n.getKey, n.getValue)))
            )
            c + entry
        }
    }
}
