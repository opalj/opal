/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
 * An efficient, '''thread-safe''' cache for information that is associated
 * with a specific `ObjectType` and an additional key (`Contour`). Conceptually, the cache
 * is a `Map` of `Map`s where the keys of the first map are `ObjectType`s and which
 * return values that are maps where the keys are `Contour`s and the values are the
 * stored/cached information.
 *
 * To minimize contention mutual exclusive access to the cache is granted at the level
 * of `ObjectType`s. I.e., two threads can concurrently access the cache (without blocking)
 * if the information is associated with two different `ObjectType`s.
 *
 * ==Example Usage==
 * To store the result of the computation of all target methods for a
 * virtual method call (given some declaring class type and a method signature), the
 * cache could be instantiated as follows:
 * {{{
 * val cache = new CallGraphCache[MethodSignature,Iterable[Method]]
 * }}}
 *
 * @note Creating a new cache is a computationally intensive task that scales
 *      linearly with the number of `ObjectType`s in a project.
 *
 * @author Michael Eichberg
 */
class CallGraphCache[Contour, Result] {
    import collection.mutable.HashMap

    private[this] val cache: Array[HashMap[Contour, Result]] =
        new Array(ObjectType.objectTypesCount)
    // to minimize contention w.r.t. accessing the cache, we create one object 
    // that we use as a mutex per class type
    private[this] val cacheMutexes: Array[Object] = {
        val a = new Array(cache.size)
        Array.fill(cache.size)(new Object)
    }

    /**
     * If a value is already stored in the cache that value is returned, otherwise
     * `f` is evaluated and the cache is updated accordingly before the value is returned.
     */
    def getOrElseUpdate(
        declaringClass: ReferenceType,
        contour: Contour,
        f: ⇒ Result): Result = {

        val id = declaringClass.id
        val cachedResults = {
            val cachedResults = cacheMutexes(id).synchronized { cache(id) }
            if (cachedResults eq null) {
                cacheMutexes(id).synchronized {
                    val cachedResults = cache(declaringClass.id)
                    if (cachedResults eq null) { // still eq null... 
                        val targets = f
                        cache(declaringClass.id) = HashMap((contour, targets))
                        return targets

                    } else {
                        cachedResults
                    }
                }
            } else {
                cachedResults
            }
        }
        cachedResults.synchronized {
            cachedResults.getOrElseUpdate(contour, f)
        }
    }
}




