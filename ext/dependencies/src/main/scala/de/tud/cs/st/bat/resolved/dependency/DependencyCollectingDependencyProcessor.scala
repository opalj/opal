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
package dependency

import analyses.SomeProject
import analyses.ProjectInformationKey
import scala.collection.JavaConversions

/**
 * ==Thread Safety==
 * This class is thread-safe. However, it does not make sense to call the method
 * toStore unless the dependency extractor that uses this processor has completed.
 *
 * @param virtualSourceElementsCount An estimation of the number of
 *      "VirtualSourceElements" that will be analyzed. In case that a program and all
 *      its libraries are analyzed this number should be equivalent to the number of all
 *      declared classes/interfaces/enums/annotations plus the number of all fields and
 *      methods.
 */
class DependencyCollectingDependencyProcessor(
        val virtualSourceElementsCountHint: Option[Int]) extends DependencyProcessor {

    import scala.collection.mutable.Set
    import scala.collection.immutable.HashMap

    import java.util.concurrent.{ ConcurrentHashMap ⇒ CMap }
    import java.util.concurrent.atomic.AtomicLong

    //    private[this] val deps = new CMap[( /*source*/ VirtualSourceElement, /*target*/ VirtualSourceElement), /*BitVector which encodes the kind of dependencies*/ AtomicLong](
    //        // we assume that every source element has roughly ten dependencies on other source elements
    //        virtualSourceElementsCountHint.getOrElse(16000) * 10
    //    )

    private[this] val deps = new CMap[VirtualSourceElement, CMap[VirtualSourceElement, Set[DependencyType]]](
        // we assume that every source element has roughly ten dependencies on other source elements
        virtualSourceElementsCountHint.getOrElse(16000) * 10
    )

    private[this] val depsOnArrayTypes =
        new CMap[VirtualSourceElement, CMap[ArrayType, Set[DependencyType]]](
            virtualSourceElementsCountHint.getOrElse(4000)
        )

    private[this] val depsOnBaseTypes =
        new CMap[VirtualSourceElement, CMap[BaseType, Set[DependencyType]]](
            virtualSourceElementsCountHint.getOrElse(4000)
        )

    private def putIfAbsentAndGet[K, V](
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

    // Current (e.g.): depsOnArrayTypes : CMap[VirtualSourceElement, CMap[ArrayType, Set[DependencyType]]]
    // GOAL(e.g.): dependenciesOnArrayTypes: Map[VirtualSourceElement, Map[ArrayType, Set[DependencyType]]]
    private def convert[K, SubK, V](map: CMap[K, CMap[SubK, V]]): Map[K, Map[SubK, V]] =
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

    def processDependency(
        source: VirtualSourceElement,
        target: VirtualSourceElement,
        dType: DependencyType): Unit = {

        val targets = putIfAbsentAndGet(deps, source, new CMap[VirtualSourceElement, Set[DependencyType]](16))
        val dependencyTypes = putIfAbsentAndGet(targets, target, Set.empty[DependencyType])
        if (!dependencyTypes.contains(dType)) {
            dependencyTypes.synchronized {
                dependencyTypes += dType
            }
        }
    }

    def processDependency(
        source: VirtualSourceElement,
        arrayType: ArrayType,
        dType: DependencyType): Unit = {

        val arrayTypes = putIfAbsentAndGet(depsOnArrayTypes, source, new CMap[ArrayType, Set[DependencyType]](16))
        val dependencyTypes = putIfAbsentAndGet(arrayTypes, arrayType, Set.empty[DependencyType])
        if (!dependencyTypes.contains(dType)) {
            dependencyTypes.synchronized {
                dependencyTypes += dType
            }
        }
    }

    def processDependency(
        source: VirtualSourceElement,
        baseType: BaseType,
        dType: DependencyType): Unit = {

        val baseTypes = putIfAbsentAndGet(depsOnBaseTypes, source, new CMap[BaseType, Set[DependencyType]](16))
        val dependencyTypes = putIfAbsentAndGet(baseTypes, baseType, Set.empty[DependencyType])
        if (!dependencyTypes.contains(dType)) {
            dependencyTypes.synchronized {
                dependencyTypes += dType
            }
        }
    }

    def toStore: DependencyStore = {

//        import scala.collection.mutable.{ Map ⇒ MutableMap }
//        import scala.collection.immutable.{ Set ⇒ ImmutableSet }
        //
        //        val dependencyTypes: MutableMap[Set[DependencyType], ImmutableSet[DependencyType]] = MutableMap.empty
        //
        //        //        val theDeps =
        //        //            (deps map { dep ⇒
        //        //                val (source, targets) = dep
        //        //                val newTargets = (targets map { targetKind ⇒
        //        //                    val (target, dTypes) = targetKind
        //        //                    (target, dependencyTypes.getOrElseUpdate(dTypes, dTypes.toSet))
        //        //                }).toMap
        //        //                (source, newTargets)
        //        //            }).toMap
        //        import scala.collection.mutable.AnyRefMap
        //
        //        val theDeps = new AnyRefMap[VirtualSourceElement, AnyRefMap[VirtualSourceElement, Long]](deps.size() / 10)
        //        for {
        //            aDep ← JavaConversions.iterableAsScalaIterable(deps.entrySet)
        //            (source, target) = aDep.getKey()
        //            deps = aDep.getValue().get()
        //        } {
        //            val newTargets = theDeps.getOrElseUpdate(source, new AnyRefMap[VirtualSourceElement, Long])
        //            newTargets.put(target, deps)
        //        }
        //        theDeps.foreachValue(_.repack())
        //        theDeps.repack()

        val theDeps = convert(deps)
        val theDepsOnArrayTypes = convert(depsOnArrayTypes)
        val theDepsOnBaseTypes = convert(depsOnBaseTypes)

        new DependencyStore(theDeps, theDepsOnArrayTypes, theDepsOnBaseTypes)
    }
}



