/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.collection.immutable.Chain
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.log.OPALLogger.info

/**
 * Encapsulates a computed schedule and enables the execution of it. Use an [[AnalysisScenario]]
 * to compute a schedule.
 *
 * @param batches The representation of the computed schedule.
 *
 * @author Michael Eichberg
 */
case class Schedule(
        batches: Chain[Chain[ComputationSpecification]]
) extends ((PropertyStore, Boolean) ⇒ Unit) {

    /**
     * Schedules the computation specifications; that is, executes the underlying analysis scenario.
     *
     * @param ps The property store which should be used to execute the analyses.
     */
    def apply(ps: PropertyStore, trace: Boolean): Unit = {
        implicit val logContext = ps.logContext
        val initInfo =
            batches.flatMap { batch ⇒
                batch.toIterator.map[(ComputationSpecification, Any)] { cs ⇒ cs -> cs.init(ps) }
            }.toMap

        // 1 - Note that the properties of lazy computations are considered to be computed as long
        //     as they are used; not only in the first phase where the analysis is scheduled!
        // 2 - Due to transitive usage of lazily computed properties the lifetime of the (lazy)
        //     analysis may be longer than the batch in which the lazy analysis is scheduled
        var currentLazilyComputedProperties = Set.empty[PropertyKind] // those which are just/still computed

        batches.toIterator.zipWithIndex foreach { batchId ⇒
            // IMPROVE Rewrite the algorithm to avoid recomputing the sets "nextBatches", "nextCSs",.. on every iteration.
            val (batch, currentBatchIndex) = batchId
            val oldLazyComputedProperties = currentLazilyComputedProperties
            currentLazilyComputedProperties = Set.empty
            val computedProperties =
                batch.foldLeft(Set.empty[PropertyKind]) { (computedProperties, cs) ⇒
                    if (cs.isLazy) currentLazilyComputedProperties ++= cs.derives
                    computedProperties ++ cs.derives
                }
            val nextBatches = batches.drop(currentBatchIndex)
            val openProperties =
                nextBatches.tail. // collect properties derived in the future
                    map(batch ⇒ batch.foldLeft(Set.empty[PropertyKind])((c, n) ⇒ c ++ n.derives)).
                    reduceOption((l, r) ⇒ l ++ r).
                    getOrElse(Set.empty)
            // Check if any of the current or future analyses still (transitively) uses
            // a lazily computed property;
            // Note that an analysis which uses an eagerly computed property which is
            // computed using lazily computed properties has no more dependency on the lazily
            // computed property!
            val oldLazyCSs = batches.take(currentBatchIndex).flatMap(batch ⇒ batch.filter(cs ⇒ cs.isLazy))
            val oldEagerCSs = batches.take(currentBatchIndex).flatMap(batch ⇒ batch.filter(cs ⇒ !cs.isLazy))
            val nextCSs = nextBatches.flatMap(batch ⇒ batch)
            val nextUsages = nextCSs.flatMap(cs ⇒ cs.uses)
            var stillUsedLazilyComputedProperties = oldLazyComputedProperties.intersect(nextUsages.toSet)
            var stillUsedLazilyComputedPropertiesCount = -1 // any value..
            // let's check which cs are computing the properties and if they are also lazy...
            // if a property is eagerly computed it acts as a boundary
            do { // => fixpoint computation
                stillUsedLazilyComputedPropertiesCount = stillUsedLazilyComputedProperties.size
                oldLazyCSs foreach { oldLazyCS ⇒
                    if (oldLazyCS.derives.intersect(stillUsedLazilyComputedProperties).nonEmpty) {
                        stillUsedLazilyComputedProperties ++= oldLazyCS.uses
                    }
                }
            } while (stillUsedLazilyComputedPropertiesCount < stillUsedLazilyComputedProperties.size)
            // let's filter those uses based on old eager analyses
            oldEagerCSs foreach { cs ⇒
                stillUsedLazilyComputedProperties --= cs.derives
            }
            currentLazilyComputedProperties ++= stillUsedLazilyComputedProperties
            val newComputedProperties = currentLazilyComputedProperties ++ computedProperties
            if (trace)
                info(
                    "analysis progress",
                    newComputedProperties.mkString(
                        s"analysis phase $currentBatchIndex: ", ", ", ""
                    )
                )
            time {
                ps.setupPhase(
                    newComputedProperties,
                    currentLazilyComputedProperties ++ openProperties // this is an overapproximation, but this is safe!
                )
                batch foreach { cs ⇒ cs.beforeSchedule(ps) }
                batch foreach { cs ⇒ cs.schedule(ps, initInfo(cs).asInstanceOf[cs.InitializationData]) }
                ps.waitOnPhaseCompletion()
                batch foreach { cs ⇒ cs.afterPhaseCompletion(ps) }
            } { t ⇒
                if (trace)
                    info(
                        "analysis progress",
                        s"analysis phase $currentBatchIndex took ${t.toSeconds}"
                    )
            }
        }
        // ... we are done now!
        ps.setupPhase(Set.empty, Set.empty)
    }

    override def toString: String = {
        batches.map(_.map(_.name).mkString("{", ", ", "}")).
            mkString("Schedule(\n\t", "\n\t", "\n)")
    }

}

