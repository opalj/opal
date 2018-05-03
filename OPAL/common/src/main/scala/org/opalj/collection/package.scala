/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

import java.util.concurrent.ConcurrentHashMap
import scala.collection.immutable.HashMap
import scala.collection.JavaConverters._

/**
 * Defines helper methods related to Scala's and OPAL's collections APIs.
 *
 * @author Michael Eichberg
 */
package object collection {

    type AnIntSet = IntSet[_]

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
