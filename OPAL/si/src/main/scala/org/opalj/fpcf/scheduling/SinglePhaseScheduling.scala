package org.opalj.fpcf.scheduling

import org.opalj.fpcf.AnalysisScenario
import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.PhaseConfiguration
import org.opalj.fpcf.PropertyStore

/**
 * Single Phase Scheduling (SPS) Strategy.
 * Schedules all computations in a single batch without considering dependencies.
 */
class SinglePhaseScheduling[A](ps: PropertyStore) extends AnalysisScenario[A](ps) with SchedulingStrategy[A] {

    override def schedule(
        ps:    PropertyStore,
        allCS: Set[ComputationSpecification[A]]
    ): List[PhaseConfiguration[A]] = {
        // SPS schedules all computations in a single batch
        List(computePhase(ps, allCS, Set.empty))
    }
}

object SinglePhaseScheduling {
    val name = "SPS"
}
