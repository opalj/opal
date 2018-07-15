/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.collection.immutable.Chain

/**
 * Encapsulates a computed schedule and enables the execution of it.
 *
 * @param batches The representation of the computed schedule.
 *
 * @author Michael Eichberg
 */
case class Schedule(
        batches: Chain[Chain[ComputationSpecification]]
) extends (PropertyStore ⇒ Unit) {

    /**
     * Schedules the computation specifications; that is, executes the underlying analysis scenario.
     *
     * @param ps The property store which should be used to execute the analyses.
     */
    def apply(ps: PropertyStore): Unit = {
        val initInfo =
            batches.flatMap { batch ⇒
                batch.toIterator.map[(ComputationSpecification, Any)] { cs ⇒ cs -> cs.init(ps) }
            }.toMap

        batches foreach { batch ⇒
            val computedProperties =
                batch.foldLeft(Set.empty[PropertyKind])((c, n) ⇒ c ++ n.derives)
            val openProperties =
                batches.dropWhile(_ ne batch).tail. // collect properties derived in the future
                    map(batch ⇒ batch.foldLeft(Set.empty[PropertyKind])((c, n) ⇒ c ++ n.derives)).
                    reduceOption((l, r) ⇒ l ++ r).
                    getOrElse(Set.empty)
            ps.setupPhase(computedProperties, openProperties)
            batch foreach { cs ⇒
                cs.beforeSchedule(ps)
                cs.schedule(ps, initInfo(cs).asInstanceOf[cs.InitializationData])
            }
            ps.waitOnPhaseCompletion()
            batch foreach { cs ⇒
                cs.afterPhaseCompletion(ps)
            }

        }
        ps.setupPhase(Set.empty, Set.empty)
    }

    override def toString: String = {
        batches.map(_.map(_.name).mkString("{", ", ", "}")).
            mkString("Schedule(\n\t", "\n\t", "\n)")
    }

}
