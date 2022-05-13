/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info
import org.opalj.util.PerformanceEvaluation.time

/**
 * Encapsulates a computed schedule and enables the execution of it. Primarily takes care
 * of calling the life-cycle methods of the analyses and setting up the phase appropriately.
 * You can use an [[AnalysisScenario]] to compute a schedule.
 *
 * @param batches The representation of the computed schedule.
 *
 * @author Michael Eichberg
 */
case class Schedule[A](
        batches:            List[PhaseConfiguration[A]],
        initializationData: Map[ComputationSpecification[A], Any]
) extends ((PropertyStore, Boolean, PropertyKindsConfiguration => Unit, List[ComputationSpecification[A]] => Unit) => List[(ComputationSpecification[A], A)]) {

    /**
     * Schedules the computation specifications; that is, executes the underlying analysis scenario.
     *
     * @param ps              The property store which should be used to execute the analyses.
     * @param afterPhaseSetup Called back after the phase with the given configuration was set up.
     * @param afterPhaseScheduling Called back after all analyses of a specific phase have been
     *        schedule (i.e., before calling waitOnPhaseCompletion).
     */
    def apply(
        ps:                   PropertyStore,
        trace:                Boolean                                   = false,
        afterPhaseSetup:      PropertyKindsConfiguration => Unit = _ => (),
        afterPhaseScheduling: List[ComputationSpecification[A]] => Unit = _ => ()
    ): List[(ComputationSpecification[A], A)] = {
        implicit val logContext: LogContext = ps.logContext

        var allExecutedAnalyses: List[(ComputationSpecification[A], A)] = Nil

        batches.iterator.zipWithIndex foreach { batchId =>
            val (PhaseConfiguration(configuration, css), id) = batchId

            if (trace) {
                info("analysis progress", s"setting up analysis phase $id: $configuration")
            }
            time {
                ps.setupPhase(configuration)
                afterPhaseSetup(configuration)
                assert(ps.isIdle, "the property store is not idle after phase setup")

                var executedAnalyses: List[(ComputationSpecification[A], A)] = Nil

                css.foreach(cs => cs.beforeSchedule(ps))
                css.foreach { cs =>
                    val a = cs.schedule(ps, initializationData(cs).asInstanceOf[cs.InitializationData])
                    executedAnalyses ::= ((cs, a))
                }
                executedAnalyses.foreach { csAnalysis =>
                    val (cs, a) = csAnalysis
                    cs.afterPhaseScheduling(ps, a)
                }

                afterPhaseScheduling(css)

                ps.waitOnPhaseCompletion()
                assert(ps.isIdle, "the property store is not idle after phase completion")

                executedAnalyses.foreach { csAnalysis =>
                    val (cs, a) = csAnalysis
                    cs.afterPhaseCompletion(ps, a)
                }
                assert(ps.isIdle, "the property store is not idle after phase completion")
                allExecutedAnalyses :::= executedAnalyses.reverse
            } { t =>
                if (trace)
                    info(
                        "analysis progress",
                        s"analysis phase $id took ${t.toSeconds}"
                    )
            }
        }
        // ... we are done now; the computed properties will no longer be computed!
        ps.setupPhase(Set.empty, Set.empty)

        allExecutedAnalyses
    }

    override def toString: String = {
        batches.map(_.scheduled.map(_.name).mkString("{", ", ", "}")).
            mkString("Schedule(\n\t", "\n\t", "\n)")
    }

}
