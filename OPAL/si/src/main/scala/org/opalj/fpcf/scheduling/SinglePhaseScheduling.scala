/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package scheduling

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
