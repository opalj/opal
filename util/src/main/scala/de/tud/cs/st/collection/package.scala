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
package de.tud.cs.st

/**
 * Various helper methods.
 *
 * @author Michael Eichberg
 */
package object collection {

    //
    // Helpers related to java.util.concurrent.ConcurrentHashMaps
    //

    import java.util.concurrent.{ ConcurrentHashMap ⇒ CMap }

    /**
     * If the `ConcurrentHashMap` (map) contains the key, the respective value is
     * returned. Otherwise, the given function `f` is evaluated and that value is
     * stored in the map and also returned. Please note, that it is possible that `f`
     * is evaluated but the result is not used, if another thread has already
     * associated a value with the respective key. In that case the result of the
     * evaluation of `f` is
     * completely thrown away.
     */
    def putIfAbsentAndGet[K, V](
        map: CMap[K, V], key: K, f: ⇒ V): V = {
        val value = map.get(key)
        if (value != null) {
            value
        } else {
            val newValue = f // we may evaluate f multiple times w.r.t. the same VirtualSourceElement
            val existingValue = map.putIfAbsent(key, newValue)
            if (existingValue != null)
                existingValue
            else
                newValue
        }
    }

    /**
     * Converts a multi-map (a Map that contains maps) based on
     * `java.util.concurrent.ConcurrentHashMap`s into a corresponding multi-map
     * based on `scala.collection.immutable.Map`s.
     * E.g.,
     * {{{
     * val source : CMap[VirtualSourceElement, CMap[ArrayType, Set[DependencyType]]] =...
     * val target : Map[VirtualSourceElement, Map[ArrayType, Set[DependencyType]]] = convert(source)
     * }}}
     */
    def convert[K, SubK, V](map: CMap[K, CMap[SubK, V]]): Map[K, Map[SubK, V]] = {
        import scala.collection.immutable.HashMap
        import scala.collection.JavaConversions

        HashMap.empty ++
            (for {
                aDep ← JavaConversions.iterableAsScalaIterable(map.entrySet)
                source = aDep.getKey()
                value = aDep.getValue()
            } yield {
                (
                    source,
                    HashMap.empty ++
                    (for {
                        target ← JavaConversions.iterableAsScalaIterable(value.entrySet)
                        key = target.getKey()
                        value = target.getValue()
                    } yield {
                        (key, value)
                    })
                )
            })
    }
}