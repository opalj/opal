/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

import java.util.{Arrays => JArrays}
import java.util.concurrent.ConcurrentHashMap
import scala.collection.immutable.ArraySeq
import scala.collection.immutable.HashMap
import scala.jdk.CollectionConverters._

/**
 * ==Design Goals==
 * OPAL's collection library is primarily designed with high performance in mind. I.e., all methods
 * provided by the collection library are reasonably optimized. However, providing a very large
 * number of methods is a non-goal. Overall, OPAL's collection library provides:
 *  - collection classes that are manually specialized for primitive data-types.
 *  - collection classes that are optimized for particularly small collections of values.
 *  - collection classes that target special use cases such as using a collection as a
 *    workset/worklist.
 *  - collection classes that offer special methods that minimize the number of steps when
 *    compared to general purpose methods.
 *
 * ==Integration With Scala's Collection Library==
 * Hence, OPAL's collection library complements Scala's default collection library and is not
 * intended to replace it. Integration with Scala's collection library is primarily provided
 * by means of iterators (OPAL's `Iterator`s inherit from Scala's `Iterator`s). Furthermore
 * the companion object of each of OPAL's collection classes generally provides factory methods
 * that facilitate the conversion from Scala collection classes to OPAL collection classes.
 *
 * ==Status==
 * The collection library is growing. Nevertheless, the existing classes are production ready.
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

        map.entrySet.asScala.foldLeft(HashMap.empty[K, Map[SubK, V]]) { (c, n) =>
            val key = n.getKey
            val values = n.getValue.entrySet.asScala
            val entry = (
                key,
                values.foldLeft(HashMap.empty[SubK, V])((c, n) => c + ((n.getKey, n.getValue)))
            )
            c + entry
        }
    }

    def binarySearch[T, X >: T <: Comparable[X]](array: ArraySeq[T], key: X): Int = {
        val data = array.unsafeArray.asInstanceOf[Array[Object]]
        JArrays.binarySearch(data, 0, data.length, key.asInstanceOf[Object])
    }

    def insertedAt[T, X >: T <: AnyRef](array: ArraySeq[T], insertionPoint: Int, e: X): ArraySeq[X] = {
        val data = array.unsafeArray.asInstanceOf[Array[Object]]
        val newData = JArrays.copyOf(data, data.length + 1)
        newData(insertionPoint) = e
        System.arraycopy(data, insertionPoint, newData, insertionPoint + 1, data.length - insertionPoint)
        ArraySeq.unsafeWrapArray(newData).asInstanceOf[ArraySeq[X]]
    }

}
