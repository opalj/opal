/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package scheduling

import com.typesafe.config.Config

import org.opalj.log.LogContext

/**
 * Single Phase Scheduling (SPS) Strategy.
 * Schedules all computations in a single batch without considering dependencies.
 */
abstract class SinglePhaseScheduling extends SchedulingStrategy {

    override def schedule[A](ps: PropertyStore, allCS: Set[ComputationSpecification[A]])(implicit
        config:     Config,
        logContext: LogContext
    ): List[PhaseConfiguration[A]] = {
        // SPS schedules all computations in a single batch
        List(computePhase(ps, allCS, Set.empty))
    }
}

object SinglePhaseScheduling extends SinglePhaseScheduling
