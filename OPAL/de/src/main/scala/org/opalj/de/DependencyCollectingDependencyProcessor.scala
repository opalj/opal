/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package de

import java.util.concurrent.{ConcurrentHashMap => CMap}
import scala.collection.mutable.Set

import org.opalj.collection.asScala
import org.opalj.br._

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
        val virtualSourceElementsCountHint: Option[Int]
) extends DependencyProcessor {

    private[this] val deps =
        new CMap[VirtualSourceElement, CMap[VirtualSourceElement, Set[DependencyType]]](
            // assumption: every source element has ~ten dependencies on other source elements
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
        dType:  DependencyType
    ): Unit = {

        val targets =
            deps.computeIfAbsent(
                source,
                (_) => new CMap[VirtualSourceElement, Set[DependencyType]](16)
            )

        val dependencyTypes = targets.computeIfAbsent(target, (_) => Set.empty[DependencyType])

        if (!dependencyTypes.contains(dType)) {
            dependencyTypes.synchronized {
                dependencyTypes += dType
            }
        }
    }

    def processDependency(
        source:    VirtualSourceElement,
        arrayType: ArrayType,
        dType:     DependencyType
    ): Unit = {

        val arrayTypes =
            depsOnArrayTypes.computeIfAbsent(
                source,
                (_) => new CMap[ArrayType, Set[DependencyType]](16)
            )

        val dependencyTypes =
            arrayTypes.computeIfAbsent(arrayType, (_) => Set.empty[DependencyType])

        if (!dependencyTypes.contains(dType)) {
            dependencyTypes.synchronized {
                dependencyTypes += dType
            }
        }
    }

    def processDependency(
        source:   VirtualSourceElement,
        baseType: BaseType,
        dType:    DependencyType
    ): Unit = {

        val baseTypes =
            depsOnBaseTypes.computeIfAbsent(
                source,
                (_) => new CMap[BaseType, Set[DependencyType]](16)
            )

        val dependencyTypes = baseTypes.computeIfAbsent(baseType, (_) => Set.empty[DependencyType])

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
        val theDeps = asScala(deps)
        val theDepsOnArrayTypes = asScala(depsOnArrayTypes)
        val theDepsOnBaseTypes = asScala(depsOnBaseTypes)

        new DependencyStore(theDeps, theDepsOnArrayTypes, theDepsOnBaseTypes)
    }
}
