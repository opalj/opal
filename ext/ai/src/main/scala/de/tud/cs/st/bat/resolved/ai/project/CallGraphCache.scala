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
package bat
package resolved
package ai
package project

/**
 * A '''thread-safe''' cache for information that is associated
 * with a specific `ObjectType` and an additional key (`Contour`). Conceptually, the cache
 * is a `Map` of `Map`s where the keys of the first map are `ObjectType`s and which
 * return values that are maps where the keys are `Contour`s and the values are the
 * stored/cached information.
 *
 * To minimize contention the cache's maps are all preinitialized based on the number of
 * different types that we have seen. This ensure that two
 * threads can always concurrently access the cache (without blocking)
 * if the information is associated with two different `ObjectType`s. If two threads
 * want to access information that is associated with the same `ObjectType` the
 * data-structures try to minimize potential contention. Hence, this is not a general
 * purpose cache. Using this cache is only appropriate if you need/will cache a lot
 * of information that is associated with different object types.
 *
 * '''It is required that the cache object is created before the threads are created
 * that use the cache!
 *
 * ==Example Usage==
 * To store the result of the computation of all target methods for a
 * virtual method call (given some declaring class type and a method signature), the
 * cache could be instantiated as follows:
 * {{{
 * val cache = new CallGraphCache[MethodSignature,Iterable[Method]]
 * }}}
 *
 * @note Creating a new cache is comparatively expensive and scales
 *      with the number of `ObjectType`s in a project.
 *
 * @author Michael Eichberg
 */
class CallGraphCache[Contour, Value] {

    //    import java.util.concurrent.{ ConcurrentHashMap ⇒ CHMap }
    //
    //    private[this] val cache: Array[CHMap[Contour, Value]] = {
    //        val size = ObjectType.objectTypesCount
    //        val concurrencyLevel = Runtime.getRuntime().availableProcessors()
    //        Array.fill(size)(new CHMap(16, concurrencyLevel))
    //    }
    //
    //    /**
    //     * If a value is already stored in the cache that value is returned, otherwise
    //     * `f` is evaluated and the cache is updated accordingly before the value is returned.
    //     * In some rare cases it may be the case that two or more functions that are associated
    //     * with the same `declaringClass` and `contour` are evaluated concurrently. In such
    //     * a case the result of only one function is stored in the cache and will later be
    //     * returned.
    //     */
    //    def getOrElseUpdate(
    //        declaringClass: ObjectType,
    //        contour: Contour)(
    //            f: ⇒ Value): Value = {
    //
    //        val id = declaringClass.id
    //        val cachedResults = cache(declaringClass.id)
    //        val cachedValue = cachedResults.get(contour)
    //        if (cachedValue != null)
    //            cachedValue
    //        else {
    //            // This is expected provide a better trade-off than to always synchronize
    //            // the evaluation of `f` w.r.t. to ObjectType based cache.
    //            val value = f
    //            cachedResults.put(contour, value)
    //            value
    //        }
    //    }

    import scala.collection.concurrent.{ Map, TrieMap }

    private[this] val cache: Array[Map[Contour, Value]] = {
        val size = ObjectType.objectTypesCount
        Array.fill(size)(TrieMap.empty)
    }

    def getOrElseUpdate(
        declaringClass: ObjectType,
        contour: Contour)(
            f: ⇒ Value): Value = {
        val cache = this.cache
        val contoursMap = cache(declaringClass.id)
        contoursMap.getOrElseUpdate(contour, f)
    }
}




