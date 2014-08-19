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
package org.opalj
package de

import br._
import br.analyses.SomeProject
import br.analyses.ProjectInformationKey

/**
 * Collects all dependencies extracted by a [[DependencyExtractor]].
 *
 * ==Thread Safety==
 * This class is thread-safe. However, it does not make sense to call the method
 * [[toStore]] unless the dependency extractor that uses this processor has completed.
 *
 * @param virtualSourceElementsCount An estimation of the number of
 *      "VirtualSourceElements" that will be analyzed. In case that a program and all
 *      its libraries are analyzed this number should be roughly equivalent to the number
 *      of all declared classes/interfaces/enums/annotations plus the number of all
 *      fields and methods.
 *
 * @author Michael Eichberg
 */
class DependencyCollectingDependencyProcessor(
        val virtualSourceElementsCountHint: Option[Int]) extends DependencyProcessor {

    import scala.collection.mutable.Set
    import java.util.concurrent.{ ConcurrentHashMap ⇒ CMap }
    import org.opalj.collection.{ convert, putIfAbsentAndGet }

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

    def processDependency(
        source: VirtualSourceElement,
        target: VirtualSourceElement,
        dType: DependencyType): Unit = {

        val targets =
            putIfAbsentAndGet(
                deps,
                source,
                new CMap[VirtualSourceElement, Set[DependencyType]](16))

        val dependencyTypes =
            putIfAbsentAndGet(targets, target, Set.empty[DependencyType])

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

        val arrayTypes =
            putIfAbsentAndGet(
                depsOnArrayTypes,
                source,
                new CMap[ArrayType, Set[DependencyType]](16))

        val dependencyTypes =
            putIfAbsentAndGet(arrayTypes, arrayType, Set.empty[DependencyType])

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

        val baseTypes =
            putIfAbsentAndGet(
                depsOnBaseTypes,
                source,
                new CMap[BaseType, Set[DependencyType]](16))

        val dependencyTypes =
            putIfAbsentAndGet(
                baseTypes, baseType, Set.empty[DependencyType])

        if (!dependencyTypes.contains(dType)) {
            dependencyTypes.synchronized {
                dependencyTypes += dType
            }
        }
    }

    /**
     * Creates a [[DependencyStore]] using the extracted dependencies.
     */
    def toStore: DependencyStore = {
        val theDeps = convert(deps)
        val theDepsOnArrayTypes = convert(depsOnArrayTypes)
        val theDepsOnBaseTypes = convert(depsOnBaseTypes)

        new DependencyStore(theDeps, theDepsOnArrayTypes, theDepsOnBaseTypes)
    }
}

