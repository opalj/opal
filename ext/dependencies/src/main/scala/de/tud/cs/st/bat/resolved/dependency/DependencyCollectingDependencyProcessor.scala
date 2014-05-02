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

    import scala.collection.concurrent.{ TrieMap ⇒ CMap }
    import scala.collection.mutable.{ Set }

    import java.util.concurrent.ConcurrentHashMap
    import java.util.concurrent.atomic.AtomicLong

    private[this] val deps = new ConcurrentHashMap[( /*source*/ VirtualSourceElement, /*target*/ VirtualSourceElement), /*BitVector which encodes the kind of dependencies*/ AtomicLong](
        // we assume that every source element has roughly ten dependencies on other source elements
        virtualSourceElementsCountHint.getOrElse(16000) * 10
    )

    //    private[this] val deps =
    //        CMap.empty[VirtualSourceElement, CMap[VirtualSourceElement, Set[DependencyType]]]0

    private[this] val depsOnArrayTypes =
        CMap.empty[VirtualSourceElement, CMap[ArrayType, Set[DependencyType]]]

    private[this] val depsOnBaseTypes =
        CMap.empty[VirtualSourceElement, CMap[BaseType, Set[DependencyType]]]

    def processDependency(
        source: VirtualSourceElement,
        target: VirtualSourceElement,
        dType: DependencyType): Unit = {

        val key = ((source, target))
        val dependenciesSet: AtomicLong = {
            val value = deps.get(key)
            if (value == null) {
                val newValue = new AtomicLong()
                val theNewValue = deps.putIfAbsent(key, newValue)
                if (theNewValue != null)
                    theNewValue
                else
                    newValue
            } else {
                value
            }
        }
        val newDependencyMask = DependencyType.bitMask(dType)
        while ({
            val currentDependenciesMask = dependenciesSet.get()
            if ((currentDependenciesMask & newDependencyMask) == newDependencyMask)
                return ;
            val newCurrentDependenciesMask = currentDependenciesMask | newDependencyMask

            !dependenciesSet.compareAndSet(currentDependenciesMask, newCurrentDependenciesMask)
        }) { /* repeat */ }

        //        val targetElements = deps.getOrElseUpdate(source, CMap.empty[VirtualSourceElement, Set[DependencyType]])
        //        val dependencyTypes = targetElements.getOrElseUpdate(target, Set.empty[DependencyType])
        //        if (!dependencyTypes.contains(dType)) {
        //            dependencyTypes.synchronized {
        //                dependencyTypes += dType
        //            }
        //        }
    }

    def processDependency(
        source: VirtualSourceElement,
        arrayType: ArrayType,
        dType: DependencyType): Unit = {

        val arrayTypes = depsOnArrayTypes.getOrElseUpdate(source, CMap.empty[ArrayType, Set[DependencyType]])
        val dependencyTypes = arrayTypes.getOrElseUpdate(arrayType, Set.empty[DependencyType])
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

        val baseTypes = depsOnBaseTypes.getOrElseUpdate(source, CMap.empty[BaseType, Set[DependencyType]])
        val dependencyTypes = baseTypes.getOrElseUpdate(baseType, Set.empty[DependencyType])
        if (!dependencyTypes.contains(dType)) {
            dependencyTypes.synchronized {
                dependencyTypes += dType
            }
        }
    }

    def toStore: DependencyStore = {

        import scala.collection.mutable.{ Map ⇒ MutableMap }
        import scala.collection.immutable.{ Set ⇒ ImmutableSet }

        val dependencyTypes: MutableMap[Set[DependencyType], ImmutableSet[DependencyType]] = MutableMap.empty

        //        val theDeps =
        //            (deps map { dep ⇒
        //                val (source, targets) = dep
        //                val newTargets = (targets map { targetKind ⇒
        //                    val (target, dTypes) = targetKind
        //                    (target, dependencyTypes.getOrElseUpdate(dTypes, dTypes.toSet))
        //                }).toMap
        //                (source, newTargets)
        //            }).toMap
        import scala.collection.mutable.AnyRefMap

        val theDeps = new AnyRefMap[VirtualSourceElement, AnyRefMap[VirtualSourceElement, Long]](deps.size() / 10)
        for {
            aDep ← JavaConversions.iterableAsScalaIterable(deps.entrySet)
            (source, target) = aDep.getKey()
            deps = aDep.getValue().get()
        } {
            val newTargets = theDeps.getOrElseUpdate(source, new AnyRefMap[VirtualSourceElement, Long])
            newTargets.put(target, deps)
        }
        theDeps.foreachValue(_.repack())
        theDeps.repack()

        val theDepsOnArrayTypes =
            (depsOnArrayTypes map { dep ⇒
                val (source, targets) = dep
                val newTargets = (targets map { targetKind ⇒
                    val (target, dTypes) = targetKind
                    (target, dependencyTypes.getOrElseUpdate(dTypes, dTypes.toSet))
                }).toMap
                (source, newTargets)
            }).toMap

        val theDepsOnBaseTypes =
            (depsOnBaseTypes map { dep ⇒
                val (source, targets) = dep
                val newTargets = (targets map { targetKind ⇒
                    val (target, dTypes) = targetKind
                    (target, dependencyTypes.getOrElseUpdate(dTypes, dTypes.toSet))
                }).toMap
                (source, newTargets)
            }).toMap

        new DependencyStore(theDeps, theDepsOnArrayTypes, theDepsOnBaseTypes)
    }
}



