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

import domain._
import bat.resolved.analyses._
import collection.Set
import collection.Map

import collection.mutable.HashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

class CHACache private (
        private[this] val cache: Array[HashMap[MethodSignature, Iterable[Method]]]) {

    private[this] val cacheMutexes: Array[Object] = {
        val a = new Array(cache.size)
        Array.fill(cache.size)(new Object)
    }

    def getOrElseUpdate(
        declaringClass: ReferenceType,
        callerSignature: MethodSignature,
        orElse: ⇒ Iterable[Method]): Iterable[Method] = {

        @inline def updateCachedResults(
            resolvedTargetsForClass: HashMap[MethodSignature, Iterable[Method]]) = {
            resolvedTargetsForClass.synchronized {
                resolvedTargetsForClass.getOrElseUpdate(
                    callerSignature,
                    orElse
                )
            }
        }

        val id = declaringClass.id
        val cachedResults = {
            val cachedResults = { cacheMutexes(id).synchronized { cache(id) } }
            if (cachedResults eq null) {
                cacheMutexes(id).synchronized {
                    val cachedResults = cache(declaringClass.id)
                    if (cachedResults eq null) { // still eq null... 
                        val targets = orElse
                        cache(declaringClass.id) = HashMap((callerSignature, targets))
                        return targets
                    } else {
                        cachedResults
                    }
                }
            } else {
                cachedResults
            }
        }
        updateCachedResults(cachedResults)
    }
}
object CHACache {

    def apply(project: SomeProject): CHACache = {
        new CHACache(new Array(project.objectTypesCount))
    }
}



