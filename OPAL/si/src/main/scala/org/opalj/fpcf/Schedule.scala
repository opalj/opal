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
        batches: Chain[BatchConfiguration]
) extends ((PropertyStore, Boolean, PhaseConfiguration ⇒ Unit, Chain[ComputationSpecification] ⇒ Unit) ⇒ Unit) {

    /**
     * Schedules the computation specifications; that is, executes the underlying analysis scenario.
     *
     * @param ps The property store which should be used to execute the analyses.
     * @param afterPhaseSetup Called back after the phase with the given configuration was set up.
     * @param afterPhaseScheduling Called back after all analyses of a specific phase have been
     *        schedule (i.e., before calling waitOnPhaseCompletion).
     */
    def apply(
        ps:                   PropertyStore,
        trace:                Boolean                                = false,
        afterPhaseSetup:      PhaseConfiguration ⇒ Unit = _ ⇒ (),
        afterPhaseScheduling: Chain[ComputationSpecification] ⇒ Unit = _ ⇒ ()
    ): Unit = {
        implicit val logContext: LogContext = ps.logContext

        val initInfo =
            batches.flatMap {
                case BatchConfiguration(_, css) ⇒
                    css.toIterator.map { cs ⇒ cs -> cs.init(ps) }
            }.toMap

        batches.toIterator.zipWithIndex foreach { batchId ⇒
            val (BatchConfiguration(configuration, css), id) = batchId

            if (trace) {
                info("analysis progress", s"setting up analysis phase $id: $configuration")
            }
            time {
                ps.setupPhase(configuration)
                afterPhaseSetup(configuration)

                css.foreach(cs ⇒ cs.beforeSchedule(ps))
                css.foreach(cs ⇒ cs.schedule(ps, initInfo(cs).asInstanceOf[cs.InitializationData]))
                afterPhaseScheduling(css)

                ps.waitOnPhaseCompletion()

                css.foreach(cs ⇒ cs.afterPhaseCompletion(ps))
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
        batches.map(_.batch.map(_.name).mkString("{", ", ", "}")).
            mkString("Schedule(\n\t", "\n\t", "\n)")
    }

}

case class BatchConfiguration(
        phaseConfiguration: PhaseConfiguration,
        batch:              Chain[ComputationSpecification]
)
