/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info
import org.opalj.collection.immutable.Chain
import org.opalj.util.PerformanceEvaluation.time

/**
 * Encapsulates a computed schedule and enables the execution of it. Use an [[AnalysisScenario]]
 * to compute a schedule.
 *
 * @param batches The representation of the computed schedule.
 *
 * @author Michael Eichberg
 */
case class Schedule(
        batches: Chain[(PhaseConfiguration,Chain[ComputationSpecification])]
) extends ((PropertyStore, Boolean) ⇒ Unit) {

    /**
     * Schedules the computation specifications; that is, executes the underlying analysis scenario.
     *
     * @param ps The property store which should be used to execute the analyses.
     */
    def apply(ps: PropertyStore, trace: Boolean): Unit = {
        implicit val logContext : LogContext = ps.logContext

        val initInfo =
            batches.flatMap { case (_,css) ⇒
                css.toIterator.map { cs ⇒ cs -> cs.init(ps) }
            }.toMap


        batches.toIterator.zipWithIndex foreach { batchId ⇒
            val ((configuration, css), id) = batchId

            if (trace) {
                info("analysis progress", s"setting up analysis phase $id: $configuration")
            }
            time {
                ps.setupPhase(configuration)

                css.foreach ( cs ⇒                    cs.beforeSchedule(ps)                    )
                css.foreach( cs ⇒ cs.schedule(ps, initInfo(cs).asInstanceOf[cs.InitializationData]))

                ps.waitOnPhaseCompletion()

                css.foreach(cs ⇒                    cs.afterPhaseCompletion(ps) )
            } { t ⇒
                if (trace)
                    info(
                        "analysis progress",
                        s"analysis phase $id took ${t.toSeconds}"
                    )
            }
        }
        // ... we are done now!
        ps.setupPhase(Set.empty, Set.empty)
    }

    override def toString: String = {
        batches.map(_._2.map(_.name).mkString("{", ", ", "}")).
            mkString("Schedule(\n\t", "\n\t", "\n)")
    }

}

