/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package cg
package properties

import org.opalj.br._
import org.opalj.br.analyses.SomeProject

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
 * that use the cache!'''
 *
 * ==Example Usage==
 * To store the result of the computation of all target methods for a
 * virtual method call (given some declaring class type and a method signature), the
 * cache could be instantiated as follows:
 * {{{
 * val cache = new CallGraphCache[MethodSignature,Iterable[Method]](project)
 * }}}
 *
 * @note Creating a new cache is comparatively expensive and depends
 *      on the number of `ObjectType`s in a project.
 *
 * @author Michael Eichberg
 */
class CallGraphCache[Contour, Value] private[this] (
        val NullPointerExceptionDefaultConstructor: Option[Method]
) {

    def this(project: SomeProject) = {
        this(
            project.classFile(ObjectType.NullPointerException) match {
                case Some(classFile) ⇒
                    classFile.findMethod("<init>", MethodDescriptor.NoArgsAndReturnVoid) match {
                        case c @ Some(_ /*defaultConstructor*/ ) ⇒ c

                        case _ ⇒
                            throw new UnknownError(
                                "java.lang.NullPointerException has no default constructor"
                            )
                    }
                case None ⇒ None
            }
        )
    }

    // RECALL: Java's ConcurrentHashMap is significantly faster than Scala's one (Scala 2.12.x)

    import java.util.concurrent.{ConcurrentHashMap ⇒ CHMap}

    private[this] val baseCache: CHMap[ObjectType, Value] = new CHMap(512)

    def getOrElseUpdate(key: ObjectType)(f: ⇒ Value): Value = {
        // we don't care if we calculate the result multiple times..
        var cachedValue = baseCache.get(key)
        if (cachedValue == null) {
            cachedValue = f
            baseCache.put(key, cachedValue)
        }
        cachedValue
    }

    private[this] val cache: Array[CHMap[Contour, Value]] = {
        // The cache is 5% larger than the number of "seen" ObjectType's to have
        // room for "new ObjectType"s discovered, e.g., by a reflection analysis
        val size = ObjectType.objectTypesCount * 105 / 100
        Array.fill(size)(new CHMap(16))
    }

    // We use the overflow cache to cache values associated with ObjectTypes
    // that are discovered (queried) after the project was loaded and for which we have
    // not reserved regular space.
    private[this] val overflowCache: CHMap[ObjectType, CHMap[Contour, Value]] =
        new CHMap(cache.length / 20 /* ~ 5%*/ )

    //    private[this] val cacheHits = new java.util.concurrent.atomic.AtomicInteger(0)
    //    private[this] val cacheUpdates = new java.util.concurrent.atomic.AtomicInteger(0)

    /**
     * If a value is already stored in the cache that value is returned, otherwise
     * `f` is evaluated and the cache is updated accordingly before the value is returned.
     * In some rare cases it may be the case that two or more functions that are associated
     * with the same `declaringClass` and `contour` are evaluated concurrently. In such
     * a case the result of only one function is stored in the cache and will later be
     * returned.
     */
    def getOrElseUpdate(
        key:     ObjectType,
        contour: Contour
    )(
        f: ⇒ Value, syncOnEvaluation: Boolean = true
    ): Value = {

        val typeBasedCache = {
            val id = key.id
            if (id < cache.length)
                cache(id)
            else {
                val typeBasedCache = overflowCache.get(key)
                if (typeBasedCache == null) {
                    val newCache = new CHMap[Contour, Value](16)
                    val existingCache = overflowCache.putIfAbsent(key, newCache)
                    if (existingCache != null)
                        existingCache
                    else
                        newCache
                } else {
                    typeBasedCache
                }
            }
        }
        val cachedValue = typeBasedCache.get(contour)
        if (cachedValue != null) {
            //            cacheHits.incrementAndGet()
            cachedValue
        } else {
            if (syncOnEvaluation) {
                // we assume that `f` is expensive to compute
                typeBasedCache.synchronized {
                    //                    cacheUpdates.incrementAndGet()
                    val value = f
                    typeBasedCache.put(contour, value)
                    value
                }
            } else {
                //                cacheUpdates.incrementAndGet()
                val value = f
                typeBasedCache.put(contour, value)
                value
            }
        }
    }

    //    lazy val statistics: Map[String, Int] =
    //        Map(
    //            "Cache Hits" -> cacheHits.get,
    //            "Cache Updates" -> cacheUpdates.get,
    //            "Cache Entries" -> (cache.foldLeft(0)(_ + _.size))
    //        )
}
