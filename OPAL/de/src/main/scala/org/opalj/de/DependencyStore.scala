/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package de

import scala.collection.Map
import scala.collection.Set
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.br._

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

/**
 * Stores extracted dependencies.
 *
 * ==Thread Safety==
 * This class is thread safe.
 *
 * @author Michael Eichberg
 */
class DependencyStore(
        val dependencies:             Map[VirtualSourceElement, Map[VirtualSourceElement, Set[DependencyType]]],
        val dependenciesOnArrayTypes: Map[VirtualSourceElement, Map[ArrayType, Set[DependencyType]]],
        val dependenciesOnBaseTypes:  Map[VirtualSourceElement, Map[BaseType, Set[DependencyType]]]
)

object DependencyStore {

    def apply[Source](
        classFiles:                Iterable[ClassFile],
        createDependencyExtractor: (DependencyProcessor) => DependencyExtractor
    )(
        implicit
        logContext: LogContext
    ): DependencyStore = {

        val dc = time {
            val dc = new DependencyCollectingDependencyProcessor(Some(classFiles.size * 10))
            val de = createDependencyExtractor(dc)
            classFiles.par.foreach { de.process(_) }
            dc
        } { ns =>
            OPALLogger.info("progress", "collecting dependencies took "+ns.toSeconds)
        }

        time {
            dc.toStore
        } { ns =>
            OPALLogger.info("progress", "creating the dependencies store took "+ns.toSeconds)
        }
    }

    def apply[Source](
        classFiles: Iterable[ClassFile]
    )(
        implicit
        logContext: LogContext
    ): DependencyStore = {
        val createDependencyExtractor = (dp) => new DependencyExtractor(dp)
        apply(classFiles, createDependencyExtractor)
    }
}
